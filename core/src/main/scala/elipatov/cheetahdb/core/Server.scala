package elipatov.cheetahdb.core

import cats.{Alternative, Monad}
import cats.effect.{Concurrent, Resource, Sync, Timer}
import cats.effect.concurrent.Ref
import cats.syntax.all._
import elipatov.cheetahdb.NodeInfo
import org.http4s.Uri
import org.http4s.client.Client
import org.http4s.implicits.http4sLiteralsSyntax
import org.slf4j.LoggerFactory

import java.net.http.HttpClient
import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

trait Server[F[_]] {
  def getGCounter(key: String): F[Option[Long]]
  def putGCounter(key: String, value: Long): F[Unit]
  def sync(state: SyncState): F[Unit]
}

case class SyncState(nodeId: Int, gCounter: Map[String, Vector[Long]])

class CRDTServer[F[_]: Sync](
    nodeId: Int,
    nodes: Vector[NodeInfo],
    httpClient: Client[F],
    gCounters: KeyValueStore[F, Vector, String, Long],
    gCountersUpdates: Ref[F, HashSet[String]],
    running: Ref[F, Boolean]
) extends Server[F] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val replicas =
    nodes.zipWithIndex.filterNot { case (_, i) => i == nodeId }.map { case (n, _) => n }

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

  private def close(): F[Unit] = running.set(false) *> flush()

  def runLoop(
      runInterval: FiniteDuration
  )(implicit T: Timer[F], C: Concurrent[F]): F[Unit] = {
    for {
      _   <- T.sleep(runInterval)
      _   <- flush()
      run <- running.get
      _   <- if (run) runLoop(runInterval) else ().pure[F]
    } yield ()
  }

  private def syncGCounter(other: Map[String, Vector[Long]]): F[Unit] = gCounters.sync(other)

  private def flush(): F[Unit] = replicas.traverse(flush).map(_ => ())

  private def flush(node: NodeInfo): F[Unit] = {
    import cats.Invariant.catsInstancesForOption

    for {
      ks <- gCountersUpdates.modify((HashSet.empty, _))
      ss <-
        ks.toList
          .traverse(k => gCounters.getState(k).map(o => o.map(v => k -> v)))
          .map(x => SyncState(nodeId, x.flatten.toMap))
      uri = Uri.fromString(s"http://${node.host}:${node.httpPort}").to.get
      api = new HttpApiClient(httpClient, uri)
      _ <- api.sync(ss).recoverWith {
        case e =>
          Sync[F].delay {
            logger.error("Flush failed", e)
          } *> gCountersUpdates.update(x => x ++ ss.gCounter.keys)
      }
    } yield ()
  }
}

object CRDTServer {
  def of[F[+_]: Sync](
      httpClient: Client[F],
      nodeId: Int,
      nodes: Vector[NodeInfo],
      syncInterval: FiniteDuration
  )(implicit T: Timer[F], C: Concurrent[F]): Resource[F, Server[F]] = {
    val srv = for {
      gCounter <- InMemoryCRDTStore.gCounterStore[F](nodeId, nodes.length)
      gCtrKeys <- Ref.of[F, HashSet[String]](HashSet.empty)
      running  <- Ref.of(true)
      srv = new CRDTServer(nodeId, nodes, httpClient, gCounter, gCtrKeys, running)
      _        <- C.start(srv.runLoop(syncInterval))
    } yield srv

    Resource.make(srv)(_.close)
  }
}
