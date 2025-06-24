package myTools.SUVM
import spinal.core._
import spinal.core.sim._
import spinal.lib._


trait Transaction {
  def name: String

  def randomize(): Unit
}