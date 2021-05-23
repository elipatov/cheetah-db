package elipatov.cheetahdb.core

import cats.{Alternative, Monad}
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.syntax.all._
import elipatov.cheetahdb.NodeInfo
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax

import java.net.http.HttpClient
import scala.collection.immutable.HashSet
import scala.collection.mutable

trait Server[F[_]] {
  def getGCounter(key: String): F[Option[Long]]
  def putGCounter(key: String, value: Long): F[Unit]
  def sync(state: SyncState): F[Unit]
  def close(): F[Unit]
}

case class SyncState(nodeId: Int, gCounter: Map[String, Vector[Long]])

class CRDTServer[F[_]: Monad](
    nodeId: Int,
    nodes: Vector[NodeInfo],
    apiClient: ApiClient[F],
    gCounters: KeyValueStore[F, Vector, String, Long],
    gCountersUpdates: Ref[F, HashSet[String]]
) extends Server[F] {
  override def getGCounter(key: String): F[Option[Long]] = gCounters.get(key)

  override def putGCounter(key: String, value: Long): F[Unit] = {
    for {
      _ <- gCounters.put(key, value)
      _ <- gCountersUpdates.update(_.incl(key))
    } yield ()
  }

  override def sync(state: SyncState): F[Unit] = {
    for {
      _ <- syncGCounter(state.gCounter)
    } yield ()
  }

  override def close(): F[Unit] = flush

  private def syncGCounter(other: Map[String, Vector[Long]]): F[Unit] = gCounters.sync(other)

  private def flush(): F[Unit] = {
    ().pure[F]
  }

}

object CRDTServer {
  import cats.Invariant.catsInstancesForOption

  def of[F[+_]: Sync](httpClient: Client[F], nodeId: Int, nodes: Vector[NodeInfo]): F[Server[F]] = {
    for {
      gCounter <- InMemoryCRDTStore.gCounterStore[F](nodeId, nodes.length)
      gCtrKeys <- Ref.of[F, HashSet[String]](HashSet.empty)
      node      = nodes(nodeId)
      uri       = Uri.fromString(s"http://${node.host}:${node.httpPort}").to.get
      apiClient = new HttpApiClient(httpClient, uri)
    } yield new CRDTServer(nodeId, nodes, apiClient, gCounter, gCtrKeys)
  }
}
