package app.reporrun.core.ai.mapper

/**
 * Errores acumulados al construir modelos desde una respuesta del contrato, que no es de fiar.
 * Los constructores de `:core:model` lanzan en la primera invariante rota; aquí cada pieza se
 * construye aparte para seguir con las demás y devolver la lista completa (traspaso de B-02).
 *
 * Los mensajes llevan la ruta del campo (`plan.days[0].exercises[2].dose`) y la regla, nunca
 * nombres ni notas: pueden acabar en un log o en un prompt de reparación.
 *
 * Es el mismo patrón que el `ModelErrors` de `:core:data`, que lee de Room. Los dos son
 * `internal`; si aparece un tercer uso, se mueve a un sitio común.
 */
internal class ModelErrors {
    private val collected = mutableListOf<String>()

    val errors: List<String>
        get() = collected.toList()

    val size: Int
        get() = collected.size

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

/** Resultado de construir un modelo desde una respuesta del contrato. */
internal sealed interface Mapped<out T> {
    data class Valid<T>(val value: T) : Mapped<T>

    /** Todas las reglas rotas, no solo la primera. */
    data class Invalid(val errors: List<String>) : Mapped<Nothing>
}
