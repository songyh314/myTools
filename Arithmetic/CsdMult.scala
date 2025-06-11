package myTools.Arithmetic

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

object MultUtils {
  def csdEncode(n: BigInt): (Seq[Int], Seq[Int]) = {
    var x = n
    var pos = 0
    val plus = scala.collection.mutable.Buffer[Int]()
    val minus = scala.collection.mutable.Buffer[Int]()

    while (x != 0) {
      val bit = x & 1
      val nextBit = (x >> 1) & 1
      if (bit == 0) {
        // 当前位为0，跳过
        pos += 1
        x = x >> 1
      } else {
        if (nextBit == 1) {
          // 需要用-1表示
          minus += pos
          // 进位
          x = x + (1 << 1)
        } else {
          // 用+1表示
          plus += pos
        }
        pos += 1
        x = x >> 1
      }
    }
    (plus.toSeq, minus.toSeq)
  }

  // 示例

  def main(args: Array[String]): Unit = {
    val n = 7137274
    val (plus, minus) = csdEncode(n)

    println(s"Input number: $n, Bin: ${n.toBinaryString}")
    println(s"+ ops at shifts: ${plus.mkString(", ")}")
    println(s"- ops at shifts: ${minus.mkString(", ")}")

    val reconstructed = plus.map(i => BigInt(1) << i).sum -
      minus.map(i => BigInt(1) << i).sum
    println(s"Reconstructed value: $reconstructed")
  }
}

case class constMulCSD(width: Int = 8, const: BigInt, groupSize: Int = 3) extends Component {

  import MultUtils._

  val io = new Bundle {
    val dataIn = slave Flow UInt(width bits)
    val dataOut = master Flow UInt(2 * width bits)
  }

  require(const > 1, s"const csd is only used when const > 1")

  val (plus, minus) = csdEncode(const)
  println(s"Input number: $const, Bin: ${const.toString(2)}")
  println(s"+ ops at shifts: ${plus.mkString(", ")}")
  println(s"- ops at shifts: ${minus.mkString(", ")}")
  val AddSeq = plus
    .grouped(groupSize)
    .map(item => {
      val tmpAdd = item.map(i => io.dataIn.payload << i).reduceBalancedTree(_ +^ _)
      RegNext(tmpAdd)
    })
    .toSeq
  val SubSeq = minus
    .grouped(groupSize)
    .map(item => {
      val tmpSub = item.map(i => io.dataIn.payload << i).reduceBalancedTree(_ +^ _)
      RegNext(tmpSub)
    })
    .toSeq
  val ret = Reg(UInt(2 * width bits))
  val tmpRet = AddSeq.reduceBalancedTree(_ +^ _) - SubSeq.reduceBalancedTree(_ +^ _)
  ret := tmpRet.resized
  io.dataOut.payload := ret
  io.dataOut.valid := Delay(io.dataIn.valid, 2)
}
