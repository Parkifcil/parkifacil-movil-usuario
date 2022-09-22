package parquimetro.mx.com.models

class Features {

    var type: String?=null
    var geometry : Geometry?=null

    constructor(
            type: String,
            geometry:Geometry
    ){
        this.type = type
        this.geometry=geometry
    }
}