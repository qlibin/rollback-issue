package common

import java.util.concurrent.{Executor, Executors}
import java.util.concurrent.atomic.AtomicInteger

import basic.TestHelpers._
import common.FutureHelper.LiftedExceptions
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}

class FutureHelperSpec extends PlaySpec with MockitoSugar {

  if (Runtime.getRuntime.availableProcessors > 1) {

    // Following tests make sense only when we have more than 1 thread available

    "FutureHelper.ensureToComplete with 4 threads in execution context" should {

      implicit val ec_4_threads = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

      "work as Future.sequence in case of all futures succeeded" in {

        val result = FutureHelper.ensureToComplete(Seq(
          Future(1),
          Future(2),
          Future(3),
          Future(4)
        ))

        result.await() mustBe Seq(1, 2, 3, 4)
      }

      "wait until all futures succeeded in case one of them fail" in {

        val counter = new AtomicInteger(0)
        val result = FutureHelper.ensureToComplete(Seq(
          Future(counter.addAndGet(1)),
          Future.failed(new Exception("error")),
          Future(counter.addAndGet(3)),
          Future {
            Thread.sleep(100)
            counter.addAndGet(4)
          }
        ))

        val error = result.failed.await()
        counter.get() mustBe 8
        error.isInstanceOf[LiftedExceptions] mustBe true

        val liftedExceptions = error.asInstanceOf[LiftedExceptions]
        liftedExceptions.exceptions must not be empty
        liftedExceptions.exceptions.head.getMessage mustBe "error"
      }
    }

    // We need these tests to prove that Future.sequence
    // doesn't wait until all futures completed in multithreaded environment
    // and therefore we need FutureHelper.ensureToComplete functionality
    "Future.sequence with 4 threads in execution context" should {

      implicit val ec_4_threads = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

      "work in case of all futures succeeded" in {

        val result = Future.sequence(Seq(
          Future(1),
          Future(2),
          Future(3),
          Future(4)
        ))

        result.await() mustBe Seq(1, 2, 3, 4)
      }

      "should NOT wait fail until all futures succeeded in case one of them fail" in {

        val counter = new AtomicInteger(0)
        val result = Future.sequence(Seq(
          Future(counter.addAndGet(1)),
          Future.failed(new Exception("error")),
          Future(counter.addAndGet(3)),
          Future {
            Thread.sleep(100)
            counter.addAndGet(4)
          }
        ))

        val error = result.failed.await()
        counter.get() < 8 mustBe true
        error.isInstanceOf[Exception] mustBe true
        error.getMessage mustBe "error"
      }
    }

  }

  "FutureHelper.ensureToComplete with single thread execution context" should {

    implicit val ec_single_thread = ExecutionContext.fromExecutor(new Executor {
      override def execute(task: Runnable): Unit = task.run()
    })

    "work as Future.sequence in case of all futures succeeded" in {

      val result = FutureHelper.ensureToComplete(Seq(
        Future(1),
        Future(2),
        Future(3),
        Future(4)
      ))

      result.await() mustBe Seq(1, 2, 3, 4)
    }

    "wait until all futures succeeded in case one of them fail" in {

      val counter = new AtomicInteger(0)
      val result = FutureHelper.ensureToComplete(Seq(
        Future(counter.addAndGet(1)),
        Future.failed(new Exception("error")),
        Future(counter.addAndGet(3)),
        Future{
          Thread.sleep(100)
          counter.addAndGet(4)
        }
      ))

      val error = result.failed.await()
      counter.get() mustBe 8
      error.isInstanceOf[LiftedExceptions] mustBe true

      val liftedExceptions = error.asInstanceOf[LiftedExceptions]
      liftedExceptions.exceptions must not be empty
      liftedExceptions.exceptions.head.getMessage mustBe "error"
    }
  }

  "Future.sequence with single thread execution context" should {

    implicit val ec_single_thread = ExecutionContext.fromExecutor(new Executor {
      override def execute(task: Runnable): Unit = task.run()
    })

    "work in case of all futures succeeded" in {

      val result = Future.sequence(Seq(
        Future(1),
        Future(2),
        Future(3),
        Future(4)
      ))

      result.await() mustBe Seq(1, 2, 3, 4)
    }

    "should wait fail until all futures succeeded in case one of them fail" in {

      val counter = new AtomicInteger(0)
      val result = Future.sequence(Seq(
        Future(counter.addAndGet(1)),
        Future.failed(new Exception("error")),
        Future(counter.addAndGet(3)),
        Future{Thread.sleep(100);counter.addAndGet(4)}
      ))

      val error = result.failed.await()
      counter.get() mustBe 8
      error.isInstanceOf[Exception] mustBe true
      error.getMessage mustBe "error"
    }
  }
}