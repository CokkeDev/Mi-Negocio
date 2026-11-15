package cl.miproyecto.minegocio.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// Ayudas para trabajar con fechas guardadas como milisegundos (Long).
// Se usa Calendar porque funciona en todos los Android desde la versión 7 (minSdk 24).

private val LOCALE_ES = Locale("es", "CL")

// Devuelve un Calendar en la fecha "t" a las 00:00:00.
private fun calendarioDia(t: Long): Calendar =
    Calendar.getInstance().apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

// Las 00:00 del día de "t".
fun inicioDelDia(t: Long = System.currentTimeMillis()): Long = calendarioDia(t).timeInMillis

// Las 00:00 del día siguiente (sirve como "hasta" de hoy).
fun inicioDelDiaSiguiente(t: Long = System.currentTimeMillis()): Long =
    calendarioDia(t).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

// El lunes de esta semana a las 00:00.
fun inicioDeSemana(t: Long = System.currentTimeMillis()): Long =
    calendarioDia(t).apply {
        // En Calendar el domingo es 1 y el lunes 2. Se cuentan los días desde el lunes.
        val desdeLunes = (get(Calendar.DAY_OF_WEEK) + 5) % 7
        add(Calendar.DAY_OF_MONTH, -desdeLunes)
    }.timeInMillis

// El día 1 del mes de "t", corrido "meses" meses (ej: -1 = el mes pasado, 1 = el próximo).
fun inicioDeMes(t: Long = System.currentTimeMillis(), meses: Int = 0): Long =
    calendarioDia(t).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, meses)
    }.timeInMillis

// El 1 de enero del año de "t".
fun inicioDeAnio(t: Long = System.currentTimeMillis(), anios: Int = 0): Long =
    calendarioDia(t).apply {
        set(Calendar.DAY_OF_YEAR, 1)
        add(Calendar.YEAR, anios)
    }.timeInMillis

// Cuántos días faltan desde hoy hasta "fecha" (0 = hoy, 1 = mañana, negativo = ya pasó).
// Se redondea porque con el cambio de horario un día puede durar 23 o 25 horas.
fun diasHasta(fecha: Long, hoy: Long = System.currentTimeMillis()): Int =
    Math.round((inicioDelDia(fecha) - inicioDelDia(hoy)).toDouble() / TimeUnit.DAYS.toMillis(1)).toInt()

// "dd/MM" -> 01/12
fun formatoDiaMes(t: Long): String = SimpleDateFormat("dd/MM", LOCALE_ES).format(t)

// "dd/MM/yyyy" -> 01/12/2026
fun formatoFecha(t: Long): String = SimpleDateFormat("dd/MM/yyyy", LOCALE_ES).format(t)

// "2026-11": la misma forma que entrega strftime('%Y-%m') en SQLite.
fun claveMes(t: Long): String = SimpleDateFormat("yyyy-MM", Locale.US).format(t)

// "Nov", "Dic"...
fun nombreMesCorto(t: Long): String =
    SimpleDateFormat("MMM", LOCALE_ES).format(t).replace(".", "").replaceFirstChar { it.uppercase() }

// "Noviembre"
fun nombreMes(t: Long): String =
    SimpleDateFormat("LLLL", LOCALE_ES).format(t).replaceFirstChar { it.uppercase() }

// "Sábado 14 de noviembre"
fun fechaLarga(t: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("EEEE d 'de' MMMM", LOCALE_ES).format(t).replaceFirstChar { it.uppercase() }
