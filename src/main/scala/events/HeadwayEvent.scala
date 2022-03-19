package events

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import sources.VehiclePositionSource.VehiclePosition

object HeadwayEvent {
  case class Update(event: StopStatusEvent.Event, replyTo: ActorRef[Result])

  trait Result
  case class Event(previousVP: StopStatusEvent.Event, currentVP: StopStatusEvent.Event) extends Result
  case object Noop extends Result

  def apply(): Behavior[Update] = {
    handle()
  }

  def handle(state: Map[(String, Int, String), StopStatusEvent.Event] = Map()): Behavior[Update] = {
    Behaviors.receiveMessage { message =>
      message match {
        case Update(event: StopStatusEvent.Arrival, replyTo) =>
          val previousVP = state.get(getKey(event))
          previousVP match {
            case Some(vp) =>
              replyTo ! Event(previousVP = vp, currentVP = event)
            case None =>
              replyTo ! Noop
          }
          handle(state + (getKey(event) -> event))
        case Update(_: StopStatusEvent.Departure, replyTo) =>
          replyTo ! Noop
          handle()
      }
    }
  }

  private def getKey(vp: StopStatusEvent.Arrival) = (vp.routeId, vp.directionId, vp.stopId)
}
