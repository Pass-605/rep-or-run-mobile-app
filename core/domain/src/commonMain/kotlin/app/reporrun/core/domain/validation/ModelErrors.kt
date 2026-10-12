package app.reporrun.core.domain.validation

/**
 * Errores acumulados al construir modelos desde datos que no se controlan: una respuesta del
 * contrato, una fila de Room, la semilla del catálogo. Los constructores de `:core:model` lanzan
 * en la primera invariante rota; aquí cada pieza se construye aparte para seguir con las demás y
 * devolver la lista completa.
 *
 * Los mensajes llevan la ruta del campo (`plan.days[0].exercises[2].dose`) y la regla, nunca
 * nombres ni notas: pueden acabar en un log o en un prompt de reparación.
 *
 * Es para los mappers de `:core:ai` y `:core:data`, los únicos sitios autorizados a construir
 * modelos desde datos no confiables. Lo que cruza datos entre sí es de [PlanValidator].
 */
class ModelErrors {
    private val collected = mutableListOf<String>()

    /** Lo anotado hasta ahora, en orden. */
    val errors: List<String>
        get() = collected.toList()

    /** Cuántos errores hay; comparar antes y después dice si un nivel falló. */
    val size: Int
        get() = collected.size

    /** Anota que [path] incumple la regla que dice [message]. */
    fun report(path: String, message: String) {
        collected += "$path: $message"
    }

    /**
     * Ejecuta [construct]; si el constructor del modelo rechaza el dato, anota el error en
     * [path] y devuelve `null` en vez de dejar subir la excepción.
     */
    inline fun <T> build(path: String, construct: () -> T): T? = try {
        construct()
    } catch (e: IllegalArgumentException) {
        report(path, e.message ?: "valor inválido")
        null
    }
}

/** Resultado de construir un modelo desde datos que no se controlan. */
sealed interface Mapped<out T> {
    /** Se pudo construir entero. */
    data class Valid<T>(val value: T) : Mapped<T>

    /** Todas las reglas rotas, no solo la primera. */
    data class Invalid(val errors: List<String>) : Mapped<Nothing>
}
