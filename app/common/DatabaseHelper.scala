package common

import java.sql.Connection

import play.api.Logger
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
        println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Close connection normally (connection: $connection)")
        connection.close()
        println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Close connection normally (connection: $connection)")
        result
      }.recoverWith {
        case e: Throwable =>
          println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Close connection after exception (connection: $connection)")
          connection.close()
          println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Close connection after exception (connection: $connection)")
          Future.failed(e)
      }
    }

    def withTransactionFuture[A](block: Connection => Future[A])(implicit ec: ExecutionContext): Future[A] = {
      withConnectionFuture(autocommit = false) { implicit connection =>
        val resultFuture = block(connection)

        resultFuture.transform(result => {
          println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Commit transaction normally (connection: $connection)")
          connection.commit()
          println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Commit transaction normally (connection: $connection)")
          result
        }, {
          case e: ControlThrowable =>
            println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Commit transaction after ControlThrowable (connection: $connection)")
            connection.commit()
            println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Commit transaction after ControlThrowable (connection: $connection)")
            e
          case e: Throwable =>
            println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Rollback after an SQL exception (connection: $connection)")
            connection.rollback()
            println(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Rollback after an SQL exception (connection: $connection)")
            e
        })
      }
    }
  }

}
