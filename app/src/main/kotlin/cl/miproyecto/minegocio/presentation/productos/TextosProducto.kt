package cl.miproyecto.minegocio.presentation.productos

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
