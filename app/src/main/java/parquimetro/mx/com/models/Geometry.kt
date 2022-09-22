package parquimetro.mx.com.models

class Geometry {

    var type: String?=null
    var coordinates:Array<Array<Array<String>>>?=null



    constructor(
            type: String,
            coordinates:Array<Array<Array<String>>>
    ){
        this.type = type
        this.coordinates=coordinates
    }
}