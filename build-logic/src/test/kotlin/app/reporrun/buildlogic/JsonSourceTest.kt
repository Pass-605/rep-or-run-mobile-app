package app.reporrun.buildlogic

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class JsonSourceTest {
    @Test
    fun namesPropertyAfterFileInCamelCase() {
        assertEquals("generateResponseSmallGym", JsonSource.propertyName("generate-response.small-gym.json"))
    }

    @Test
    fun escapesWhatWouldBreakAKotlinStringLiteral() {
        assertEquals("""{\"a\": \"\${'$'}x\\n\"}\n""", JsonSource.escape("{\"a\": \"\$x\\n\"}\n"))
    }

    @Test
    fun rendersOnePropertyPerFile() {
        val source = JsonSource.render(
            packageName = "app.example",
            objectName = "Examples",
            taskName = "generateExamples",
            jsonByFileName = mapOf("a.json" to "{}", "b-c.json" to "[]"),
        )

        assertContains(source, "package app.example")
        assertContains(source, "internal object Examples {")
        assertContains(source, "val a: String = listOf(")
        assertContains(source, "val bC: String = listOf(")
    }
}
