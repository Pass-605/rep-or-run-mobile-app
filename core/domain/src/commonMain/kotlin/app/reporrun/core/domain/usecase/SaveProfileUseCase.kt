package app.reporrun.core.domain.usecase

import app.reporrun.core.domain.repository.ProfileRepository
import app.reporrun.core.model.ProfileId
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

/** Resultado de [SaveProfileUseCase]. */
sealed interface SaveProfileResult {
    /** El perfil quedó guardado como [profile]. */
    data class Saved(val profile: UserProfile) : SaveProfileResult

    /** No se guardó nada. [errors] trae todos los campos que fallan, no solo el primero. */
    data class Invalid(val errors: List<FieldError<ProfileField>>) : SaveProfileResult
}

/**
 * Valida y guarda el perfil. Hay uno por instalación: guardar sustituye al anterior.
 *
 * La validación va antes de construir [UserProfile], así un dato mal tecleado es un
 * [SaveProfileResult.Invalid] y no una excepción del constructor.
 */
class SaveProfileUseCase(private val repository: ProfileRepository, private val clock: Clock) {
    suspend operator fun invoke(input: ProfileInput): SaveProfileResult {
        val clean = input.normalized()
        val errors = clean.errors()
        val profile = if (errors.isEmpty()) clean.toProfileOrNull(currentId(), clock.now()) else null
        return if (profile == null) {
            SaveProfileResult.Invalid(errors)
        } else {
            repository.saveProfile(profile)
            SaveProfileResult.Saved(profile)
        }
    }

    private suspend fun currentId(): ProfileId = repository.observeProfile().first()?.id ?: SINGLE_PROFILE_ID

    private companion object {
        val SINGLE_PROFILE_ID = ProfileId("profile")
    }
}

/**
 * Los errores de un perfil a medio rellenar, sin guardar nada. Aplica las mismas reglas que
 * [SaveProfileUseCase]: un formulario en varios pasos pregunta aquí si puede avanzar, en vez de
 * comparar él los números.
 *
 * Devuelve los errores de **todos** los campos, también los de pasos que la persona aún no ha
 * visto; quien llama se queda con los del paso en el que está.
 */
class ValidateProfileUseCase {
    operator fun invoke(input: ProfileInput): List<FieldError<ProfileField>> = input.normalized().errors()
}

/** El perfil guardado, o `null` antes de completar el onboarding. */
class ObserveProfileUseCase(private val repository: ProfileRepository) {
    operator fun invoke(): Flow<UserProfile?> = repository.observeProfile()
}
