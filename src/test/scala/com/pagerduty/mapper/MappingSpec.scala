package com.pagerduty.mapper

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, FreeSpec}


trait MappingSpec extends FreeSpec with Matchers with MockFactory {
  val targetId = "id0"
  object StringSer
  def mappingFor[Entity](target: Class[Entity]) = {
    EntityMapping[String, Entity](target, Map(classOf[String] -> StringSer), Map.empty)
  }
}
