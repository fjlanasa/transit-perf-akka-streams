package schedule

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.Scheduler
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}
import java.net.URL
import java.nio.file.{Paths, Files}
import java.util.zip.ZipFile
import scala.concurrent.duration._
import scala.collection.mutable
import com.univocity.parsers.common.record.Record
import java.nio.file.StandardCopyOption

object ScheduleState {
  sealed trait Msg
  case class Req(tripId: String, replyTo: ActorRef[Option[Trip]]) extends Msg
  case class Refresh(url: String) extends Msg

  case class Trip(
      tripId: String,
      routeId: String,
      serviceId: String,
      directionId: Int,
      shapeId: String,
      firstStopId: String,
      firstStopSequence: Int,
      lastStopId: String,
      lastStopSequence: Int
  )

  def apply(url: String): Behavior[Msg] = {
    Behaviors.withTimers((timers) => {
      timers.startTimerAtFixedRate(Refresh(url), 1.hour)
      val trips = fetchTrips(url)
      handle(trips, url)
    })
  }

  def handle(state: mutable.Map[String, Trip], url: String): Behavior[Msg] =
    Behaviors.receiveMessage { message =>
      message match {
        case Req(tripId, replyTo) =>
          state.get(tripId) match {
            case Some(trip) =>
              replyTo ! Some(trip)
            case None =>
              replyTo ! None
          }
          handle(state, url)
        case Refresh(url) =>
          val trips = fetchTrips(url)
          handle(trips, url)
      }
    }

  private def fetchTrips(url: String): mutable.Map[String, Trip] = {
    val parserSettings = new CsvParserSettings()
    parserSettings.setHeaderExtractionEnabled(true)
    val parser = new CsvParser(parserSettings)
    val remote = new URL(url)
    val targetFile = Paths.get("gtfs")
    Files.copy(
      remote.openStream(),
      targetFile,
      StandardCopyOption.REPLACE_EXISTING
    )
    val zipfile = new ZipFile(targetFile.toString())
    val stopTimesEntry = zipfile.getEntry("stop_times.txt")
    parser.beginParsing(zipfile.getInputStream(stopTimesEntry))
    var row: Record = parser.parseNextRecord()
    val stopTimesMap: mutable.Map[String, (String, Int, String, Int)] =
      mutable.Map()
    while (row != null) {
      val rowMap = row.toFieldMap()
      val rowTripId = rowMap.get("trip_id")
      val rowStopId = rowMap.get("stop_id")
      val rowStopSequence = rowMap.get("stop_sequence").toInt
      stopTimesMap.get(rowMap.get("trip_id")) match {
        case Some(
              (firstStopId, firstStopSequence, lastStopId, lastStopSequence)
            ) =>
          stopTimesMap += (rowTripId -> (
            if (firstStopSequence < rowStopSequence) firstStopId else rowStopId,
            firstStopSequence.min(rowStopSequence),
            if (lastStopSequence > rowStopSequence) lastStopId else rowStopId,
            lastStopSequence.max(rowStopSequence)
          ))
        case None =>
          stopTimesMap += (rowTripId -> (
            rowStopId,
            rowStopSequence,
            rowStopId,
            rowStopSequence
          ))
      }
      row = parser.parseNextRecord()
    }
    val tripsEntry = zipfile.getEntry("trips.txt")
    parser.beginParsing(zipfile.getInputStream(tripsEntry))
    row = parser.parseNextRecord()
    val tripsMap: mutable.Map[String, (String, String, Int, String)] =
      mutable.Map()
    while (row != null) {
      val rowMap = row.toFieldMap()
      tripsMap += (rowMap.get("trip_id") -> (rowMap.get("route_id"), rowMap.get(
        "service_id"
      ), rowMap.get("direction_id").toInt, rowMap.get("shape_id")))
      row = parser.parseNextRecord()
    }
    tripsMap.foldLeft(mutable.Map[String, Trip]())((acc, curr) => {
      val (tripId, tripDetails) = curr
      val (routeId, serviceId, directionId, shapeId) = tripDetails
      stopTimesMap.get(tripId) match {
        case Some(
              (firstStopId, firstStopSequence, lastStopId, lastStopSequence)
            ) =>
          acc += (tripId -> Trip(
            tripId = tripId,
            routeId = routeId,
            serviceId = serviceId,
            directionId = directionId,
            shapeId = shapeId,
            firstStopId = firstStopId,
            firstStopSequence = firstStopSequence,
            lastStopId = lastStopId,
            lastStopSequence = lastStopSequence
          ))
          acc
        case _ =>
          acc
      }
    })
  }
}
