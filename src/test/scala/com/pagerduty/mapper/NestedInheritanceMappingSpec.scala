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

  @Entity
  class ParentOfNestedSuperclass(
      @Column(name = "f0") val field: NestedSuperclass
  ) {
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
