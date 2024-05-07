package xiangshan.vector.vbackend.vexecute.vexucx
import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy.LazyModule
import xiangshan.FuType
import xiangshan.backend.execute.exu.ExuType
import xiangshan.backend.execute.exucx.{BasicExuComplex, BasicExuComplexImp}
import xiangshan.vector.HasVectorParameters
import xiangshan.vector.vbackend.vexecute.vexu.{VAluExu, VDivExu, VFpExu, VMacExu}

class VectorFuComplex(id: Int)(implicit p:Parameters) extends BasicExuComplex{
  private val valu = LazyModule(new VAluExu(id, "VectorFuComplex"))
  // private val vdiv = LazyModule(new VDivExu(id, "VectorFuComplex"))
  // private val vfp = LazyModule(new VFpExu(id, "VectorFuComplex"))
  // private val vmac = LazyModule(new VMacExu(id, "VectorFuComplex"))

  private val fuSeq = Seq(valu)

  for(fu <- fuSeq){
    fu.issueNode :*= issueNode
    writebackNode :=* fu.writebackNode
  }

  lazy val module = new Impl
  class Impl extends BasicExuComplexImp(this, 0) with HasVectorParameters {
    require(issueNode.in.length == 1)
    require(issueNode.out.length == fuSeq.length)
    private val issueIn = issueNode.in.head._1
    private val fuIssPorts = issueNode.out.map(_._1)
    // private val vfpIss = issueNode.out.filter(_._2._2.exuType == ExuType.vfp).head._1
    // private val vdivIss = issueNode.out.filter(_._2._2.exuType == ExuType.vdiv).head._1
    val io = IO(new Bundle {
      val vstart = Input(UInt(log2Up(VLEN).W))
      val vcsr = Input(UInt(3.W))
      val frm = Input(UInt(3.W))
    })

    fuIssPorts.foreach(_ <> issueIn)
    fuSeq.foreach(fu =>fu.module.redirectIn := Pipe(redirectIn))
    fuSeq.foreach({
      case fu:VAluExu =>
        fu.module.io.vstart := io.vstart
        fu.module.io.vcsr := io.vcsr
        fu.module.io.frm := io.frm
      case fu: VDivExu =>
        fu.module.io.vstart := io.vstart
        fu.module.io.vcsr := io.vcsr
        fu.module.io.frm := io.frm
      case fu: VFpExu =>
        fu.module.io.vstart := io.vstart
        fu.module.io.vcsr := io.vcsr
        fu.module.io.frm := io.frm
      case fu: VMacExu =>
        fu.module.io.vstart := io.vstart
        fu.module.io.vcsr := io.vcsr
        fu.module.io.frm := io.frm
    })

    issueIn.issue.ready := true.B

    private val issueFuHit = issueNode.in.head._2._2.exuConfigs.flatMap(_.fuConfigs).map(_.fuType === issueIn.issue.bits.uop.ctrl.fuType).reduce(_|_)
    when(issueIn.issue.valid){assert(issueFuHit)}
  }
}
