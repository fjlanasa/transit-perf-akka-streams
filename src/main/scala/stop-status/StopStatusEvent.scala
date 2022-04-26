package stopstatus

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import vehicleposition.VehiclePositionSource.VehiclePosition

object StopStatusEvent {
  case class Update(
      vehiclePosition: VehiclePosition,
      replyTo: ActorRef[Seq[Event]]
  )

  sealed trait Event
  case class Arrival(
      vehicleId: String,
      routeId: String,
      tripId: String,
      directionId: Int,
      stopId: String,
      timestamp: Long
  ) extends Event
  case class Departure(
      vehicleId: String,
      routeId: String,
      tripId: String,
      directionId: Int,
      stopId: String,
      timestamp: Long
  ) extends Event

  def apply(): Behavior[Update] = {
    handle()
  }

  private def handle(
      state: Map[String, VehiclePosition] = Map()
  ): Behavior[Update] =
    Behaviors.receiveMessage { message =>
      val vehicleId = message.vehiclePosition.vehicleId
      val currentState = Some(message.vehiclePosition)
      val previousState = state.get(vehicleId)
      (currentState, previousState) match {
        case (_, None) => {
          message.replyTo ! List.empty
          handle(state + (vehicleId -> message.vehiclePosition))
        }
        case (Some(currentVP), Some(previousVP))
            if currentVP.stopId == previousVP.stopId && currentVP.stopStatus == previousVP.stopStatus =>
          message.replyTo ! List.empty
          if (
            currentVP.latitude != previousVP.latitude || currentVP.longitude != previousVP.longitude
          ) {
            handle(state + (vehicleId -> message.vehiclePosition))
          } else {
            handle(state)
          }
        case (Some(currentVP), Some(previousVP)) => {
          (currentVP.stopStatus, previousVP.stopStatus) match {
            case ("STOPPED_AT", "IN_TRANSIT_TO" | "INCOMING_AT") =>
              message.replyTo ! List(
                Arrival(
                  vehicleId = currentVP.vehicleId,
                  routeId = currentVP.routeId,
                  tripId = currentVP.tripId,
                  directionId = currentVP.routeDirection,
                  stopId = currentVP.stopId,
                  timestamp = currentVP.timestamp
                )
              )
            case ("IN_TRANSIT_TO", "STOPPED_AT") =>
              message.replyTo ! List(
                Departure(
                  vehicleId = currentVP.vehicleId,
                  routeId = currentVP.routeId,
                  tripId = currentVP.tripId,
                  directionId = currentVP.routeDirection,
                  stopId = previousVP.stopId,
                  timestamp = currentVP.timestamp
                )
              )
            case ("IN_TRANSIT_TO", _) =>
              message.replyTo ! List(
                Arrival(
                  vehicleId = previousVP.vehicleId,
                  routeId = previousVP.routeId,
                  tripId = previousVP.tripId,
                  directionId = previousVP.routeDirection,
                  stopId = previousVP.stopId,
                  timestamp = previousVP.timestamp
                ),
                Departure(
                  vehicleId = currentVP.vehicleId,
                  routeId = currentVP.routeId,
                  tripId = currentVP.tripId,
                  directionId = currentVP.routeDirection,
                  stopId = previousVP.stopId,
                  timestamp = currentVP.timestamp
                )
              )
            case _ =>
              message.replyTo ! List.empty
          }
          handle(state + (vehicleId -> message.vehiclePosition))
        }
      }
      handle(state + (vehicleId -> message.vehiclePosition))
    }
}
