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
  def write(targetId: Any, value: Option[Any], mutation: MutationAdapter, ttlSeconds: Option[Int])
  : Unit

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
