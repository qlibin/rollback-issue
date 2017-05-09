package common

import java.sql.Connection

import play.api.db.Database

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.ControlThrowable


object DatabaseHelper {

  /**
    * That's just a Future'ized version of
    * https://github.com/playframework/playframework/blob/master/framework/src/play-jdbc/src/main/scala/play/api/db/Databases.scala
    */
  implicit class DatabaseAware(database: Database) {

    def withConnectionFuture[A](block: Connection => Future[A])(implicit ec: ExecutionContext): Future[A] = {
      withConnectionFuture(autocommit = true)(block)
    }

    def withConnectionFuture[A](autocommit: Boolean = true)(block: Connection => Future[A])(implicit ec: ExecutionContext): Future[A] = {
      val connection = database.getConnection(autocommit)

      val resultFuture = block(connection)

      resultFuture.map { result =>
        connection.close()
        result
      }.recoverWith {
        case e: Throwable =>
          connection.close()
          Future.failed(e)
      }
    }

    def withTransactionFuture[A](block: Connection => Future[A])(implicit ec: ExecutionContext): Future[A] = {
      withConnectionFuture(autocommit = false) { implicit connection =>
        val resultFuture = block(connection)

        resultFuture.map { result =>
          connection.commit()
          result
        }.recoverWith {
          case e: ControlThrowable =>
            connection.commit()
            Future.failed(e)
          case e: Throwable =>
            connection.rollback()
            Future.failed(e)
        }
      }
    }
  }

}
