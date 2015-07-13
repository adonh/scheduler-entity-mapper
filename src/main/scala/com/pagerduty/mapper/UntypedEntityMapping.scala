package com.pagerduty.mapper

import java.lang.annotation.Annotation
import com.pagerduty.mapper.annotations.impl._

private[mapper] object UntypedEntityMapping {
  val entityAnnotationClass = classOf[EntityAnnotation]
  val superclassAnnotationClass = classOf[SuperclassAnnotation]
  val discriminatorAnnotationClass = classOf[DiscriminatorAnnotation]
  val ttlAnnotationClass = classOf[TtlAnnotation]

  val columnAnnotationClass = classOf[ColumnAnnotation]
  val idAnnotationClass = classOf[IdAnnotation]
  val serializerAnnotationClass = classOf[SerializerAnnotation]

  def apply(
      target: Class[_],
      name: Option[String],
      registeredSerializers: Map[Class[_], Any],
      customMappers: Map[Class[_ <: Annotation], Mapping => Mapping])
  : UntypedEntityMapping = {
    val ttlOp = Option(target.getAnnotation(ttlAnnotationClass)).map(_.seconds)
    if (target.getAnnotation(superclassAnnotationClass) != null) {
      new InheritanceMapping(target, name, ttlOp, registeredSerializers, customMappers)
    }
    else {
      new ClassMapping(target, name, ttlOp, registeredSerializers, customMappers)
    }
  }
}


/**
 * Common interface that captures entity mapping.
 *
 * The target class must be annotated as @Entity. Target fields must be annotated as @Column.
 * Custom @Serializer annotation may be required for non-standard types.
 */
private[mapper] trait UntypedEntityMapping extends Mapping {

  /**
   * Defines a class managed by this mapping.
   */
  def target: Class[_]

  /**
   * Defines an optional name for this mapping.
   */
  def name: Option[String]

  /**
   * Defines an optional ttl for this mapping.
   */
  def ttlSeconds: Option[Int]

  /**
   * Allows to check if a give entity mapping manages ids.
   */
  def isIdDefined: Boolean

  /**
   * Uses reflection to get id of a give entity.
   */
  def getId(entity: Any): Any

  /**
   * Uses reflection to set an id for a given entity.
   */
  def setId(entity: Any, id: Any): Unit


  protected def prefixed(fieldName: String): String = name match {
    case Some(prefix) => s"$prefix.$fieldName"
    case None => fieldName
  }

  /**
   * Validates schema against conflicting entries with the same key, throwing an exception when
   * a conflict is found.
   */
  protected def validateSchema(target: Class[_], serializersByColMapping: Set[(String, Any)])
  : Unit = {
    val serializerClassByColMapping = serializersByColMapping.map { case (name, ser) =>
      name -> ser.getClass
    }
    val dups = serializerClassByColMapping
      .groupBy { case (colName, _) => colName }
      .filter { case (colName, collisions) => collisions.size > 1 }
      .keySet

    if (!dups.isEmpty) {
      throw new EntityMapperException(s"Conflicting column declarations for ${target.getName} " +
        s"entity mapping for the following column names: ${dups.mkString(", ")}.")
    }
  }
}
