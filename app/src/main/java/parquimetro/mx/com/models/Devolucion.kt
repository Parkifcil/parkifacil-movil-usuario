package parquimetro.mx.com.models

class Devolucion {
    var tiempoDevuelto: Int = 0
    var id: Int = 0
    var intTiempoMinimo: Int = 0
    var montoTotalDevolucion: Double = 0.0
    var montoTotalEstacionado: Double = 0.0
    var minutosOcupados: Int = 0
    var intIdVehiculoId: Int = 0

    constructor(
        tiempoDevuelto: Int,
        id: Int,
        intTiempoMinimo: Int,
        montoTotalDevolucion: Double,
        montoTotalEstacionado: Double,
        minutosOcupados: Int
    ) {
        this.tiempoDevuelto = tiempoDevuelto
        this.id = id
        this.intTiempoMinimo = intTiempoMinimo
        this.montoTotalDevolucion = montoTotalDevolucion
        this.montoTotalEstacionado = montoTotalEstacionado
        this.minutosOcupados = minutosOcupados
    }

    constructor(
        tiempoDevuelto: Int,
        id: Int,
        intTiempoMinimo: Int,
        montoTotalDevolucion: Double,
        montoTotalEstacionado: Double,
        minutosOcupados: Int,
        intIdVehiculoId: Int
    ) {
        this.tiempoDevuelto = tiempoDevuelto
        this.id = id
        this.intTiempoMinimo = intTiempoMinimo
        this.montoTotalDevolucion = montoTotalDevolucion
        this.montoTotalEstacionado = montoTotalEstacionado
        this.minutosOcupados = minutosOcupados
        this.intIdVehiculoId = intIdVehiculoId
    }
}