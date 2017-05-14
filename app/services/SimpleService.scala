package services

import javax.inject.Inject

import common.FutureHelper
import play.api.db.DBApi
import repositories.{AsyncRepository, SequentialRepository, SyncRepository}

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future

class SimpleService @Inject() (dbApi: DBApi,
                               asyncRepository: AsyncRepository,
                               syncRepository: SyncRepository,
                               seqRepository: SequentialRepository) {

  private val database = dbApi.database("default")

  def doAsyncOperations(): Future[Seq[Int]] = {

    import common.DatabaseHelper._
    import play.api.libs.concurrent.Execution.Implicits._

    database.withTransactionFuture { implicit connection =>

      val insertResults: IndexedSeq[Future[Int]] =
        for {i <- 1 to 500} yield asyncRepository.create(s"row $i")

      FutureHelper.ensureToComplete(insertResults)

    }
  }

  def doSyncOperations(): Future[Seq[Int]] = {

    import common.ConnectionWithThread._
    import common.DatabaseHelperOneConnection._
    import play.api.libs.concurrent.Execution.defaultContext

    database.withTransactionFuture { implicit connectionWithThread =>

      val insertResults: IndexedSeq[Future[Int]] =
        for {i <- 1 to 500} yield syncRepository.create(s"row $i")

      FutureHelper.ensureToComplete(insertResults)(defaultContext)

    }
  }

  def doSequentialOperations(): Seq[Int] = {

    database.withTransaction { implicit connection =>

      val insertResults: IndexedSeq[Int] =
        for {i <- 1 to 500} yield seqRepository.create(s"row $i")

      insertResults

    }
  }

  def cleanup() = {
    database.withConnection { implicit connection =>
      seqRepository.cleanup()
    }
  }
}
