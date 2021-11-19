package events

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object HeadwayEvent {
  case class Update(event: StopStatusEvent.Event, replyTo: ActorRef[Result])

  trait Result
  case class Event(previousVP: StopStatusEvent.ParsedVehiclePosition, currentVP: StopStatusEvent.ParsedVehiclePosition) extends Result
  case object Noop extends Result

  def apply(): Behavior[Update] = {
    handle()
  }

  def handle(state: Map[(String, Int, String), StopStatusEvent.ParsedVehiclePosition] = Map()): Behavior[Update] = {
    Behaviors.receiveMessage { message =>
      message match {
        case Update(event: StopStatusEvent.Arrival, replyTo) =>
          val previousVP = state.get(getKey(event.vehiclePosition))
          previousVP match {
            case Some(vp) =>
              replyTo ! Event(previousVP = vp, currentVP = event.vehiclePosition)
            case None =>
              replyTo ! Noop
          }
          handle(state + (getKey(event.vehiclePosition) -> event.vehiclePosition))
        case Update(_: StopStatusEvent.Departure, replyTo) =>
          replyTo ! Noop
          handle()
      }
    }
  }

  private def getKey(vp: StopStatusEvent.ParsedVehiclePosition) = (vp.routeId, vp.routeDirection, vp.stopId)
}
