package headway

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorFlow}
import scala.concurrent.duration.DurationInt

import headway.HeadwayEvent
import stopstatus.StopStatusEvent
import akka.stream.scaladsl.Source

object HeadwayEventFlow {
  def apply(
      actor: ActorRef[HeadwayEvent.Update]
  ): Flow[StopStatusEvent.Event, HeadwayEvent.Event, NotUsed] = {
    implicit val timeout: akka.util.Timeout = 1.second
    ActorFlow
      .ask(ref = actor)(makeMessage =
        (
            el: StopStatusEvent.Event,
            replyTo: ActorRef[Seq[HeadwayEvent.Event]]
        ) => {
          HeadwayEvent.Update(el, replyTo)
        }
      )
      .flatMapConcat(x => Source(x))
  }
}
