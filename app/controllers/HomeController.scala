package controllers

import javax.inject._

import common.DatabaseHelper._
import org.postgresql.util.PSQLException
import play.api.Logger
import play.api.db.DBApi
import play.api.mvc._
import repositories.{Row, SimpleRepository}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json

import scala.concurrent.Future

@Singleton
class HomeController @Inject()(dbApi: DBApi, simpleRepository: SimpleRepository) extends Controller {

  private val database = dbApi.database("default")

  def test = Action.async {

    database.withConnectionFuture { implicit connection =>
      simpleRepository.cleanup()
    }.flatMap { _ =>

      val eventualRows: Future[Seq[Row]] = database.withTransactionFuture { implicit connection =>

        val insertResults = for {i <- 1 to 500}
          yield simpleRepository.create(s"row $i")

        Future.sequence(insertResults).flatMap{_ =>
          Logger.info(s"\t\t\tNo errors. See what we have in the table")
          simpleRepository.findAll
        }

      }.recoverWith {
        case e: PSQLException =>
          Logger.error(s"\t\t\tCatch the SQl exception and see what we have in the table", e)
          database.withConnectionFuture { implicit connection =>
            simpleRepository.findAll
          }
      }

      eventualRows

    }.map { rows => Ok(Json.toJson(rows)) }

  }
}
