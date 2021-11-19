package events

import akka.actor.ActorRef

object TravelTimeEvent {
  case class Update(event: StopStatusEvent.Event, replyTo: ActorRef)

  trait Result
  case class Event() extends Result
  case object Noop extends Result
}
