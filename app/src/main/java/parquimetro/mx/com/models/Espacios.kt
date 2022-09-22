package parquimetro.mx.com.models

class Espacios {

    var id: Int?=null
    var str_latitud:String?=null
    var str_longitud : String?=null
    var str_clave : String?=null
    var bit_status : Boolean?=null
    var bit_ocupado : Boolean?=null




    constructor(


            id: Int,
            str_latitud:String,
            str_longitud:String,
            str_clave:String,
            bit_status:Boolean,
            bit_ocupado:Boolean




    ){


        this.id = id
        this.str_latitud=str_latitud
        this.str_longitud=str_longitud
        this.str_clave = str_clave
        this.bit_status = bit_status
        this.bit_ocupado = bit_ocupado








    }
}