package app.reporrun.feature.gym

/*
 * Ida y vuelta entre lo que hay en un campo de kilos y el número. No valida nada: solo impide
 * teclear lo que no es un número y traduce la coma. Los límites son del caso de uso.
 */

private const val DECIMAL_SEPARATOR = ','
private const val MAX_INTEGER_DIGITS = 4
private const val MAX_DECIMAL_DIGITS = 2

/**
 * Deja dígitos y una sola coma, con dos decimales: «2,25». El punto se acepta y se convierte,
 * porque hay teclados numéricos que solo ofrecen uno de los dos.
 */
internal fun String.asKgInput(): String {
    val clean = replace('.', DECIMAL_SEPARATOR).filter { it.isDigit() || it == DECIMAL_SEPARATOR }
    val integer = clean.substringBefore(DECIMAL_SEPARATOR).take(MAX_INTEGER_DIGITS)
    if (DECIMAL_SEPARATOR !in clean) return integer
    val decimals = clean.substringAfter(DECIMAL_SEPARATOR).filter(Char::isDigit).take(MAX_DECIMAL_DIGITS)
    return "$integer$DECIMAL_SEPARATOR$decimals"
}

/** Los kilos de un campo, o `null` si está vacío o a medio escribir («,»). */
internal fun String.toKgOrNull(): Double? = replace(DECIMAL_SEPARATOR, '.').toDoubleOrNull()

/** Unos kilos como se escriben: «30», «2,5». Sin decimales si no los tiene. */
internal fun Double.toKgText(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString().replace('.', DECIMAL_SEPARATOR)
