package pl.touk.nussknacker.engine.demo

import com.typesafe.config.Config
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import pl.touk.nussknacker.engine.api.deployment.{CustomAction, CustomActionError, CustomActionRequest, CustomActionResult, ProcessManager, ProcessStateDefinitionManager}
import pl.touk.nussknacker.engine.ModelData
import pl.touk.nussknacker.engine.api.deployment.simple.SimpleStateStatus._
import pl.touk.nussknacker.engine.management.{FlinkConfig, FlinkProcessStateDefinitionManager, FlinkStreamingProcessManagerProvider, FlinkStreamingRestManager}
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client.{NothingT, SttpBackend}

import scala.concurrent.Future

class DemoProcessManagerProvider extends FlinkStreamingProcessManagerProvider {
  import net.ceedubs.ficus.readers.ArbitraryTypeReader._
  import net.ceedubs.ficus.Ficus._
  import pl.touk.nussknacker.engine.util.config.ConfigEnrichments._

  override def createProcessManager(modelData: ModelData, config: Config): ProcessManager = {
    implicit val backend: SttpBackend[Future, Nothing, NothingT] = AsyncHttpClientFutureBackend.usingConfig(new DefaultAsyncHttpClientConfig.Builder().build())
    val flinkConfig = config.rootAs[FlinkConfig]
    val CustomAction1Name = "Action 1"

    new FlinkStreamingRestManager(flinkConfig, modelData) {

      override def invokeCustomAction(actionRequest: CustomActionRequest): Future[Either[CustomActionError, CustomActionResult]] = {
        actionRequest.name match {
          case `CustomAction1Name` => Future.successful(Right(CustomActionResult(s"$CustomAction1Name successful")))
          case _ => Future.successful(Left(CustomActionError("Not implemented")))
        }
      }

      override def processStateDefinitionManager: ProcessStateDefinitionManager = new FlinkProcessStateDefinitionManager {

        override val customActions: List[CustomAction] = List(
          CustomAction(CustomAction1Name, allowedProcessStates = List(NotDeployed, Canceled))
        )
      }
    }
  }
}