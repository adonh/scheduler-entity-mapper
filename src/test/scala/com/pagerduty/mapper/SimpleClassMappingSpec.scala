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

package test {
  class NoAnnotationEntity(@Column(name = "field") val field: String) {
    def this() = this(null)
  }

  @Entity class NoDefaultConstructor(@Column(name = "field") val field: String)

  class DeclaredSerializerNoConstructor(val param: String)
  @Entity class DeclaredSerializerNoConstructorEntity(
      @Serializer(classOf[DeclaredSerializerNoConstructor])@Column(name = "field") val field: Int
  ) {
    def this() = this(100)
  }

  @Entity class MultipleIdAnnotations(
      @Id val id0: String,
      @Id val id1: String,
      @Column(name = "field") val field: String
  ) {
    def this() = this(null, null, null)
  }

  @Entity class EmptyColName(@Column(name = "") val field: String) {
    def this() = this(null)
  }

  @Entity class IdAnnotationEntityNoCol(@Id val id: String) {
    def this() = this(null)
  }

  @Entity class NoIdAnnotationEntityNoCol()

  @Entity @Ttl(seconds = 40) class EntityWithTtl(@Column(name = "field") val field: String) {
    def this() = this(null)
  }

  @Entity class InstantiationEntity(@Column(name = "field") val field: String) {
    var usedDefaultConstructor = false
    def this() = {
      this(null)
      usedDefaultConstructor = true
    }
  }

  @Entity class IdAnnotationEntity(@Id val id: String, @Column(name = "field") val field: String) {
    def this() = this(null, null)
  }

  @Entity class NoIdAnnotationEntity(@Column(name = "field") val field: String) {
    def this() = this(null)
  }

  @Entity case class SimpleEntity( // Using case class for equals() implementation.
      @Column(name = "f0") val field0: String,
      @Column(name = "f1") val field1: String,
      val transient: String
  ) {
    def this() = this("default0", "default1", "defaultTransient")
  }

  case class DeclaredSerializer() // Using case class for equals() implementation.
  @Entity class DeclaredSerializerEntiy(
      @Column(name = "f0") val field0: Int,
      @Serializer(classOf[DeclaredSerializer])@Column(name = "f1") val field1: Double
  ) {
    def this() = this(100, 100.0)
  }
}

