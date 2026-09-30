package app.reporrun.core.domain.repository

import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Perfil de la persona. Hay uno por instalación. */
interface ProfileRepository {
    /** El perfil guardado, o `null` antes de completar el onboarding. */
    fun observeProfile(): Flow<UserProfile?>

    /** Crea o sustituye el perfil. */
    suspend fun saveProfile(profile: UserProfile)
}
