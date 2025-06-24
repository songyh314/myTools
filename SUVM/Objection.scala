package myTools.SUVM

import scala.collection.mutable.Queue
import spinal.core.sim._

class SimObjection {
  private var count = 0

  def raise(): Unit = {
    count += 1
    println(s"[objection] raised -> count = $count")
  }

  def drop(): Unit = {
    count -= 1
    println(s"[objection] dropped -> count = $count")
    if (count < 0) {
      count = 0 // Reset to zero to avoid negative count
      throw new RuntimeException("Objection dropped more times than raised!")
    }
  }

  def isClear: Boolean = synchronized {
    count == 0
  }
}