package myTools.RamIf

import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc.SizeMapping

case class myRamConfig(dataWidth: Int, addrWidth: Int, readLatency: Int = 1)
case class myRamIF(config: myRamConfig) extends Bundle with IMasterSlave {
  val re = Bool()
  val rAddr = UInt(config.addrWidth bits)
  val rData = Bits(config.dataWidth bits)
  val we = Bool()
  val wAddr = UInt(config.addrWidth bits)
  val wData = Bits(config.dataWidth bits)

  override def asMaster(): Unit = {
    out(re, we, rAddr, wAddr, wData)
    in(rData)
  }

  def >>(sink: myRamIF): Unit = {
    sink.rAddr := this.rAddr
    sink.re := this.re
    this.rData := sink.rData

    sink.we := this.we
    sink.wAddr := this.wAddr
    sink.wData := this.wData
  }
  def <<(sink: myRamIF): Unit = {
    sink >> this
  }
  def << (that:myRamReadOnly):Unit={
    this.re := that.re
    this.rAddr := that.rAddr
    that.rData := this.rData
  }
  def << (that:myRamWriteOnly):Unit={
    this.we := that.we
    this.wAddr := that.wAddr
    this.wData := that.wData
  }
}

class myRamDecoder(inputConfig: myRamConfig, decodings: Seq[SizeMapping]) extends Component {

  val io = new Bundle {
    val input = slave(myRamIF(inputConfig))
    val outputs = Vec(master(myRamIF(inputConfig)), decodings.size)
  }

  val sel = Bits(decodings.size bits)

  for ((output, index) <- io.outputs.zipWithIndex) {
    output.rAddr := io.input.rAddr
    output.wAddr := io.input.wAddr
    output.wData := io.input.wData
    output.we := io.input.we
    output.re := io.input.re && sel(index)
  }

  for ((decoding, psel) <- (decodings, sel.asBools).zipped) {
    psel := decoding.hit(io.input.rAddr) & io.input.re
  }

  val selIndex = Delay(OHToUInt(sel), inputConfig.readLatency)
  io.input.rData := io.outputs(selIndex).rData
}

object myRamDecoder {

  /** Map all slave bram bus on a master bram bus
    */
  def apply(master: myRamIF, slaves: Seq[(myRamIF, SizeMapping)]): myRamDecoder = {

    val decoder = new myRamDecoder(master.config, slaves.map(_._2))

    // connect the master bus to the decoder
    decoder.io.input <> master

    // connect all slave to the decoder
    (slaves.map(_._1), decoder.io.outputs).zipped.map(_ << _)

    decoder
  }
}
