package myTools.eda

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import java.io.File

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
}