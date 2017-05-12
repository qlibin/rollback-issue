package controllers

import javax.inject._

import common.DatabaseHelper._
import play.api.db.DBApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc._
import repositories.SimpleRepository

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


@Singleton
class HomeController @Inject()(dbApi: DBApi, simpleRepository: SimpleRepository) extends Controller {

  private val database = dbApi.database("default")

  private def lift[T](futures: Seq[Future[T]]) =
    futures.map(_.map { Success(_) }.recover { case t => Failure(t) })

  def waitAll[T](futures: Seq[Future[T]]) =
    Future.sequence(lift(futures)) // having neutralized exception completions through the lifting, .sequence can now be used

  def test = Action.async {

    val connection = database.getConnection(autocommit = false)
    println(s"\n\n\nSimple rollback test BEGIN")
    Await.ready(simpleRepository.create(s"row 1")(connection), 20 seconds)
    Await.ready(simpleRepository.create(s"row 2")(connection), 20 seconds)
//    connection.rollback() // in HikariProxyConnection this thing resets dirty state of connection
    Await.ready(simpleRepository.create(s"row 3 after rollback")(connection), 20 seconds)
    connection.close() // in HikariProxyConnection this does rollback if connection in dirty state
    // this means if we call rollback before that HikariProxyConnection will not rollback during close
    println(s"\n\n\nSimple rollback test END")

    database.withConnectionFuture { implicit connection =>
      simpleRepository.findAll
    }.map { rows => Ok(Json.toJson(rows)) }

  }
}
