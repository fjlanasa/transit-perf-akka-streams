package dwell

import stopstatus.StopStatusEvent
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object DwellEvent {
  case class Update(
      event: StopStatusEvent.Event,
      replyTo: ActorRef[Seq[DwellEvent.Event]]
  )

  case class Event(
      arrival: StopStatusEvent.Event,
      departure: StopStatusEvent.Event
  )

  def apply(): Behavior[Update] = {
    handle()
  }

  def handle(
      state: Map[(String, String, String), StopStatusEvent.Event] = Map()
  ): Behavior[Update] = {
    Behaviors.receiveMessage { message =>
      message match {
        case Update(event: StopStatusEvent.Arrival, replyTo) =>
          val vp = event
          replyTo ! List.empty
          handle(state + ((vp.vehicleId, vp.stopId, vp.tripId) -> event))
        case Update(event: StopStatusEvent.Departure, replyTo) =>
          val currentVP = event
          val previousVP = state.get(getKey(currentVP))
          previousVP match {
            case Some(vp) =>
              replyTo ! List(Event(arrival = vp, departure = currentVP))
              handle(state - getKey(currentVP))
            case _ =>
              replyTo ! List.empty
              handle(state)
          }

      }
    }
  }

  private def getKey(vp: StopStatusEvent.Departure): (String, String, String) =
    (vp.vehicleId, vp.stopId, vp.tripId)
}
