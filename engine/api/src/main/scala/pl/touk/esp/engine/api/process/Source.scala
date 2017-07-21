package pl.touk.esp.engine.api.process

import pl.touk.esp.engine.api.test.TestDataParser

/**
  * Common trait for source of events. For Flink see [[pl.touk.esp.engine.flink.api.process.FlinkSource]]
  * @tparam T - type of event that is generated by this source. This is needed to handle e.g. syntax suggestions in UI
  */
trait Source[T] {

}

trait TestDataGenerator { self: Source[_] =>
  def generateTestData(size: Int) : Array[Byte]
}


/**
  * [[pl.touk.esp.engine.api.process.SourceFactory]] has to have method annotated with [[pl.touk.esp.engine.api.MethodToInvoke]]
  * that returns [[pl.touk.esp.engine.api.process.Source]]
* */
trait SourceFactory[T] extends Serializable {
  def clazz : Class[_]

  def testDataParser: Option[TestDataParser[T]]

}

trait StandaloneSourceFactory[T] extends SourceFactory[T] {
  def toObject(obj: Array[Byte]): T
}