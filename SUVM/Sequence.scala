package myTools.SUVM


import scala.collection.mutable.Queue
import spinal.core._
import spinal.core.sim._
import spinal.lib._

case class Sequence(val priority: Int) {
  private val txQueue = scala.collection.mutable.Queue[Transaction]()
  private var _locked = false
  var sequencer: Option[ISequencer] = None

  def enqueue(tx: Transaction): Unit = synchronized {
    txQueue.enqueue(tx)
  }

  def getNext: Option[Transaction] = synchronized {
    if (txQueue.nonEmpty) Some(txQueue.dequeue()) else None
  }

  def lock(): Boolean = synchronized {
    if (!_locked) {
      val success = (sequencer.exists(_.requestLock(this)))
      _locked = success
      success
    } else {
      println(s"[${this}] Lock request denied, this sequence has already been locked.")
      false
    }
  }

  def unlock(): Unit = synchronized {
    if (_locked) {
      _locked = false
      sequencer.foreach(_.releaseLock(this))
    }
  }

  def isLocked: Boolean = synchronized {
    _locked
  }

  def hasPending: Boolean = synchronized {
    txQueue.nonEmpty
  }

  //  def body():Unit

  def doItem(tx: Transaction)(init: Transaction => Unit = _ => ()): Unit = {
    init(tx)
    enqueue(tx)
    println(s"[${this}] doItem: ${tx}")
  }

  override def toString: String = this.getClass.getSimpleName
}


trait ISequencer {
  def requestLock(seq: Sequence): Boolean

  def releaseLock(seq: Sequence): Unit

  def selectNext(): Option[Transaction]

  def register(seq: Sequence): Unit
}

class Sequencer extends ISequencer {
  private val sequences = scala.collection.mutable.ListBuffer[Sequence]()
  private var lockedSequence: Option[Sequence] = None

  override def register(seq: Sequence): Unit = synchronized {
    sequences += seq
    seq.sequencer = Some(this)
  }

  override def requestLock(seq: Sequence): Boolean = synchronized {
    if (lockedSequence.isEmpty) {
      lockedSequence = Some(seq)
      println(s"[Sequencer] Lock granted to ${seq}")
      true
    } else {
      println(s"[Sequencer] Lock denied to ${seq}, already locked by ${lockedSequence.get}")
      false
    }
  }

  override def releaseLock(seq: Sequence): Unit = synchronized {
    if (lockedSequence.contains(seq)) {
      lockedSequence = None
      println(s"[Sequencer] Lock released by ${seq}")
    }
  }


override def selectNext(): Option[Transaction] = synchronized {
  // 优先处理 locked sequence
  lockedSequence match {
    case Some(seq) if seq.hasPending => seq.getNext
    case _ =>
      val sortedSeqs = sequences.filter(s => s.hasPending && !s.isLocked).sortBy(-_.priority)
      for (seq <- sortedSeqs) {
        val txOpt = seq.getNext
        if (txOpt.isDefined) return txOpt
      }
      None
  }
}

}

object SequenceTest extends App {
  val seq1 = Sequence(priority = 1)
  val seq2 = Sequence(priority = 2)

  case class Tx(str: String) extends Transaction {
    var s: Option[String] = Some(str)

    override def name: String = s"Tx"

    override def randomize(): Unit = {

    }
  }

  val sequencer = new Sequencer
  sequencer.register(seq1)
  sequencer.register(seq2)

  seq1.doItem(Tx("Item1"))()
  seq1.doItem(Tx("Item2"))()
  seq2.doItem(Tx("Item3"))()

  println(sequencer.selectNext()) // Should return Item1
  println(sequencer.selectNext()) // Should return Item2
  println(sequencer.selectNext()) // Should return Item3

}