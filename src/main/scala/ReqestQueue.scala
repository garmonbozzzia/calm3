import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{OverflowStrategy, QueueOfferResult}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

import Inits._

object ReqestQueue {
  val poolClientFlow = Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]]("calm.dhamma-eu.org")
  val queue =
    Source.queue[(HttpRequest, Promise[HttpResponse])](4, OverflowStrategy.dropNew)
      .via(poolClientFlow)
      .toMat(Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p))    => p.failure(e)
      }))(Keep.left)
      .run()
  def apply(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap {
      case QueueOfferResult.Enqueued    => responsePromise.future
      case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }
}
