package app.reporrun.core.model

import kotlin.jvm.JvmInline

/*
 * Identificadores del dominio como value classes: un PlanId no se puede pasar donde va un
 * GymId. Los que viajan por el contrato ([EquipmentId], [ExerciseCode]) validan su formato
 * con el mismo patrón que `docs/contract/v1/common.schema.json`.
 */

/** Perfil de la persona. Hay uno por instalación. */
@JvmInline
value class ProfileId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProfileId vacío" }
    }
}

@JvmInline
value class GymId(val value: String) {
    init {
        require(value.isNotBlank()) { "GymId vacío" }
    }
}

/** Aparato del inventario. Formato del contrato: `eq_` + 1-32 caracteres `[A-Za-z0-9_-]`. */
@JvmInline
value class EquipmentId(val value: String) {
    init {
        require(PATTERN.matches(value)) { "EquipmentId fuera de formato: $value" }
    }

    private companion object {
        val PATTERN = Regex("^eq_[A-Za-z0-9_-]{1,32}$")
    }
}

/** Clave de una entrada del catálogo semilla, en `snake_case` (`chest_press`). */
@JvmInline
value class CatalogKey(val value: String) {
    init {
        require(PATTERN.matches(value)) { "CatalogKey fuera de formato: $value" }
    }

    private companion object {
        val PATTERN = Regex("^[a-z0-9]+(_[a-z0-9]+)*$")
    }
}

@JvmInline
value class BlockPreferencesId(val value: String) {
    init {
        require(value.isNotBlank()) { "BlockPreferencesId vacío" }
    }
}

/**
 * Versión de plan: `ROR-AAAA-MM-DD-vN`. Una versión nueva es un plan nuevo con su propio id;
 * nunca se edita uno existente (plan §4.12).
 */
@JvmInline
value class PlanId(val value: String) {
    init {
        require(PATTERN.matches(value)) { "PlanId fuera de formato: $value" }
    }

    /** El `N` de `-vN`. */
    val version: Int
        get() = value.substringAfterLast("-v").toInt()

    private companion object {
        val PATTERN = Regex("^ROR-[0-9]{4}-[0-9]{2}-[0-9]{2}-v[1-9][0-9]*$")
    }
}

@JvmInline
value class PlanDayId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanDayId vacío" }
    }
}

@JvmInline
value class PlanExerciseId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanExerciseId vacío" }
    }
}

/**
 * Código de un ejercicio dentro de su plan: letra del día + orden (`L01`, `X04`). Formato
 * del contrato: 1-2 mayúsculas y dos dígitos.
 */
@JvmInline
value class ExerciseCode(val value: String) {
    init {
        require(PATTERN.matches(value)) { "ExerciseCode fuera de formato: $value" }
    }

    private companion object {
        val PATTERN = Regex("^[A-Z]{1,2}[0-9]{2}$")
    }
}

@JvmInline
value class PlanChangeId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlanChangeId vacío" }
    }
}

@JvmInline
value class ReviewScheduleId(val value: String) {
    init {
        require(value.isNotBlank()) { "ReviewScheduleId vacío" }
    }
}

@JvmInline
value class ReviewId(val value: String) {
    init {
        require(value.isNotBlank()) { "ReviewId vacío" }
    }
}

@JvmInline
value class CheckInId(val value: String) {
    init {
        require(value.isNotBlank()) { "CheckInId vacío" }
    }
}

@JvmInline
value class ExerciseFeedbackId(val value: String) {
    init {
        require(value.isNotBlank()) { "ExerciseFeedbackId vacío" }
    }
}

@JvmInline
value class ChangeProposalId(val value: String) {
    init {
        require(value.isNotBlank()) { "ChangeProposalId vacío" }
    }
}

@JvmInline
value class ExportRecordId(val value: String) {
    init {
        require(value.isNotBlank()) { "ExportRecordId vacío" }
    }
}
