package parquimetro.mx.com.models

/**
 *
 * @author Sebastian Cruz
 * @author Irais AV
 * @date 02/ 02/ 2022
 */

class Unidades {
    var responseCode: Int? = null
    var responseText: String? = null
    var data: List<Unidad>? = null


    var id: Int?=null
    var strPlacas:String?=null
    var strColor:String?=null
    var strModelo:String?=null


    constructor(id: Int, strPlacas:String, strColor:String, strModelo:String){
        this.id =id
        this.strPlacas=strPlacas
        this.strColor = strColor
        this.strModelo = strModelo
    }

}