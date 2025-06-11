package myTools.utils

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.fsm._


object fsmUtils extends StateMachine {
  def isNext(target: State): Bool = stateNext === enumOf(target)
}
