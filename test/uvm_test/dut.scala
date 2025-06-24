package myTools.test.uvm_test

import spinal.core._
import spinal.core.sim._
import spinal.lib.{MS, _}

import scala.language.postfixOps


case class dut_io() extends Bundle{
  val din = slave Flow UInt(8 bits)
  val dout = master Flow UInt(8 bits)
}
case class DUT() extends Component {
  val io = new dut_io
  io.dout.payload := RegNext(io.din.payload + U(1)) init (0)
  io.dout.valid := RegNext(io.din.valid)
}