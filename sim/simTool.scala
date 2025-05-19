package myTools.sim

import spinal.core._
import spinal.core.sim._

import scala.collection.mutable

trait simEnv {
  type I
  type O
  val drvQueue                = mutable.Queue[I]()
  val drvMon                  = mutable.Queue[I]()
  val refQueue                = mutable.Queue[O]()
  val resQueue                = mutable.Queue[O]()
  @volatile var stop: Boolean = false

  val dutClockDomain: ClockDomain
  def simInit(): Unit
  def Driver(): Unit
  def Monitor(): Unit
//  def refModule(): Unit

  def scoreBoard(): Unit = {
    val score = fork {
      while (!stop) {
        if (refQueue.nonEmpty && resQueue.nonEmpty) {
          val drvData = drvMon.dequeue()
          val calRes  = resQueue.dequeue()
          val calRef  = refQueue.dequeue()
//          println(s"data:${drvData} res:${calRes}  ref:${calRef}")
          assert(calRes == calRef, s"data mismatch input:${drvData} res:${calRes}  ref:${calRef}")
        }
        dutClockDomain.waitSampling()
      }
    }
  }

  def waitSimDone(): Unit = {
    dutClockDomain.waitSampling(10)
    while (refQueue.nonEmpty || resQueue.nonEmpty) {
      dutClockDomain.waitSampling(10)
    }
    stop = true
    dutClockDomain.waitSampling(10)
    println("sim finish")
    simSuccess()
  }

  def waitClean(): Unit = {
    dutClockDomain.waitSampling(10)
    while (refQueue.nonEmpty || resQueue.nonEmpty) {
      dutClockDomain.waitSampling(1)
    }
    dutClockDomain.waitSampling(10)
  }

  def insertData(test: I): Unit = {
    drvQueue.enqueue(test)
    drvMon.enqueue(test)
  }
  def simEnvStart(): Unit       = {
    simInit()
    Driver()
    Monitor()
    scoreBoard()
  }

}
