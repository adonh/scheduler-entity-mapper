package com.pagerduty.mapper

import com.pagerduty.mapper.annotations._


package test {
  class NoAnnotationEntity(@Column(name = "field") val field: String) {
    def this() = this(null)
  }

  @Entity class NoDefaultConstructor(@Column(name = "field") val field: String)

  class DeclaredSerializerNoConstructor(val param: String)
  @Entity class DeclaredSerializerNoConstructorEntity(
      @Serializer(classOf[DeclaredSerializerNoConstructor]) @Column(name = "field")
      val field: Int) {
    def this() = this(100)
  }

  @Entity class MultipleIdAnnotations(
      @Id val id0: String,
      @Id val id1: String,
      @Column(name = "field") val field: String) {
    def this() = this(null, null, null)
  }

  @Entity class EmptyColName(@Column(name = "") val field: String) {
    def this() = this(null)
  }

  @Entity @Ttl(seconds = 40) class EntityWithTtl()

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
      val transient: String) {
    def this() = this("default0", "default1", "defaultTransient")
  }

  case class DeclaredSerializer() // Using case class for equals() implementation.
  @Entity class DeclaredSerializerEntiy(
      @Column(name = "f0") val field0: Int,
      @Serializer(classOf[DeclaredSerializer]) @Column(name = "f1") val field1: Double) {
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
        classOf[Entity], Map(classOf[Int] -> IntSer), Map.empty)

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
