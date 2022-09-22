package parquimetro.mx.com.models

class Tarifa {
    var id: Int? = null
    var fltTarifaMin: Double? = 0.0
    var fltTarifaIntervalo: Double? = 0.0
    var fltTarifaMax: Double? = 0.0
    var boolCobroFraccion: Boolean? = null
    var intIntervaloMinutos: Int? = 0
    var intTiempoMaximo: Int? = 0
    var intTiempoMinimo: Int? = 0
    var intidconcesionId: Int? = 0

    constructor(
        id: Int?,
        fltTarifaMin: Double?,
        fltTarifaIntervalo: Double?,
        fltTarifaMax: Double?,
        boolCobroFraccion: Boolean?,
        intIntervaloMinutos: Int?,
        intTiempoMaximo: Int?,
        intTiempoMinimo: Int?,
        intidconcesionId: Int?
    ) {
        this.id = id
        this.fltTarifaMin = fltTarifaMin
        this.fltTarifaIntervalo = fltTarifaIntervalo
        this.fltTarifaMax = fltTarifaMax
        this.boolCobroFraccion = boolCobroFraccion
        this.intIntervaloMinutos = intIntervaloMinutos
        this.intTiempoMaximo = intTiempoMaximo
        this.intTiempoMinimo = intTiempoMinimo
        this.intidconcesionId = intidconcesionId
    }
}