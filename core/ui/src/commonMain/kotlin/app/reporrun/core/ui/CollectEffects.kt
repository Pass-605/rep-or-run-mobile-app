package app.reporrun.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Recoge los efectos de un ViewModel mientras la pantalla está visible.
 *
 * - `repeatOnLifecycle(STARTED)`: con la app en segundo plano no se recoge nada, así que no se
 *   navega por detrás; el efecto espera en el `Channel` del ViewModel.
 * - `Dispatchers.Main.immediate`: sin él hay un salto entre que el `Channel` entrega el efecto
 *   y que se ejecuta [onEffect], y si la pantalla se para justo ahí el efecto se pierde. Es
 *   código de UI, no lógica: por eso el dispatcher va escrito aquí y no inyectado.
 */
@Composable
fun <T> CollectEffects(effects: Flow<T>, onEffect: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // La lambda puede cambiar entre recomposiciones sin que haya que reiniciar la recogida.
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                effects.collect { currentOnEffect(it) }
            }
        }
    }
}
