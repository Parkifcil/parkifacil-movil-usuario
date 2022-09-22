package parquimetro.mx.com.models

class DataTarifa {

    var responseCode: Int? = null
    var responseText: String? = null
    var data: Tarifa? = null

    constructor(
        responseCode: Int?,
        responseText: String?,
        data: Tarifa?
    ){
        this.responseCode = responseCode
        this.responseText = responseText
        this.data = data
    }
}