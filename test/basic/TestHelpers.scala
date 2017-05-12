package basic

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object TestHelpers {

  implicit class AwaitableFuture[T](future: Future[T]) {
    def await() = TestHelpers.await(future)
  }

  def await[T](future: Future[T]): T = Await.result(future, 20 seconds)

}

