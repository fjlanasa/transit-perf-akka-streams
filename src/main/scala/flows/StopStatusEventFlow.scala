package flows

import akka.NotUsed
import akka.actor.typed.ActorRef
import events.StopStatusEvent
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorFlow}
import com.google.transit.realtime.GtfsRealtime.FeedEntity

import scala.concurrent.duration.DurationInt

object StopStatusEventFlow {
  def apply(actor: ActorRef[StopStatusEvent.Update]): Flow[FeedEntity, StopStatusEvent.Result, NotUsed] = {
    implicit val timeout: akka.util.Timeout = 1.second
    ActorFlow.ask(ref = actor)(makeMessage = (el: FeedEntity, replyTo: ActorRef[StopStatusEvent.Result]) => {
      StopStatusEvent.Update(el.getVehicle(), replyTo)})
  }
}
