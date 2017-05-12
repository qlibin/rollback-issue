package controllers

import javax.inject._

import common.DatabaseHelper._
import common.FutureHelper
import common.FutureHelper._
import play.api.db.DBApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import repositories.{Row, SimpleRepository}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future

@Singleton
class HomeController @Inject()(dbApi: DBApi, simpleRepository: SimpleRepository) extends Controller {

  private val database = dbApi.database("default")

  def test = Action.async {

    database.withConnectionFuture { implicit connection =>
      simpleRepository.cleanup()
    }.flatMap { _ =>

      val eventualRows: Future[Seq[Row]] = database.withTransactionFuture { implicit connection =>

        val insertResults: IndexedSeq[Future[Int]] =
          for {i <- 1 to 500} yield simpleRepository.create(s"row $i")

        FutureHelper.ensureToComplete(insertResults).flatMap{ _ =>
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
