package com.pagerduty.mapper


/**
 * ResultAdapter interface captures interaction required to read entity data.
 */
trait ResultAdapter {

  /**
   * Retrieves column data from query result.
   *
   * @param targetId target entity id
   * @param colName column name
   * @param serializer column value serializer
   * @return Some(value) if column exists, None otherwise
   */
  def get(targetId: Any, colName: String, serializer: Any): Option[Any]
}
