package app.reporrun.core.domain.usecase

import kotlin.uuid.Uuid

/**
 * Genera la parte aleatoria de los identificadores que crea la app. Se inyecta para poder
 * fijarla en los tests.
 */
fun interface IdGenerator {
    /**
     * Un identificador nuevo de 1 a 32 caracteres `[A-Za-z0-9]`, el hueco que deja el formato
     * de `EquipmentId` del contrato tras el prefijo `eq_`. No sale de ningún dato de la persona.
     */
    fun newId(): String
}

/** [IdGenerator] por defecto: un UUID v4 aleatorio en hexadecimal, sin guiones. */
object UuidIdGenerator : IdGenerator {
    override fun newId(): String = Uuid.random().toHexString()
}
