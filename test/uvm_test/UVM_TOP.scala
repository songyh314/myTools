package myTools.test.uvm_test

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import myTools.SUVM._

import scala.language.postfixOps



//case class myDriver() extends BaseDriver[myTx] {
//
//  override def drive(tx: myTx): Unit = ???
//
//  override def scoreboard: Option[Scoreboard[Transaction]] = ???
//}


case class myTx(data: Int = 0) extends Transaction {

  var din: Option[BigInt] = Some(BigInt(data))
  var dout: Option[BigInt] = None

  override def name: String = s"myTx"

  override def randomize(): Unit = {
    din = Some(BigInt(8, scala.util.Random))
    //    dout = Some(din.get + 1)
  }

  override def toString: String = s"myTx(din=$din, dout=$dout)"
}

case class myRefModel() extends RefModel[myTx] {
  override def process(tx: myTx): Seq[myTx] = {
    tx.dout = Some(tx.din.get + 1)
    Seq(tx)
  }
}

class MyScoreboard(model: RefModel[myTx])
  extends Scoreboard[myTx](model, "MyScoreboard") {

  override def compareTransactions(expected: myTx, actual: myTx): Boolean = {
    expected.dout == actual.dout
  }
}


case class MyDriver(
                     override val drive: myTx => Unit,
                     override val sequencer: ISequencer,
                     override val hook: Option[DriverHook[myTx]] = None,
                     override val scoreboard: Option[Scoreboard[myTx]] = None
                   ) extends BaseDriver[myTx] {

}

case class MyMonitor(
                      override val monFn: () => Option[myTx],
                      override val hook: Option[MonitorHook[myTx]] = None,
                      override val scoreboard: Option[Scoreboard[myTx]] = None
                    ) extends BaseMonitor[myTx](monFn, hook)


class MyTop(dut: DUT) {

  import dut._

  val myScoreboard = new MyScoreboard(myRefModel()).withClockDomain(dut.clockDomain)
  val mySequencer = new Sequencer
  //  val seq_1 = Sequence(priority = 1)
  val seq_2 = Sequence(priority = 2)
  //  (0 until 128).foreach(_ => seq_1.doItem(myTx())(_.randomize()))
  (0 until 128).foreach(i => seq_2.doItem(myTx(i))())
  //  mySequencer.register(seq_1)
  mySequencer.register(seq_2)

  // 定义外部的驱动函数
  val myDriveFn: myTx => Unit = (tx: myTx) => {
    io.din.payload #= tx.din.get
    io.din.valid #= true
    clockDomain.waitSampling()
    io.din.valid #= false
    println(s"Driver drove: ${tx.din.get} -> ${io.din.payload.toBigInt}")
  }


  val drv = MyDriver(
    drive = myDriveFn,
    sequencer = mySequencer,
    hook = None,
    scoreboard = Some(myScoreboard)
  )


  // 提前定义一个采样函数
  val myMonFn: () => Option[myTx] = () => {
    clockDomain.waitActiveEdgeWhere(io.dout.valid.toBoolean)
    val tx = myTx()
    tx.dout = Some(io.dout.payload.toBigInt)
    println(s"Monitor sampled: ${tx.dout}")
    Some(tx)
  }
  val mon = MyMonitor(
    monFn = myMonFn,
    hook = None,
    scoreboard = Some(myScoreboard)
  )

  // 实例化 BaseMonitor，直接传入 monFn
  def start(): Unit = {
    myScoreboard.start(10)
    drv.start()
    mon.start()
  }

  def run(): Unit = {
    start() // 启动 driver / monitor / scoreboard
    clockDomain.waitSampling(10)
    while (!myScoreboard.isFinished) {
      clockDomain.waitSampling()
    }
    //    myScoreboard.flush()
    println("[UVM_TOP] Test finished")
    simSuccess()

  }

}

object SimTop extends App {
  SimConfig.withWave.workspacePath("Learn/uvm/test").compile(DUT()).doSim { dut =>
    val env = new MyTop(dut)

    dut.clockDomain.forkStimulus(10 ns)
    dut.clockDomain.waitSampling(10)
    //    SimTimeout(1000 ns)
    env.run()
  }
}





