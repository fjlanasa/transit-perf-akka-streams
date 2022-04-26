package headway

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import stopstatus.StopStatusEvent
import vehicleposition.VehiclePositionSource.VehiclePosition

object HeadwayEvent {
  case class Update(event: StopStatusEvent.Event, replyTo: ActorRef[Seq[Event]])

  case class Event(
      previousVP: StopStatusEvent.Event,
      currentVP: StopStatusEvent.Event
  )

  def apply(): Behavior[Update] = {
    handle()
  }

  def handle(
      state: Map[(String, Int, String), StopStatusEvent.Event] = Map()
  ): Behavior[Update] = {
    Behaviors.receiveMessage { message =>
      message match {
        case Update(event: StopStatusEvent.Arrival, replyTo) =>
          val previousVP = state.get(getKey(event))
          previousVP match {
            case Some(vp) =>
              replyTo ! List(Event(previousVP = vp, currentVP = event))
            case None =>
              replyTo ! List.empty
          }
          handle(state + (getKey(event) -> event))
        case Update(_: StopStatusEvent.Departure, replyTo) =>
          replyTo ! List.empty
          handle()
      }
    }
  }

  private def getKey(vp: StopStatusEvent.Arrival) =
    (vp.routeId, vp.directionId, vp.stopId)
}
