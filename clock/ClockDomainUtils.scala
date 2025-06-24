package myTools.clock

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.collection.mutable.Set
import scala.collection.mutable

import scala.language.postfixOps


object ClockDomainUtils {
  case class Derivation(parent: ClockDomain, method: String, param: Option[Int])

  private val derivationMap = scala.collection.mutable.Map[ClockDomain, Derivation]()

  def register(child: ClockDomain, parent: ClockDomain, method: String, param: Option[Int] = None): Unit = {
    derivationMap(child) = Derivation(parent, method, param)
  }

  def getDerivations(cd: ClockDomain): Map[ClockDomain, Derivation] = derivationMap.toMap

  def collectClockDomains(root: Component): Set[ClockDomain] = {
    val domains = mutable.Set[ClockDomain]()

    def walk(c: Component): Unit = {
      c.dslBody.foreachStatements {
        case b: BaseType if b.clockDomain != null => domains += b.clockDomain
        case _ =>
      }
      c.children.foreach {
        case cc: Component => walk(cc)
        case _ =>
      }
    }

    walk(root)
    domains.distinctLinked
  }


  case class ClockDomainInfo(cd: ClockDomain, derivation: Option[Derivation])

  def analyze(domains: Set[ClockDomain]): Seq[ClockDomainInfo] = {
    domains.toSeq.map(cd => ClockDomainInfo(cd = cd, derivation = derivationMap.get(cd)))
  }


  def printDerivationTree(infos: Seq[ClockDomainInfo]): Unit = {
    val childrenMap = infos.groupBy(_.derivation.map(_.parent))

    def printTree(cd: ClockDomain, indent: String): Unit = {
      val name = cd.clock.getName()
      val suffix = derivationMap.get(cd) match {
        case Some(d) =>
          val parentName = d.parent.clock.getName()
          val paramStr = d.param.map(p => s"($p)").getOrElse("")
          s" <= $parentName [${d.method}$paramStr]"
        case None => ""
      }
      println(s"$indent- $name$suffix")
      childrenMap.get(Some(cd)).foreach { children =>
        children.foreach(child => printTree(child.cd, indent + "  "))
      }
    }

    infos.filter(_.derivation.isEmpty).map(_.cd).foreach(cd => printTree(cd, ""))
  }

  implicit class RichClockDomain(cd: ClockDomain) {
    def divBy(div: Int): ClockDomain = {
      require(div >= 1, "Divider must be >= 1")
      val dividerArea = new ClockingArea(cd) {
        val counter = Reg(UInt(log2Up(div) bits)) init 0
        val clkReg = Reg(Bool()) init False

        counter := counter + 1
        when(counter === (div - 1)) {
          counter := 0
          clkReg := ~clkReg
        }
        val dividedClock = clkReg
        val newCD = ClockDomain(
          clock = dividedClock,
          reset = cd.reset,
          softReset = cd.softReset,
          clockEnable = cd.clockEnable,
          config = cd.config.copy()
        )
        ClockDomainUtils.register(newCD, cd, "divBy", Some(div))
        newCD.clock.setName(s"${cd.clock.name}_div$div")
        newCD
      }
      dividerArea.newCD
    }
  }

  case class ClockDomainSummary(
                                 cd: ClockDomain,
                                 parent: Option[ClockDomain],
                                 divFactor: Option[Int]
                               )

  def getUniqueClockDomainSummaries(root: Component): Seq[ClockDomainSummary] = {
    // derivationMap 是 Map[ClockDomain, DerivationInfo]
    //    val allDomains = (derivationMap.keys ++ derivationMap.values.map(_.parent)).toSet
    //
    //    allDomains.toSeq.map { cd =>
    //      derivationMap.get(cd) match {
    //        case Some(derivation) if derivation.method == "divBy" =>
    //          ClockDomainSummary(cd, Some(derivation.parent), derivation.param.collect { case i: Int => i })
    //        case Some(derivation) =>
    //          ClockDomainSummary(cd, Some(derivation.parent), None)
    //        case None =>
    //          // 没有记录父节点，说明是根时钟域
    //          ClockDomainSummary(cd, None, None)
    //      }
    //    }
    val allCDs = collectClockDomains(root) ++ derivationMap.keys ++ derivationMap.values.map(_.parent)
    val uniqueCDs = allCDs.toSet

    val ret = uniqueCDs.toSeq.map { cd =>
      derivationMap.get(cd) match {
        case Some(derivation) if derivation.method == "divBy" =>
          ClockDomainSummary(cd, Some(derivation.parent), derivation.param.collect { case i: Int => i })
        case Some(derivation) =>
          ClockDomainSummary(cd, Some(derivation.parent), None)
        case None =>
          // 没有记录父节点，说明是根时钟域（独立创建的）
          ClockDomainSummary(cd, None, None)
      }
    }
    println(ret)
    ret.groupBy(_.cd.clock.getName()) // 用 clock 信号做 key
      .values
      .map(_.head)
      .toSeq
  }


  def getUniqueClockDomainSummariesAndPrint(top: Component): Unit = {
    val allCDs = collectClockDomains(top)
    val infos = analyze(allCDs)
    val summaries = getUniqueClockDomainSummaries(top).distinct
    summaries.foreach { s =>
      val cdName = s.cd.clock.getName()
      val parentName = s.parent.map {
        _.clock.getName()
      }.getOrElse("N/A")
      val divStr = s.divFactor.map(_.toString).getOrElse("N/A")

      println(s"ClockDomain: $cdName, Parent: $parentName, DivFactor: $divStr")
    }

  }

}

