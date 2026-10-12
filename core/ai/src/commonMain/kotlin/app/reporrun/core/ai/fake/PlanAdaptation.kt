package app.reporrun.core.ai.fake

import app.reporrun.core.ai.contract.DayOfWeekDto
import app.reporrun.core.ai.contract.EquipmentDto
import app.reporrun.core.ai.contract.MetricDto
import app.reporrun.core.ai.contract.PlanDayDto
import app.reporrun.core.ai.contract.PlanDto
import app.reporrun.core.ai.contract.PlanExerciseDto
import app.reporrun.core.domain.port.PlanGenerationRequest
import app.reporrun.core.domain.validation.codeLetter
import app.reporrun.core.model.DayOfWeek
import app.reporrun.core.model.Equipment
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType

/*
 * Adapta el plan del ejemplo del contrato a una persona concreta. El ejemplo nombra las
 * máquinas de su propio gimnasio de ejemplo; devuelto tal cual, `PlanValidator` lo rechazaría
 * siempre (plan §4.1) y la app no saldría del error.
 *
 * Qué se garantiza: ningún ejercicio ni alternativa usa un aparato fuera del inventario, las
 * sesiones caen en días disponibles, los códigos son únicos y llevan la letra de su día, y el
 * cardio usa un modo que la persona eligió. Qué no: que el resultado sea un buen entrenamiento
 * cuando el gimnasio no se parece al del ejemplo. Es un fake.
 */

/** Cómo de parecido es el aparato del inventario al que el ejemplo quería. */
private enum class Match {
    /** El mismo aparato: el ejercicio del ejemplo vale tal cual. */
    SAME_EQUIPMENT,

    /** Otro aparato que se carga igual: vale para cualquier métrica. */
    SAME_LOAD_TYPE,

    /** Cualquier aparato de fuerza: solo para ejercicios de repeticiones. */
    ANY_STRENGTH,
}

private data class Substitute(val equipment: Equipment, val match: Match)

/**
 * Las entradas del catálogo semilla que son el mismo aparato que cada uno del gimnasio de
 * ejemplo. Un aparato del inventario con el mismo `id` que el del ejemplo también cuenta.
 */
private val CATALOG_KEYS: Map<String, Set<String>> = mapOf(
    "eq_chest_press" to setOf("chest_press"),
    "eq_row_chest_sup" to setOf("chest_supported_row"),
    "eq_lat_pulldown" to setOf("lat_pulldown"),
    "eq_shoulder_press" to setOf("shoulder_press_machine"),
    "eq_pendulum_squat" to setOf("pendulum_squat"),
    "eq_leg_curl" to setOf("lying_leg_curl"),
    "eq_leg_extension" to setOf("leg_extension"),
    "eq_cable_tower" to setOf("functional_trainer", "cable_crossover"),
    "eq_dumbbells" to setOf("dumbbells"),
    "eq_mat" to setOf("exercise_mat"),
)

/** El calentamiento del ejemplo nombra máquinas; este no, y vale para cualquier día adaptado. */
private const val GENERIC_WARMUP: String =
    "5-7 min de movimiento suave a RPE 3-4 y 2-3 aproximaciones al primer ejercicio. " +
        "Las aproximaciones no cuentan como series efectivas."

private const val CODE_DIGITS = 2

/**
 * El plan del ejemplo, adaptado a [request]. Si ningún aparato del inventario sirve para un
 * ejercicio, devuelve un plan sin días.
 *
 * @param exampleEquipment el inventario del gimnasio de ejemplo, para saber cómo se carga cada
 *   aparato que nombra el plan.
 */
internal fun PlanDto.adaptedTo(request: PlanGenerationRequest, exampleEquipment: List<EquipmentDto>): PlanDto {
    val usedIds = days.flatMap { day -> day.exercises.flatMap { it.equipmentIds() } }.toSet()
    val substitutes = substitutes(exampleEquipment.filter { it.id in usedIds }, request.equipment)
    val cardioMode = request.preferences.cardioModes.firstOrNull()
    val chosenDays = chooseDays(days.map { it.dayOfWeek }, request.preferences.availableDays)

    return copy(
        days = days.zip(chosenDays).mapNotNull { (day, dayOfWeek) -> day.adapted(dayOfWeek, substitutes, cardioMode) },
    )
}

private fun PlanExerciseDto.equipmentIds(): List<String> = alternatives.map { it.equipmentId } + equipmentId

/**
 * Un aparato del inventario para cada uno del ejemplo, sin repetir. Se reparte en tres vueltas,
 * de la coincidencia más estricta a la más laxa, para que un «cualquiera» no se quede con el
 * aparato que otro del ejemplo necesitaba tal cual.
 */
private fun substitutes(wanted: List<EquipmentDto>, inventory: List<Equipment>): Map<String, Substitute> {
    val strength = inventory.filter { it.loadType != LoadType.CARDIO && it.category != EquipmentCategory.CARDIO }
    val found = mutableMapOf<String, Substitute>()
    for (match in Match.entries) {
        for (example in wanted.filter { it.id !in found }) {
            val taken = found.values.map { it.equipment.id }
            strength.firstOrNull { it.id !in taken && it.fits(example, match) }
                ?.let { found[example.id] = Substitute(it, match) }
        }
    }
    return found
}

