package elipatov.cheetahdb.core

import cats.effect.{IO, Sync}
import elipatov.cheetahdb.core.sync.SyncState
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, _}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io.{->, /, Ok, POST, Root, _}
import cats.effect._
import cats.syntax.all._
import cats.{Applicative, Monoid}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.slf4j.LoggerFactory

import java.time.{LocalDate, Year}
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.Try



class HttpApp {
  import io.circe.Codec
  import io.circe.generic.auto._
  import io.circe.generic.extras.semiauto.deriveEnumerationCodec

  def routes: HttpRoutes[IO] = {
    sync
  }

  private val sync = {
    HttpRoutes.of[IO] {
      // curl -XPOST "localhost:8080/game" -i -d '{"min": 5, "max": 18}' -H "Content-Type: application/json"
      case req @ POST -> Root / "internal" / "sync" =>
        val response =
          for {
            sync <- req.as[SyncState]
            resp <- Accepted()
          } yield resp

      response

    }
  }
}
