package parquimetro.mx.com.models

class DataTarjeta {
    var responseCode: Int? = null
    var responseText: String? = null
    var data: List<Tarjeta>? = null

    constructor(
        responseCode: Int?,
        responseText: String?,
        data: List<Tarjeta>?
    ){
        this.responseCode = responseCode
        this.responseText = responseText
        this.data = data
    }
}