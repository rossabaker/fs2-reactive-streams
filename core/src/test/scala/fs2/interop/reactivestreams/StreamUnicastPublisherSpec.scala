package fs2
package interop
package reactivestreams

import java.util.concurrent.Executors

import cats.effect._
import org.reactivestreams._
import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.scalatest.testng.TestNGSuiteLike

import scala.concurrent.ExecutionContext

class FailedSubscription(sub: Subscriber[_]) extends Subscription {
  def cancel(): Unit = {}
  def request(n: Long): Unit = {}
}

class FailedPublisher extends Publisher[Int] {
  def subscribe(subscriber: Subscriber[_ >: Int]): Unit = {
    subscriber.onSubscribe(new FailedSubscription(subscriber))
    subscriber.onError(new Error("BOOM"))
  }
}

class StreamUnicastPublisherSpec extends PublisherVerification[Int](new TestEnvironment(1000L)) with TestNGSuiteLike {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  def createPublisher(n: Long): StreamUnicastPublisher[IO, Int] = {
    val s =
      if (n == java.lang.Long.MAX_VALUE) Stream.range(1, 20).repeat
      else Stream(1).repeat.scan(1)(_ + _).map(i => if (i > n) None else Some(i)).unNoneTerminate

    s.covary[IO].toUnicastPublisher()
  }

  def createFailedPublisher(): FailedPublisher = new FailedPublisher()
}
