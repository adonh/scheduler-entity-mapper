package com.pagerduty.mapper


/**
 * MutationAdapter interface captures interaction required to mutate entity data.
 */
trait MutationAdapter {

  /**
   * Inserts column data into mutation batch.
   *
   * @param targetId target entity id
   * @param colName column name
   * @param serializer value serializer
   * @param value column value
   * @param ttlSeconds optional TTL value in seconds
   */
  def insert(targetId: Any, colName: String, serializer: Any, value: Any, ttlSeconds: Option[Int])
  : Unit

  /**
   * Inserts remove command into mutation batch.
   *
   * @param targetId target entity id
   * @param colName column name
   */
  def remove(targetId: Any, colName: String): Unit
}
