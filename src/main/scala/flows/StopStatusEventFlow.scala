package flows

import akka.NotUsed
import akka.actor.typed.ActorRef
import events.StopStatusEvent
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorFlow}
import sources.VehiclePositionSource.VehiclePosition

import scala.concurrent.duration.DurationInt

object StopStatusEventFlow {
  def apply(
      actor: ActorRef[StopStatusEvent.Update]
  ): Flow[VehiclePosition, Seq[StopStatusEvent.Event], NotUsed] = {
    implicit val timeout: akka.util.Timeout = 1.second
    ActorFlow.ask(ref = actor)(makeMessage =
      (el: VehiclePosition, replyTo: ActorRef[Seq[StopStatusEvent.Event]]) => {
        StopStatusEvent.Update(el, replyTo)
      }
    )
  }
}
