package elipatov.cheetahdb

import cats.effect.{Async, Blocker, ContextShift, ExitCode, IO, IOApp, Resource}
import cats.implicits.catsSyntaxApplicativeId
import com.typesafe.config.ConfigFactory
import elipatov.cheetahdb.core.{CRDTServer, HttpApi, Server}
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT

import scala.concurrent.ExecutionContext
import scala.io.{BufferedSource, Source}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val nodeId        = 0
    val replicasCount = 3

    (
      for {
        cfg <- Resource.make(loadConfig("application.conf").pure[IO])(_ => IO.unit)
        srv <- Resource.make(CRDTServer.of[IO](cfg.nodeId, cfg.nodes.length))(_.close())
      } yield (srv, cfg)
    ).use {
        case (srv, cfg) => {
          val api = new HttpApi(srv)
          for {
            _ <- IO.unit
            node = cfg.nodes.filter(_.nodeId == cfg.nodeId).head
            _ <- httpServer(node, api.routes.orNotFound)
          } yield ()
        }
      }
      .as(ExitCode.Success)
  }

  private def loadConfig(configFile: String) = Settings(ConfigFactory.load(configFile))

  private def httpServer(node: NodeInfo, httpApp: HttpApp[IO]): IO[Unit] =
    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(node.httpPort, node.host)
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
}
