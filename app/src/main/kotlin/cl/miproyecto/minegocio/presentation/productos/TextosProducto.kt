package cl.miproyecto.minegocio.presentation.productos

import cl.miproyecto.minegocio.core.utils.formatoDiaMes
import cl.miproyecto.minegocio.domain.model.Lote

// Texto del diálogo "Eliminar producto".
// Si al producto todavía le quedan unidades, se avisa: dejan de contarse mientras esté
// eliminado (antes el diálogo no lo decía y el stock "desaparecía" sin aviso).
fun textoConfirmarEliminar(nombre: String, unidadesEnLotes: Int): String {
    val base = "¿Seguro que quieres eliminar “$nombre”? Dejará de aparecer, " +
        "pero sus ventas se conservan en los reportes."
    if (unidadesEnLotes <= 0) return base
    val unidades = if (unidadesEnLotes == 1) "1 unidad" else "$unidadesEnLotes unidades"
    return base + "\n\nTodavía tiene $unidades en lotes que dejarán de contarse como stock. " +
        "Si se perdieron, regístralas antes como merma. " +
        "Podrás reactivarlo desde Configuración › Productos eliminados."
}

// Nombre con que se muestra cada lote: "Lote del 24/11" (la fecha en que se compró).
// Antes se mostraba "Lote #4" con el número interno de la base de datos, que es compartido por
// TODOS los productos: el primer lote de un producto podía llamarse "Lote #4".
// Si hay varios lotes comprados el mismo día se agrega un número: "Lote del 24/11 (1)", "(2)"...
fun etiquetasDeLotes(lotes: List<Lote>): Map<Long, String> {
    val porDia = lotes.groupBy { formatoDiaMes(it.fechaIngreso) }
    return lotes.associate { lote ->
        val dia = formatoDiaMes(lote.fechaIngreso)
        val delMismoDia = porDia.getValue(dia).sortedBy { it.idLote }
        val nombre = if (delMismoDia.size == 1) "Lote del $dia" else "Lote del $dia (${delMismoDia.indexOf(lote) + 1})"
        lote.idLote to nombre
    }
}
