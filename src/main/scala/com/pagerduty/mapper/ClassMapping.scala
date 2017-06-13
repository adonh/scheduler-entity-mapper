/*
 * Copyright (c) 2015, PagerDuty
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.pagerduty.mapper

import java.lang.annotation.Annotation
import java.lang.reflect.{Field, ParameterizedType, Type}

/**
  * Manages a single class.
  * Allows to read objects from row results and write objects to mutation batch.
  */
private[mapper] class ClassMapping(
    val target: Class[_],
    val allowEmptyMapping: Boolean,
    val name: Option[String],
    val ttlSeconds: Option[Int],
    val registeredSerializers: Map[Class[_], Any],
    val customMappers: Map[Class[_ <: Annotation], Mapping => Mapping])
    extends UntypedEntityMapping {
  import ClassMapping.WrapperType
  import UntypedEntityMapping._

  private def checkEntityAnnotation(target: Class[_]): Unit = {
    if (target.getAnnotation(EntityAnnotationClass) == null) {
      val msg = name match {
        case Some(fieldName) =>
          s"Unable to resolve serializer for field '$fieldName'. " +
            s"Consider annotating class ${target.getName} as @Entity."
        case None =>
          s"Class ${target.getName} must be annotated as @Entity."
      }
      throw new EntityMapperException(msg)
    }
  }
  private def checkNoArgConstructor(target: Class[_]): Unit = {
    val noArgsConstructor = target.getConstructors.find(_.getParameterTypes.size == 0)
    if (!noArgsConstructor.isDefined) {
      throw new EntityMapperException(
        s"Entity class ${target.getName} must provide a no-argument constructor."
      )
    }
  }
  checkEntityAnnotation(target)
  checkNoArgConstructor(target)

  protected def mappingForField(colName: String, field: Field): (Field, Mapping) = {
    def classFor(tpe: Type): Class[_] = tpe match {
      case c: Class[_] => c
      case p: ParameterizedType => p.getRawType.asInstanceOf[Class[_]]
    }
    def detectWrapperAndType(field: Field): (WrapperType, Class[_]) = {
      field.getGenericType match {
        case p: ParameterizedType if p.getRawType == classOf[Option[_]] =>
          (WrapperType.Option, classFor(p.getActualTypeArguments.head))
        case t: Type =>
          (WrapperType.Bare, classFor(t))
      }
    }
    def detectSerializer(unwrappedType: Class[_], filed: Field): Option[Any] = {
      Option(field.getAnnotation(SerializerAnnotationClass)) match {
        case Some(serializerAnnotation) =>
          Some(serializerAnnotation.value.newInstance)
        case None if registeredSerializers.contains(unwrappedType) =>
          Some(registeredSerializers(unwrappedType))
        case _ =>
          None
      }
    }
    def checkDetectedSerialization(unwrappedType: Class[_], serializerOp: Option[Any]) = {
      if (unwrappedType == classOf[Object] && !serializerOp.isDefined) {
        throw new EntityMapperException(
          s"Cannot infer serializer for ${target.getName}.${field.getName}, " +
            "please provide explicit @Serializer annotation."
        )
      }
    }
    def resolveUnwrappedMapping(unwrappedType: Class[_], serializerOp: Option[Any]) = {
      serializerOp match {
        case Some(serializer) => new FieldMapping(serializer, prefixed(colName))
        case None =>
          try {
            UntypedEntityMapping(
              unwrappedType,
              Some(prefixed(colName)),
              registeredSerializers,
              customMappers
            )
          } catch {
            case e: EntityMapperException =>
              throw new EntityMapperException(
                s"Cannot create mapping for ${target.getName}: ${e.getMessage}",
                e
              )
          }
      }
    }

    val (wrapper, unwrappedType) = detectWrapperAndType(field)
    val serializer = detectSerializer(unwrappedType, field)
    checkDetectedSerialization(unwrappedType, serializer)
    val unwrappedMapping = resolveUnwrappedMapping(unwrappedType, serializer)

    val mapping = wrapper match {
      case WrapperType.Option => new OptionMapping(unwrappedMapping)
      case WrapperType.Bare => unwrappedMapping
    }

    val customMappings = customMappers.filter {
      case (annotation, _) => Option(field.getAnnotation(annotation)).isDefined
    }.values
    val combinedMapping = customMappings.foldLeft(mapping)((mapping, builder) => builder(mapping))

    field -> combinedMapping
  }

  protected val fieldMapping: Seq[(Field, Mapping)] = {
    def colNameFor(field: Field): Option[String] = {
      Option(field.getAnnotation(ColumnAnnotationClass)).map { columnAnnotation =>
        if (columnAnnotation.name == null || columnAnnotation.name.trim().isEmpty) {
          throw new EntityMapperException(
            s"@Column annotation on field '${field.getName}' " +
              s"'in class ${target.getName} must have a non-empty name.")
        }
        field.setAccessible(true)
        columnAnnotation.name
      }
    }
    val fieldsByColName = allDeclaredFields(target).flatMap { field =>
      colNameFor(field).map(_ -> field)
    }

    for ((colName, field) <- fieldsByColName) yield {
      mappingForField(colName, field)
    }
  }
  private def checkEntityMappingNonEmpty(target: Class[_], fieldMapping: Seq[(Field, Mapping)]): Unit = {
    if (!allowEmptyMapping && fieldMapping.isEmpty) {
      throw new EntityMapperException(
        s"The class ${target.getName} must have at least one " +
          "@Column annotated field, or it must belong to an inheritance mapping."
      )
    }
  }
  checkEntityMappingNonEmpty(target, fieldMapping)

  val fieldNames: Set[String] = {
    fieldMapping.map { case (field, _) => field.getName }.toSet
  }

  protected def allDeclaredFields(clazz: Class[_]): Seq[Field] = {
    val superFields = Option(clazz.getSuperclass).map(allDeclaredFields).getOrElse(Seq.empty)
    superFields ++ clazz.getDeclaredFields
  }

  protected def findIdField(fields: Seq[Field]): Option[Field] = {
    val ids = fields.filter { field =>
      if (Option(field.getAnnotation(UntypedEntityMapping.IdAnnotationClass)).isDefined) {
        field.setAccessible(true)
        true
      } else false
    }
    if (ids.size > 1) {
      throw new EntityMapperException(
        s"Founds multiple @Id annotated fields in class ${target.getName}."
      )
    }
    ids.headOption
  }

  protected val idField: Option[Field] = findIdField(allDeclaredFields(target))
  def isIdDefined: Boolean = idField.isDefined
  def getId(entity: Any) = idField.get.get(entity)
  def setId(entity: Any, id: Any) = idField.get.set(entity, id)

  def serializersByColName = fieldMapping.flatMap {
    case (field, mapping) =>
      mapping.serializersByColName
  }
  validateSchema(target, serializersByColName.toSet)

  def write(targetId: Any, entity: Option[Any], mutation: MutationAdapter, ttlSeconds: Option[Int]): Unit = {
    write(targetId, entity, mutation, Set.empty, ttlSeconds)
  }

  def write(
      targetId: Any,
      entity: Option[Any],
      mutation: MutationAdapter,
      exclusion: Set[String], // Required for NoneAsNoop to function correctly.
      ttlSeconds: Option[Int]
    ): Unit = {
    for ((field, mapping) <- fieldMapping if !exclusion.contains(field.getName)) {
      val value = if (entity.isDefined) Option(field.get(entity.get)) else None
      mapping.write(targetId, value, mutation, ttlSeconds)
    }
  }

  def read(targetId: Any, result: ResultAdapter): MappedResult = {
    val entity = target.newInstance()
    val isDefined = readInto(targetId, entity, result)
    if (isDefined) new MappedValue(true, entity) else Undefined
  }

  def readDefined(targetId: Any, result: ResultAdapter): MappedResult = {
    val entity = target.newInstance()
    readInto(targetId, entity, result)
    new MappedValue(true, entity)
  }

  protected def readInto(targetId: Any, entity: Any, result: ResultAdapter): Boolean = {
    var defined = false
    for ((field, mapping) <- fieldMapping) {
      mapping.read(targetId, result) match {
        case MappedValue(hasColumns, fieldValue) =>
          field.set(entity, fieldValue)
          defined = defined | hasColumns
        case Undefined =>
        // ignore
      }
    }
    defined
  }

  override def toString(): String = s"ClassMapping(${target.getName}, $name)"
}

private[mapper] object ClassMapping {
  private object WrapperType extends Enumeration {
    val Bare, Option = Value
  }
  private type WrapperType = WrapperType.Value
}
