package stopstatus

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorFlow}

import stopstatus.StopStatusEvent
import vehicleposition.VehiclePositionSource.VehiclePosition

import scala.concurrent.duration.DurationInt
import akka.stream.scaladsl.Source

object StopStatusEventFlow {
  def apply(
      actor: ActorRef[StopStatusEvent.Update]
  ): Flow[VehiclePosition, StopStatusEvent.Event, NotUsed] = {
    implicit val timeout: akka.util.Timeout = 1.second
    ActorFlow
      .ask(ref = actor)(makeMessage =
        (el: VehiclePosition, replyTo: ActorRef[Seq[StopStatusEvent.Event]]) =>
          {
            StopStatusEvent.Update(el, replyTo)
          }
      )
      .flatMapConcat(x => Source(x))
  }
}
