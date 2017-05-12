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

  def waitAll[T](futures: Seq[Future[T]]) =
    Future.sequence(lift(futures)) // having neutralized exception completions through the lifting, .sequence can now be used

  def test = Action.async {

    database.withConnectionFuture { implicit connection =>
      simpleRepository.cleanup()
    }.flatMap { _ =>

      val eventualRows: Future[Seq[Row]] = database.withTransactionFuture { implicit connection =>

        val insertResults: IndexedSeq[Future[Int]] =
          for {i <- 1 to 500} yield simpleRepository.create(s"row $i")

        waitAll(insertResults).flatMap{ (results: Seq[Try[Int]]) =>
          val error: Option[Try[Int]] = results.find(_.isFailure) // find first exception
          error match {
            case Some(Failure(e)) => throw e // throw it so that DatabaseHelper could rollback the transaction
            case _ => // do something on success
              println(s"${Thread.currentThread().getName} \t\t\tNo errors. See what we have in the table")
              simpleRepository.findAll
          }
        }

      }.recoverWith {
        case e: PSQLException =>
          println(s"${Thread.currentThread().getName} \t\t\tCatch the SQl exception (exception: ${e.getMessage}) and see what we have in the table after the exception")
          database.withConnectionFuture { implicit connectionWithThread =>
            simpleRepository.findAll
          }
      }

      eventualRows

    }.map { rows => Ok(Json.toJson(rows)) }

  }
}