private fun Equipment.fits(example: EquipmentDto, match: Match): Boolean = when (match) {
    Match.SAME_EQUIPMENT -> id.value == example.id || catalogKey?.value in CATALOG_KEYS[example.id].orEmpty()
    Match.SAME_LOAD_TYPE -> loadType.name == example.loadType.name
    Match.ANY_STRENGTH -> true
}

/**
 * Tantos días disponibles como sesiones tiene el ejemplo, o los que haya. Se quedan primero
 * los del ejemplo que la persona tiene libres y se completa con el resto, en orden de semana.
 */
private fun chooseDays(exampleDays: List<DayOfWeekDto>, available: Set<DayOfWeek>): List<DayOfWeek> {
    val exampleNames = exampleDays.map { it.name }
    val (same, others) = available.sorted().partition { it.name in exampleNames }
    return (same + others).take(exampleDays.size).sorted()
}

private fun PlanDayDto.adapted(day: DayOfWeek, substitutes: Map<String, Substitute>, cardioMode: String?): PlanDayDto? {
    val kept = exercises.mapNotNull { it.adapted(substitutes) }.mapIndexed { index, exercise ->
        val order = index + 1
        exercise.copy(order = order, code = day.codeLetter + order.toString().padStart(CODE_DIGITS, '0'))
    }
    val sameCardio = cardioMode != null && cardioMode.equals(this.cardioMode, ignoreCase = true)
    // Con todos sus aparatos y su mismo cardio, el día es el del ejemplo y su calentamiento vale.
    val untouched = sameCardio && exercises.all { substitutes[it.equipmentId]?.match == Match.SAME_EQUIPMENT }
    val withCardio = cardioMode != null && cardioMin != null

    return if (kept.isEmpty()) {
        null
    } else {
        copy(
            dayOfWeek = DayOfWeekDto.valueOf(day.name),
            // El contrato numera los días desde el lunes, 0.
            dayIndex = day.ordinal,
            cardioMin = cardioMin.takeIf { withCardio },
            cardioMax = cardioMax.takeIf { withCardio },
            cardioMode = cardioMode.takeIf { withCardio },
            rpe = rpe.takeIf { withCardio },
            // Velocidad e inclinación son de la caminadora del ejemplo, no de otro modo.
            speed = speed.takeIf { sameCardio },
            incline = incline.takeIf { sameCardio },
            warmup = if (untouched) warmup else GENERIC_WARMUP,
            exercises = kept,
        )
    }
}

/**
 * El ejercicio sobre el aparato que le tocó, o `null` si no le tocó ninguno que le sirva. Con
 * el mismo aparato va tal cual; con otro, toma su nombre y una carga de ensayo sin cifras. Una
 * alternativa solo se conserva con el mismo aparato: es opcional, no hace falta inventarla.
 */
private fun PlanExerciseDto.adapted(substitutes: Map<String, Substitute>): PlanExerciseDto? {
    val substitute = substitutes[equipmentId]
        ?.takeIf { it.match != Match.ANY_STRENGTH || metric == MetricDto.REPS }
    val keptAlternatives = alternatives.mapNotNull { alternative ->
        substitutes[alternative.equipmentId]
            ?.takeIf { it.match == Match.SAME_EQUIPMENT }
            ?.let { alternative.copy(equipmentId = it.equipment.id.value) }
    }

    return when (substitute?.match) {
        null -> null

        Match.SAME_EQUIPMENT -> copy(equipmentId = substitute.equipment.id.value, alternatives = keptAlternatives)

        Match.SAME_LOAD_TYPE, Match.ANY_STRENGTH -> copy(
            equipmentId = substitute.equipment.id.value,
            name = substitute.equipment.name,
            trial = trialLoadFor(substitute.equipment.loadType),
            alternatives = keptAlternatives,
        )
    }
}

/**
 * Carga de ensayo para un aparato que no es el del ejemplo: dice cómo se lee la carga según su
 * [LoadType] (plan §4.5) y no da kilos, porque los del ejemplo eran para otra máquina y nada
 * los calcula a partir de la persona (plan §4.4).
 */
private fun trialLoadFor(loadType: LoadType): String = when (loadType) {
    LoadType.SELECTOR -> "Empieza con una placa baja del selector y ajusta según la técnica y el RIR"
    LoadType.PLATES_TOTAL -> "Empieza sin discos o con los más ligeros; la carga son los discos totales"
    LoadType.SMITH -> "Empieza con la barra de la Smith sin discos; los discos se cuentan aparte"
    LoadType.SLED -> "El carro vacío ya pesa: empieza sin discos"
    LoadType.DUMBBELL -> "Empieza con mancuernas ligeras; la carga es por mancuerna"
    LoadType.BARBELL -> "Empieza con la barra sola; la carga es la barra más los discos"
    LoadType.BODYWEIGHT -> "Peso corporal, 0 kg"
    LoadType.CARDIO, LoadType.NONE -> "Sin carga externa"
}
