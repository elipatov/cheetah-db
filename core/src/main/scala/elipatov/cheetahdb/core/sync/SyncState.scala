package elipatov.cheetahdb.core.sync

case class SyncState(nodeId: Int, gCounter: Map[String, Vector[Long]]) {

}
