package traveltime

import akka.actor.ActorRef

import stopstatus.StopStatusEvent

object TravelTimeEvent {
  case class Update(event: StopStatusEvent.Event, replyTo: ActorRef)

  case class Event()
}
