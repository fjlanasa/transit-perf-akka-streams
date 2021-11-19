package flows

import akka.NotUsed
import akka.actor.typed.ActorRef
import events.{StopStatusEvent, DwellEvent}
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorFlow}

import scala.concurrent.duration.DurationInt

object DwellEventFlow {
  def apply(actor: ActorRef[DwellEvent.Update]): Flow[StopStatusEvent.Event, DwellEvent.Result, NotUsed] = {
    implicit val timeout: akka.util.Timeout = 1.second
    ActorFlow.ask(ref = actor)(makeMessage = (el: StopStatusEvent.Event, replyTo: ActorRef[DwellEvent.Result]) => {
      DwellEvent.Update(el, replyTo)})
  }
}

