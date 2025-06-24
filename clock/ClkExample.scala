package myTools.clock

import myTools.clock.ClockDomainUtils
import spinal.core._
import spinal.core.ClockDomain._
import spinal.core.sim._
import spinal.lib._
import myTools.clock.ClockDomainUtils._

import scala.language.postfixOps

case class ClkExample() extends Component {
  val io = new Bundle {
    val clk, rst = in Bool()
    val data = in Bits (8 bits)
    val clk_sys, rst_sys = in Bool()
    val res = out UInt (8 bits)
    val ccData = out Bits (8 bits)
  }
  noIoPrefix()
  val cd = ClockDomain(clock = io.clk, reset = io.rst, config = ClockDomainConfig(clockEdge = RISING, resetKind = SYNC))
  val slowClk = cd.divBy(4)
  val cd_sys = ClockDomain(
    clock = io.clk_sys,
    reset = io.rst_sys,
    config = ClockDomainConfig(clockEdge = RISING, resetKind = SYNC)
  )
  val clkArea = new ClockingArea(cd) {
    val Reg_clkArea = RegNext(io.data) init 0
  }
  val sysArea = new ClockingArea(cd_sys) {
    val buf = BufferCC(input = clkArea.Reg_clkArea, init = B(0), bufferDepth = Some(2), randBoot = false)
  }

  val slowArea = new ClockingArea(slowClk) {
    val cnt = Reg(UInt(8 bits)) init 0
    cnt := cnt + 1
  }
  io.res := slowArea.cnt
  io.ccData := sysArea.buf
}

object GenV extends App {
  val path = s"Learn/rtl/"
  SpinalConfig(
    mode = Verilog,
    nameWhenByFile = false,
    anonymSignalPrefix = "tmp",
    targetDirectory = path,
    genLineComments = true,
    keepAll = true
  ).generate(
    {
      val top = new ClkExample()
      val allCDs = ClockDomainUtils.collectClockDomains(top)
      val info = ClockDomainUtils.analyze(allCDs)
      ClockDomainUtils.printDerivationTree(info)
      top
    }
  )
}

object GenV_2 extends App {
  val path = s"Learn/rtl/"
  SpinalConfig(
    mode = Verilog,
    nameWhenByFile = false,
    anonymSignalPrefix = "tmp",
    targetDirectory = path,
    genLineComments = true,
    keepAll = true
  ).generate(
    {
      val top = new ClkExample()
      getUniqueClockDomainSummariesAndPrint(top)
      top
    }
  )
}

