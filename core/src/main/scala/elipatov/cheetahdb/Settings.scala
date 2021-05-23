package elipatov.cheetahdb

import com.typesafe.config.Config
import scala.jdk.CollectionConverters._

case class Settings(root: Config) {
  final val nodeId: Int = root.getInt("nodeId")
  final val nodes: Vector[NodeInfo] = root.getConfigList("nodes").asScala.map(NodeInfo(_)).toVector
}

case class NodeInfo(cfg: Config) {
  final val host: String = cfg.getString("host")
  final val httpPort: Int = cfg.getInt("httpPort")
}