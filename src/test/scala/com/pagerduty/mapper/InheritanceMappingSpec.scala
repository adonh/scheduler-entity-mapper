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

import com.pagerduty.mapper.annotations._

package testsuper {
  @Entity @Superclass(subclasses = Array(classOf[NoAnnotationEntity]))
  class SuperOfNoAnnotationEntity()

  @Discriminator("disc") class NoAnnotationEntity(
    @Column(name = "field") val field: String
  )
      extends SuperOfNoAnnotationEntity {
    def this() = this(null)
  }

  @Entity @Superclass(subclasses = Array(classOf[NoDefaultConstructor]))
  class SuperOfNoDefaultConstructorEntity()

  @Entity @Discriminator("disc") class NoDefaultConstructor(
    @Column(name = "field") val field: String
  )
      extends SuperOfNoDefaultConstructorEntity

  @Entity @Superclass(subclasses = Array(classOf[DeclaredSerializerNoConstructorEntity]))
  class SuperOfDeclaredSerializerNoConstructorEntity()

  class DeclaredSerializerNoConstructor(val param: String)
  @Entity @Discriminator("disc") class DeclaredSerializerNoConstructorEntity(
      @Serializer(classOf[DeclaredSerializerNoConstructor])@Column(name = "field") val field: Int
  ) {
    def this() = this(100)
  }

  @Entity @Superclass(subclasses = Array(classOf[NoDiscriminatorEntity]))
  class SuperOfNoDiscriminatorEntity()

  @Entity class NoDiscriminatorEntity(
    @Column(name = "field") val field: String
  )
      extends SuperOfNoDiscriminatorEntity {
    def this() = this(null)
  }

  @Superclass(
    discriminatorColumn = "collision",
    subclasses = Array(classOf[ColNameCollision])
  )
  @Entity
  class SuperOfColNameCollision()

  @Entity @Discriminator("disc") class ColNameCollision(
    @Column(name = "collision") val field: String
  )
      extends SuperOfColNameCollision {
    def this() = this(null)
  }

  @Entity @Superclass(subclasses = Array(classOf[EntityWithTtl1])) @Ttl(seconds = 202)
  class SuperWithTtl

  @Entity @Discriminator("disc") @Ttl(seconds = 50) class EntityWithTtl1() extends SuperWithTtl

  @Entity @Superclass(subclasses = Array(classOf[EntityWithTtl2]))
  class SuperWithoutTtl

  @Entity @Discriminator("disc") @Ttl(seconds = 60) class EntityWithTtl2() extends SuperWithoutTtl

  @Superclass(subclasses = Array(classOf[DiscCollisionEntity1], classOf[DiscCollisionEntity2]))
  @Entity class SuperOfDiscCollisionEntities

  @Entity @Discriminator("disc0") class DiscCollisionEntity1() extends SuperOfDiscCollisionEntities
  @Entity @Discriminator("disc0") class DiscCollisionEntity2() extends SuperOfDiscCollisionEntities

  @Entity
  @Superclass(subclasses = Array(
    classOf[DistinctSerializerEntity1], classOf[DistinctSerializerEntity2]
  ))
  class DistinctSerializerInheritance

  case class DistinctSerializer1() // Using case class for equals() implementation.
  case class DistinctSerializer2() // Using case class for equals() implementation.

  @Entity @Discriminator("disc1")
  class DistinctSerializerEntity1(
    @Serializer(classOf[DistinctSerializer1])@Column(name = "field") val filed: Int
  )
      extends DistinctSerializerInheritance {
    def this() = this(100)
  }

  @Entity @Discriminator("disc2")
  class DistinctSerializerEntity2(
    @Serializer(classOf[DistinctSerializer2])@Column(name = "field") val filed: Int
  )
      extends DistinctSerializerInheritance {
    def this() = this(100)
  }

  @Entity
  @Superclass(subclasses = Array(
    classOf[SameSerializerEntity1], classOf[SameSerializerEntity2]
  ))
  class SameSerializerInheritance

  class SameSerializer() // equals() must not be implemented.

  @Entity @Discriminator("disc1")
  class SameSerializerEntity1(
    @Serializer(classOf[SameSerializer])@Column(name = "field") val filed: Int
  )
      extends SameSerializerInheritance {
    def this() = this(100)
  }

  @Entity @Discriminator("disc2")
  class SameSerializerEntity2(
    @Serializer(classOf[SameSerializer])@Column(name = "field") val filed: Int
  )
      extends SameSerializerInheritance {
    def this() = this(100)
  }

  @Entity @Superclass(subclasses = Array(classOf[InstantiationEntity]))
  class SuperOfInstantiationEntity(@Column(name = "superField") val superField: String) {
    var superUsedDefaultConstructor = false
    def this() = {
      this(null)
      superUsedDefaultConstructor = true
    }
  }

  @Entity @Discriminator("disc") class InstantiationEntity(
    @Column(name = "field") val field: String
  )
      extends SuperOfInstantiationEntity {
    var usedDefaultConstructor = false
    def this() = {
      this(null)
      usedDefaultConstructor = true
    }
  }

  @Entity @Superclass(subclasses = Array(classOf[IdAnnotationEntity]))
  class SuperWithId(@Id val id: String)

  @Entity @Discriminator("disc")
  class IdAnnotationEntity(id: String, @Id @Column(name = "field") val field: String)
      extends SuperWithId(id) {
    def this() = this(null, null)
  }

  @Entity @Superclass(subclasses = Array(classOf[InheritanceWithId1]))
  class InheritanceWithId

  @Entity @Discriminator("disc")
  class InheritanceWithId1(@Id val id: String, @Column(name = "field") val field: String)
      extends InheritanceWithId {
    def this() = this(null, null)
  }

  @Entity
  @Superclass(subclasses = Array(classOf[InheritanceWithoutId1], classOf[InheritanceWithoutId2]))
  class InheritanceWithoutId

  @Entity @Discriminator("disc1")
  class InheritanceWithoutId1(@Id val id: String, @Column(name = "field") val field: String)
      extends InheritanceWithoutId {
    def this() = this(null, null)
  }

  @Entity @Discriminator("disc2")
  class InheritanceWithoutId2(@Column(name = "field") val field: String)
      extends InheritanceWithoutId {
    def this() = this(null)
  }

  @Entity
  @Superclass(subclasses = Array(classOf[SimpleEntity1], classOf[SimpleEntity2]))
  class SimpleInheritance(
      @Column(name = "commonField") val commonField: String,
      val commonTransient: String
  ) {
    def this() = this("defaultCommonField", "defaultCommonTransient")
  }

  @Entity @Discriminator("disc1")
  class SimpleEntity1(
    commonField: String,
    commonTransient: String,
    @Column(name = "field") val field: String,
    val transient: String
  )
      extends SimpleInheritance(commonField, commonTransient) {
    def this() = {
      this("defaultCommonField", "defaultCommonTransient", "defaultField", "defaultTransient")
    }
  }
  @Entity @Discriminator("disc2")
  class SimpleEntity2( // Using case class for equals() implementation.
    @Column(name = "anotherField") val anotherField: String
  )
      extends SimpleInheritance {
    def this() = this("defaultAnotherField")
  }

  // Note: this class is not part of SimpleInheritance mapping declaration.
  @Entity @Discriminator("disc3")
  class SimpleEntity3(
    @Column(name = "anotherField") val anotherField: String
  )
      extends SimpleInheritance {
    def this() = this("defaultAnotherField")
  }
}

