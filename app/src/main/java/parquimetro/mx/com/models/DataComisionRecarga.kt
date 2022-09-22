package parquimetro.mx.com.models

class DataComisionRecarga {
    var responseCode: Int? = null
    var responseText: String? = null
    var data: List<ComisionRecarga>? = null

    constructor(
        responseCode: Int?,
        responseText: String?,
        data: List<ComisionRecarga>?
    ){
        this.responseCode = responseCode
        this.responseText = responseText
        this.data = data
    }
}