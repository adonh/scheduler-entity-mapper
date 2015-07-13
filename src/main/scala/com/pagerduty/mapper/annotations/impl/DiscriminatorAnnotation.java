package com.pagerduty.mapper.annotations.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java annotation implementation. May require additional Scala compiler hints and is not meant
 * to be used directly. See the parent package object for usable alias and documentation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface DiscriminatorAnnotation {

  /**
   * Discriminator column value.
   */
  String value();
}
