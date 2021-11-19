package sources

import akka.NotUsed
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import com.google.transit.realtime.GtfsRealtime.{FeedEntity, FeedMessage}

import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}

object FeedEntitySource {
  implicit val ec = ExecutionContext.global

  private def poll(fileLocation: String) = Future {
    val url = new URL(fileLocation)
    val message = FeedMessage.parseFrom(url.openStream())
    message.getEntityList().asScala.toList
  }

  def apply(url: String): Source[FeedEntity, Cancellable] = {
    Source.tick(
      0.second,
      1.second,
      NotUsed
    ).mapAsync(1) { _ =>
      poll(url)
    }.flatMapConcat((x: List[FeedEntity]) => {
      Source(x)
    })
  }
}
