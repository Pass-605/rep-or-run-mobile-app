package app.reporrun.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.reporrun.core.designsystem.RepOrRunDimens
import app.reporrun.core.designsystem.RepOrRunIcons
import app.reporrun.core.designsystem.RepOrRunLicenses
import app.reporrun.core.designsystem.component.RepOrRunCard
import app.reporrun.core.designsystem.component.RepOrRunIconButton
import app.reporrun.core.designsystem.component.RepOrRunScreenFrame
import app.reporrun.core.designsystem.component.RepOrRunScrollContent
import app.reporrun.feature.settings.resources.Res
import app.reporrun.feature.settings.resources.licenses_back
import app.reporrun.feature.settings.resources.licenses_font_summary
import app.reporrun.feature.settings.resources.licenses_font_title
import app.reporrun.feature.settings.resources.licenses_intro
import app.reporrun.feature.settings.resources.licenses_libraries_summary
import app.reporrun.feature.settings.resources.licenses_libraries_title
import app.reporrun.feature.settings.resources.licenses_title
import org.jetbrains.compose.resources.stringResource

/**
 * Lo que se distribuye dentro de la app y no es nuestro. Todas son Apache 2.0; si entra una con
 * otra licencia, no cabe en esta lista: necesita su propia tarjeta con su texto.
 *
 * Son nombres propios: no van a `strings.xml` porque no se traducen. Las de test y de build
 * (Turbine, detekt, Spotless) no se distribuyen y no están.
 */
private val ApacheLibraries = listOf(
    "Kotlin y kotlinx (coroutines, serialization, datetime) · JetBrains",
    "Compose Multiplatform, Navigation y Lifecycle · JetBrains y The Android Open Source Project",
    "AndroidX (Activity, Room, SQLite, DataStore) · The Android Open Source Project",
    "Koin · Kotzilla y colaboradores",
    "Okio · Square, Inc.",
)

/**
 * Las licencias. No tiene ViewModel: es texto fijo, y el del OFL sale del recurso que viaja con
 * las fuentes. No está dibujada en Figma: usa los componentes y los roles del tema, como el aviso.
 */
@Composable
internal fun LicensesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    // Leer un recurso es E/S: hasta que llega, la tarjeta enseña solo su cabecera.
    val outfitLicense by produceState<String?>(initialValue = null) { value = RepOrRunLicenses.outfit() }

    RepOrRunScreenFrame(modifier = modifier) {
        RepOrRunScrollContent {
            RepOrRunIconButton(
                icon = RepOrRunIcons.ArrowBack,
                contentDescription = stringResource(Res.string.licenses_back),
                onClick = onBack,
            )
            Text(
                stringResource(Res.string.licenses_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(Res.string.licenses_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LicenseCard(
                title = stringResource(Res.string.licenses_font_title),
                summary = stringResource(Res.string.licenses_font_summary),
                body = outfitLicense,
            )
            LicenseCard(
                title = stringResource(Res.string.licenses_libraries_title),
                summary = stringResource(Res.string.licenses_libraries_summary),
                body = ApacheLibraries.joinToString(separator = "\n"),
            )
        }
    }
}

@Composable
private fun LicenseCard(title: String, summary: String, body: String?) {
    RepOrRunCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(RepOrRunDimens.FieldLabelSpacing)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (body != null) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
