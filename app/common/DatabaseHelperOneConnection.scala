package common

import play.api.db.Database

import scala.concurrent.Future
import scala.util.control.ControlThrowable
import common.ConnectionWithThread._


object DatabaseHelperOneConnection {

  /**
    * That's just a Future'ized version of
    * https://github.com/playframework/playframework/blob/master/framework/src/play-jdbc/src/main/scala/play/api/db/Databases.scala
    */
  implicit class DatabaseAware(database: Database) {

    def withConnectionFuture[A](block: ConnectionWithThread => Future[A]): Future[A] = {
      withConnectionFuture(autocommit = true)(block)
    }

    def withConnectionFuture[A](autocommit: Boolean = true)(block: ConnectionWithThread => Future[A]): Future[A] = {
      val connection = database.getConnection(autocommit)
      implicit val connectionWithThread = ConnectionWithThread(connection)

      val resultFuture = block(connectionWithThread)

      resultFuture.map { result =>
        debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Close connection normally (connection: $connection)")
        connection.close()
        debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Close connection normally (connection: $connection)")
        result
      }.recoverWith {
        case e: Throwable =>
          debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Close connection after exception (connection: $connection)")
          connection.close()
          debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Close connection after exception (connection: $connection)")
          Future.failed(e)
      }
    }

    def withTransactionFuture[A](block: ConnectionWithThread => Future[A]): Future[A] = {
      withConnectionFuture(autocommit = false) { implicit connectionWithThread =>
        val resultFuture = block(connectionWithThread)

        resultFuture.transform(result => {
          debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Commit transaction normally (connection: ${connectionWithThread.connection})")
          connectionWithThread.connection.commit()
          debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Commit transaction normally (connection: ${connectionWithThread.connection})")
          result
        }, {
          case e: ControlThrowable =>
            debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Commit transaction after ControlThrowable (connection: ${connectionWithThread.connection})")
            connectionWithThread.connection.commit()
            debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Commit transaction after ControlThrowable (connection: ${connectionWithThread.connection})")
            e
          case e: Throwable =>
            debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tStart: Rollback after an SQL exception (connection: ${connectionWithThread.connection})")
            connectionWithThread.connection.rollback()
            debug(s" ${Thread.currentThread().getName}\t${System.nanoTime()} \tDONE: Rollback after an SQL exception (connection: ${connectionWithThread.connection})")
            e
        })
      }
    }
  }

  private def debug(msg: String) = {
//    println(msg)
  }

}
