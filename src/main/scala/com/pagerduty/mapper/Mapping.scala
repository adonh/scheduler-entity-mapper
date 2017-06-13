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

/**
  * Common interface that captures a mapping. Can be an entity mapping, field mapping,
  * option mapping, or a custom mapping.
  */
trait Mapping {

  /**
    * Specifies column name to serializer mapping.
    */
  def serializersByColName: Seq[(String, Any)]

  /**
    * Writes a value into mutation batch.
    *
    * @param targetId target entity id
    * @param value mapping value
    * @param mutation outgoing mutation
    * @param ttlSeconds mutation TTL argument applied to the values represented by this mapping
    */
  def write(targetId: Any, value: Option[Any], mutation: MutationAdapter, ttlSeconds: Option[Int]): Unit

  /**
    * Reads a value from result.
    *
    * If a column is not defined in Cassandra, the value may be null, or it may be replaces by a
    * synthetic value, for example None for Option. MappedResult.hasColumns can be used to
    * distinguish such cases.
    *
    * @param targetId target entity id
    * @param result query result
    * @return (isDefined, entity)
    */
  def read(targetId: Any, result: ResultAdapter): MappedResult
}
