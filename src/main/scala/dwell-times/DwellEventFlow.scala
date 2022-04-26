package dwell

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.ActorFlow
import scala.concurrent.duration.DurationInt

import dwell.DwellEvent
import stopstatus.StopStatusEvent
import akka.stream.scaladsl.Source

object DwellEventFlow {
  def apply(
      actor: ActorRef[DwellEvent.Update]
  ): Flow[StopStatusEvent.Event, DwellEvent.Event, NotUsed] = {
    implicit val timeout: akka.util.Timeout = 1.second
    ActorFlow
      .ask(ref = actor)(makeMessage =
        (el: StopStatusEvent.Event, replyTo: ActorRef[Seq[DwellEvent.Event]]) =>
          {
            DwellEvent.Update(el, replyTo)
          }
      )
      .flatMapConcat(x => Source(x))
  }
}
