package com.pagerduty.mapper

import com.pagerduty.mapper.annotations._


package test {
  @Entity class ParentOfNoAnnotationNestedField(
      @Column(name = "nested") val nested: NoAnnotationEntity) {
    def this() = this(null)
  }

  @Entity class ParentOfNoDefaultConstructorNestedField(
      @Column(name = "nested") val nested: NoDefaultConstructor) {
    def this() = this(null)
  }

  @Entity class ParentWithNoTtl(
      @Column(name = "nested") val nested: EntityWithTtl) {
    def this() = this(null)
  }

  @Entity @Ttl(seconds = 101) class ParentWithTtl(
      @Column(name = "nested") val nested: EntityWithTtl) {
    def this() = this(null)
  }

  @Entity class ParentOfInstantiationNestedField(
      @Column(name = "nested") val nested: InstantiationEntity) {
    def this() = this(null)
  }

  @Entity class ParentOfNestedField(
      @Column(name = "field") val field: String,
      @Column(name = "nested") val nested: SimpleEntity) {
    def this() = this("default", new SimpleEntity)
  }

  @Entity class ParentOfDeclaredSerializerField(
      @Column(name = "nested") val nested: DeclaredSerializerEntiy) {
    def this() = this(new DeclaredSerializerEntiy)
  }
}

class NestedClassMappingSpec extends MappingSpec {
  "Nested class mapping should" - {

    "when creating new mapping" - {
      "throw exception when missing nested @Entity annotation" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[test.ParentOfNoAnnotationNestedField])
        }
      }
      "throw exception when missing default constructor for nested @Entity" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[test.ParentOfNoDefaultConstructorNestedField])
        }
      }
      "ignore nested TTL settings when parent has no TTL" in {
        val mapping = mappingFor(classOf[test.ParentWithNoTtl])
        mapping.ttlSeconds shouldBe None
      }
      "ignore nested TTL settings when parent has TTL" in {
        val mapping = mappingFor(classOf[test.ParentWithTtl])
        mapping.ttlSeconds shouldBe Some(101)
      }
    }

    "when instantiating entity" - {
      "use default constructor for nested fields" in {
        type Entity = test.ParentOfInstantiationNestedField
        val mapping = mappingFor(classOf[Entity])
        val resAdapter = mock[ResultAdapter]
        (resAdapter.get _).expects(targetId, "nested.field", StringSer).returns(Some("value0"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.nested.usedDefaultConstructor shouldBe true
      }
    }

    "when reading entity" - {
      type Entity = test.ParentOfNestedField
      val mapping = mappingFor(classOf[Entity])
      val resAdapter = mock[ResultAdapter]

      "read only annotated @Column fields keeping defaults non-anntated fields" in {
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(Some("value"))
        (resAdapter.get _).expects(targetId, "nested.f0", StringSer).returns(Some("value0"))
        (resAdapter.get _).expects(targetId, "nested.f1", StringSer).returns(Some("value1"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.field shouldBe "value"
        entity.nested.field0 shouldBe "value0"
        entity.nested.field1 shouldBe "value1"
        entity.nested.transient shouldBe "defaultTransient"
      }
      "keep defaults for nested @Column fields with null values" in {
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(Some("value"))
        (resAdapter.get _).expects(targetId, "nested.f0", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "nested.f1", StringSer).returns(Some("value1"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.nested.field0 shouldBe "default0"
      }
      "keep default value for nested field when all its @Column values are null" in {
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(Some("value"))
        (resAdapter.get _).expects(targetId, "nested.f0", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "nested.f1", StringSer).returns(None)
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.nested shouldBe new test.SimpleEntity
      }
      "provide entity value when at least one @Column of a nested field has a value" in {
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "nested.f0", StringSer).returns(Some("value0"))
        (resAdapter.get _).expects(targetId, "nested.f1", StringSer).returns(None)
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.field shouldBe "default"
        entity.nested.field0 shouldBe "value0"
        entity.nested.field1 shouldBe "default1"
        entity.nested.transient shouldBe "defaultTransient"
      }
      "read entity with no columns as None" in {
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "nested.f0", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "nested.f1", StringSer).returns(None)
        mapping.read(targetId, resAdapter) shouldBe None
      }
    }

    "when writing entity" - {
      type Entity = test.ParentOfNestedField
      val mapping = mappingFor(classOf[Entity])
      val mutAdapter = mock[MutationAdapter]
      val entity = new Entity("value", new test.SimpleEntity("value0", "value1", "transientValue"))

      "write only annotated @Column fields" in {
        (mutAdapter.insert _).expects(targetId, "field", StringSer, "value", None)
        (mutAdapter.insert _).expects(targetId, "nested.f0", StringSer, "value0", None)
        (mutAdapter.insert _).expects(targetId, "nested.f1", StringSer, "value1", None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete nested field @Columns with null values" in {
        val entity = new Entity("value", new test.SimpleEntity(null, "value1", "transientValue"))
        (mutAdapter.insert _).expects(targetId, "field", StringSer, "value", None)
        (mutAdapter.remove _).expects(targetId, "nested.f0")
        (mutAdapter.insert _).expects(targetId, "nested.f1", StringSer, "value1", None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete all @Column fields when writing None entity value" in {
        (mutAdapter.remove _).expects(targetId, "field")
        (mutAdapter.remove _).expects(targetId, "nested.f0")
        (mutAdapter.remove _).expects(targetId, "nested.f1")
        mapping.write(targetId, None, mutAdapter, None)
      }
      "when nested field value is null, delete all its @Columns" in {
        val entity = new Entity("value", null)
        (mutAdapter.insert _).expects(targetId, "field", StringSer, "value", None)
        (mutAdapter.remove _).expects(targetId, "nested.f0")
        (mutAdapter.remove _).expects(targetId, "nested.f1")
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly pass TTL value" in {
        val ttl = Some(10)
        (mutAdapter.insert _).expects(targetId, "field", StringSer, "value", ttl)
        (mutAdapter.insert _).expects(targetId, "nested.f0", StringSer, "value0", ttl)
        (mutAdapter.insert _).expects(targetId, "nested.f1", StringSer, "value1", ttl)
        mapping.write(targetId, Some(entity), mutAdapter, ttl)
      }
    }

    "when dealing with custom serializers" - {
      object IntSer
      val declaredSer = new test.DeclaredSerializer
      type Entity = test.ParentOfDeclaredSerializerField
      val mapping = EntityMapping[String, Entity](
        classOf[Entity], Map(classOf[Int] -> IntSer), Map.empty)

      "correctly pass serializers when reading entity" in {
        val resAdapter = mock[ResultAdapter]
        (resAdapter.get _).expects(targetId, "nested.f0", IntSer).returns(None)
        (resAdapter.get _).expects(targetId, "nested.f1", declaredSer).returns(None)
        mapping.read(targetId, resAdapter)
      }
      "correctly pass serializers when wiring entity" in {
        val mutAdapter = mock[MutationAdapter]
        val entity = new Entity(new test.DeclaredSerializerEntiy(20, 20.0))
        (mutAdapter.insert _).expects(targetId, "nested.f0", IntSer, 20, None)
        (mutAdapter.insert _).expects(targetId, "nested.f1", declaredSer, 20.0, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
    }
  }
}
