package pl.touk.nussknacker.engine.standalone.utils

import pl.touk.nussknacker.engine.api.test.NewLineSplittedTestDataParser
import pl.touk.nussknacker.engine.api.typed.TypedMap

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

class QueryStringTestDataParser extends NewLineSplittedTestDataParser[TypedMap] {
  override def parseElement(testElement: String): TypedMap = {
    val params = testElement.split("&").map { param =>
      param.split("=").toList match {
        case name :: value :: Nil => (name, value)
        case _ => throw new IllegalArgumentException(s"Failed to parse $testElement as query string")
      }
    }.toList.groupBy(_._1).mapValues {
      case oneElement :: Nil => oneElement._2
      case more => more.map(_._2).asJava
    }.toList
    //TODO: validation??
    TypedMap(ListMap(params: _*))
  }
}
