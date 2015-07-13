package com.pagerduty.mapper


/**
 * Special mapping that allows to wrap and unwrap Option values.
 */
private[mapper] class OptionMapping(protected val mapping: Mapping)
  extends Mapping
{
  def serializersByColName = mapping.serializersByColName

  def write(targetId: Any, value: Option[Any], mutation: MutationAdapter, ttlSeconds: Option[Int])
  : Unit = value match {
    case Some(v @ Some(_)) => mapping.write(targetId, v, mutation, ttlSeconds)
    case _ => mapping.write(targetId, None, mutation, None)
  }

  def read(targetId: Any, result: ResultAdapter): MappedResult = {
    mapping.read(targetId, result) match {
      case MappedValue(hasColumns, value) if hasColumns => new MappedValue(true, Some(value))
      case _ => new MappedValue(false, None)
    }
  }

  override def toString(): String = s"OptionMapping($mapping)"
}
