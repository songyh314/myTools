package myTools.SUVM

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import myTools.SUVM


trait Driver[T <: Transaction] {
  def start(): Unit
}


trait DriverHook[T <: Transaction] {
  def preDrv(tx: T): Unit = {}

  def postDrv(tx: T): Unit = {}
}

trait DriverHandle[T <: Transaction] {
  def drive: T => Unit
}

trait SequenceProvider {
  def sequencer: ISequencer
}


abstract class BaseDriver[T <: Transaction](val hook: Option[DriverHook[T]] = None) extends Driver[T]
  with ScoreboardDriven[T]
  with SequenceProvider
  with DriverHandle[T] {
  //  def drive(tx: T): Unit

//  this: DriverHandle[T] =>
  override def Active: Boolean = true

  override def start(): Unit = fork {
    println(s"Driver ${this.getClass.getSimpleName} started, objectionClear is ${objectionClear}")
    fork {
      while (!objectionClear) {
        //        clockDomain.waitSampling()
        sequencer.selectNext() match {
          case Some(tx: T) =>
            println(s"Driver ${this.getClass.getSimpleName} driving transaction: $tx")
            hook.foreach(_.preDrv(tx))
            drive(tx)
            push(tx) // 将事务推送到 Scoreboard
            hook.foreach(_.postDrv(tx))
          case None => sleep(1) // idle
        }
      }
    }
  }
}
