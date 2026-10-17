package app.reporrun.buildlogic

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class LocalConfigTest {
    private val names = listOf("ROR_APP_KEY", "ROR_BASE_URL_ANDROID")

    @Test
    fun readsValuesFromLocalProperties() {
        val text = "sdk.dir=/sdk\nROR_APP_KEY=abc\nROR_BASE_URL_ANDROID=http\\://10.0.2.2\\:8787\n"

        assertEquals(
            mapOf("ROR_APP_KEY" to "abc", "ROR_BASE_URL_ANDROID" to "http://10.0.2.2:8787"),
            LocalConfig.resolve(names, text, emptyMap()),
        )
    }

    @Test
    fun localPropertiesWinsOverEnvironment() {
        val resolved = LocalConfig.resolve(names, "ROR_APP_KEY=file", mapOf("ROR_APP_KEY" to "env"))

        assertEquals("file", resolved["ROR_APP_KEY"])
    }

    @Test
    fun environmentCoversWhatLocalPropertiesLacksOrLeavesBlank() {
        val resolved = LocalConfig.resolve(
            names,
            "ROR_APP_KEY=   ",
            mapOf("ROR_APP_KEY" to "env-key", "ROR_BASE_URL_ANDROID" to "env-url"),
        )

        assertEquals(mapOf("ROR_APP_KEY" to "env-key", "ROR_BASE_URL_ANDROID" to "env-url"), resolved)
    }

    @Test
    fun leavesOutWhatIsNowhere() {
        val resolved = LocalConfig.resolve(names, "", mapOf("ROR_APP_KEY" to ""))

        assertEquals(emptyMap(), resolved)
        assertEquals(names, LocalConfig.missing(names, resolved))
    }

    @Test
    fun missingMessageNamesEachKeyAndWhereToPutIt() {
        val message = LocalConfig.missingMessage(":core:network:generate", listOf("ROR_APP_KEY", "ROR_BASE_URL_IOS"))

        assertContains(message, ":core:network:generate")
        assertContains(message, "ROR_APP_KEY, ROR_BASE_URL_IOS")
        assertContains(message, "local.properties.example")
        assertContains(message, "faltan ")
        assertContains(message, "variables de entorno")
    }

    @Test
    fun missingMessageSpeaksInSingularForOneKey() {
        val message = LocalConfig.missingMessage(":core:network:generate", listOf("ROR_APP_KEY"))

        assertContains(message, "falta ROR_APP_KEY. Ponla")
        assertContains(message, "una variable de entorno")
    }

    @Test
    fun rendersOneEscapedConstantPerValueInOrder() {
        val source = LocalConfig.render(
            packageName = "app.example",
            objectName = "Config",
            taskName = "generateConfig",
            values = mapOf("B" to "x\"\$y", "A" to "1"),
        )

        assertContains(source, "package app.example")
        assertContains(source, "internal object Config {")
        assertContains(source, "    const val A: String = \"1\"\n    const val B: String = \"x\\\"\\\$y\"")
    }
}
