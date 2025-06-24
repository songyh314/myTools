package myTools.SUVM

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.collection.mutable

trait RefModel[T <: Transaction] {
  def process(tx: T): Seq[T] // 返回一组参考输出
}

trait ScoreboardDriven[T <: Transaction] {
  def Active: Boolean

  def scoreboard: Option[Scoreboard[T]]

  def push(tx: T): Unit = scoreboard match {
    case Some(sb: Scoreboard[T]) => if (Active) sb.pushInput(tx) else sb.pushActual(tx)
    case _ => ()
  }

  def objectionClear: Boolean = scoreboard.exists(_.objection.isClear)
}
//
///**
// * Scoreboard 接口：用于接收 Monitor 的实际事务
// */
//trait Scoreboard[T <: Transaction] {
//  def pushActual(tx: T): Unit
//
//  def pushInput(tx: T): Unit
//
//  def flush(): Unit
//}
//
//import scala.collection.mutable
//
///**
// * Scoreboard 实现，带参考模型 RefModel：
// * - 接收 Driver 发送的输入事务（pushInput）
// * - 使用 RefModel 生成期望输出
// * - 接收 Monitor 采样的实际输出（pushActual）
// * - 自动进行对比
// */
//class RefScoreboard[T <: Transaction](
//                                       refModel: RefModel[T],
//                                       name: String = "Scoreboard",
//                                       expectTotal: Option[Int] = None
//                                     ) extends Scoreboard[T] {
//
//  private val expectedQueue = mutable.Queue[T]()
//  private val actualQueue = mutable.Queue[T]()
//  private var inputCount = 0
//
//  val objection = new SimObjection()
//
//
//  /** Monitor 调用，推送 DUT 实际输出事务 */
//  override def pushActual(tx: T): Unit = synchronized {
//    actualQueue.enqueue(tx)
//    println(s"[$name] [Actual  ] <= $tx")
//    compare()
//  }
//
//  /** Driver 的 input 被推送进来，RefModel 生成预期事务 */
//  override def pushInput(tx: T): Unit = synchronized {
//    val expected = refModel.process(tx)
//    expected.foreach { e =>
//      println(s"[$name] [Expected] <= $e")
//      expectedQueue.enqueue(e)
//    }
//    inputCount += 1
//    compare()
//  }
//
//  /** 执行事务比对 */
//  private def compare(): Unit = {
//    while (expectedQueue.nonEmpty && actualQueue.nonEmpty) {
//      val e = expectedQueue.dequeue()
//      val a = actualQueue.dequeue()
//      if (e.toString == a.toString) {
//        println(s"[$name] [PASS    ] $e == $a")
//      } else {
//        println(s"[$name] [FAIL    ] Expected: $e, Got: $a")
//      }
//    }
//  }
//
//  /** 仿真结束前可手动调用，检查是否有未消费的项 */
//  override def flush(): Unit = synchronized {
//    compare()
//    if (expectedQueue.nonEmpty || actualQueue.nonEmpty) {
//      println(s"[$name] [WARNING ] Unmatched: " +
//        s"${expectedQueue.size} expected, ${actualQueue.size} actual")
//    } else {
//      println(s"[$name] [CLEAN   ] All matched.")
//    }
//  }
//
//  def phaseControl(thersholdCycles: Int = 10): Unit = {
//    objection.raise()
//    val t = fork {
//      var idleCycles = 0
//
//      while (true) {
//        val empty = expectedQueue.isEmpty && actualQueue.isEmpty
//        val countOverflow = expectTotal.forall(inputCount >= _)
//
//        if (empty && countOverflow) {
//          idleCycles += 1
//        } else {
//          idleCycles = 0 // 重置空闲计数
//        }
//
//        if (idleCycles >= thersholdCycles) {
//          println(s"[$name] [IDLE    ] No activity for $thersholdCycles cycles, ending phase.")
//          objection.drop()
//          return
//        }
//
//        sleep(1)
//      }
//    }
//  }
//
//  def isFinished: Boolean = objection.isClear
//}

