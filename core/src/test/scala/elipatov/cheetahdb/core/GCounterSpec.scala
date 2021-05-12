package elipatov.cheetahdb.core

import cats.effect.IO
import cats.implicits.toTraverseOps
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class GCounterSpec extends AnyFreeSpec with ScalaCheckDrivenPropertyChecks with Matchers {
  val replicasCount = 3

  def init(count0: Int, count1: Int, count2: Int) =
    for {
      node0 <- GCounterCvRDT.of[IO](0, replicasCount)
      _     <- (1 to count0).toList.traverse(_ => node0.modify(1))
      node1 <- GCounterCvRDT.of[IO](1, replicasCount)
      _     <- (1 to count1).toList.traverse(_ => node1.modify(1))
      node2 <- GCounterCvRDT.of[IO](2, replicasCount)
      _     <- node2.modify(count2)
    } yield (node0, node1, node2)

  "G-Counter" - {
    "each node has it's own value" in {
      forAll { (c0: Byte, c1: Byte, c2: Byte) =>
        val res = for {
          ns <- init(c0, c1, c2)
          (node0, node1, node2) = ns
          count0 <- node0.get
          count1 <- node1.get
          count2 <- node2.get
        } yield (count0, count1, count2)

        res.unsafeRunSync() == (c0, c1, c2)
      }
    }

    "merge node0 -> node1" in {
      forAll { (c0: Byte, c1: Byte, c2: Byte) =>
        val res = for {
          ns <- init(c0, c1, c2)
          (node0, node1, node2) = ns
          counts0 <- node0.getState()
          _       <- node1.merge(counts0)
          count0  <- node0.get
          count1  <- node1.get
          count2  <- node2.get
        } yield (count0, count1, count2)

        res.unsafeRunSync() == (c0, c0 + c1, c2)
      }
    }

    "merge node0 -> node1; node1 -> node2; node2 -> node0; node0 -> node1" in {
      forAll { (c0: Byte, c1: Byte, c2: Byte) =>
        val res = for {
          ns <- init(c0, c1, c2)
          (node0, node1, node2) = ns
          counts0 <- node0.getState()
          _       <- node1.merge(counts0)
          counts1 <- node1.getState()
          _       <- node2.merge(counts1)
          counts2 <- node2.getState()
          _       <- node1.merge(counts2)
          counts0 <- node0.getState()
          _       <- node1.merge(counts0)
          count0  <- node0.get
          count1  <- node1.get
          count2  <- node2.get
        } yield (count0, count1, count2)

        res.unsafeRunSync() == (c0 + c1 + c2, c0 + c1 + c2, c0 + c1 + c2)
      }
    }
  }

}
