package app.reporrun.core.designsystem.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunTheme
import app.reporrun.core.designsystem.component.ErrorState
import app.reporrun.core.designsystem.component.LoadingState
import app.reporrun.core.designsystem.component.RepOrRunBottomNav
import app.reporrun.core.designsystem.component.RepOrRunButton
import app.reporrun.core.designsystem.component.RepOrRunButtonStyle
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunCheckMark
import app.reporrun.core.designsystem.component.RepOrRunChip
import app.reporrun.core.designsystem.component.RepOrRunDivider
import app.reporrun.core.designsystem.component.RepOrRunEmphasisCard
import app.reporrun.core.designsystem.component.RepOrRunField
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.designsystem.component.RepOrRunListRow
import app.reporrun.core.designsystem.component.RepOrRunNavItem
import app.reporrun.core.designsystem.component.RepOrRunStatusChip
import app.reporrun.core.designsystem.component.RepOrRunStep
import app.reporrun.core.designsystem.component.RepOrRunStepper
import app.reporrun.core.designsystem.component.RepOrRunSwitch

@Composable
internal fun TypographySample() {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            "BLOQUE 1 · DÍA 3 DE 14",
            style = RepOrRunTheme.extendedTypography.overline,
            color = colors.onSurfaceVariant,
        )
        Text("Tu plan", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Text(
            "Con esto ajustamos el volumen y la progresión.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
internal fun EmphasisCardSample() {
    RepOrRunEmphasisCard(Modifier.fillMaxWidth()) {
        Text("SESIÓN DE HOY", style = RepOrRunTheme.extendedTypography.overline)
        Text("Upper A + core", style = MaterialTheme.typography.headlineMedium)
        Text("9 ejercicios · 18 series · 75 min", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun FieldSample() {
    var weight by remember { mutableStateOf("72,5") }
    var waist by remember { mutableStateOf("") }
    Row(horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing)) {
        RepOrRunField(weight, { weight = it }, label = "Peso", modifier = Modifier.weight(1f), unit = "kg")
        RepOrRunField(
            waist,
            { waist = it },
            label = "Cintura",
            modifier = Modifier.weight(1f),
            unit = "cm",
            placeholder = "0",
            errorText = "Entre 40 y 200",
        )
    }
}

@Composable
internal fun ChipSample() {
    var selected by remember { mutableIntStateOf(1) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("Principiante", "Intermedio", "Avanzado").forEachIndexed { index, label ->
            RepOrRunChip(label, selected = index == selected, onClick = { selected = index })
        }
    }
    RepOrRunCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ChipSpacing)) {
            RepOrRunStatusChip("Hecho")
            RepOrRunStatusChip("En curso", highlighted = true)
            RepOrRunStatusChip("Pendiente")
        }
    }
}

@Composable
internal fun StepperSample() {
    RepOrRunCard(Modifier.fillMaxWidth()) {
        RepOrRunStepper(
            steps = listOf(
                RepOrRunStep("Calentamiento", "10:30"),
                RepOrRunStep("Ejercicios", "en curso"),
                RepOrRunStep("Enfriamiento", "—"),
            ),
            currentStep = 1,
        )
    }
}

@Composable
internal fun ListRowSample() {
    var registry by remember { mutableStateOf(true) }
    var body by remember { mutableStateOf(false) }
    RepOrRunCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = RepOrRunDimens.CardHorizontalPadding),
    ) {
        RepOrRunListRow("Perfil", subtitle = "30 años · intermedio", leadingIcon = RepOrRunIcons.Person, onClick = {})
        RepOrRunDivider()
        RepOrRunListRow(
            "Borrar todos los datos",
            subtitle = "Se va todo: plan, historial y ajustes",
            leadingIcon = RepOrRunIcons.Close,
            titleColor = RepOrRunTheme.extendedColors.accentText,
            onClick = {},
        )
        RepOrRunDivider()
        RepOrRunListRow("Hoja de registro", subtitle = "Una tabla por día para anotar a mano") {
            RepOrRunSwitch(registry, { registry = it })
        }
        RepOrRunDivider()
        RepOrRunListRow("Datos corporales", subtitle = "Peso y medidas en la portada") {
            RepOrRunSwitch(body, { body = it })
        }
        RepOrRunDivider()
        RepOrRunListRow("Gimnasio Central", subtitle = "Activo") { RepOrRunCheckMark(checked = true) }
        RepOrRunDivider()
        RepOrRunListRow("Gimnasio Norte", subtitle = "Toca para usarlo", onClick = {}) {
            RepOrRunCheckMark(checked = false)
        }
    }
}

@Composable
internal fun ButtonSample() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RepOrRunDimens.ListRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RepOrRunIconButton(RepOrRunIcons.ArrowBack, contentDescription = "Volver", onClick = {})
        RepOrRunIconButton(RepOrRunIcons.Add, contentDescription = "Añadir máquina", onClick = {}, emphasis = true)
        RepOrRunButton(
            "Rechazar",
            onClick = {},
            modifier = Modifier.weight(1f),
            style = RepOrRunButtonStyle.Outline,
        )
    }
    RepOrRunButton("Continuar", onClick = {}, modifier = Modifier.fillMaxWidth(), icon = RepOrRunIcons.ArrowForward)
    RepOrRunButton("Generar plan", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
}

@Composable
internal fun StateSample() {
    LoadingState(message = "Armando 4 sesiones con tus 18 máquinas.")
    ErrorState(
        title = "No se pudo generar el plan",
        message = "El servidor está ocupado. Espera un minuto y vuelve a intentarlo.",
        actionLabel = "Reintentar",
    )
}

@Composable
internal fun BottomNavSample() {
    var selected by remember { mutableIntStateOf(0) }
    RepOrRunBottomNav(
        items = listOf(
            RepOrRunNavItem("Inicio", RepOrRunIcons.Home),
            RepOrRunNavItem("Gimnasio", RepOrRunIcons.Dumbbell),
            RepOrRunNavItem("Revisión", RepOrRunIcons.Calendar),
            RepOrRunNavItem("Ajustes", RepOrRunIcons.Person),
        ),
        selectedIndex = selected,
        onSelect = { selected = it },
    )
}
