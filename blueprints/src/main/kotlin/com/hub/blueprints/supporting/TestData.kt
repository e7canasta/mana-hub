package com.hub.blueprints.supporting

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * Generadores de datos de prueba para blueprints.
 * Nombres únicos por ejecución para evitar colisiones.
 */
object TestData {

    private val counter = AtomicInteger(0)

    fun uniqueSuffix(): String = "${System.currentTimeMillis().toString(36)}-${counter.incrementAndGet()}"

    fun facilityName(base: String = "Residencia"): String = "$base ${uniqueSuffix()}"

    fun username(base: String = "user"): String = "${base}_${uniqueSuffix()}"

    fun residentName(first: String, last: String): String = "$first $last ${uniqueSuffix()}"

    fun today(): LocalDate = LocalDate.now()

    fun yesterday(): LocalDate = LocalDate.now().minusDays(1)
}
