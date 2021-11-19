package events

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object DwellEvent {
  case class Update(event: StopStatusEvent.Event, replyTo: ActorRef[DwellEvent.Result])

  trait Result
  case class Event(arrival: StopStatusEvent.ParsedVehiclePosition, departure: StopStatusEvent.ParsedVehiclePosition) extends Result
  case object Noop extends Result

  def apply(): Behavior[Update] = {
    handle()
  }

  def handle(state: Map[(String, String, String), StopStatusEvent.ParsedVehiclePosition] = Map()): Behavior[Update] = {
    Behaviors.receiveMessage { message =>
      message match {
        case Update(event: StopStatusEvent.Arrival, replyTo) =>
          val vp = event.vehiclePosition
          replyTo ! Noop
          handle(state + ((vp.vehicleId, vp.stopId, vp.tripId) -> event.vehiclePosition))
        case Update(event: StopStatusEvent.Departure, replyTo) =>
          val currentVP = event.vehiclePosition
          val previousVP = state.get(getKey(currentVP))
          previousVP match {
            case Some(vp) =>
              replyTo ! Event(arrival = vp, departure = currentVP)
              handle(state - getKey(currentVP))
            case _ =>
              replyTo ! Noop
          }

      }
      Behaviors.same
    }
  }

  private def getKey(vp: StopStatusEvent.ParsedVehiclePosition): (String, String, String) = (vp.vehicleId, vp.stopId, vp.tripId)
}
