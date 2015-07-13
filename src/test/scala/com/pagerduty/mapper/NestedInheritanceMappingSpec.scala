package com.pagerduty.mapper

import com.pagerduty.mapper.annotations._


package test {

  @Entity
  class ParentOfNestedSuperclass(
      @Column(name = "f0") val field: NestedSuperclass) {
    def this() = this(null)
  }

  @Entity @Superclass(subclasses = Array(classOf[NestedSubclass1], classOf[NestedSubclass2]))
  class NestedSuperclass()

  @Entity @Discriminator("disc1")
  case class NestedSubclass1() extends NestedSuperclass

  @Entity @Discriminator("disc2")
  case class NestedSubclass2() extends NestedSuperclass
}


class NestedInheritanceMappingSpec extends MappingSpec {
  "Nested inheritance mapping should" - {

    "when reading entity" - {
      type Entity = test.ParentOfNestedSuperclass
      val mapping = mappingFor(classOf[Entity])
      val resAdapter = mock[ResultAdapter]

      "read discriminator for nested inheritance mapping" in {
        (resAdapter.get _).expects(targetId, "f0.discriminator", StringSer).returns(Some("disc1"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.field shouldBe new test.NestedSubclass1
      }
      "read nested inheritance mapping field as None when there is no discriminator" in {
        (resAdapter.get _).expects(targetId, "f0.discriminator", StringSer).returns(None)
        mapping.read(targetId, resAdapter) shouldBe None
      }
    }

    "when writing entity" - {
      type Entity = test.ParentOfNestedSuperclass
      val mapping = mappingFor(classOf[Entity])
      val mutAdapter = mock[MutationAdapter]
      val entity = new test.ParentOfNestedSuperclass(new test.NestedSubclass2())

      "write discriminator for nested inheritance mapping" in {
        (mutAdapter.insert _).expects(targetId, "f0.discriminator", StringSer, "disc2", None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "delete discriminator for nested inheritance mapping when writing None value" in {
        (mutAdapter.remove _).expects(targetId, "f0.discriminator")
        mapping.write(targetId, None, mutAdapter, None)
      }
      "correctly pass TTL value" in {
        val ttl = Some(10)
        (mutAdapter.insert _).expects(targetId, "f0.discriminator", StringSer, "disc2", ttl)
        mapping.write(targetId, Some(entity), mutAdapter, ttl)
      }
    }
  }
}
