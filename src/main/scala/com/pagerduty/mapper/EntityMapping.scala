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

object EntityMapping {

  /**
   * Main factory for creating EntityMappings.
   *
   * @param target target class
   * @param registeredSerializers serializer lookup map
   * @param customMappers custom mappers
   * @tparam Id entity Id type
   * @tparam Entity entity type
   * @return a new entity mapping
   */
  def apply[Id, Entity](
    target: Class[Entity],
    registeredSerializers: Map[Class[_], Any],
    customMappers: Map[Class[_ <: Annotation], Mapping => Mapping]
  ): EntityMapping[Id, Entity] = {
    val constructorTarget = target
    new EntityMapping[Id, Entity] {
      val untypedMapping = UntypedEntityMapping(
        constructorTarget, None, registeredSerializers, customMappers
      )
      def target = untypedMapping.target.asInstanceOf[Class[Entity]]
      def ttlSeconds = untypedMapping.ttlSeconds
      def isIdDefined = untypedMapping.isIdDefined
      def getId(entity: Entity) = untypedMapping.getId(entity).asInstanceOf[Id]
      def setId(entity: Entity, id: Id) = untypedMapping.setId(entity, id)
      val serializersByColName = untypedMapping.serializersByColName.toMap
      def write(
        targetId: Id, value: Option[Entity], mutation: MutationAdapter, ttlSeconds: Option[Int]
      ): Unit = {
        untypedMapping.write(targetId, value, mutation, ttlSeconds)
      }
      def read(targetId: Id, result: ResultAdapter) = {
        untypedMapping.read(targetId, result) match {
          case MappedValue(_, value) => Some(value.asInstanceOf[Entity])
          case Undefined => None
        }
      }
    }
  }
}

/**
 * Typed EntityMapping provides low level interface to write entities to mutation adapter and
 * read them from result adapter.
 *
 * @tparam Id
 * @tparam Entity
 */
trait EntityMapping[Id, Entity] {

  /**
   * Returns a class managed by this mapping.
   */
  def target: Class[Entity]

  /**
   * Returns an optional ttl for this mapping.
   */
  def ttlSeconds: Option[Int]

  /**
   * Checks if there is an @Id annotation field.
   */
  def isIdDefined: Boolean

  /**
   * Uses reflection to get id of a give entity.
   *
   * @throws EntityMapperException when @Id is no defined.
   */
  def getId(entity: Entity): Id

  /**
   * Uses reflection to set an id for a given entity.
   *
   * @throws EntityMapperException when @Id is no defined.
   */
  def setId(entity: Entity, id: Id): Unit

  /**
   * Returns a map of serializers by column name.
   */
  def serializersByColName: Map[String, Any]

  /**
   * Writes entity value into mutation batch.
   *
   * Note that @Id field will not be written unless it is explicitly marked as @Column.
   *
   * @param targetId entity id
   * @param value entity value
   * @param mutation outgoing mutation
   * @param ttlSeconds mutation TTL argument applied to the values represented by this mapping
   */
  def write(targetId: Id, value: Option[Entity], mutation: MutationAdapter, ttlSeconds: Option[Int]): Unit

  /**
   * Reads entity value from result.
   *
   * Note that @Id field will not be set unless it is explicitly marked as @Column. You can use
   * [[setId()]] method to manually set @Id field when desired.
   *
   * @param targetId entity id
   * @param result query result
   * @return Some(entity) when result contains at least one value needed by the mapping,
   *         None otherwise
   */
  def read(targetId: Id, result: ResultAdapter): Option[Entity]
}
