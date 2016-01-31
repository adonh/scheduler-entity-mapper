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
import com.pagerduty.mapper.annotations.impl._

private[mapper] object UntypedEntityMapping {
  val EntityAnnotationClass = classOf[EntityAnnotation]
  val SuperclassAnnotationClass = classOf[SuperclassAnnotation]
  val DiscriminatorAnnotationClass = classOf[DiscriminatorAnnotation]
  val TtlAnnotationClass = classOf[TtlAnnotation]

  val ColumnAnnotationClass = classOf[ColumnAnnotation]
  val IdAnnotationClass = classOf[IdAnnotation]
  val SerializerAnnotationClass = classOf[SerializerAnnotation]

  def apply(
    target: Class[_],
    name: Option[String],
    registeredSerializers: Map[Class[_], Any],
    customMappers: Map[Class[_ <: Annotation], Mapping => Mapping]
  ): UntypedEntityMapping = {
    val ttlOp = Option(target.getAnnotation(TtlAnnotationClass)).map(_.seconds)
    if (target.getAnnotation(SuperclassAnnotationClass) != null) {
      new InheritanceMapping(target, name, ttlOp, registeredSerializers, customMappers)
    } else {
      new ClassMapping(target, false, name, ttlOp, registeredSerializers, customMappers)
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
  protected def validateSchema(target: Class[_], serializersByColMapping: Set[(String, Any)]): Unit = {
    val serializerClassByColMapping = serializersByColMapping.map {
      case (name, ser) =>
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
