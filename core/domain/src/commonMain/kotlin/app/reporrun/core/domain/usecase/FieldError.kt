package app.reporrun.core.domain.usecase

/**
 * Por qué se rechaza un dato de un formulario. Lleva el límite que se incumplió y **nunca** el
 * valor tecleado: perfil y medidas son datos de salud, y un error acaba fácilmente en un log.
 */
sealed interface FieldIssue {
    /** Falta un dato obligatorio. */
    data object Required : FieldIssue

    /**
     * El número no está entre [min] y [max], los dos incluidos salvo que [minExclusive] diga
     * que [min] queda fuera.
     */
    data class OutOfRange(val min: Double, val max: Double, val minExclusive: Boolean = false) : FieldIssue

    /** El texto tiene menos de [minLength] caracteres. */
    data class TooShort(val minLength: Int) : FieldIssue

    /** El texto, o alguno de los textos de una lista, tiene más de [maxLength] caracteres. */
    data class TooLong(val maxLength: Int) : FieldIssue

    /** Hay más de [max] elementos elegidos. */
    data class TooMany(val max: Int) : FieldIssue
}

/** Un [issue] en el campo [field] de un formulario. */
data class FieldError<F : Enum<F>>(val field: F, val issue: FieldIssue)

/**
 * Acumula los errores de un formulario para devolverlos todos juntos en vez de parar en el
 * primero. Las comprobaciones de rango y longitud dejan pasar `null`: la obligatoriedad es de
 * [required].
 */
internal class FieldChecks<F : Enum<F>> {
    private val found = mutableListOf<FieldError<F>>()

    val errors: List<FieldError<F>>
        get() = found.toList()

    fun add(field: F, issue: FieldIssue) {
        found += FieldError(field, issue)
    }

    fun required(field: F, value: Any?) {
        if (value == null) add(field, FieldIssue.Required)
    }

    /** `NaN` nunca está en un rango, así que también se rechaza. */
    fun inRange(field: F, value: Double?, range: ClosedFloatingPointRange<Double>) {
        if (value != null && value !in range) add(field, FieldIssue.OutOfRange(range.start, range.endInclusive))
    }

    fun inRange(field: F, value: Int?, range: IntRange) {
        if (value != null && value !in range) {
            add(field, FieldIssue.OutOfRange(range.first.toDouble(), range.last.toDouble()))
        }
    }

    fun maxLength(field: F, value: String?, maxLength: Int) {
        if (value != null && value.length > maxLength) add(field, FieldIssue.TooLong(maxLength))
    }

    fun maxCount(field: F, count: Int, max: Int) {
        if (count > max) add(field, FieldIssue.TooMany(max))
    }
}
