package myTools.eda
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import java.io.File

object edaFlow {
  def XilinxDefaultClockDomain = ClockDomainConfig (
    clockEdge = RISING, resetKind = SYNC, resetActiveLevel = HIGH, softResetActiveLevel = HIGH, clockEnableActiveLevel = HIGH
  )

}

abstract class EdaFlow {
  def genScript():String
  def runScript():Unit
}