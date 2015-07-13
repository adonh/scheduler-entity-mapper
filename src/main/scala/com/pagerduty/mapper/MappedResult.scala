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
