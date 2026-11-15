package cl.miproyecto.minegocio.data.mapper

import cl.miproyecto.minegocio.data.local.entity.GastoEntity
import cl.miproyecto.minegocio.data.local.entity.PerdidaEntity
import cl.miproyecto.minegocio.domain.model.CategoriaGasto
import cl.miproyecto.minegocio.domain.model.Gasto
import cl.miproyecto.minegocio.domain.model.Perdida

// Traductores de Perdida y Gasto (igual que ProductoMapper y LoteMapper).

fun Perdida.toEntity(): PerdidaEntity =
    PerdidaEntity(
        idPerdida = idPerdida,
        idLote = idLote,
        cantidad = cantidad,
        motivo = motivo,
        nota = nota,
        fecha = fecha,
    )

fun GastoEntity.toDomain(): Gasto =
    Gasto(
        idGasto = idGasto,
        descripcion = descripcion,
        monto = monto,
        // En la base de datos la categoría es texto ("SERVICIOS"); aquí se vuelve enum.
        categoria = CategoriaGasto.desdeNombre(categoria),
        fecha = fecha,
    )

fun Gasto.toEntity(): GastoEntity =
    GastoEntity(
        idGasto = idGasto,
        descripcion = descripcion,
        monto = monto,
        // ".name" da el nombre del enum como texto: "SERVICIOS".
        categoria = categoria.name,
        fecha = fecha,
    )
