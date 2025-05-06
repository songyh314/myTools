package myTools.sim

import spinal.core._
import spinal.core.sim._

import scala.collection.mutable

trait simEnv {
  type I
  type O
  val drvQueue = mutable.Queue[I]()
  val drvMon = mutable.Queue[I]()
  val refQueue = mutable.Queue[O]()
  val resQueue = mutable.Queue[O]()
  @volatile private var stop: Boolean = false
  def clockDomain:ClockDomain

  def Driver():Unit
  def Monitor():Unit

  def socreBoard():Unit = {
    fork{
      while (!stop){
        if (refQueue.nonEmpty && resQueue.nonEmpty) {
          val drvData = drvMon.dequeue()
          val calRes = resQueue.dequeue()
          val calRef = refQueue.dequeue()
          assert(calRes == calRef,s"data mismatch input:${drvData} res:${calRes}  ref:${calRef}")
        }
        clockDomain.waitSampling()
      }
    }
  }

  def waitSimDone(): Unit = {
    clockDomain.waitSampling(10)
    while (refQueue.nonEmpty || resQueue.nonEmpty) {
      clockDomain.waitSampling(10)
    }
    stop = true
    clockDomain.waitSampling(10)
    println("sim finish")
    simSuccess()
  }

  def waitClean(): Unit = {
    clockDomain.waitSampling(10)
    while (refQueue.nonEmpty || resQueue.nonEmpty) {
      clockDomain.waitSampling(1)
    }
    clockDomain.waitSampling(10)
  }

  def insertData(test: I): Unit = {
    drvQueue.enqueue(test)
    drvMon.enqueue(test)
  }
  

}