class SimpleClassMappingSpec extends MappingSpec {
  "Simple class mapping should" - {

    "when creating new mapping" - {
      "throw exception when target has no @Entity annotation" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[test.NoAnnotationEntity])
        }
      }
      "throw exception when target has no default constructor" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[test.NoDefaultConstructor])
        }
      }
      "throw exception when target has declared serializers without default constructor" in {
        intercept[InstantiationException] {
          mappingFor(classOf[test.DeclaredSerializerNoConstructorEntity])
        }
      }
      "throw exception when there are multiple @Id annotations" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[test.MultipleIdAnnotations])
        }
      }
      "throw exception when column name is empty" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[test.EmptyColName])
        }
      }
      "throw exception when Entity without inheritance has no columns" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[test.IdAnnotationEntityNoCol])
        }
        intercept[EntityMapperException] {
          mappingFor(classOf[test.NoIdAnnotationEntityNoCol])
        }
      }
      "detect TTL settings" in {
        val mapping = mappingFor(classOf[test.EntityWithTtl])
        mapping.ttlSeconds shouldBe Some(40)
      }
      "handle absence of TTL settings" in {
        val mapping = mappingFor(classOf[test.SimpleEntity])
        mapping.ttlSeconds shouldBe None
      }
    }

    "when instantiating entity" - {
      "use default constructor" in {
        type Entity = test.InstantiationEntity
        val mapping = mappingFor(classOf[Entity])
        val resAdapter = mock[ResultAdapter]
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(Some("value0"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.usedDefaultConstructor shouldBe true
      }
    }

    "when there is @Id annotation" - {
      val mapping = mappingFor(classOf[test.IdAnnotationEntity])

      "detect presence of id" in {
        mapping.isIdDefined shouldBe true
      }
      "correctly get entity id" in {
        val curId = targetId
        val entity = new test.IdAnnotationEntity(curId, "value0")
        mapping.getId(entity) shouldBe curId
      }
      "correctly set entity id" in {
        val entity = new test.IdAnnotationEntity(targetId, "value0")
        val newId = "id1"
        mapping.setId(entity, newId)
        entity.id shouldBe newId
      }
    }

    "when there no @Id annotation" - {
      val mapping = mappingFor(classOf[test.NoIdAnnotationEntity])

      "detect absence of id" in {
        mapping.isIdDefined shouldBe false
      }
      "throw exception when trying to get id" in {
        val entity = new test.NoIdAnnotationEntity("value0")
        intercept[NoSuchElementException] {
          mapping.getId(entity)
        }
      }
      "throw exception when trying to set id" in {
        val entity = new test.NoIdAnnotationEntity("value0")
        intercept[NoSuchElementException] {
          mapping.setId(entity, targetId)
        }
      }
    }

    "when reading entity" - {
      type Entity = test.SimpleEntity
      val mapping = mappingFor(classOf[Entity])
      val resAdapter = mock[ResultAdapter]

      "read only annotated @Column fields keeping defaults for non-annotated fields" in {
        (resAdapter.get _).expects(targetId, "f0", StringSer).returns(Some("value0"))
        (resAdapter.get _).expects(targetId, "f1", StringSer).returns(Some("value1"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.field0 shouldBe "value0"
        entity.field1 shouldBe "value1"
        entity.transient shouldBe "defaultTransient"
      }
      "keep defaults for @Column fields with null values" in {
        (resAdapter.get _).expects(targetId, "f0", StringSer).returns(Some("value0"))
        (resAdapter.get _).expects(targetId, "f1", StringSer).returns(None)
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.field1 shouldBe "default1"
      }
      "read entity with no columns as None" in {
        (resAdapter.get _).expects(targetId, "f0", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "f1", StringSer).returns(None)
        mapping.read(targetId, resAdapter) shouldBe None
      }
    }

    "when writing entity" - {
      type Entity = test.SimpleEntity
      val mapping = mappingFor(classOf[Entity])
      val mutAdapter = mock[MutationAdapter]
      val entity = new Entity("value0", "value1", "transientValue")

      "write only annotated @Column fields" in {
        (mutAdapter.insert _).expects(targetId, "f0", StringSer, "value0", None)
        (mutAdapter.insert _).expects(targetId, "f1", StringSer, "value1", None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete @Column fields with null values" in {
        val entity = new Entity("value0", null, "transientValue")
        (mutAdapter.insert _).expects(targetId, "f0", StringSer, "value0", None)
        (mutAdapter.remove _).expects(targetId, "f1")
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete all @Column fields when writing None entity value" in {
        (mutAdapter.remove _).expects(targetId, "f0")
        (mutAdapter.remove _).expects(targetId, "f1")
        mapping.write(targetId, None, mutAdapter, None)
      }
      "correctly pass TTL value" in {
        val ttl = Some(10)
        (mutAdapter.insert _).expects(targetId, "f0", StringSer, "value0", ttl)
        (mutAdapter.insert _).expects(targetId, "f1", StringSer, "value1", ttl)
        mapping.write(targetId, Some(entity), mutAdapter, ttl)
      }
    }

    "when dealing with custom serializers" - {
      object IntSer
      val declaredSer = new test.DeclaredSerializer
      type Entity = test.DeclaredSerializerEntiy
      val mapping = EntityMapping[String, Entity](
        classOf[Entity], Map(classOf[Int] -> IntSer), Map.empty
      )

      "correctly pass serializers when reading entity" in {
        val resAdapter = mock[ResultAdapter]
        (resAdapter.get _).expects(targetId, "f0", IntSer).returns(None)
        (resAdapter.get _).expects(targetId, "f1", declaredSer).returns(None)
        mapping.read(targetId, resAdapter)
      }
      "correctly pass serializers when wiring entity" in {
        val mutAdapter = mock[MutationAdapter]
        val entity = new Entity(20, 20.0)
        (mutAdapter.insert _).expects(targetId, "f0", IntSer, 20, None)
        (mutAdapter.insert _).expects(targetId, "f1", declaredSer, 20.0, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
    }
  }
}
