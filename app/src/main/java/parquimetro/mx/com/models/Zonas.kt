package parquimetro.mx.com.models

class Zonas {

    var id: Int?=null
    var strLatitud:String?=null
    var strLongitud : String?=null
    var strDescripcion : String?=null
    var strSiglas: String? = null

    constructor(
            id: Int,
            strLatitud:String,
            strLongitud:String,
            strDescripcion:String
    ){
        this.id = id
        this.strLatitud=strLatitud
        this.strLongitud=strLongitud
        this.strDescripcion =strDescripcion
    }

    constructor(
        id: Int,
        strDescripcion:String,
        strSiglas: String?
    ){
        this.id = id
        this.strDescripcion =strDescripcion
        this.strSiglas = strSiglas
    }

    constructor(
        strDescripcion:String
    ){
        this.strDescripcion =strDescripcion
    }
}