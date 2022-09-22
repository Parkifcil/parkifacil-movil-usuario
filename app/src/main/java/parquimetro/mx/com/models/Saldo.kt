package parquimetro.mx.com.models

class Saldo {

   // var id: String?=null
    //var data: Int?=null
  //  var flt_monto_inicial : Double?=null
  //  var flt_monto_final:Double?=null
 //   var str_forma_pago : String?=null

    var dblSaldoActual: Double = 0.0
    var dblSaldoAnterior: Double = 0.0

    constructor(
         //   id: String,
           //data: Int,
        //    flt_monto_inicial:Double,
         //   flt_monto_final:Double,
        //    str_forma_pago:String,
            dblSaldoActual:Double
    ){
       // this.id = id
       // this.data = data
      //  this.flt_monto_inicial=flt_monto_inicial
      //  this.flt_monto_final=flt_monto_final
      //  this.str_forma_pago =str_forma_pago
        this.dblSaldoActual = dblSaldoActual
    }

    constructor(
        dblSaldoActual:Double,
        dblSaldoAnterior: Double = 0.0
    ){
        this.dblSaldoActual = dblSaldoActual
        this.dblSaldoAnterior = dblSaldoAnterior
    }
}