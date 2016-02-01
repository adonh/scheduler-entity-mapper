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

package testoption {

  @Entity class CannotDetectSerializer(@Column(name = "field") val field: Option[Int]) {
    def this() = this(null)
  }

  @Entity class AutoDetectedAnyRefOption(
      @Column(name = "field") val field: Option[String]
  ) {
    def this() = this(null)
  }

  case class DeclaredSerializer() // Using case class for equals() implementation.

  @Entity class DeclaredAnyRefOption(
      @Column(name = "field")@Serializer(classOf[DeclaredSerializer]) val field: Option[String]
  ) {
    def this() = this(null)
  }

  @Entity class DeclaredAnyValOption(
      @Column(name = "field")@Serializer(classOf[DeclaredSerializer]) val field: Option[Int]
  ) {
    def this() = this(null)
  }

  @Entity class SimpleValueOption(
      // Extra field to keep entity defined when opField is None.
      @Column(name = "field") val field: String,
      @Column(name = "opField") val opField: Option[String]
  ) {
    def this() = this(null, Some("defaultOpFiled"))
  }

  @Entity class NestedOpField(
      @Column(name = "nestedOp0") val nestedOp0: Option[String],
      @Column(name = "nestedOp1") val nestedOp1: Option[String]
  ) {
    def this() = this(Some("defaultNestedOp0"), Some("defaultNestedOp1"))
  }

  @Entity class ParentOfNestedOp(
      @Column(name = "field") val field: String,
      @Column(name = "opField") val opField: Option[NestedOpField]
  ) {
    def this() = this(
      "defaultField",
      Some(new NestedOpField(Some("defaultOp0"), Some("defaultOp1")))
    )
  }

  @Entity @Superclass(subclasses = Array(
    classOf[NestedInheritanceEntity1], classOf[NestedInheritanceEntity2]
  ))
  class SuperOfOpInheritance

  @Entity @Discriminator("disc1") class NestedInheritanceEntity1(
    @Column(name = "maybeOp0") val nestedOp0: String,
    @Column(name = "maybeOp1") val nestedOp1: String
  )
      extends SuperOfOpInheritance {
    def this() = this("defaultNestedOp0", "defaultNestedOp1")
  }

  @Entity @Discriminator("disc2") class NestedInheritanceEntity2(
    @Column(name = "maybeOp0") val nestedOp0: Option[String],
    @Column(name = "maybeOp1") val nestedOp1: Option[String]
  )
      extends SuperOfOpInheritance {
    def this() = this(Some("defaultNestedOp0"), Some("defaultNestedOp1"))
  }

  @Entity class ParentOfInheritanceOp(
      @Column(name = "field") val field: String,
      @Column(name = "opField") val opField: Option[SuperOfOpInheritance]
  ) {
    def this() = this(
      "defaultField",
      Some(new NestedInheritanceEntity2(Some("defaultOp0"), Some("defaultOp1")))
    )
  }
}

