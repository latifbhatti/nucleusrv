package nucleusrv.csr

import chisel3._
import chisel3.util._

case class CSROperations(
    val READ    :UInt   =   0.U(2.W),
    val WRITE   :UInt   =   1.U(2.W),
    val SET     :UInt   =   2.U(2.W),
    val CLEAR   :UInt   =   3.U(2.W)
)

class CSRRegFile extends Module{
    val io = IO(new CSRRegFileIO)

    /****************** Initializations ******************/
    // Registers
    val MISA_REG            = RegInit(0.U(32.W))
    val MHARTID_REG         = RegInit(0.U(32.W))
    val MCAUSE_REG          = RegInit(0.U(32.W))
    val MTVEC_REG           = RegInit(0.U(32.W))
    val MEPC_REG            = RegInit(0.U(32.W))
    val MIE_REG             = RegInit(0.U(32.W))
    
    // MSTATUS
    val MSTATUS_TW_REG      = RegInit(0.U(1.W))
    val MSTATUS_MPRV_REG    = RegInit(0.U(1.W))
    val MSTATUS_MPP_REG     = RegInit(0.U(2.W))
    val MSTATUS_MPIE_REG    = RegInit(0.U(1.W))
    val MSTATUS_MIE_REG     = RegInit(0.U(1.W))

    // Hardwired
    MISA_REG                := io.MISA.i_value
    MHARTID_REG             := io.MHARTID.i_value

    // Wires
    val w_data                  = Wire(UInt(32.W))
    val r_data                  = Wire(UInt(32.W))
    val MSTATUS_WIRE            = WireInit(Cat("b0".U(10.W), MSTATUS_TW_REG, "b0".U(3.W), MSTATUS_MPRV_REG, "b0".U(4.W), MSTATUS_MPP_REG, "b0".U(3.W), MSTATUS_MPIE_REG, "b0".U(3.W), MSTATUS_MIE_REG, "b0".U(3.W)))
    val MCAUSE_WLRL_WIRE        = WireInit(MCAUSE_REG(30,0))
    val MCAUSE_INTERRUPT_WIRE   = WireInit(MCAUSE_REG(31))
    val MTVEC_MODE_WIRE         = WireInit(MTVEC_REG(1,0))
    val MTVEC_BASE_WIRE         = WireInit(MTVEC_REG(31,2))

    val csr_opr = CSROperations()
    /*************************************************/

    /****************** Read Logic ******************/
    var READ,WRITE,SET,CLEAR = Wire(UInt(2.W))
    Seq(READ,WRITE,SET,CLEAR) zip Seq(csr_opr.READ, csr_opr.WRITE, csr_opr.SET, csr_opr.CLEAR) map (x => x._1 := x._2)

    val READ_CASES = Array(
        AddressMap.MISA    -> MISA_REG,
        AddressMap.MHARTID -> MHARTID_REG,
        AddressMap.MSTATUS -> MSTATUS_WIRE,
        AddressMap.MCAUSE  -> MCAUSE_REG,
        AddressMap.MTVEC   -> MTVEC_REG,
        AddressMap.MEPC    -> MEPC_REG,
        AddressMap.MIE     -> MIE_REG
    )

    r_data := MuxLookup(io.CSR.i_addr, DontCare, READ_CASES)

    io.CSR.o_data := r_data
    /*************************************************/

    /****************** Write Logic ******************/
    val set_data   = r_data |  io.CSR.i_data
    val clear_data = r_data & ~io.CSR.i_data

    // Identify the operation
    w_data := MuxLookup(io.CSR.i_opr, DontCare, Array(
        WRITE -> io.CSR.i_data,
        SET   -> set_data,
        CLEAR -> clear_data
    ))

    // Write to the register
    when(io.CSR.i_w_en){
        switch(io.CSR.i_addr){
            is(AddressMap.MSTATUS){
                MSTATUS_TW_REG   := w_data(21)
                MSTATUS_MPRV_REG := w_data(17)
                MSTATUS_MPP_REG  := w_data(12,11)
                MSTATUS_MPIE_REG := w_data(7)
                MSTATUS_MIE_REG  := w_data(3)
            }
            is(AddressMap.MCAUSE){
                MCAUSE_REG       := w_data
            }
            is(AddressMap.MTVEC){
                MTVEC_REG        := w_data
            }
            is(AddressMap.MEPC){
                MEPC_REG         := w_data
            }
            is(AddressMap.MIE){
                MIE_REG          := w_data
            }
        }
    }
    /*************************************************/
}