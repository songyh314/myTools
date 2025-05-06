package myTools.RamIf

import spinal.core._
import spinal.lib._

case class myRamReadOnly(config: myRamConfig) extends Bundle with IMasterSlave {
  val re = Bool()
  val rAddr = UInt(config.addrWidth bits)
  val rData = Bits(config.dataWidth bits)
  override def asMaster(): Unit = {
    out(re, rAddr)
    in(rData)
  }
  def >>(sink: myRamReadOnly): Unit = {
    sink.rAddr := this.rAddr
    sink.re := this.re
    this.rData := sink.rData
  }
  //  def <<(sink: myRamReadOnly): Unit = sink >> this

  def >>(sink: myRamIF): Unit = {
    sink.rAddr := this.rAddr
    sink.re := this.re
    this.rData := sink.rData
  }
  //  def <<(sink: myRamIF): Unit = sink >> this
}
