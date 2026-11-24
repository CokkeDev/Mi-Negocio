package cl.miproyecto.minegocio.data.mapper

import cl.miproyecto.minegocio.data.local.entity.LoteEntity
import cl.miproyecto.minegocio.domain.model.Lote

// De la base de datos -> a la app
fun LoteEntity.toDomain(): Lote =
    Lote(
        idLote = idLote,
        idProducto = idProducto,
        precioCompra = precioCompra,
        fechaIngreso = fechaIngreso,
        fechaVencimiento = fechaVencimiento,
        unidadesIniciales = unidadesIniciales,
        unidadesDisponibles = unidadesDisponibles,
        costoTotal = costoTotal,
    )

// De la app -> a la base de datos
fun Lote.toEntity(): LoteEntity =
    LoteEntity(
        idLote = idLote,
        idProducto = idProducto,
        precioCompra = precioCompra,
        fechaIngreso = fechaIngreso,
        fechaVencimiento = fechaVencimiento,
        unidadesIniciales = unidadesIniciales,
        unidadesDisponibles = unidadesDisponibles,
        costoTotal = costoTotal,
    )