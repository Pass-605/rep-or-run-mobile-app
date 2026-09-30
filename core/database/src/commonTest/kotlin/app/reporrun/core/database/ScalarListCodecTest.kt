package app.reporrun.core.database

import app.reporrun.core.model.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScalarListCodecTest {
    private val converters = CollectionConverters()

    @Test
    fun encodesStringsAsAJsonArray() {
        assertEquals("""["Caminadora","Bici"]""", ScalarListCodec.encodeStrings(listOf("Caminadora", "Bici")))
    }

    @Test
    fun roundTripsStringsWithSeparatorsQuotesAndControlCharacters() {
        val tricky = listOf("", "a,b", "comillas \"dobles\"", "barra \\ invertida", "línea\nnueva\ttab", "\u0001", "ñ€")

        assertEquals(tricky, ScalarListCodec.decodeStrings(ScalarListCodec.encodeStrings(tricky)))
    }

    @Test
    fun distinguishesEmptyListFromListWithOneEmptyString() {
        assertEquals(emptyList(), ScalarListCodec.decodeStrings(ScalarListCodec.encodeStrings(emptyList())))
        assertEquals(listOf(""), ScalarListCodec.decodeStrings(ScalarListCodec.encodeStrings(listOf(""))))
    }

    @Test
    fun readsJsonWrittenByOtherTools() {
        assertEquals(listOf("a/b", "é"), ScalarListCodec.decodeStrings(""" [ "a\/b" , "\u00e9" ] """))
    }

    @Test
    fun roundTripsInts() {
        assertEquals("[12,10,-1]", ScalarListCodec.encodeInts(listOf(12, 10, -1)))
        assertEquals(listOf(12, 10, -1), ScalarListCodec.decodeInts("[12, 10, -1]"))
    }

    @Test
    fun rejectsMalformedText() {
        listOf("", "[", "[\"a\"", "[\"a\",]", "[1] x", "\"a\"", "[\"\\q\"]").forEach { text ->
            assertFailsWith<IllegalArgumentException>(text) { ScalarListCodec.decodeStrings(text) }
        }
    }

    @Test
    fun writesEnumSetsInDeclarationOrder() {
        val text = converters.daysToText(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY))

        assertEquals("""["MONDAY","FRIDAY"]""", text)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), converters.textToDays(text))
    }
}
