package parquimetro.mx.com.models

class MovimientoActivo {

    var id:Int?=null
    var intIdVehiculoId:Int?=null
    var strPlaca:String?=null
    var strModelo: String? = null
    var strColor: String? = null
    var dtHoraInicio:String?=null
    var dtmHoraFin:String?=null
    var intTiempo:Int?=null
    var fltMonto : Double?=null
    var strComentarios:String?=null
    var intIdMulta:Long?=null
    var estado: Boolean? = false
    var tipo: String? = null
    var strNumeroCajon: String? = null
    var idUser: String? = null

    constructor(
        id:Int,
        strPlaca:String,
        dtHoraInicio:String,
        dtmHoraFin:String,
        intTiempo:Int,
        fltMonto:Double,
        strComentarios:String
    ){
        this.id = id
        this.strPlaca = strPlaca
        this.dtHoraInicio =  dtHoraInicio
        this.dtmHoraFin = dtmHoraFin
        this.intTiempo =  intTiempo
        this.fltMonto = fltMonto
        this.strComentarios = strComentarios
    }

    constructor(
        id:Int,
        intIdVehiculoId:Int,
        strPlaca:String,
        dtHoraInicio:String,
        dtmHoraFin:String,
        intTiempo:Int,
        strModelo: String,
        strColor: String,
        strNumeroCajon: String
    ){
        this.id = id
        this.intIdVehiculoId = intIdVehiculoId
        this.strPlaca = strPlaca
        this.dtHoraInicio =  dtHoraInicio
        this.dtmHoraFin = dtmHoraFin
        this.intTiempo =  intTiempo
        this.strModelo = strModelo
        this.strColor = strColor
        this.strNumeroCajon = strNumeroCajon
    }

    constructor(
        id: Int?,
        intIdVehiculoId: Int?,
        strPlaca: String?,
        dtHoraInicio: String?,
        dtmHoraFin: String?
    ) {
        this.id = id
        this.intIdVehiculoId = intIdVehiculoId
        this.strPlaca = strPlaca
        this.dtHoraInicio = dtHoraInicio
        this.dtmHoraFin = dtmHoraFin
    }

    constructor(
        id: Int?,
        intIdVehiculoId: Int?,
        strPlaca: String?,
        dtHoraInicio: String?,
        dtmHoraFin: String?,
        tipo: String?,
        strNumeroCajon: String?,
        idUser: String?
    ) {
        this.id = id
        this.intIdVehiculoId = intIdVehiculoId
        this.strPlaca = strPlaca
        this.dtHoraInicio = dtHoraInicio
        this.dtmHoraFin = dtmHoraFin
        this.tipo = tipo
        this.strNumeroCajon = strNumeroCajon
        this.idUser = idUser
    }

    constructor(
        intIdVehiculoId: Int?,
        strPlaca: String?,
        dtHoraInicio: String?,
        dtmHoraFin: String?,
        tipo: String?,
        idUser: String?,
        strNumeroCajon: String?
    ) {
        this.intIdVehiculoId = intIdVehiculoId
        this.strPlaca = strPlaca
        this.dtHoraInicio = dtHoraInicio
        this.dtmHoraFin = dtmHoraFin
        this.tipo = tipo
        this.idUser = idUser
        this.strNumeroCajon = strNumeroCajon
    }

}