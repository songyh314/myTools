package myTools.SUVM
import spinal.core._
import spinal.core.sim._
import spinal.lib._
import scala.collection.mutable
import scala.reflect.ClassTag

trait FactoryCreatable {
  def name: String
}

object SimpleFactory {
  private val registry = mutable.Map[String, () => FactoryCreatable]()

  def register[T <: FactoryCreatable](ctor: () => T): Unit = {
    val name = ctor().name
    registry += name -> ctor
    println(s"[Factory] Registered $name")
  }

  def create[T <: FactoryCreatable](name: String): Option[T] = {
    registry.get(name).map(_.apply().asInstanceOf[T])
  }

  def list(): Unit = {
    println("[Factory] Registered components:")
    registry.keys.foreach(n => println(s"  - $n"))
  }
}