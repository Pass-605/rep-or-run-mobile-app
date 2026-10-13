package app.reporrun.core.domain.usecase

import app.cash.turbine.test
import app.reporrun.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeUseCasesTest {
    private val settings = FakeSettingsRepository()
    private val observeThemeMode = ObserveThemeModeUseCase(settings)
    private val setThemeMode = SetThemeModeUseCase(settings)

    @Test
    fun followsTheSystemUntilAThemeIsChosen() = runTest {
        assertEquals(ThemeMode.SYSTEM, observeThemeMode().first())
    }

    @Test
    fun emitsEveryThemeTheModeChangesTo() = runTest {
        observeThemeMode().test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())

            setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())

            setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem())
        }
    }
}
