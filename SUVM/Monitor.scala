package myTools.SUVM

import spinal.core._
import spinal.core.sim._
import spinal.lib._


trait Monitor[T <: Transaction] {
  def start(): Unit
}

trait MonitorHook[T <: Transaction] {
  def preMon(tx: T): Unit = {}

  def postMon(tx: T): Unit = {}
}

trait MonitorHandle[T <: Transaction] {
  def mon: () => T
}

abstract class BaseMonitor[T <: Transaction](
                                              val monFn: () => Option[T],
                                              val hook: Option[MonitorHook[T]] = None) extends Monitor[T]
  with ScoreboardDriven[T]
//  with MonitorHandle[T]
   {
  //  def mon(): Option[T]

  override def Active: Boolean = false

  override def start(): Unit = fork {
    println(s"Monitor ${this.getClass.getSimpleName} started")
    fork {
      while (!objectionClear) {
        //        clockDomain.waitSampling()
        monFn() match {
          case Some(rx: T) =>
            hook.foreach(_.preMon(rx))
            push(rx) // 将事务推送到 Scoreboard
            hook.foreach(_.postMon(rx))
          case None => // idle
        }
      }
    }
  }
}
