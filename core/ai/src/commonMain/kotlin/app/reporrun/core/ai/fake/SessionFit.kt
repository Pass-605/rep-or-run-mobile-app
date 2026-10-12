package app.reporrun.core.ai.fake

import app.reporrun.core.domain.validation.SessionVolume
import app.reporrun.core.model.PlanDay

/** Con cardio, un día no baja de aquí: antes de dejarlo en un ejercicio se quita el cardio. */
private const val MIN_EXERCISES_WITH_CARDIO = 2

/**
 * El día recortado para caber en [sessionMinutes] según [SessionVolume], la misma cuenta que
 * hace `PlanValidator`. El ejemplo del contrato está pensado para 75 minutos y con sesiones más
 * cortas no pasaría la regla de volumen.
 *
 * Se queda con lo primero que cabe: con cardio, quitando ejercicios del final mientras queden
 * [MIN_EXERCISES_WITH_CARDIO]; después sin cardio, con todos y quitando del final hasta dejar
 * uno. Quitar del final deja los códigos (`L01`, `L02`…) como estaban.
 */
internal fun PlanDay.fittedTo(sessionMinutes: Int): PlanDay {
    val withCardio = (exercises.size downTo MIN_EXERCISES_WITH_CARDIO).asSequence()
        .map { copy(exercises = exercises.take(it)) }
    val withoutCardio = (exercises.size downTo 1).asSequence()
        .map { copy(cardio = null, exercises = exercises.take(it)) }

    return (withCardio + withoutCardio).firstOrNull { SessionVolume.fits(it, sessionMinutes) }
        ?: copy(cardio = null, exercises = exercises.take(1))
}
