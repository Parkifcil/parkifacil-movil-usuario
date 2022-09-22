package parquimetro.mx.com.models

class Poligonos {

    var type: String?=null
    var features :Array<Features>?=null

    constructor(
            type: String,
            features:Array<Features>
    ){
        this.type = type
        this.features=features
    }
}