class OptionMappingSpec extends MappingSpec {
  "Option mapping should" - {

    "when creating new mapping" - {
      "throw exception when unable to detect primitive serializer" in {
        intercept[EntityMapperException] {
          mappingFor(classOf[testoption.CannotDetectSerializer])
        }
      }
    }
    "when resolving serializers" - {
      val resAdapter = mock[ResultAdapter]
      val declaredSer = new testoption.DeclaredSerializer

      "auto-detect provided serializers for AnyRef fields" in {
        val mapping = mappingFor(classOf[testoption.AutoDetectedAnyRefOption])
        (resAdapter.get _).expects(targetId, "field", StringSer).returns(None)
        mapping.read(targetId, resAdapter)
      }
      "override auto-detected serializers with declared ones for AnyRef fields" in {
        val mapping = mappingFor(classOf[testoption.DeclaredAnyRefOption])
        (resAdapter.get _).expects(targetId, "field", declaredSer).returns(None)
        mapping.read(targetId, resAdapter)
      }
      "work with declared serializers for AnyVal fields" in {
        val mapping = mappingFor(classOf[testoption.DeclaredAnyValOption])
        (resAdapter.get _).expects(targetId, "field", declaredSer).returns(None)
        mapping.read(targetId, resAdapter)
      }
    }

    "when wrapping simple fields" - {
      type Entity = testoption.SimpleValueOption
      val mapping = mappingFor(classOf[Entity])
      val resAdapter = mock[ResultAdapter]
      val mutAdapter = mock[MutationAdapter]

      "correctly read wrapped value" in {
        (resAdapter.get _).expects(targetId, "opField", StringSer).returns(Some("value"))
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.opField shouldBe Some("value")
      }
      "correctly write wrapped value" in {
        val entity = new testoption.SimpleValueOption("*", Some("value"))
        (mutAdapter.insert _).expects(targetId, "opField", StringSer, "value", None)
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly read None value" in {
        (resAdapter.get _).expects(targetId, "opField", StringSer).returns(None)
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.opField shouldBe None
      }
      "correctly write None value" in {
        val entity = new testoption.SimpleValueOption("*", None)
        (mutAdapter.remove _).expects(targetId, "opField")
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly pass TTL value" in {
        val ttl = Some(10)
        val entity = new testoption.SimpleValueOption("*", Some("value"))
        (mutAdapter.insert _).expects(targetId, "opField", StringSer, "value", ttl)
        (mutAdapter.insert _).stubs(targetId, *, *, *, ttl)
        mapping.write(targetId, Some(entity), mutAdapter, ttl)
      }
    }

    "when wrapping nested fields" - {
      type Entity = testoption.ParentOfNestedOp
      val mapping = mappingFor(classOf[Entity])
      val resAdapter = mock[ResultAdapter]
      val mutAdapter = mock[MutationAdapter]

      "correctly read wrapped value" in {
        (resAdapter.get _).expects(targetId, "opField.nestedOp0", StringSer).returns(Some("value"))
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.opField.get.nestedOp0 shouldBe Some("value")
      }
      "correctly write wrapped value" in {
        val entity = new testoption.ParentOfNestedOp(
          "*", Some(new testoption.NestedOpField(Some("value"), Some("*")))
        )
        (mutAdapter.insert _).expects(targetId, "opField.nestedOp0", StringSer, "value", None)
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly read None value" in {
        (resAdapter.get _).expects(targetId, "opField.nestedOp0", StringSer).returns(None)
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.opField.get.nestedOp0 shouldBe None
      }
      "correctly write None value" in {
        val entity = new testoption.ParentOfNestedOp(
          "*", Some(new testoption.NestedOpField(None, Some("*")))
        )
        (mutAdapter.remove _).expects(targetId, "opField.nestedOp0")
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly read None entity value" in {
        (resAdapter.get _).expects(targetId, "opField.nestedOp0", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "opField.nestedOp1", StringSer).returns(None)
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.opField shouldBe None
      }
      "correctly write None entity value" in {
        val entity = new testoption.ParentOfNestedOp("*", None)
        (mutAdapter.remove _).expects(targetId, "opField.nestedOp0")
        (mutAdapter.remove _).expects(targetId, "opField.nestedOp1")
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly write entity value with Nones" in {
        val entity = new testoption.ParentOfNestedOp(
          "*", Some(new testoption.NestedOpField(None, None))
        )
        (mutAdapter.remove _).expects(targetId, "opField.nestedOp0")
        (mutAdapter.remove _).expects(targetId, "opField.nestedOp1")
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
    }

    "when wrapping inheritance mapping fields" - {
      type Entity = testoption.ParentOfInheritanceOp
      val mapping = mappingFor(classOf[Entity])
      val resAdapter = mock[ResultAdapter]
      val mutAdapter = mock[MutationAdapter]
      val disc = Some("disc2")

      "correctly read wrapped value" in {
        (resAdapter.get _).expects(targetId, "opField.discriminator", StringSer).returns(disc)
        (resAdapter.get _).expects(targetId, "opField.maybeOp0", StringSer).returns(Some("value"))
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        val opField = entity.opField.get.asInstanceOf[testoption.NestedInheritanceEntity2]
        opField.nestedOp0 shouldBe Some("value")
      }
      "correctly write wrapped value" in {
        val entity = new testoption.ParentOfInheritanceOp(
          "*", Some(new testoption.NestedInheritanceEntity2(Some("value"), Some("*")))
        )
        (mutAdapter.insert _).expects(targetId, "opField.discriminator", StringSer, "disc2", None)
        (mutAdapter.insert _).expects(targetId, "opField.maybeOp0", StringSer, "value", None)
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly read None value" in {
        (resAdapter.get _).expects(targetId, "opField.discriminator", StringSer).returns(disc)
        (resAdapter.get _).expects(targetId, "opField.maybeOp0", StringSer).returns(None)
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        val opField = entity.opField.get.asInstanceOf[testoption.NestedInheritanceEntity2]
        opField.nestedOp0 shouldBe None
      }
      "correctly write None value" in {
        val entity = new testoption.ParentOfInheritanceOp(
          "*", Some(new testoption.NestedInheritanceEntity2(None, Some("value")))
        )
        (mutAdapter.remove _).expects(targetId, "opField.maybeOp0")
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly read entity value with Nones" in {
        (resAdapter.get _).expects(targetId, "opField.discriminator", StringSer).returns(disc)
        (resAdapter.get _).expects(targetId, "opField.maybeOp0", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "opField.maybeOp1", StringSer).returns(None)
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        val opField = entity.opField.get.asInstanceOf[testoption.NestedInheritanceEntity2]
        opField.nestedOp0 shouldBe None
        opField.nestedOp1 shouldBe None
      }
      "correctly write entity value with Nones" in {
        val entity = new testoption.ParentOfInheritanceOp(
          "*", Some(new testoption.NestedInheritanceEntity2(None, None))
        )
        (mutAdapter.insert _).expects(targetId, "opField.discriminator", StringSer, "disc2", None)
        (mutAdapter.remove _).expects(targetId, "opField.maybeOp0")
        (mutAdapter.remove _).expects(targetId, "opField.maybeOp1")
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
      "correctly read None entity value" in {
        (resAdapter.get _).expects(targetId, "opField.discriminator", StringSer).returns(None)
        (resAdapter.get _).expects(targetId, "opField.maybeOp0", StringSer).never()
        (resAdapter.get _).expects(targetId, "opField.maybeOp1", StringSer).never()
        (resAdapter.get _).stubs(targetId, *, *).returns(Some("*"))
        val Some(entity: Entity) = mapping.read(targetId, resAdapter)
        entity.opField shouldBe None
      }
      "correctly write None entity value" in {
        val entity = new testoption.ParentOfInheritanceOp("*", None)
        (mutAdapter.remove _).expects(targetId, "opField.discriminator")
        (mutAdapter.remove _).expects(targetId, "opField.maybeOp0").atLeastOnce()
        (mutAdapter.remove _).expects(targetId, "opField.maybeOp1").atLeastOnce()
        (mutAdapter.insert _).stubs(targetId, *, *, *, None)
        mapping.write(targetId, Some(entity), mutAdapter, None)
      }
    }
  }
}
