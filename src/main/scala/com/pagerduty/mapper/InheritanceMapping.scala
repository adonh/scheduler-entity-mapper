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
import org.slf4j.LoggerFactory


/**
 * Maps single table inheritance (STI) using discriminator column.
 *
 * The target class must be annotated as @Superclass(subclasses). Only provided subclasses can
 * be used with the inheritance mapping. Each subclass must have @Discriminator annotation.
 * @Superclass annotation will be ignore for all the subclasses, so it is possible to have
 * multiple inheritance mappings that service different inheritance subtrees.
 */
private[mapper] class InheritanceMapping(
    val target: Class[_],
    val name: Option[String],
    val ttlSeconds: Option[Int],
    val registeredSerializers: Map[Class[_], Any],
    val customMappers: Map[Class[_ <: Annotation], Mapping => Mapping])
  extends UntypedEntityMapping
{
  import UntypedEntityMapping._
  import InheritanceMapping.log

  /**
   * ClassMapping for each subclass.
   */
  protected val mappingByClassName: Map[String, (String, ClassMapping)] = {
    def classMapping(subclass: Class[_]): (String, ClassMapping) = {
      val discriminatorAnnotation = subclass.getAnnotation(DiscriminatorAnnotationClass)
      if (discriminatorAnnotation == null) {
        throw new EntityMapperException(s"Class ${subclass.getName} " +
          s"must have @Discriminator annotation, because it is a part of ${target.getName} " +
          "inheritance mapping.")
      }
      val discriminator = discriminatorAnnotation.value
      val ttlOp = Option(target.getAnnotation(TtlAnnotationClass)).map(_.seconds)
      val classMapping = new ClassMapping(
        subclass, true, name, ttlOp, registeredSerializers, customMappers)
      (discriminator, classMapping)
    }

    target.getAnnotation(SuperclassAnnotationClass).subclasses.map { subclass =>
      subclass.getName -> classMapping(subclass)
    }.toMap
  }
  protected val mappingByDiscriminator: Map[String, ClassMapping] = {
    val classesByDiscriminator = mappingByClassName.toSeq
      .groupBy { case (_, (disciminator, _)) => disciminator }
      .mapValues(_.map { case (clazz, _) => clazz })

    classesByDiscriminator.find { case (discriminator, classes) => classes.size > 1 } match {
      case Some((discriminator, classes)) =>
        throw new EntityMapperException(s"Classes ${classes.mkString(", ")} " +
          s"have the same @Discriminator value '$discriminator'.")
      case None =>
        // ignore
    }

    mappingByClassName.map { case (_, (disciminator, mapping)) => disciminator -> mapping }
  }
  protected val allMappings: Seq[ClassMapping] = mappingByDiscriminator.values.toSeq

  protected def mappingFor(clazz: Class[_]): (String, ClassMapping) = {
    val className = if (clazz.getName.endsWith("$")) clazz.getSuperclass.getName else clazz.getName
    mappingByClassName.getOrElse(className, throw new EntityMapperException(
      s"Class ${clazz.getName} is not part of the ${target.getName} inheritance mapping."))
  }

  protected val discriminatorMapping: FieldMapping = {
    val discriminatorColumn = target.getAnnotation(SuperclassAnnotationClass).discriminatorColumn
    val stringSerializer = registeredSerializers(classOf[String])
    new FieldMapping(stringSerializer, prefixed(discriminatorColumn))
  }


  val isIdDefined: Boolean = allMappings.forall(_.isIdDefined)
  def getId(entity: Any): Any = mappingFor(entity.getClass)._2.getId(entity)
  def setId(entity: Any, id: Any): Unit = mappingFor(entity.getClass)._2.setId(entity, id)

  val serializersByColName: Seq[(String, Any)] = {
    val schema = allMappings.flatMap(_.serializersByColName).toSet
    val reserved = schema.find { case (colName, _) => colName == discriminatorMapping.name }
    if (reserved.isDefined) {
      throw new EntityMapperException(s"Column name '${reserved.get._1}' " +
        s"is reserved as discriminator column name in ${target.getName} inheritance mapping.")
    }
    validateSchema(target, schema)
    schema.toSeq ++ discriminatorMapping.serializersByColName
  }

  def write(
      targetId: Any, entity: Option[Any], mutation: MutationAdapter, ttlSeconds: Option[Int])
  : Unit = {
    if (entity.isDefined) {
      val (discriminator, mapping) = mappingFor(entity.get.getClass)
      // Remove all the columns from other mappings.
      allMappings.filterNot(_ == mapping).foreach {
        _.write(targetId, None, mutation, mapping.fieldNames, None)
      }
      // Write discriminator.
      discriminatorMapping.write(targetId, Some(discriminator), mutation, ttlSeconds)
      // Write mapping.
      mapping.write(targetId, entity, mutation, ttlSeconds)
    }
    else {
      discriminatorMapping.write(targetId, None, mutation, None)
      allMappings.foreach(_.write(targetId, None, mutation, None))
    }
  }

  def read(targetId: Any, result: ResultAdapter): MappedResult = {
    discriminatorMapping.read(targetId, result).flatMap { case discriminator: String =>
      mappingByDiscriminator.get(discriminator).map(_.readDefined(targetId, result)).getOrElse {
        log.error(s"Not found mapping for discriminator '$discriminator' " +
          s"for ${target.getName} inheritance mapping for entity " +
          s"'$targetId'.")
        Undefined
      }
    }
  }

  override def toString(): String = s"InheritanceMapping(${target.getName}, $name)"
}


private[mapper] object InheritanceMapping {
  val log = LoggerFactory.getLogger(classOf[InheritanceMapping])
}
