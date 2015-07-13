package com.pagerduty.mapper


/**
 * Allows reading and writing a single value, backed by a serializer.
 */
private[mapper] class FieldMapping(
    protected val serializer: Any,
    val name: String)
  extends Mapping
{
  def serializersByColName = Seq(name -> serializer)

  def write(targetId: Any, value: Option[Any], mutation: MutationAdapter, ttlSeconds: Option[Int])
  : Unit = value match {
    case Some(v) => mutation.insert(targetId, name, serializer, v, ttlSeconds)
    case None => mutation.remove(targetId, name)
  }

  def read(targetId: Any, result: ResultAdapter): MappedResult = {
    result.get(targetId, name, serializer) match {
      case Some(entity) => MappedValue(true, entity)
      case None => Undefined
    }
  }

  override def toString(): String = s"FieldMapping($name, ${serializer.getClass.getName})"
}
