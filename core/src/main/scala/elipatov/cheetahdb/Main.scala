package elipatov.cheetahdb

import cats.effect.{Async, Blocker, ContextShift, ExitCode, IO, IOApp, Resource}
import cats.implicits.catsSyntaxApplicativeId
import com.typesafe.config.ConfigFactory
import elipatov.cheetahdb.core.{CRDTServer, HttpApi, Server}
import org.http4s.HttpApp
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.client.dsl.io._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.implicits._
import io.circe.generic.auto._
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder

import scala.concurrent.ExecutionContext
import scala.io.{BufferedSource, Source}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    (
      for {
        cfg  <- Resource.make(loadConfig("application.conf").pure[IO])(_ => IO.unit)
        http <- BlazeClientBuilder[IO](ExecutionContext.global).resource
        srv  <- CRDTServer.of[IO](http, cfg.nodeId, cfg.nodes)
      } yield (srv, cfg)
    ).use {
        case (srv, cfg) => {
          val api = new HttpApi[IO](srv)
          for {
            _ <- IO.unit
            node = cfg.nodes(cfg.nodeId)
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
