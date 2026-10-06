package app.reporrun.feature.profile

/*
 * Ida y vuelta entre lo que hay en un campo numérico y el número. No valida nada: solo impide
 * teclear lo que no es un número y traduce la coma. Los límites son del caso de uso.
 */

private const val DECIMAL_SEPARATOR = ','
private const val MAX_INTEGER_DIGITS = 3
private const val MAX_DECIMAL_DIGITS = 1

/** Deja solo dígitos: lo que admite el campo de la edad. */
internal fun String.asIntegerInput(): String = filter(Char::isDigit).take(MAX_INTEGER_DIGITS)

/**
 * Deja dígitos y una sola coma, con un decimal: «72,5». El punto se acepta y se convierte,
 * porque hay teclados numéricos que solo ofrecen uno de los dos.
 */
internal fun String.asDecimalInput(): String {
    val clean = replace('.', DECIMAL_SEPARATOR).filter { it.isDigit() || it == DECIMAL_SEPARATOR }
    val integer = clean.substringBefore(DECIMAL_SEPARATOR).take(MAX_INTEGER_DIGITS)
    if (DECIMAL_SEPARATOR !in clean) return integer
    val decimals = clean.substringAfter(DECIMAL_SEPARATOR).filter(Char::isDigit).take(MAX_DECIMAL_DIGITS)
    return "$integer$DECIMAL_SEPARATOR$decimals"
}

/** El número de un campo decimal, o `null` si está vacío o a medio escribir («,»). */
internal fun String.toDecimalOrNull(): Double? = replace(DECIMAL_SEPARATOR, '.').toDoubleOrNull()

/** Un número como se escribe en un campo: «170», «72,5». Sin decimales si no los tiene. */
internal fun Double.toFieldText(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString().replace('.', DECIMAL_SEPARATOR)
