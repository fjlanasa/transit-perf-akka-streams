package sources

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.google.transit.realtime.GtfsRealtime.{
  FeedEntity,
  FeedMessage,
  VehiclePosition => GtfsVehiclePosition
}

import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

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
    longitude: Float
)

  implicit val ec = ExecutionContext.global

  private def poll(fileLocation: String) = Future {
    val url = new URL(fileLocation)
    val message = FeedMessage.parseFrom(url.openStream())
    message
      .getEntityList()
      .asScala
      .toList
      .map((x) =>
        VehiclePosition(
          vehicleId = x.getVehicle().getVehicle().getId(),
          routeId = x.getVehicle().getTrip().getRouteId(),
          tripId = x.getVehicle().getTrip().getTripId(),
          routeDirection = x.getVehicle().getTrip().getDirectionId(),
          stopId = x.getVehicle().getStopId(),
          stopStatus = x.getVehicle().getCurrentStatus().toString(),
          timestamp = x.getVehicle().getTimestamp(),
          latitude = x.getVehicle().getPosition().getLatitude(),
          longitude = x.getVehicle().getPosition().getLongitude()
        )
      )
  }

  def apply(url: String): Source[VehiclePosition, Cancellable] = {
    Source
      .tick(
        0.second,
        1.second,
        NotUsed
      )
      .mapAsync(1) { _ =>
        poll(url)
      }
      .flatMapConcat((x: List[VehiclePosition]) => {
        Source(x)
      })
  }
}
