package elipatov.cheetahdb

import cats.effect.{Async, Blocker, ContextShift, ExitCode, IO, IOApp, Resource}
import elipatov.cheetahdb.core.{CRDTServer, HttpApi, Server}
import org.http4s.HttpApp
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT

import scala.concurrent.ExecutionContext
import scala.io.{BufferedSource, Source}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val nodeId = 0
    val replicasCount = 3

    Resource.make(CRDTServer.of[IO](nodeId, replicasCount))(_.close())
      .use ( srv => {
          val api = new HttpApi(srv)
          for {
            _ <- httpServer(api.routes.orNotFound)
          } yield()
        }


    ).as(ExitCode.Success)
  }

  private def fileResource(nodeId: Int, replicasCount: Int): Resource[IO, Server[IO]] =
    Resource.make(CRDTServer.of[IO](nodeId, replicasCount))(_.close())


  private def httpServer(httpApp: HttpApp[IO]): IO[Unit] =
    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(port = 8080, host = "localhost")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
}
