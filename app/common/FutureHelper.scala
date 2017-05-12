package common

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object FutureHelper {

  case class LiftedExceptions(msg: String, exceptions: Seq[Throwable]) extends Exception(msg)

  private def lift[T](futures: Seq[Future[T]])
                     (implicit executor: ExecutionContext) =
    futures.map(_.map { Success(_) }.recover { case t => Failure(t) }) // wrap all futures' results with Try

  private def liftedSequence[T](futures: Seq[Future[T]])
                       (implicit executor: ExecutionContext): Future[Seq[Try[T]]] =
    Future.sequence(lift(futures)) // having neutralized exception completions through the lifting, .sequence can now be used

  def ensureToComplete[T](futures: Seq[Future[T]])
                         (implicit executor: ExecutionContext): Future[Seq[T]] =
    liftedSequence(futures).map { (results: Seq[Try[T]]) =>
      results.partition(_.isFailure) match {
        case (exceptions, successResults) if exceptions.isEmpty => successResults.map(_.get)
        case (exceptions, _) =>
          throw LiftedExceptions("Failed futures from sequence", exceptions.map(_.asInstanceOf[Failure[T]].exception))
      }
    }
}
