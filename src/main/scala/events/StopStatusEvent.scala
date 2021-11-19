package events

import com.google.transit.realtime.GtfsRealtime.VehiclePosition
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

object StopStatusEvent {
  case class Update(vehiclePosition: VehiclePosition, replyTo: ActorRef[Result])

  sealed trait Result
  sealed trait Event extends Result
  case class Arrival(vehiclePosition: ParsedVehiclePosition) extends Event
  case class Departure(vehiclePosition: ParsedVehiclePosition) extends Event
  case object Noop extends Result

  case class ParsedVehiclePosition(vehicleId: String, tripId: String, routeId: String, routeDirection: Int, stopId: String, stopStatus: String, timestamp: Long)

  def apply(): Behavior[Update] = {
    handle()
  }

  private def handle(state: Map[String, VehiclePosition] = Map()): Behavior[Update] =
    Behaviors.receiveMessage { message =>
      val vehicleId = message.vehiclePosition.getVehicle().getId()
      val currentState = parseVehiclePosition(Some(message.vehiclePosition))
      val previousState = parseVehiclePosition(state.get(vehicleId))
      (currentState, previousState) match {
        case (_, None) => {
          message.replyTo ! Noop
        }
        case (Some(currentVP), Some(previousVP)) if currentVP.stopId == previousVP.stopId && currentVP.stopStatus == previousVP.stopStatus =>
          message.replyTo ! Noop
        case (Some(currentVP), Some(previousVP)) => {
          currentVP.stopStatus match {
            case "INCOMING_AT" =>
              message.replyTo ! Noop
            case "STOPPED_AT" =>
              message.replyTo ! Arrival(vehiclePosition = currentVP)
            case "IN_TRANSIT_TO" =>
              message.replyTo ! Departure(
                vehiclePosition = ParsedVehiclePosition(
                  vehicleId = currentVP.vehicleId,
                  tripId = previousVP.tripId,
                  routeId = previousVP.routeId,
                  routeDirection = previousVP.routeDirection,
                  stopId = previousVP.stopId,
                  stopStatus = currentVP.stopStatus,
                  timestamp = currentVP.timestamp
                )
              )
          }
        }
      }
      handle(state + (vehicleId -> message.vehiclePosition))
    }

  private def parseVehiclePosition(vp: Option[VehiclePosition]): Option[ParsedVehiclePosition] = {
    vp match {
      case Some(vehiclePosition: VehiclePosition) =>
        Some(ParsedVehiclePosition(
          vehicleId = vehiclePosition.getVehicle().getId(),
          tripId = vehiclePosition.getTrip().getTripId(),
          routeId = vehiclePosition.getTrip().getRouteId(),
          routeDirection = vehiclePosition.getTrip().getDirectionId(),
          stopId = vehiclePosition.getStopId(),
          stopStatus = vehiclePosition.getCurrentStatus().toString(),
          timestamp = vehiclePosition.getTimestamp()
        ))
      case _ => None

    }
  }

}
