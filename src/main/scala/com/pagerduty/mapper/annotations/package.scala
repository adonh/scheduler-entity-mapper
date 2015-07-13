package com.pagerduty.mapper

import com.pagerduty.mapper.annotations.impl._
import scala.annotation.meta.field


package object annotations {

  /**
   * Used to annotate persistable classes.
   * {{{
   * @Entity
   * case class MyClass(...)
   * }}}
   */
  type Entity = EntityAnnotation

  /**
   * Used to annotate the root of persistable class tree. All the persistable subclasses must
   * be defined in the annotation.
   * {{{
   * @Entity
   * @Superclass(Array(classOf[SubClass1], classOf[SubClass2]))
   * case class SuperClass(...)
   * }}}
   */
  type Superclass = SuperclassAnnotation

  /**
   * Used to define discriminator value for persistable classes that are part of inheritance
   * mapping.
   * {{{
   * @Entity
   * @Discriminator("subclass1")
   * case class SubClass1(...)
   * }}}
   */
  type Discriminator = DiscriminatorAnnotation

  /**
   * Used to define TTL in seconds. When combined with inheritance mapping, and both superclass and
   * a subclass specify TTL, the most specific TTL (the subclass) will take precedence.
   *
   * If a class has fields that themselves are annotate as @Entity and specify a @Ttl, those other
   * TTL values are ignored. All the fields in a class will have the same TTL value as specified
   * with Entity that maps to a column family. If the outermost Entity does not specify a TTL,
   * then none of the fields will have TTL set.
   *
   * {{{
   * @Entity
   * @Ttl(seconds=2*3600)
   * case class MyClass(...)
   * }}}
   */
  type Ttl = TtlAnnotation

  /**
   * Used to annotate id field in an @Entity.
   */
  type Id = IdAnnotation @field

  /**
   * Used to annotate field in an @Entity.
   */
  type Column = ColumnAnnotation @field

  /**
   * Used on fields in conjunction with @Column to indicate special serializer.
   */
  type Serializer = SerializerAnnotation @field
}
