package com.pagerduty.mapper

/**
 * Exception that can be thrown by EntityMapper.
 */
class EntityMapperException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)
