package app.reporrun.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.reporrun.core.designsystem.resources.Res
import app.reporrun.core.designsystem.resources.ic_add
import app.reporrun.core.designsystem.resources.ic_arrow_back
import app.reporrun.core.designsystem.resources.ic_arrow_forward
import app.reporrun.core.designsystem.resources.ic_calendar
import app.reporrun.core.designsystem.resources.ic_check
import app.reporrun.core.designsystem.resources.ic_chevron_forward
import app.reporrun.core.designsystem.resources.ic_close
import app.reporrun.core.designsystem.resources.ic_dumbbell
import app.reporrun.core.designsystem.resources.ic_home
import app.reporrun.core.designsystem.resources.ic_notifications
import app.reporrun.core.designsystem.resources.ic_pdf
import app.reporrun.core.designsystem.resources.ic_person
import app.reporrun.core.designsystem.resources.ic_play
import app.reporrun.core.designsystem.resources.ic_search
import app.reporrun.core.designsystem.resources.ic_settings
import app.reporrun.core.designsystem.resources.ic_share
import org.jetbrains.compose.resources.painterResource

/**
 * Los iconos de las pantallas v2, exportados de Figma tal cual (24 dp). El color lo pone
 * quien los pinta con `Icon(tint = …)`; el del archivo no se ve.
 */
object RepOrRunIcons {
    val Add: Painter @Composable get() = painterResource(Res.drawable.ic_add)
    val ArrowBack: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_back)
    val ArrowForward: Painter @Composable get() = painterResource(Res.drawable.ic_arrow_forward)
    val Calendar: Painter @Composable get() = painterResource(Res.drawable.ic_calendar)
    val Check: Painter @Composable get() = painterResource(Res.drawable.ic_check)
    val ChevronForward: Painter @Composable get() = painterResource(Res.drawable.ic_chevron_forward)
    val Close: Painter @Composable get() = painterResource(Res.drawable.ic_close)
    val Dumbbell: Painter @Composable get() = painterResource(Res.drawable.ic_dumbbell)
    val Home: Painter @Composable get() = painterResource(Res.drawable.ic_home)
    val Notifications: Painter @Composable get() = painterResource(Res.drawable.ic_notifications)
    val Pdf: Painter @Composable get() = painterResource(Res.drawable.ic_pdf)
    val Person: Painter @Composable get() = painterResource(Res.drawable.ic_person)
    val Play: Painter @Composable get() = painterResource(Res.drawable.ic_play)
    val Search: Painter @Composable get() = painterResource(Res.drawable.ic_search)
    val Settings: Painter @Composable get() = painterResource(Res.drawable.ic_settings)
    val Share: Painter @Composable get() = painterResource(Res.drawable.ic_share)
}
