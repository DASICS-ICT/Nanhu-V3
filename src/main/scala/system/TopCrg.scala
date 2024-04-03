package system
import chisel3._
import xs.utils.mbist.STD_CLKGT_func
import xs.utils.{DFTResetSignals, ResetGen}

class TopCrg extends Module {
  val io = IO(new Bundle {
    val dfx = Input(new DFTResetSignals)
    val sysClock = Output(Clock())
    val periClock = Output(Clock())
    val sysReset = Output(AsyncReset())
  })
  private val rstSync = Module(new ResetGen(2))
  rstSync.dft := io.dfx
  io.sysReset := rstSync.o_reset
  private val gt_ff = RegInit(true.B)
  gt_ff := ~gt_ff
  private val clkGt = Module(new STD_CLKGT_func)
  clkGt.io.TE := false.B
  clkGt.io.E := gt_ff
  clkGt.io.CK := clock
  clkGt.io.dft_l3dataram_clk := false.B
  clkGt.io.dft_l3dataramclk_bypass := false.B
  io.sysClock := clock
  io.periClock := clkGt.io.Q
}
