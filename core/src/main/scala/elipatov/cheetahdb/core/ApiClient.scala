package elipatov.cheetahdb.core

import cats.{Monad, MonadThrow}
import cats.effect.BracketThrow
import cats.syntax.all._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl._
import org.http4s.{Method, Uri, _}

trait ApiClient[F[_]] {
  def sync(state: SyncState): F[Unit]
}

private final class HttpApiClient[F[_]: Monad](client: Client[F], uri: Uri)(implicit
    M: MonadThrow[F],
    B: BracketThrow[F],
    decoder: EntityDecoder[F, Unit]
) extends ApiClient[F] {
  override def sync(state: SyncState): F[Unit] = {
    for {
      req  <- Method.PUT.apply(state, uri / "internal" / "sync")
      resp <- client.run(req).use { _.as[Unit] }
    } yield resp
  }
}
