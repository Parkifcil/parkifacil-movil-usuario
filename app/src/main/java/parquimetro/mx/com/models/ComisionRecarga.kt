package parquimetro.mx.com.models

class ComisionRecarga {
    var porcentajeComisionRecarga: Double? = null
    var fltTarifaMinima: Double? = null
    var fltIntervaloTarifa: Double? = null
    var intIntervaloEstacionamiento: Int? = null
    var intMinimoEstacionamiento: Int? = null
    var intMaximoEstacionamiento: Int? = null

    constructor(
        porcentajeComisionRecarga: Double?
    ){
        this.porcentajeComisionRecarga = porcentajeComisionRecarga
    }
}