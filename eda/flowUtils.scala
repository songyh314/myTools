package myTools.eda

import myTools.clock.ClockDomainUtils.ClockDomainSummary
import spinal.core._
import spinal.core.sim._
import spinal.lib._

import scala.collection.mutable
import scala.collection.mutable.Set
import scala.language.postfixOps
import java.io.{File, FileWriter}
import java.nio.file.Paths
import scala.math.BigDecimal.RoundingMode

trait xDevice {
  def family: String

  def part: String
}

case class myDevice(family: String, part: String) extends xDevice


trait DesignPath {
  def rtlPath: Seq[String]

  def xcix: Seq[String]
}

case class myPath(rtlPath: Seq[String], xcix: Seq[String]) extends DesignPath {}

trait Setting {
  def vivadoPath: String = "/opt/Xilinx/Vivado/2023.1/bin"

  def processorCount: Int = 16

  def targetFreq: HertzNumber
}

case class mySetting(targetFreq: HertzNumber) extends Setting

trait TaskType

object SYN extends TaskType

object IMPL extends TaskType

object FULL extends TaskType



object flowTools {
  def getFiles(path: String): Seq[String] = {
    val f = new File(path)
    if (f.exists() && f.isDirectory) {
      f.listFiles.filter(_.isFile).map(_.getName).toSeq
    } else {
      Seq.empty[String]
    }
  }

  def genXdc(workspacePath: String, clockDomainSummary: Seq[ClockDomainSummary], clkFreqConfig: Map[String, HertzNumber]): Unit = {
    val xdc = new FileWriter(Paths.get(workspacePath, "doit.xdc").toFile)
    val rootClk = Set[String]()
    clockDomainSummary.foreach { s =>
      s.parent match {
        case Some(parentCD) => {
          xdc.write(
            s"""create_clock -name ${s.cd.clock.getName()} -source [get_pins ${s.cd.clock.getName()}_div${s.divFactor.getOrElse(1)}_reg/C] -divide_by ${s.divFactor.getOrElse(1)}
               |[get_pins ${s.cd.clock.getName()}_div${s.divFactor.getOrElse(1)}_reg/Q]\n """.stripMargin)
        }
        case None => {
          val clkName = s.cd.clock.getName()
          rootClk += clkName
          val period = clkFreqConfig.getOrElse(clkName, 300 MHz).toTime
          xdc.write(s"""create_clock -name ${clkName} -period ${((period * 1e9) toBigDecimal).setScale(4, RoundingMode.HALF_UP)} [get_ports clk] \n""")
        }
      }
    }
    if (rootClk.size > 1) {
      var async = s"set_clock_groups -asynchronous "
      rootClk.foreach { clkName =>
        async += s"-group $clkName "
      }
      async += "\n"
      xdc.write(async)
    }

  }
}