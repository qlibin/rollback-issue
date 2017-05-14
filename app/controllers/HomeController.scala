package controllers

import javax.inject._

import org.scalameter.{Key, Warmer, _}
import play.api.libs.json.Json
import play.api.mvc._
import services.SimpleService

import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class HomeController @Inject()(simpleService: SimpleService) extends Controller {

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 15,
    Key.exec.maxWarmupRuns -> 20,
    Key.exec.benchRuns -> 30,
    Key.verbose -> true
  ) withWarmer new Warmer.Default

  def benchmark = Action {

    simpleService.cleanup()
    Thread.sleep(1500)

    val asyncExecTime = standardConfig measure {
      Await.result(simpleService.doAsyncOperations(), 20 seconds)
    }

    simpleService.cleanup()
    Thread.sleep(1500)

    val syncExecTime = standardConfig measure {
      Await.result(simpleService.doSyncOperations(), 20 seconds)
    }

    simpleService.cleanup()
    Thread.sleep(1500)

    val seqExecTime = standardConfig measure {
      simpleService.doSequentialOperations()
    }

    val syncSpeedup = asyncExecTime/syncExecTime
    val seqSpeedup = asyncExecTime/seqExecTime

    Ok(Json.obj(
      "asyncExecTime" -> asyncExecTime.toString(),
      "syncExecTime" -> syncExecTime.toString(),
      "seqExecTime" -> seqExecTime.toString(),
      "sync-speedup" -> syncSpeedup,
      "seq-speedup" -> seqSpeedup
    ))

  }

}
