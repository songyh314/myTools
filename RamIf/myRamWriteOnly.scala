package myTools.RamIf

import spinal.core._
import spinal.lib._

case class myRamWriteOnly(config: myRamConfig) extends Bundle with IMasterSlave {
  val we = Bool()
  val wAddr = UInt(config.addrWidth bits)
  val wData = Bits(config.dataWidth bits)

  override def asMaster(): Unit = {
    out(we, wAddr, wData)
  }
  def <<(that: myRamWriteOnly): Unit = that >> this
  def >>(that: myRamWriteOnly): Unit = {
    that.we := this.we
    that.wAddr := this.wAddr
    that.wData := this.wData
  }

  def << (that: myRamIF): Unit = this >> that
  def >> (that: myRamIF): Unit = {
    that.we := this.we
    that.wAddr := this.wAddr
    that.wData := this.wData
  }

}