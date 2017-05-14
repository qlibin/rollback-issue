package repositories

import java.sql.Connection
import javax.inject.Singleton

import anorm._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SyncRepository {

  private def debug(msg: String) = {
//    println(msg)
  }

  def create(text: String)(implicit connection: Connection, ec: ExecutionContext): Future[Int] = Future {

    val thread = Thread.currentThread().getName

    debug(s" $thread\t${System.nanoTime()} \t\tTry to insert `$text` (connection: $connection)")

    try {
      val res = insert(text, thread)

      debug(s" $thread\t${System.nanoTime()} \t\t\t\tInserted `$text` (connection: $connection)")

      res
    } catch {
      case e: Throwable =>
        debug(s" $thread\t${System.nanoTime()} \t\t\t\tException for `$text` (connection: $connection) (exception: ${e.getMessage})")
        throw e
    }
  }

  private def insert(text: String, createdBy: String)(implicit connection: Connection): Int = {
    val message = s"$text, connection: $connection"
    SQL"""
         INSERT INTO test_table(
           text,
           created,
           created_by)
         VALUES(
           $message,
           NOW(),
           $createdBy)
      """.executeUpdate()
  }

}
