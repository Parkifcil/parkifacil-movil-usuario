package parquimetro.mx.com.models

class DataResponse {
    var responseCode: Int? = null
    var responseText: String? = null
    var data: ResponseModel? = null

    constructor(
        responseCode: Int?,
        responseText: String?,
        data: ResponseModel?
    ){
        this.responseCode = responseCode
        this.responseText = responseText
        this.data = data
    }
}