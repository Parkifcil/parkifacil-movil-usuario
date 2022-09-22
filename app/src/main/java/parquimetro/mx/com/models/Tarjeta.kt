package parquimetro.mx.com.models

class Tarjeta {
    var id: Int? = null
    var dcManoVigencia: Int? = null
    var dcmMesVigencia: Int? = null
    var strTarjeta: String? = null
    var strTitular: String? = null

    constructor(
        id: Int?,
        dcManoVigencia: Int?,
        dcmMesVigencia: Int?,
        strTarjeta: String?,
        strTitular: String?
    ) {
        this.id = id
        this.dcManoVigencia = dcManoVigencia
        this.dcmMesVigencia = dcmMesVigencia
        this.strTarjeta = strTarjeta
        this.strTitular = strTitular
    }
}