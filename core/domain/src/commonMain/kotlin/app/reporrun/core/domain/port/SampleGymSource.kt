package app.reporrun.core.domain.port

import app.reporrun.core.model.BlockPreferences
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.GymId

/**
 * El gimnasio de ejemplo del contrato, para probar la app de punta a punta contra el proxy
 * local en modo fake: solo con su inventario, sus días y sus minutos responde `200`. Solo se
 * ofrece en depuración. Los datos son sintéticos.
 */
fun interface SampleGymSource {
    /** El gimnasio de ejemplo, con su inventario colgando de [gymId]. */
    fun sampleGym(gymId: GymId): SampleGym
}

/**
 * @property equipment el inventario, con los identificadores del ejemplo: el plan de ejemplo
 *   los nombra, y con otros el proxy lo rechazaría.
 * @property preferences los días, los minutos y el cardio del ejemplo.
 */
data class SampleGym(val name: String, val equipment: List<Equipment>, val preferences: BlockPreferences)
