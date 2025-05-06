package myTools

//
//import scala.collection.mutable
//import spinal.core._
//import spinal.lib._
//
//case class simEnv() {
//  val drvQueue = mutable.Queue[BigInt]()
//  val drvMon = mutable.Queue[BigInt]()
//  val refQueue = mutable.Queue[BigInt]()
//  val resQueue = mutable.Queue[BigInt]()
//
//  @volatile private var stop: Boolean = false
//  def refModule(dataIn: BigInt): Unit = {
//
//    refQueue.enqueue(ref)
//  }
//  def Driver(): Unit = {
//    val drv = fork {
//      while (!stop) {
//        if (drvQueue.nonEmpty) {
//          val test = drvQueue.dequeue()
//          refModule(test)
//          io.dataIn.payload #= test
//          io.dataIn.valid #= true
//          clockDomain.waitSampling()
//          io.dataIn.valid #= false
//        } else { clockDomain.waitSampling() }
//      }
//    }
//  }
//  def Monitor(): Unit = {
//    val mon = fork {
//      while (!stop) {
//        if (io.dataOut.valid.toBoolean) { resQueue.enqueue(io.dataOut.payload.toBigInt) }
//        clockDomain.waitSampling()
//      }
//    }
//  }
//  def scoreBoard(): Unit = {
//    val score = fork {
//      while (!stop) {
//        if (refQueue.nonEmpty && resQueue.nonEmpty) {
//          val drvData = drvMon.dequeue()
//          val calRes = resQueue.dequeue()
//          val calRef = refQueue.dequeue()
//          assert(calRes == calRef, s"data mismatch input:${drvData} res:${calRes}  ref:${calRef}")
//          if(calRes != calRef){
//            println(s"data:${drvData} res:${calRes}  ref:${calRef}")
//          }
//        }
//        clockDomain.waitSampling()
//      }
//    }
//  }
//  def simEnvStart(): Unit = {
//    //      simInit()
//    Driver()
//    Monitor()
//    scoreBoard()
//  }
//  def waitSimDone(): Unit = {
//    clockDomain.waitSampling(10)
//    while (refQueue.nonEmpty || resQueue.nonEmpty) {
//      clockDomain.waitSampling(10)
//    }
//    stop = true
//    clockDomain.waitSampling(10)
//    println("sim finish")
//    simSuccess()
//  }
//  def waitClean():Unit={
//    clockDomain.waitSampling(10)
//    while (refQueue.nonEmpty || resQueue.nonEmpty) {
//      clockDomain.waitSampling(1)
//    }
//    clockDomain.waitSampling(10)
//  }
//  def insertData(test: BigInt = 0): Unit = {
//    drvQueue.enqueue(test)
//    drvMon.enqueue(test)
//  }
//}
//
//object dutSimFlow extends App {
//  val dut = SimConfig.withXSim.withWave.compile(new simEnv())
//  val period = 10
//  dut.doSim("test") { dut =>
//    import dut._
//    SimTimeout(1000 * period)
//    clockDomain.forkStimulus(period)
//    simEnvStart()
//    for (i <- 0 until (256)) {
//      //----------------------
//      //ur stimulus logic
//      //----------------------
//      insertData(/*ur stimu vector*/)
//    }
//    waitClean()
//    for (i <- 0 until (256)) {
//      //----------------------
//      //ur stimulus logic
//      //----------------------
//      insertData(/*ur stimu vector*/)
//    }
//    waitSimDone()
//  }
//}
