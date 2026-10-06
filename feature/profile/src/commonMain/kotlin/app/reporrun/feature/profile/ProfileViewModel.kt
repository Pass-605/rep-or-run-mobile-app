package app.reporrun.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.reporrun.core.domain.usecase.FieldError
import app.reporrun.core.domain.usecase.ObserveProfileUseCase
import app.reporrun.core.domain.usecase.ProfileField
import app.reporrun.core.domain.usecase.ProfileInput
import app.reporrun.core.domain.usecase.SaveProfileResult
import app.reporrun.core.domain.usecase.SaveProfileUseCase
import app.reporrun.core.domain.usecase.ValidateProfileUseCase
import app.reporrun.core.model.MuscleGroup
import app.reporrun.core.model.UserProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El formulario del perfil. Un solo ViewModel para los dos pasos: vive lo que el grafo del
 * feature, no lo que una pantalla, y por eso lo tecleado en el paso 1 sigue ahí al volver del 2.
 *
 * **No decide qué es válido.** Convierte el texto de los campos en un [ProfileInput] y pregunta:
 * a [ValidateProfileUseCase] para pasar de paso y para admitir otro músculo prioritario, y a
 * [SaveProfileUseCase] para guardar. Los límites no están aquí ni en la pantalla.
 *
 * El patrón (estado en el sitio, efectos por `Channel`, `onEvent` como única entrada) es el de
 * `OnboardingViewModel`, que explica el porqué de cada pieza.
 */
class ProfileViewModel(
    observeProfile: ObserveProfileUseCase,
    private val validateProfile: ValidateProfileUseCase,
    private val saveProfile: SaveProfileUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init {
        // El perfil guardado se lee **una vez** y no se observa: si se siguiera recogiendo,
        // guardar pisaría lo que la persona tuviera a medio escribir.
        viewModelScope.launch {
            val saved = observeProfile().first()
            _state.update { (saved?.toUiState() ?: it).copy(isLoading = false) }
        }
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.TextChanged -> changeText(event.field, event.value)

            is ProfileEvent.ExperienceSelected -> _state.update {
                it.copy(experience = event.experience, errors = it.errors - ProfileField.EXPERIENCE)
            }

            is ProfileEvent.GoalSelected -> _state.update {
                it.copy(goal = event.goal, errors = it.errors - ProfileField.GOAL)
            }

            is ProfileEvent.MuscleToggled -> toggleMuscle(event.muscle)

            ProfileEvent.ContinueClicked -> continueToGoal()

            ProfileEvent.SaveClicked -> save()
        }
    }

    private fun changeText(field: ProfileTextField, value: String) {
        _state.update {
            val changed = when (field) {
                ProfileTextField.ALIAS -> it.copy(alias = value)
                ProfileTextField.AGE -> it.copy(age = value.asIntegerInput())
                ProfileTextField.HEIGHT -> it.copy(height = value.asDecimalInput())
                ProfileTextField.WEIGHT -> it.copy(weight = value.asDecimalInput())
                ProfileTextField.WAIST -> it.copy(waist = value.asDecimalInput())
                ProfileTextField.HIP -> it.copy(hip = value.asDecimalInput())
                ProfileTextField.LIMITATIONS -> it.copy(limitations = value)
                ProfileTextField.PREFERENCES -> it.copy(preferences = value)
            }
            // El error de un campo se va en cuanto se toca: vuelve, si sigue mal, al continuar.
            changed.copy(errors = it.errors - field.field)
        }
    }

    private fun toggleMuscle(muscle: MuscleGroup) {
        _state.update {
            val candidate = it.copy(priorityMuscles = it.priorityMuscles.toggle(muscle))
            // Si con este ya son demasiados, lo dice el caso de uso: no se marca y se enseña
            // su error. Quitar uno nunca falla, así que también limpia el aviso.
            val tooMany = validateProfile(candidate.toInput()).find { error -> error.field == PRIORITY_MUSCLES }
            if (tooMany == null) {
                candidate.copy(errors = it.errors - PRIORITY_MUSCLES)
            } else {
                it.copy(errors = it.errors + (PRIORITY_MUSCLES to tooMany.issue))
            }
        }
    }

    private fun continueToGoal() {
        // El objetivo aún no se ha podido elegir: del resultado solo cuentan los campos del paso 1.
        val errors = validateProfile(_state.value.toInput()).filter { it.field in BASICS_FIELDS }
        _state.update { it.copy(errors = errors.byField()) }
        if (errors.isEmpty()) _effects.trySend(ProfileEffect.BasicsCompleted)
    }

    private fun save() {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            when (val result = saveProfile(_state.value.toInput())) {
                // `isSaving` no vuelve a `false`: la pantalla se va.
                is SaveProfileResult.Saved -> _effects.send(ProfileEffect.ProfileSaved)

                is SaveProfileResult.Invalid -> {
                    _state.update { it.copy(isSaving = false, errors = result.errors.byField()) }
                    if (result.errors.any { it.field in BASICS_FIELDS }) {
                        _effects.send(ProfileEffect.BasicsNeedReview)
                    }
                }
            }
        }
    }

    private companion object {
        val PRIORITY_MUSCLES = ProfileField.PRIORITY_MUSCLES

        /** Los campos que se rellenan en el paso 1. */
        val BASICS_FIELDS = setOf(
            ProfileField.ALIAS,
            ProfileField.AGE,
            ProfileField.HEIGHT_CM,
            ProfileField.WEIGHT_KG,
            ProfileField.WAIST_CM,
            ProfileField.HIP_CM,
            ProfileField.EXPERIENCE,
        )
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

private fun List<FieldError<ProfileField>>.byField() = associate { it.field to it.issue }

/** Lo que hay en los campos, como lo pide el caso de uso. Un campo vacío es «sin dato». */
private fun ProfileUiState.toInput() = ProfileInput(
    alias = alias,
    age = age.toIntOrNull(),
    heightCm = height.toDecimalOrNull(),
    weightKg = weight.toDecimalOrNull(),
    waistCm = waist.toDecimalOrNull(),
    hipCm = hip.toDecimalOrNull(),
    experience = experience,
    goal = goal,
    priorityMuscles = priorityMuscles,
    limitations = limitations,
    preferences = preferences,
)

private fun UserProfile.toUiState() = ProfileUiState(
    alias = alias.orEmpty(),
    age = age.toString(),
    height = heightCm.toFieldText(),
    weight = weightKg?.toFieldText().orEmpty(),
    waist = waistCm?.toFieldText().orEmpty(),
    hip = hipCm?.toFieldText().orEmpty(),
    experience = experience,
    goal = goal,
    priorityMuscles = priorityMuscles,
    limitations = limitations,
    preferences = preferences,
)
