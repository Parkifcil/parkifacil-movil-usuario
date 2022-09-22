package parquimetro.mx.com.models

import java.util.*

class SessionStorage {

    companion object {

        var jsonVehiculos : String =""
        var idPlaca : Int = 0
        var dcmLatitud:Double= 0.0
        var dcmLongitud:Double= 0.0
        var jsonData:String=""
        var tokenSession:String=""
        var idUser:String=""
        var strPlaca:String=""
        var status:Int=0
        var idMovimiento:Int=0
        var idZona:Int=0
        var idEspacio:Int=0
        var fechaPago:String=""
        var fechaInicio:String=""
        var fechaFin:String=""
        var fechaInicioDate: Date? = null
        var fechaFinDate: Date? = null
        var tiempoPagado:Long=0
        var idSaldo:Int=0
        var strNombreUsuario:String=""
        var dcmSaldo:Double= 0.0
        var concesion: Int = 0
        var idTarjeta: Int = 0
        var noTarjeta: String = ""
        var recarga: Double? = 0.0
        var multa: Double? = 0.0
        var strNumeroCajon: String = ""

        //La siguiente variable la estoy creando para validar si el usuario ya autentico su cuenta
        var authConfirm:String=""

        //La siguientes variable la estoy creando para validar si la placa ya se encuentra registrada
        var placaConfirm:String=""
        var dataPlaca:String=""
        var dataColor:String=""
        var dataModelo:String=""
        var dataId:String=""

        //La siguiente variable es para cancelar el evento onClick si el usuario entra a la pestaña llamada vehiculos
        var placaStatus:Int=0

    }

    constructor(){

        jsonVehiculos = jsonVehiculos
        idPlaca = idPlaca
        dcmLatitud = dcmLongitud
        dcmLongitud = dcmLongitud
        jsonData = jsonData
        tokenSession = tokenSession
        idUser = idUser
        strPlaca= strPlaca
        status = status
        idMovimiento = idMovimiento
        idZona = idZona
        idEspacio = idEspacio
        fechaPago = fechaPago
        fechaInicio =  fechaInicio
        fechaFin = fechaFin
        tiempoPagado =  tiempoPagado
        idSaldo = idSaldo
        strNombreUsuario =  strNombreUsuario
        dcmSaldo = dcmSaldo


    }
}