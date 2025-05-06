package myTools.eda

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import java.io.File




//trait FlattenHierarchy extends Enumeration with SynthesisOption
//object none extends FlattenHierarchy
//object rebuilt extends FlattenHierarchy
//object full extends FlattenHierarchy
//
//trait GatedClockConversion extends Enumeration with SynthesisOption
//object off extends GatedClockConversion
//object on extends GatedClockConversion
//object auto extends GatedClockConversion

object FlattenHierarchy {
  sealed trait Type
  case object none extends Type
  case object rebuilt extends Type
  case object full extends Type
  val defaultOption:Type = rebuilt
}
object GatedClockConversion {
  sealed trait Type
  case object off extends Type
  case object on extends Type
  case object auto extends Type
  val defaultOption:Type = auto
}

//[-directive]                              Synthesis directive. Values: default,
//RuntimeOptimized, AreaOptimized_high,
//AreaOptimized_medium, AlternateRoutability,
//AreaMapLargeShiftRegToBRAM,
//AreaMultThresholdDSP, FewerCarryChains.
//  Default: default
object Directive {
  sealed trait Type
  case object default extends Type
  case object RuntimeOptimized extends Type
  case object AreaOptimized_high extends Type
  case object AreaOptimized_medium extends Type
  case object AlternateRoutability extends Type
  case object AreaMapLargeShiftRegToBRAM extends Type
  case object AreaMultThresholdDSP extends Type
  case object FewerCarryChains extends Type
  val defaultOption:Type = default
}

object Bufg {
  val defaultOption: Int = 12
}

object EnableLutComb {
  val defaultOption:Boolean = true
}

object Mode {
  sealed trait Type
  case object default extends Type
  case object out_of_context extends Type
  val defaultOption:Type = default
}

object OOC {
  val defaultOption:Boolean = true
}

object KeepEquivalentRegisters {
  val defaultOption:Boolean = true
}

object ResourceShare {
  sealed trait Type
  case object auto extends Type
  case object on extends Type
  case object off extends Type
  val defaultOption:Type = auto
}

object ReTiming {

  val defaultOption:Boolean = true
}

trait SynthesisOption

case class XilinxSynthesisOption (
                                 flattenHierarchy: FlattenHierarchy.Type = FlattenHierarchy.defaultOption,
                                 gatedClockConversion: GatedClockConversion.Type = GatedClockConversion.defaultOption,
                                 directive: Directive.Type = Directive.defaultOption,
                                 bufg: Int = Bufg.defaultOption,
                                 enableLutComb: Boolean = EnableLutComb.defaultOption,
//                                 mode: Mode.Type = Mode.defaultOption,
                                 ooc:Boolean = OOC.defaultOption,
                                 keepEquivalentRegisters: Boolean = KeepEquivalentRegisters.defaultOption,
                                 resourceShare: ResourceShare.Type = ResourceShare.defaultOption,
                                 reTiming: Boolean = ReTiming.defaultOption
                                 ) extends SynthesisOption

