package flows

import akka.NotUsed
import akka.actor.typed.ActorRef
import events.{HeadwayEvent, StopStatusEvent}
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorFlow}

import scala.concurrent.duration.DurationInt

object HeadwayEventFlow {
  def apply(actor: ActorRef[HeadwayEvent.Update]): Flow[StopStatusEvent.Event, HeadwayEvent.Result, NotUsed] = {
    implicit val timeout: akka.util.Timeout = 1.second
    ActorFlow.ask(ref = actor)(makeMessage = (el: StopStatusEvent.Event, replyTo: ActorRef[HeadwayEvent.Result]) => {
      HeadwayEvent.Update(el, replyTo)})
  }
}
