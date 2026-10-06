package app.reporrun.feature.gym

import androidx.compose.runtime.Composable
import app.reporrun.core.model.EquipmentCategory
import app.reporrun.core.model.LoadType
import app.reporrun.feature.gym.resources.Res
import app.reporrun.feature.gym.resources.category_bench
import app.reporrun.feature.gym.resources.category_bodyweight
import app.reporrun.feature.gym.resources.category_cable
import app.reporrun.feature.gym.resources.category_cardio
import app.reporrun.feature.gym.resources.category_free_weight
import app.reporrun.feature.gym.resources.category_machine
import app.reporrun.feature.gym.resources.category_other
import app.reporrun.feature.gym.resources.category_smith
import app.reporrun.feature.gym.resources.inventory_custom
import app.reporrun.feature.gym.resources.inventory_range
import app.reporrun.feature.gym.resources.inventory_range_max
import app.reporrun.feature.gym.resources.inventory_range_min
import app.reporrun.feature.gym.resources.load_barbell
import app.reporrun.feature.gym.resources.load_bodyweight
import app.reporrun.feature.gym.resources.load_cardio
import app.reporrun.feature.gym.resources.load_dumbbell
import app.reporrun.feature.gym.resources.load_none
import app.reporrun.feature.gym.resources.load_plates_total
import app.reporrun.feature.gym.resources.load_selector
import app.reporrun.feature.gym.resources.load_sled
import app.reporrun.feature.gym.resources.load_smith
import org.jetbrains.compose.resources.stringResource

/* Los nombres que ve la persona de lo que en el modelo son enums, y el subtítulo de una fila. */

@Composable
internal fun EquipmentCategory.label(): String = stringResource(
    when (this) {
        EquipmentCategory.MACHINE -> Res.string.category_machine
        EquipmentCategory.CABLE -> Res.string.category_cable
        EquipmentCategory.FREE_WEIGHT -> Res.string.category_free_weight
        EquipmentCategory.SMITH -> Res.string.category_smith
        EquipmentCategory.BENCH -> Res.string.category_bench
        EquipmentCategory.CARDIO -> Res.string.category_cardio
        EquipmentCategory.BODYWEIGHT -> Res.string.category_bodyweight
        EquipmentCategory.OTHER -> Res.string.category_other
    },
)

/** En minúscula, como va en el subtítulo de una fila («Máquina · selector»). */
@Composable
internal fun LoadType.label(): String = stringResource(
    when (this) {
        LoadType.SELECTOR -> Res.string.load_selector
        LoadType.PLATES_TOTAL -> Res.string.load_plates_total
        LoadType.SMITH -> Res.string.load_smith
        LoadType.SLED -> Res.string.load_sled
        LoadType.DUMBBELL -> Res.string.load_dumbbell
        LoadType.BARBELL -> Res.string.load_barbell
        LoadType.BODYWEIGHT -> Res.string.load_bodyweight
        LoadType.CARDIO -> Res.string.load_cardio
        LoadType.NONE -> Res.string.load_none
    },
)

/** El mismo nombre para un chip, que empieza con mayúscula. */
@Composable
internal fun LoadType.chipLabel(): String = label().replaceFirstChar { it.uppercase() }

/**
 * «Máquina · selector», más el rango de carga si se conoce y «personalizada» si no viene del
 * catálogo. Una máquina de cardio dice solo «Cardio», como en Figma: su tipo de carga no añade
 * nada.
 */
@Composable
internal fun InventoryRow.Item.subtitle(): String = listOfNotNull(
    category.label(),
    loadType.takeIf { it != LoadType.CARDIO }?.label(),
    rangeLabel(),
    if (isCustom) stringResource(Res.string.inventory_custom) else null,
).joinToString(SUBTITLE_SEPARATOR)

@Composable
private fun InventoryRow.Item.rangeLabel(): String? = when {
    minKg != null && maxKg != null -> stringResource(Res.string.inventory_range, minKg.toKgText(), maxKg.toKgText())
    minKg != null -> stringResource(Res.string.inventory_range_min, minKg.toKgText())
    maxKg != null -> stringResource(Res.string.inventory_range_max, maxKg.toKgText())
    else -> null
}

private const val SUBTITLE_SEPARATOR = " · "
