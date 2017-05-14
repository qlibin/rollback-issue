package common

import java.sql.Connection
import java.util.concurrent.Executor

import scala.concurrent.ExecutionContext

case class ConnectionWithThread(connection: Connection) {
  val synchronousExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(new Executor {
    override def execute(task: Runnable): Unit = task.run()
  })
}
object ConnectionWithThread {
  implicit def connectionWithThread2ExecutionContext(implicit connectionWithThread: ConnectionWithThread): ExecutionContext = connectionWithThread.synchronousExecutionContext
  implicit def connectionWithThread2Connection(implicit connectionWithThread: ConnectionWithThread): Connection = connectionWithThread.connection
}