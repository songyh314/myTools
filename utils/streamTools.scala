package myTools.utils

import spinal.core._
import spinal.lib._

object StreamRenameUtil {
  def apply(topLevel:Component) = {
    Rename(topLevel,true)
  }

  def Rename(toplevel:Component,isCurrentComponent:Boolean):Boolean={
    //current component process
    if(!isCurrentComponent){
      toplevel.dslBody.foreachStatements{
        case bt:BaseType if bt.parent.isInstanceOf[Stream[_]] => streamRename( bt.parent.asInstanceOf[Stream[_]])
        case bt:BaseType if bt.parent.isInstanceOf[Flow[_]] => flowRename( bt.parent.asInstanceOf[Flow[_]])
        case _ =>
      }
    }else{
      toplevel.dslBody.foreachStatements{
        case bt:BaseType if bt.parent.isInstanceOf[Stream[_]] => toplevel.addPrePopTask(()=>{streamRename( bt.parent.asInstanceOf[Stream[_]])})
        case bt:BaseType if bt.parent.isInstanceOf[Flow[_]] => toplevel.addPrePopTask(()=>{flowRename( bt.parent.asInstanceOf[Flow[_]])})
        case _ =>
      }
    }

    for(child<-toplevel.children){
      Rename(child,false)
    }
    true
  }

  def streamRename(streamPort:Stream[_])={
    streamPort.flatten.foreach((bt)=>{
      val signalName=bt.getName()
      if(signalName.contains("fragment")){
        bt.setName(signalName.replace("_payload_fragment_","_"))
      }else{
        bt.setName(signalName.replace("_payload_","_"))
      }
    })
  }

  def flowRename(flowPort:Flow[_])={
    flowPort.flatten.foreach((bt)=>{
      val signalName=bt.getName()
      if(signalName.contains("fragment")){
        bt.setName(signalName.replace("_payload_fragment_","_"))
      }else{
        bt.setName(signalName.replace("_payload_","_"))
      }
    })
  }

}