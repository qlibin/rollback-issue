package repositories

import java.sql.Connection
import javax.inject.Singleton

import anorm._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

@Singleton
class AsyncRepository {

  private def debug(msg: String) = {
    //    println(msg)
  }

  def create(text: String)(implicit connection: Connection): Future[Int] = Future {

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

