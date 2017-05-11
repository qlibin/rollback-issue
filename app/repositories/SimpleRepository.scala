package repositories

import java.sql.Connection
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Singleton

import anorm._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SimpleRepository {

  val counter: AtomicInteger = new AtomicInteger(0)

  def create(text: String)(implicit connection: Connection, ec: ExecutionContext): Future[Int] = Future {

    val thread = Thread.currentThread().getName
    val simpleNumber = counter.incrementAndGet()

    println(s" $thread\t${System.nanoTime()} \t\tTry to insert `$text` (connection: $connection)")

    if (simpleNumber % 33 == 0) {

      println(s" $thread\t${System.nanoTime()} \t\t\tGenerate an SQL exception for `$text` (connection: $connection)")

      generateSqlException(text)

    } else {

      try {
        val res = insert(text, thread)

        println(s" $thread\t${System.nanoTime()} \t\t\t\tInserted `$text` (connection: $connection)")

        res
      } catch {
        case e: Throwable =>
          println(s" $thread\t${System.nanoTime()} \t\t\t\tException for `$text` (connection: $connection) (exception: ${e.getMessage})")
          throw e
      }

    }
  }

  private def generateSqlException(text: String)(implicit connection: Connection): Int = {
    SQL"""
         INSERT INTO test_table(qqq)
         VALUES("generate an SQL error for Simple `$text`")
      """.executeUpdate()
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

  def findAll(implicit connection: Connection, ec: ExecutionContext): Future[Seq[Row]] = Future {
    SQL"""
         SELECT * FROM test_table ORDER BY created
      """.as(rowParser.*)
  }

  def cleanup()
            (implicit connection: Connection, ec: ExecutionContext): Future[Boolean] = Future {
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

