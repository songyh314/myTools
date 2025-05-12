package myTools.eda


import org.apache.commons.io.FileUtils
import spinal.core._
import spinal.lib.DoCmd.doCmd
import spinal.lib.eda.bench.{Report, Rtl}

import java.io.{File, FileWriter}
import java.nio.file.Paths
import scala.io.Source
import scala.language.postfixOps
import scala.math.BigDecimal.RoundingMode


case class XilinxNonProjectFlow(
                       vivadoPath: String,
                       workspacePath: String,
                       rtl: Rtl,
                       family: String,
                       device: String,
                       frequencyTarget: HertzNumber = null,
                       processorCount: Int = 16,
                       xcix: Seq[String] = null,
                       taskType: TaskType,
                       xilinxSynthesisOption: XilinxSynthesisOption
                     ) extends EdaFlow {
  val targetPeriod = (if (frequencyTarget != null) frequencyTarget else 200 MHz).toTime
  val workspaceFile = new File(workspacePath)
  FileUtils.deleteDirectory(workspaceFile)
  workspaceFile.mkdir()
  for (file <- rtl.getRtlPaths()) {
    FileUtils.copyFileToDirectory(new File(file), workspaceFile)
  }


  val tcl = new java.io.FileWriter(Paths.get(workspacePath, "doit.tcl").toFile)


  override def genScript(): String = {
    var script = ""

    script += s"create_project -force ${rtl.getName()}_project ./${rtl.getName()}_project -part $device\n"
    val readDesign: String = rtl.getRtlPaths().map { file =>
      if (file.endsWith(".v")) {
        s"read_verilog ${Paths.get(file).getFileName()}\n"
      }
      else if (file.endsWith(".bin")) {
        s"add_files ${Paths.get(file).getFileName()}\n"
      }
    }.mkString("\n")
    script += readDesign
    script += "add_files -fileset constrs_1 ./doit.xdc\n"
    if (xcix != null) {
      script += xcix.map { item => s"add_files {$item}" }.mkString("\n")
    }
    script += "import_files -force\n"


    def addXdc(): Unit = {
      val xdc = new FileWriter(Paths.get(workspacePath, "doit.xdc").toFile)
      xdc.write(s"""create_clock -name clk -period ${((targetPeriod * 1e9) toBigDecimal).setScale(4,RoundingMode.HALF_UP)} [get_ports clk]""")
      xdc.write(s"\n")
      xdc.write(
        s"""set_input_delay ${((targetPeriod * 1e8) toBigDecimal).setScale(4,RoundingMode.HALF_UP)} -clock [get_clocks clk] [get_ports -filter {DIRECTION == IN && NAME !~ "rst"}]"""
      )
      xdc.write(s"\n")
      xdc.write(
        s"""set_output_delay ${((targetPeriod * 1e8) toBigDecimal).setScale(4,RoundingMode.HALF_UP)} -clock [get_clocks clk] [get_ports -filter {DIRECTION == OUT}]"""
      )
      xdc.flush();
      xdc.close();
    }

    def addSynTask(): Unit = {
      script += s"synth_design -part ${device} -top ${rtl.getName()}\t"
      if (xilinxSynthesisOption.ooc) {
        script += s"-mode out_of_context\t"
      }
      xilinxSynthesisOption.flattenHierarchy match {
        case FlattenHierarchy.none => script += s"-flatten_hierarchy none\t"
        case FlattenHierarchy.full => script += s"-flatten_hierarchy full\t"
        case FlattenHierarchy.rebuilt => script += s"-flatten_hierarchy rebuilt\t"
      }
      xilinxSynthesisOption.gatedClockConversion match {
        case GatedClockConversion.on => script += s"-gated_clock_conversion on\t"
        case GatedClockConversion.off => script += s"-gated_clock_conversion off\t"
        case GatedClockConversion.auto => script += s"-gated_clock_conversion auto\t"
      }
      if (xilinxSynthesisOption.bufg != -1) script += s"-bufg ${xilinxSynthesisOption.bufg}\t"
      if (!xilinxSynthesisOption.enableLutComb) script += s"-no_lc\t"
      xilinxSynthesisOption.directive match {
        case Directive.default => script += s""
        case Directive.RuntimeOptimized => script += s"-directive RuntimeOptimized\t"
        case Directive.AreaOptimized_high => script += s"-directive AreaOptimized_high\t"
        case Directive.AreaOptimized_medium => script += s"-directive AreaOptimized_medium\t"
        case Directive.AlternateRoutability => script += s"-directive AlternateRoutability\t"
        case Directive.AreaMapLargeShiftRegToBRAM => script += s"-directive AreaMapLargeShiftRegToBRAM\t"
        case Directive.AreaMultThresholdDSP => script += s"-directive AreaMultThresholdDSP\t"
        case Directive.FewerCarryChains => script += s"-directive FewerCarryChains\t"
        case _ => script += s""
      }
      xilinxSynthesisOption.resourceShare match {
        case ResourceShare.on => script += s"-resource_sharing on\t"
        case ResourceShare.off => script += s"-resource_sharing off\t"
        case ResourceShare.auto => script += s"-resource_sharing auto\t"
      }
      if (xilinxSynthesisOption.keepEquivalentRegisters) {
        script += s"-keep_equivalent_registers\t"
      }
      if (xilinxSynthesisOption.reTiming) {
        script += s"-retiming\t"
      } else {
        script += s"-no_retimiming\t"
      }
      script += s"\nwrite_checkpoint -force ${rtl.getName()}_synth.dcp\n"
      script += s"report_timing_summary -delay_type max -report_unconstrained -check_timing_verbose -max_paths 10 -input_pins -file syn_timing.rpt\n"
    }

    def addImplTask(): Unit = {
      script += s"opt_design\n"
      script += s"place_design -directive Explore\n"
//      script += s"report_timing\n"
      script += s"write_checkpoint -force ${rtl.getName()}_after_place.dcp\n"
      script += s"phys_opt_design\n"
//      script += s"report_timing\n"
      script += s"write_checkpoint -force ${rtl.getName()}_after_place_phys_opt.dcp\n"
      script += s"route_design\n"
      script += s"write_checkpoint -force ${rtl.getName()}_after_route.dcp\n"
//      script += s"report_timing\n"
      script += s"phys_opt_design\n"
      script += s"report_timing_summary -delay_type max -report_unconstrained -check_timing_verbose -max_paths 10 -input_pins -file impl_timing.rpt\n"
      script += s"report_utilization\n"
      script += s"report_timing_summary -warn_on_violation\n"
      script += s"report_pulse_width -warn_on_violation -all_violators\n"
      script += s"report_design_analysis -logic_level_distribution\n"
      script += s"write_checkpoint -force ${rtl.getName()}_after_route_phys_opt.dcp\n"
    }

    taskType match {
      case SYN => {
        addXdc()
        addSynTask()
        script += s"report_timing_summary -warn_on_violation\n"
        script += s"report_utilization\n"
      }
      case IMPL => {
        addXdc()
        addSynTask()
        addImplTask()
      }
    }
    tcl.write(script)
    tcl.flush()
    tcl.close()
    script
  }

  override def runScript(): Unit = {
    doCmd(s"$vivadoPath/vivado -nojournal -log doit.log -mode batch -source doit.tcl", workspacePath)
  }

  genScript()
  runScript()

  val log = Source.fromFile(Paths.get(workspacePath, "doit.log").toFile)
  val report: String = log.getLines().mkString
  val r: Report = new Report {
    // Non-logic elements such as PLL or BRAMs may have stricter timing then logic, check for their pulse slack
    // getFMax() will then take this into account later. Uses "report_pulse_width -warn_on_violation -all_violators"
    //
    // Pulse Width Checks
    // <...>
    // Check Type        Corner  Lib Pin             Reference Pin  Required(ns)  Actual(ns)  Slack(ns)  Location      Pin
    // Min Period        n/a     RAMB18E2/CLKARDCLK  n/a            1.569         2.857       1.288      RAMB18_X9Y68  fifo128x32_inst/f/logic_ram_reg/CLKARDCLK
    // Low Pulse Width   Slow    RAMB18E2/CLKARDCLK  n/a            0.542         1.429       0.887      RAMB18_X9Y68  fifo128x32_inst/f/logic_ram_reg/CLKARDCLK
    // High Pulse Width  Slow    RAMB18E2/CLKARDCLK  n/a            0.542         1.429       0.887      RAMB18_X9Y68  fifo128x32_inst/f/logic_ram_reg/CLKARDCLK

    def getPulseSlack(): Double /*nanoseconds*/ = {
      // if not found, assume only logic is involved and do not take pulse slack into account
      var lowest_pulse_slack: Double = 100000.0
      val pulse_strings = "(Min Period|Low Pulse Width|High Pulse Width)(?:\\s+\\S+){5}(?:\\s+)-?(\\d+.?\\d+)+".r
        .findAllIn(report)
        .toList
      // iterate through pulse slack lines
      for (pulse_string <- pulse_strings) {
        // iterate through number columns
        val pulse_slack_numbers = "\\s-?([0-9]+\\.?[0-9]+)+".r.findAllIn(pulse_string).toList
        // third number column is pulse slack
        if (pulse_slack_numbers.length >= 3) {
          if (pulse_slack_numbers.apply(2).toDouble < lowest_pulse_slack) {
            lowest_pulse_slack = pulse_slack_numbers.apply(2).toDouble
          }
        }
      }
      return lowest_pulse_slack
    }

    private def findFirst2StageInReport(regex1st: String, regex2nd: String): String = {
      try {
        regex1st.r.findFirstIn(regex2nd.r.findFirstIn(report).get).get
      } catch {
        case e: Exception => "???"
      }
    }

    override def getFMax(): Double = {
      val intFind = "-?(\\d+\\.?)+"
      var slack =
        try {
          (family match {
            case "Artix 7" | "Kintex 7" | "Virtex 7" | "Kintex UltraScale" | "Kintex UltraScale+" |
                 "Virtex UltraScale+" | "Zynq UltraScale+ MPSoCS" =>
              findFirst2StageInReport(intFind, "-?(\\d+.?)+ns  \\(required time - arrival time\\)")
          }).toDouble
        } catch {
          case e: Exception => -100000.0
        }
      val pulse_slack = getPulseSlack()
      if (pulse_slack < slack) {
        slack = pulse_slack
      }
      return 1.0 / (targetPeriod.toDouble - slack * 1e-9)
    }

    override def getArea(): String = {
      // 0, 30, 0.5, 15,5
      val intFind = "(\\d+,?\\.?\\d*)"
      val leArea =
        try {
          family match {
            case "Artix 7" | "Kintex 7" | "Virtex 7" =>
              findFirst2StageInReport(intFind, "Slice LUTs\\*?[ ]*\\|[ ]*(\\d+,?)+") + " LUT " +
                findFirst2StageInReport(intFind, "Slice Registers[ ]*\\|[ ]*(\\d+,?)+") + " FF " +
                findFirst2StageInReport(intFind, "\\| Block RAM Tile[ ]*\\|([ ]*\\S+\\s+\\|){5}") + " BRAM " +
                findFirst2StageInReport(intFind, "\\| DSPs[ ]*\\|([ ]*\\S+\\s+\\|){5}") + " DSP "
            // Assume the the resources table is the only one with 5 columns (this is the case in Vivado 2021.2)
            // (Not very version-proof, we should actually first look at the right table header first...)
            case "Kintex UltraScale" | "Kintex UltraScale+" | "Virtex UltraScale+" | "Zynq UltraScale+ MPSoCS" =>
              findFirst2StageInReport(intFind, "\\| CLB LUTs[ ]*\\|([ ]*\\S+\\s+\\|){5}") + " LUT " +
                findFirst2StageInReport(intFind, "\\| CLB Registers[ ]*\\|([ ]*\\S+\\s+\\|){5}") + " FF " +
                findFirst2StageInReport(intFind, "\\| Block RAM Tile[ ]*\\|([ ]*\\S+\\s+\\|){5}") + " BRAM " +
                findFirst2StageInReport(intFind, "\\| DSPs[ ]*\\|([ ]*\\S+\\s+\\|){5}") + " DSP " +
                findFirst2StageInReport(intFind, "\\| URAM[ ]*\\|([ ]*\\S+\\s+\\|){5}") + " URAM "
          }
        } catch {
          case e: Exception => "???"
        }
      return leArea
    }
  }

}

object XilinxNonProjectFlow {
  def apply(
             vivadoPath: String,
             workspacePath: String,
             rtl: Rtl,
             family: String,
             device: String,
             frequencyTarget: HertzNumber = null,
             processorCount: Int = 16,
             xcix: Seq[String] = null,
             taskType: TaskType,
             xilinxSynthesisOption: XilinxSynthesisOption
           ): Report = {
    val flow = new XilinxNonProjectFlow(vivadoPath, workspacePath, rtl, family, device, frequencyTarget, processorCount, xcix, taskType, xilinxSynthesisOption)
    flow.r
  }
}