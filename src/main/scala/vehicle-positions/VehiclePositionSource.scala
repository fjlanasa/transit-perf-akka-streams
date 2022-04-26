package vehicleposition

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.google.transit.realtime.GtfsRealtime.{
  FeedEntity,
  FeedMessage,
  VehiclePosition => GtfsVehiclePosition
}

import akka.actor.typed.scaladsl.AskPattern._
import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.mutable.ListBuffer
import akka.actor.typed.ActorRef
import schedule.ScheduleState
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import scala.util.Success
import akka.actor.typed.Scheduler

object VehiclePositionSource {
  case class VehiclePosition(
      vehicleId: String,
      routeId: String,
      tripId: String,
      routeDirection: Int,
      stopId: String,
      stopStatus: String,
      timestamp: Long,
      latitude: Float,
      longitude: Float,
      trip: Option[ScheduleState.Trip]
  )

  implicit val ec = ExecutionContext.global

  private def poll(
      fileLocation: String,
      scheduleActor: ActorRef[ScheduleState.Req]
  )(implicit system: ActorSystem[_]) = Future {
    val url = new URL(fileLocation)
    val message = FeedMessage.parseFrom(url.openStream())
    message
      .getEntityList()
      .asScala
      .toList
      .map(x =>
        VehiclePosition(
          vehicleId = x.getVehicle().getVehicle().getId(),
          routeId = x.getVehicle().getTrip().getRouteId(),
          tripId = x.getVehicle().getTrip().getTripId(),
          routeDirection = x.getVehicle().getTrip().getDirectionId(),
          stopId = x.getVehicle().getStopId(),
          stopStatus = x.getVehicle().getCurrentStatus().toString(),
          timestamp = x.getVehicle().getTimestamp(),
          latitude = x.getVehicle().getPosition().getLatitude(),
          longitude = x.getVehicle().getPosition().getLongitude(),
          trip = None
        )
      )
  }

  def apply(url: String, scheduleActor: ActorRef[ScheduleState.Req])(implicit
      system: ActorSystem[_]
  ): Source[VehiclePosition, Cancellable] = {
    implicit val timeout: Timeout = 10.seconds
    implicit val ec = system.executionContext
    Source
      .tick(
        0.second,
        1.second,
        NotUsed
      )
      .mapAsync(1) { _ =>
        poll(url, scheduleActor)
      }
      .flatMapConcat((x: List[VehiclePosition]) => {
        Source(x)
      })
      .mapAsync(1) { x =>
        scheduleActor
          .ask((ref: ActorRef[Option[ScheduleState.Trip]]) => {
            ScheduleState.Req(x.tripId, ref)
          })
          .flatMap {
            case Some(trip) =>
              Future.successful(x.copy(trip = Some(trip)))
            case _ =>
              Future.successful(x)
          }
      }
  }
}
