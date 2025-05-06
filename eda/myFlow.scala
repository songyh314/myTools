package myTools.eda

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl

import java.io.File
import scala.language.postfixOps


class myFlow[T <: Component](design: => T,
                             topName: String,
                             device: xDevice,
                             workspace: String,
                             xcix: Seq[String] = null,
                             setting: Setting) {


  private val rootSpace = workspace ++ "/" ++ topName
  private val rtlSpace = rootSpace ++ "/rtl"
  private val fpgaSpace = rootSpace ++ "/fpga"

  SpinalConfig(mode = Verilog,
    nameWhenByFile = false,
    anonymSignalPrefix = "tmp",
    genLineComments = true,
    targetDirectory = rtlSpace).generate(design).toplevelName
  private val rtlPath = flowTools.getFiles(rtlSpace)
  private val absRtlSpace = new File(rtlSpace).getAbsolutePath
  val absRtlPath: Seq[String] = rtlPath.map(path => absRtlSpace ++ "/" ++ path)

  val rtl = new Rtl {
    /** Name */
    override def getName(): String = topName

    override def getRtlPaths(): Seq[String] = absRtlPath
  }


  val task = XilinxProjectFlow(vivadoPath = setting.vivadoPath,
    workspacePath = fpgaSpace,
    rtl = rtl,
    family = device.family,
    device = device.part,
    frequencyTarget = setting.targetFreq,
    processorCount = setting.processorCount,
    xcix = xcix)
  println(s"${device.family} -> ${(task.getFMax / 1e6).toInt} MHz ${task.getArea} ")
}


class myXilinx(design: => Component,
               device: xDevice,
               workspace: String,
               xcix: Seq[String] = null,
               setting: Setting = mySetting(targetFreq = 300 MHz),
               xilinxSynthesisOption: XilinxSynthesisOption,
               taskType: TaskType,
               nonProjectMode: Boolean = true
              ) {

  //  private val rootSpace = workspace ++ "/" ++ topName
  private val rtlSpace = workspace ++ "/rtl"
  private val fpgaSpace = workspace ++ "/fpga"

  private val topName = SpinalConfig(mode = Verilog,
    nameWhenByFile = false,
    anonymSignalPrefix = "tmp",
    genLineComments = true,
    targetDirectory = rtlSpace).generate(design).toplevelName

  private val rtlPath = flowTools.getFiles(rtlSpace)
  private val absRtlSpace = new File(rtlSpace).getAbsolutePath
  val absRtlPath: Seq[String] = rtlPath.map(path => absRtlSpace ++ "/" ++ path)

  val rtl = new Rtl {
    /** Name */
    override def getName(): String = topName

    override def getRtlPaths(): Seq[String] = absRtlPath
  }


  val task = if (nonProjectMode) {
    XilinxNonProjectFlow(vivadoPath = setting.vivadoPath,
      workspacePath = fpgaSpace,
      rtl = rtl,
      family = device.family,
      device = device.part,
      frequencyTarget = setting.targetFreq,
      processorCount = setting.processorCount,
      xcix = xcix,
      taskType = taskType,
      xilinxSynthesisOption = xilinxSynthesisOption
    )
  } else {
    XilinxProjectFlow(vivadoPath = setting.vivadoPath,
      workspacePath = fpgaSpace,
      rtl = rtl,
      family = device.family,
      device = device.part,
      frequencyTarget = setting.targetFreq,
      processorCount = setting.processorCount,
      xcix = xcix)
  }
  println(s"${device.family} -> ${(task.getFMax / 1e6).toInt} MHz ${task.getArea} ")

}