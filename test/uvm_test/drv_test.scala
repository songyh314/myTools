package myTools.test.uvm_test
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import scala.language.postfixOps

class Drv(dut:DUT) {
  import dut._
  def drv(d:Int):Unit = {
    io.din.valid #= true
    io.din.payload #= BigInt(d)
    clockDomain.waitSampling()
    io.din.valid #= false
  }
}

object DrvSimTop extends App {
  SimConfig.withWave.withIVerilog.workspacePath("Learn/uvm/drv_test").compile(DUT()).doSim { dut =>
    val env = new Drv(dut)

    dut.clockDomain.forkStimulus(10 ns)
    dut.clockDomain.waitSampling(10)
    //    SimTimeout(1000 ns)
    for (i <- 0 until 32){
      env.drv(i)
    }
    dut.clockDomain.waitSampling(10)
  }
}


