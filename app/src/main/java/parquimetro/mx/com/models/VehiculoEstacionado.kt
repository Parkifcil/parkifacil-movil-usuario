package parquimetro.mx.com.models

class VehiculoEstacionado {
    lateinit var estacionado: MovimientoActivo
    lateinit var libre: Unidad

    constructor(estacionado: MovimientoActivo) {
        this.estacionado = estacionado
    }

    constructor(libre: Unidad) {
        this.libre = libre
    }
}