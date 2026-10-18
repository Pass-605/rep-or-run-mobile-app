package app.reporrun

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { RepOrRunApp(onDarkThemeChange = ::applySystemBars) }
    }

    /**
     * Los iconos de las barras del sistema siguen al tema de la app, no al del sistema: con
     * «Oscuro» elegido y el dispositivo en claro, `enableEdgeToEdge()` a secas los dejaría
     * oscuros sobre un fondo oscuro.
     */
    private fun applySystemBars(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
