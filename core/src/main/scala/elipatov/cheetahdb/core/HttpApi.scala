package elipatov.cheetahdb.core

import cats.{Applicative, MonadThrow}
import cats.effect.{BracketThrow, _}
import cats.syntax.all._
import org.http4s.{HttpRoutes, _}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe.CirceSensitiveDataEntityDecoder.circeEntityDecoder

import scala.language.postfixOps

class HttpApi[F[_]: Sync](server: Server[F])(implicit
    M: MonadThrow[F],
    B: BracketThrow[F],
    decoder: EntityDecoder[F, SyncState]
) {
  val dsl = org.http4s.dsl.Http4sDsl[F]
  import dsl._
  def routes: HttpRoutes[F] = {
    sync <+> gCounter
  }

  private val sync = {
    HttpRoutes.of[F] {
      // curl -XPUT "localhost:8080/internal/sync" -i -d '{"nodeId": 0, "gCounter": {"key0": [0, 7, 13]}}' -H "Content-Type: application/json"
      case req @ PUT -> Root / "internal" / "sync" =>
        for {
          state <- req.as[SyncState].flatMap(x => x.pure[F])
          _     <- server.sync(state)
          resp  <- Ok()
        } yield resp
    }
  }

  private val gCounter = HttpRoutes.of[F] {
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

  private def fromOption[T](value: Option[T])(implicit encoder: EntityEncoder[F, T]) =
    value match {
      case Some(v) => Ok(v)(Applicative[F], encoder)
      case None    => NotFound()(Applicative[F])
    }
}
