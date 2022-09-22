package parquimetro.mx.com.models

class Ciudades {

    var id: Int?=null
    var strLatitud:String?=null
    var strLongitud : String?=null
    var strCiudad : String?=null

    constructor(
            id: Int,
            strLatitud:String,
            strLongitud:String,
            strCiudad:String
    ){
        this.id = id
        this.strLatitud=strLatitud
        this.strLongitud=strLongitud
        this.strCiudad =strCiudad
    }

}