//abstract class Scoreboard[T <: Transaction](
//                                             val refModel: RefModel[T],
//                                             val name: String = "Scoreboard",
//                                             val expectTotal: Option[Int] = None
//                                           ) {
//  protected val expectedQueue = mutable.Queue[T]()
//  protected val actualQueue = mutable.Queue[T]()
//  private var inputCount = 0
//  val objection = new SimObjection()
//
//  /** 子类必须实现比较逻辑 */
//  def compareTransactions(expected: T, actual: T): Boolean
//
//  /** 推送实际输出事务（Monitor） */
//  def pushActual(tx: T): Unit = synchronized {
//    actualQueue.enqueue(tx)
//    println(s"[$name] [Actual  ] <= $tx")
//    compare()
//  }
//
//  /** 推送输入事务并经由参考模型生成期望输出（Driver） */
//  def pushInput(tx: T): Unit = synchronized {
//    val expected = refModel.process(tx)
//    expected.foreach { e =>
//      println(s"[$name] [Expected] <= $e")
//      expectedQueue.enqueue(e)
//    }
//    inputCount += 1
//    compare()
//  }
//
//  /** 比较期望输出与实际输出 */
//  private def compare(): Unit = synchronized {
//    while (expectedQueue.nonEmpty && actualQueue.nonEmpty) {
//      val e = expectedQueue.dequeue()
//      val a = actualQueue.dequeue()
//      if (compareTransactions(e, a)) {
//        println(s"[$name] [PASS    ] $e == $a")
//      } else {
//        println(s"[$name] [FAIL    ] Expected: $e, Got: $a")
//      }
//    }
//  }
//
//  /** 强制完成比较并打印未比对的事务 */
//  def flush(): Unit = synchronized {
//    compare()
//    if (expectedQueue.nonEmpty || actualQueue.nonEmpty) {
//      println(s"[$name] [WARNING ] Unmatched: " +
//        s"${expectedQueue.size} expected, ${actualQueue.size} actual")
//    } else {
//      println(s"[$name] [CLEAN   ] All matched.")
//    }
//  }
//
//  /** 提供仿真结束控制 */
//  def phaseControl(thresholdCycles: Int = 10): Unit = {
//    objection.raise()
//    fork {
//      var idleCycles = 0
//
//      while (true) {
//        val empty = expectedQueue.isEmpty && actualQueue.isEmpty
//        val countOverflow = expectTotal.forall(inputCount >= _)
//
//        if (empty && countOverflow) {
//          idleCycles += 1
//        } else {
//          idleCycles = 0
//        }
//
//        if (idleCycles >= thresholdCycles) {
//          println(s"[$name] [IDLE    ] No activity for $thresholdCycles cycles, ending phase.")
//          objection.drop()
//          return
//        }
//
//        sleep(1)
//      }
//    }
//  }
//  private def startCompareThread(): Unit = fork {
//    while (!objection.isClear) {
//      compare()
//      sleep(1) // 每周期检查一次
//    }
//  }
//
//
//
//  def isFinished: Boolean = objection.isClear
//}

abstract class Scoreboard[T <: Transaction](
                                             val refModel: RefModel[T],
                                             val name: String = "Scoreboard",
                                             val expectTotal: Option[Int] = None
                                           ) {
  protected val expectedQueue = mutable.Queue[T]()
  protected val actualQueue = mutable.Queue[T]()
  private var inputCount = 0
  val objection = new SimObjection()

  var clk: ClockDomain = null // 延迟绑定

  def withClockDomain(clock: ClockDomain): this.type = {
    this.clk = clock
    this
  }

  /** 子类必须实现的比较函数 */
  def compareTransactions(expected: T, actual: T): Boolean

  /** Monitor 推送输出 */
  def pushActual(tx: T): Unit = synchronized {
    actualQueue.enqueue(tx)
    println(s"[$name] [Actual  ] <= $tx")
  }

  /** Driver 推送输入，RefModel 生成期望输出 */
  def pushInput(tx: T): Unit = synchronized {
    val expected = refModel.process(tx)
    expected.foreach { e =>
      println(s"[$name] [Expected] <= $e")
      expectedQueue.enqueue(e)
    }
    inputCount += 1
  }

  /** 比较线程：独立执行匹配判断 */
  private def compare(): Unit = synchronized {
    while (expectedQueue.nonEmpty && actualQueue.nonEmpty) {
      val e = expectedQueue.dequeue()
      val a = actualQueue.dequeue()
      if (compareTransactions(e, a)) {
        println(s"[$name] [PASS    ] $e == $a")
      } else {
        println(s"[$name] [FAIL    ] Expected: $e, Got: $a")
      }
    }
  }

  /** 启动比较进程 + phase objection 控制 */
  def start(thresholdCycles: Int = 10): Unit = synchronized {
    println(s"[$name] [START   ] Scoreboard started.")
    objection.raise()

    fork {
      var idleCycles = 0
      var exit = false
      while (!exit) {
        val empty = expectedQueue.isEmpty && actualQueue.isEmpty
        val countOverflow = expectTotal.forall(inputCount >= _)

        compare()

        if (empty && countOverflow) idleCycles += 1
        else idleCycles = 0

        if (idleCycles >= thresholdCycles) {
          println(s"[$name] [IDLE    ] No activity for $thresholdCycles cycles, ending phase.")
          objection.drop()
          //          return
          exit = true
        }
        clk.waitSampling() // 等待时钟周期
      }
    }
  }

  /** 仿真结束前手动检查残留项 */
  def flush(): Unit = synchronized {
    compare()
    if (expectedQueue.nonEmpty || actualQueue.nonEmpty) {
      println(s"[$name] [WARNING ] Unmatched: " +
        s"${expectedQueue.size} expected, ${actualQueue.size} actual")
    } else {
      println(s"[$name] [CLEAN   ] All matched.")
    }
  }

  def isFinished: Boolean = objection.isClear
}
