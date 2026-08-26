package com.hub.blueprints.supporting

/**
 * Utilidades de impresión para blueprints.
 * Formatea la salida para que se lea como una narrativa de dominio.
 */

fun banner(title: String, contextGroup: String) {
    println()
    println("╔══════════════════════════════════════════════════════════════╗")
    println("║  ${title.padEnd(56)}║")
    println("║  Context Group: ${contextGroup.padEnd(42)}║")
    println("╚══════════════════════════════════════════════════════════════╝")
    println()
}

fun step(number: Int, title: String) {
    println("═══ Step $number: $title ═══")
}

fun ok(message: String) {
    println("  ✓ $message")
}

fun info(message: String) {
    println("  → $message")
}

fun warn(message: String) {
    println("  ⚠ $message")
}

fun error(message: String) {
    println("  ✗ $message")
}

fun data(label: String, value: Any?) {
    println("    $label: $value")
}

fun separator() {
    println()
}

fun summary(title: String, items: Map<String, Any?>) {
    println()
    println("═══════════════════════════════════════════════════════════")
    println("  $title")
    println("═══════════════════════════════════════════════════════════")
    items.forEach { (key, value) ->
        println("  $key: $value")
    }
    println("═══════════════════════════════════════════════════════════")
}