class InheritanceMappingSpec extends MappingSpec {
  "Inheritance mapping should" - {

    "when creating new mapping" - {
      "throw exception when one of subclasses has no @Entity annotation" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testsuper.SuperOfNoAnnotationEntity])
        }
      }
      "throw exception when one of subclasses has no default constructor" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testsuper.SuperOfNoDefaultConstructorEntity])
        }
      }
      "throw exception when a subclass has a declared serializer without default constructor" in {
        intercept[InstantiationException] {
          mappingFor(classOf[test.DeclaredSerializerNoConstructorEntity])
        }
      }
      "throw exception when one of subclasses has no @Discriminator annotation" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testsuper.SuperOfNoDiscriminatorEntity])
        }
      }
      "throw exception on @Discriminator value collisions" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testsuper.SuperOfDiscCollisionEntities])
        }
      }
      "throw exception when there are multiple @Id annotations" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testsuper.SuperWithId])
        }
      }
      "throw exception when there are conflicting field declarations" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testsuper.DistinctSerializerInheritance])
        }
      }
      "throw exception when a field declaration conflicts with discriminator column" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testsuper.SuperOfColNameCollision])
        }
      }
      "ignore subclass TTL settings when superclass has no TTL" in {
        val mapping = mappingFor(classOf[testsuper.SuperWithoutTtl])
        mapping.ttlSeconds shouldBe None
      }
      "ignore subclass TTL settings when superclass has TTL" in {
        val mapping = mappingFor(classOf[testsuper.SuperWithTtl])
        mapping.ttlSeconds shouldBe Some(202)
      }
      "handle multiple instances of the same serializer" in {
        mappingFor(classOf[testsuper.SameSerializerInheritance])
      }
    }

    "when instantiating entity" - {
      "use default constructor" in {
        type Entity = testsuper.SuperOfInstantiationEntity
        type SubEntity = testsuper.InstantiationEntity
        val mapping = mappingFor(classOf[Entity])
        val resAdapter = mock[ResultAdapter]
        (resAdapter.get _).expects(targetId, "discriminator", StringSer).returns(Some("disc"))
        (resAdapter.get _).expects(targetId, "superField", StringSer).returns(Some("superValue"))
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(Some("value"))
        val Some(entity: SubEntity) = mapping.read(targetId, resAdapter)
        entity.superUsedDefaultConstructor shouldBe true
        entity.usedDefaultConstructor shouldBe true
      }
    }

    "when there is @Id annotation on all subclasses" - {
      val mapping = mappingFor(classOf[testsuper.InheritanceWithId])

      "detect presence of id" in {
        mapping.isIdDefined shouldBe true
      }
      "correctly get entity id" in {
        val curId = targetId
        val entity = new testsuper.InheritanceWithId1(curId, "value0")
        mapping.getId(entity) shouldBe curId
      }
      "correctly set entity id" in {
        val entity = new testsuper.InheritanceWithId1(targetId, "value0")
        val newId = "id1"
        mapping.setId(entity, newId)
        entity.id shouldBe newId
      }
    }

    "when there no @Id annotation on some subclasses" - {
      val mapping = mappingFor(classOf[testsuper.InheritanceWithoutId])

      "detect absence of id" in {
        mapping.isIdDefined shouldBe false
      }
      "correctly get entity id when possible" in {
        val curId = targetId
        val entity = new testsuper.InheritanceWithoutId1(curId, "value0")
        mapping.getId(entity) shouldBe curId
      }
      "correctly set entity id when possible" in {
        val entity = new testsuper.InheritanceWithoutId1(targetId, "value0")
        val newId = "id1"
        mapping.setId(entity, newId)
        entity.id shouldBe newId
      }
      "throw exception when trying to get missing id" in {
        val entity = new testsuper.InheritanceWithoutId2("value0")
        intercept[NoSuchElementException] {
          mapping.getId(entity)
        }
      }
      "throw exception when trying to set missing id" in {
        val entity = new testsuper.InheritanceWithoutId2("value0")
        intercept[NoSuchElementException] {
          mapping.setId(entity, targetId)
        }
      }
    }

    "when reading entity" - {
      type Entity = testsuper.SimpleInheritance
      val mapping = mappingFor(classOf[Entity])
      val resAdapter = mock[ResultAdapter]

      "resolve class mapping from discriminator" in {
        (resAdapter.get _).expects(targetId, "discriminator", StringSer).returns(Some("disc2"))
        (resAdapter.get _).expects(targetId, "commonField", StringSer).returns(Some("value0"))
        (resAdapter.get _).expects(targetId, "anotherField", StringSer).returns(Some("value1"))
        val Some(entity) = mapping.read(targetId, resAdapter)
        entity shouldBe a[testsuper.SimpleEntity2]
      }
      "read only annotated @Column fields keeping defaults for non-annotated fields" in {
        (resAdapter.get _).expects(targetId, "discriminator", StringSer).returns(Some("disc1"))
        (resAdapter.get _).expects(targetId, "commonField", StringSer).returns(Some("value0"))
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(Some("value1"))
        val Some(entity: testsuper.SimpleEntity1) = mapping.read(targetId, resAdapter)
        entity.commonField shouldBe "value0"
        entity.commonTransient shouldBe "defaultCommonTransient"
        entity.field shouldBe "value1"
        entity.transient shouldBe "defaultTransient"
      }
      "keep defaults for @Column fields with null values" in {
        (resAdapter.get _).expects(targetId, "discriminator", StringSer).returns(Some("disc1"))
        (resAdapter.get _).expects(targetId, "commonField", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(None)
        val Some(entity: testsuper.SimpleEntity1) = mapping.read(targetId, resAdapter)
        entity.commonField shouldBe "defaultCommonField"
        entity.field shouldBe "defaultField"
      }
      "read as defined when only discriminator is present" in {
        (resAdapter.get _).expects(targetId, "discriminator", StringSer).returns(Some("disc2"))
        (resAdapter.get _).stubs(targetId, *, *).returns(None)
        val Some(entity: testsuper.SimpleEntity2) = mapping.read(targetId, resAdapter)
        entity should not be (null)
      }
      "read entity with no discriminator as None" in {
        (resAdapter.get _).expects(targetId, "discriminator", StringSer).returns(None)
        mapping.read(targetId, resAdapter) shouldBe None
      }
      "read entities with unknown discriminator as None" in {
        (resAdapter.get _).expects(targetId, "discriminator", StringSer).returns(Some("unknown"))
        mapping.read(targetId, resAdapter) shouldBe None
      }
    }

    "when writing entity" - {
      type Entity = testsuper.SimpleInheritance
      val mapping = mappingFor(classOf[Entity])
      val mutAdapter = mock[MutationAdapter]
      val entity = new testsuper.SimpleEntity1(
        "commonValue", "commonTransientValue", "value", "transientValue"
      )

      "throw exception when writing class which is not part of InheritanceMapping" in {
        intercept[EntityMapperException] {
          mapping.write(targetId, Some(new testsuper.SimpleEntity3()), mutAdapter, None)
        }
      }
      "write only annotated @Column fields and discriminator" in {
        (mutAdapter.insert _).expects(targetId, "discriminator", StringSer, "disc1", None)
        (mutAdapter.insert _).expects(targetId, "commonField", StringSer, "commonValue", None)
        (mutAdapter.insert _).expects(targetId, "field", StringSer, "value", None)
        (mutAdapter.remove _).stubs(*, *)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete @Column fields belonging to other subclass mappings" in {
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        (mutAdapter.remove _).expects(targetId, "anotherField")
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete @Column fields with null values" in {
        val entity = new testsuper.SimpleEntity1(
          null, "commonTransientValue", null, "transientValue"
        )
        (mutAdapter.insert _).expects(targetId, "discriminator", StringSer, "disc1", None)
        (mutAdapter.remove _).expects(targetId, "commonField")
        (mutAdapter.remove _).expects(targetId, "field")
        (mutAdapter.remove _).expects(targetId, "anotherField")
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete @Column fields for all subclass mappings when writing None entity value" in {
        (mutAdapter.remove _).expects(targetId, "discriminator")
        (mutAdapter.remove _).expects(targetId, "commonField").atLeastOnce()
        (mutAdapter.remove _).expects(targetId, "field")
        (mutAdapter.remove _).expects(targetId, "anotherField")
        mapping.write(targetId, None, mutAdapter, None)
      }
      "correctly pass TTL value" in {
        val ttl = Some(10)
        (mutAdapter.insert _).expects(targetId, "discriminator", StringSer, "disc1", ttl)
        (mutAdapter.insert _).expects(targetId, "commonField", StringSer, "commonValue", ttl)
        (mutAdapter.insert _).expects(targetId, "field", StringSer, "value", ttl)
        (mutAdapter.remove _).stubs(*, *)
        mapping.write(targetId, Some(entity), mutAdapter, ttl)
      }
    }
  }
}
