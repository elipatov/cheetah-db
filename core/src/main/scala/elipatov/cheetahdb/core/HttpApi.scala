package elipatov.cheetahdb.core

import cats.effect.{IO, Sync}
import cats.effect._
import cats.syntax.all._
import cats.{Applicative, Monoid}
import elipatov.cheetahdb.core.SyncState
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, _}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io.{->, /, Ok, POST, Root, _}
import cats.effect._
import cats.syntax.all._
import cats.{Applicative, Monad, Monoid}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.slf4j.LoggerFactory

import java.time.{LocalDate, Year}
import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import scala.util.Try

class HttpApi(server: Server[IO]) {
  import io.circe.Codec
  import io.circe.generic.auto._
  import io.circe.generic.extras.semiauto.deriveEnumerationCodec

  def routes: HttpRoutes[IO] = {
    sync <+> gCounter
  }

  private val sync = {
    HttpRoutes.of[IO] {
      // curl -XPOST "localhost:8080/internal/sync" -i -d '{"nodeId": 0, "gCounter": {"key0": [0, 7, 13]}}' -H "Content-Type: application/json"
      case req @ POST -> Root / "internal" / "sync" =>
        for {
          state <- req.as[SyncState]
          _     <- server.sync(state)
          resp  <- Ok()
        } yield resp
    }
  }

  private val gCounter = HttpRoutes.of[IO] {
    // curl "localhost:8080/v1/gcounter/key0" -i
    case GET -> Root / "v1" / "gcounter" / key => server.getGCounter(key).flatMap(fromOption(_))
    // curl -XPUT "localhost:8080/v1/gcounter/key0" -i -d '3' -H "Content-Type: application/json"
    case req @ PUT -> Root / "v1" / "gcounter" / key =>
      for {
        count <- req.as[Long]
        resp <-
          if (count > 0) server.putGCounter(key, count) *> Ok()
          else BadRequest("Count must be greater than 0")
      } yield resp
  }

  private def fromOption[T](value: Option[T])(implicit encoder: EntityEncoder[IO, T]) =
    value match {
      case Some(v) => Ok(v)(IO.ioEffect, encoder)
      case None    => NotFound()(Applicative[IO])
    }
}
