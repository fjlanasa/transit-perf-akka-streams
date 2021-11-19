import akka.stream.scaladsl.{Broadcast, GraphDSL, RunnableGraph, Sink}
import akka.NotUsed
import akka.actor.typed.{Behavior, ActorSystem => TypedActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.ClosedShape
import com.google.transit.realtime.GtfsRealtime.FeedEntity
import sources.FeedEntitySource
import events.{DwellEvent, HeadwayEvent, StopStatusEvent}
import flows.{DwellEventFlow, HeadwayEventFlow, StopStatusEventFlow}

object Main extends App {
  def apply(): Behavior[NotUsed] = {
    Behaviors.setup { context =>
      implicit val system = context.system

      val stopStatusActor = context.spawn(StopStatusEvent(), "stop-status-event")
      val dwellEventActor = context.spawn(DwellEvent(), "dwell-event")
      val headwayEventActor = context.spawn(HeadwayEvent(), "headway-event")

      val graph = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        val feedEntitySource = builder.add(FeedEntitySource("https://cdn.mbta.com/realtime/VehiclePositions.pb"))
        val feedEntityBroadcast = builder.add(Broadcast[FeedEntity](2))
        val feedEntitySink = builder.add(Sink.foreach[FeedEntity]((x: FeedEntity) => println(s"Feed Entity $x")))

        val stopStatusFlow = builder.add(StopStatusEventFlow(stopStatusActor).collect {
          case result: StopStatusEvent.Event => result
        })
        val stopStatusBroadcast = builder.add(Broadcast[StopStatusEvent.Event](3))
        val stopStatusSink = builder.add(Sink.foreach[StopStatusEvent.Event]((x: StopStatusEvent.Event) => println(s"Stop Status Event: $x")))

        val dwellEventFlow = builder.add(DwellEventFlow(dwellEventActor).collect {
          case result: DwellEvent.Event => result
        })
        val dwellEventSink = builder.add(Sink.foreach[DwellEvent.Event]((x: DwellEvent.Event) => println(s"Dwell Event: $x")))

        val headwayEventFlow = builder.add(HeadwayEventFlow(headwayEventActor).collect {
          case result: HeadwayEvent.Event => result
        })
        val headwayEventSink = builder.add(Sink.foreach[HeadwayEvent.Event]((x: HeadwayEvent.Event) => println(s"Headway Event: $x")))

        feedEntitySource ~> feedEntityBroadcast ~> feedEntitySink
        feedEntityBroadcast ~> stopStatusFlow ~> stopStatusBroadcast
        stopStatusBroadcast ~> stopStatusSink
        stopStatusBroadcast ~> dwellEventFlow ~> dwellEventSink
        stopStatusBroadcast ~> headwayEventFlow ~> headwayEventSink

        ClosedShape
      }
      RunnableGraph.fromGraph(graph).run()
      Behaviors.empty
    }
  }

  TypedActorSystem(Main(), "Main")

}