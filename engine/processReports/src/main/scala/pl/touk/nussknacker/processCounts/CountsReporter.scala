package pl.touk.nussknacker.processCounts

import java.time.LocalDateTime
import com.typesafe.config.Config

import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

object CountsReporterCreator {

  val reporterCreatorConfigPath = "countsSettings"

}

//By default it's InfluxCountsReporterCreator, other implementation can be used via ServiceLoaderMechanism
//@see NussknackerApp#prepareCountsReporter
trait CountsReporterCreator {

  def createReporter(env: String, config: Config): CountsReporter

}


trait CountsReporter extends AutoCloseable {

  def prepareRawCounts(processId: String, countsRequest: CountsRequest)(implicit ec: ExecutionContext): Future[String => Option[Long]]

}

object CannotFetchCountsError {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def restartsDetected(dates: List[LocalDateTime]): CannotFetchCountsError = CannotFetchCountsError(
    s"Counts unavailable, as process was restarted/deployed on following dates: ${dates.map(_.format(dateTimeFormatter)).mkString(", ")}"
  )
}

case class CannotFetchCountsError(msg: String) extends Exception(msg)

sealed trait CountsRequest

case class RangeCount(fromDate: LocalDateTime, toDate: LocalDateTime) extends CountsRequest

case class ExecutionCount(pointInTime: LocalDateTime) extends CountsRequest

