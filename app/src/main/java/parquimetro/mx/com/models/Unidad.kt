package parquimetro.mx.com.models

class Unidad {
    var id: Int? = null
    var createdDate: String? = null
    var lastModifiedBy: String? = null
    var lastModifiedDate: String? = null
    var bitStatus: Boolean? = null
    var strColor: String? = null
    var strModelo: String? = null
    var strPlacas: String? = null
    var intIdUsuarioId: String? = null
    var strMarca: String? = null
    var estado: Boolean? = true

    constructor(
        id: Int?,
        strColor: String?,
        strModelo: String?,
        strPlacas: String?
    ) {
        this.id = id
        this.strColor = strColor
        this.strModelo = strModelo
        this.strPlacas = strPlacas
    }

    constructor(
        id: Int?,
        createdDate: String?,
        lastModifiedBy: String?,
        lastModifiedDate: String?,
        bitStatus: Boolean?,
        strColor: String?,
        strModelo: String?,
        strPlacas: String?,
        intIdUsuarioId: String?,
        strMarca: String?
    ) {
        this.id = id
        this.createdDate = createdDate
        this.lastModifiedBy = lastModifiedBy
        this.lastModifiedDate = lastModifiedDate
        this.bitStatus = bitStatus
        this.strColor = strColor
        this.strModelo = strModelo
        this.strPlacas = strPlacas
        this.intIdUsuarioId = intIdUsuarioId
        this.strMarca = strMarca
    }
}