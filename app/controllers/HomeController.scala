package controllers

import javax.inject._

import common.DatabaseHelper._
import org.postgresql.util.PSQLException
import play.api.db.DBApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import repositories.{Row, SimpleRepository}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class HomeController @Inject()(dbApi: DBApi, simpleRepository: SimpleRepository) extends Controller {

  private val database = dbApi.database("default")

  private def lift[T](futures: Seq[Future[T]]) =
    futures.map(_.map { Success(_) }.recover { case t => Failure(t) }) // wrap all futures' results with Try

  def liftedSequence[T](futures: Seq[Future[T]]): Future[Seq[Try[T]]] =
    Future.sequence(lift(futures)) // having neutralized exception completions through the lifting, .sequence can now be used

  case class LiftedExceptions(msg: String, exceptions: Seq[Throwable]) extends Exception(msg)

  def ensureToComplete[T](futures: Seq[Future[T]]): Future[Seq[T]] =
    liftedSequence(futures).map { (results: Seq[Try[T]]) =>
      results.partition(_.isFailure) match {
        case (exceptions, successResults) if exceptions.isEmpty => successResults.map(_.get)
        case (exceptions, _) =>
          throw LiftedExceptions("Failed futures from sequence", exceptions.map(_.asInstanceOf[Failure[T]].exception))
      }
    }

  def test = Action.async {

    database.withConnectionFuture { implicit connection =>
      simpleRepository.cleanup()
    }.flatMap { _ =>

      val eventualRows: Future[Seq[Row]] = database.withTransactionFuture { implicit connection =>

        val insertResults: IndexedSeq[Future[Int]] =
          for {i <- 1 to 500} yield simpleRepository.create(s"row $i")

        ensureToComplete(insertResults).flatMap{ _ =>
          println(s"${Thread.currentThread().getName} \t\t\tNo errors. See what we have in the table")
          simpleRepository.findAll
        }

      }.recoverWith {
        case e: LiftedExceptions =>
          println(s"${Thread.currentThread().getName} \t\t\tCatch the SQl exception (exception: ${e.getMessage}) and see what we have in the table after the exception")
          database.withConnectionFuture { implicit connectionWithThread =>
            simpleRepository.findAll
          }
      }

      eventualRows

    }.map { rows => Ok(Json.toJson(rows)) }

  }
}
