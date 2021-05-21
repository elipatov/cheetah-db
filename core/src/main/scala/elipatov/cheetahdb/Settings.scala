package elipatov.cheetahdb

import com.typesafe.config.Config
import scala.jdk.CollectionConverters._

case class Settings(root: Config) {
  final val nodeId: Int = root.getInt("nodeId")
  final val nodes = root.getConfigList("nodes").asScala.map(NodeInfo(_))
}

case class NodeInfo(cfg: Config) {
  final val nodeId: Int = cfg.getInt("nodeId")
  final val host: String = cfg.getString("host")
  final val httpPort: Int = cfg.getInt("httpPort")
}