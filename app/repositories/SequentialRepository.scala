package repositories

import java.sql.Connection
import javax.inject.Singleton

import anorm._

@Singleton
class SequentialRepository {

  private def debug(msg: String) = {
    //    println(msg)
  }

  def create(text: String)(implicit connection: Connection): Int = {

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

  private val rowParser = Macro.namedParser[Row]

  def findAll(implicit connection: Connection): Seq[Row] = {
    SQL"""
         SELECT * FROM test_table ORDER BY created
      """.as(rowParser.*)
  }

  def cleanup()
            (implicit connection: Connection): Boolean = {
    SQL"""
       CREATE TABLE IF NOT EXISTS test_table (
         id                     SERIAL       PRIMARY KEY NOT NULL,
         text                   TEXT         NOT NULL,
         created                TIMESTAMP    NOT NULL DEFAULT now(),
         created_by             TEXT         NOT NULL
       );
       DELETE FROM test_table
       """.execute()
  }
}

