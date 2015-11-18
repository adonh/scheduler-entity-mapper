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
 * Result of reading entity from Astyanax column list.
 */
sealed abstract class MappedResult protected (
    /** Indicates if there was at least one column value for mapped columns. */
    val hasColumns: Boolean)
{
  def map(transform: Any => Any): MappedResult
  def flatMap(transform: Any => MappedResult): MappedResult
}


/**
 * Result that has a defined value. Sometimes we can get back a synthetic value, for example
 * {{{None}}}. So we have to rely on checking for presence of columns explicitly in order to
 * properly set entities that have no footprint in the database.
 */
case class MappedValue(override val hasColumns: Boolean, value: Any)
  extends MappedResult(hasColumns)
{
  require(value != null, "Mapped value must be defined.")
  def map(transform: Any => Any) = new MappedValue(hasColumns, transform(value))
  def flatMap(transform: Any => MappedResult) = transform(value) match {
    case MappedValue(hasColumns, value) => MappedValue(this.hasColumns || hasColumns, value)
    case Undefined => Undefined
  }
}


/**
 * Result that has no value.
 */
case object Undefined extends MappedResult(false) {
  def map(transform: Any => Any) = Undefined
  def flatMap(transform: Any => MappedResult) = Undefined
}
