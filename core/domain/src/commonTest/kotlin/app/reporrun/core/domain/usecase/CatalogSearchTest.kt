package app.reporrun.core.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogSearchTest {
    private val pecDeck = CatalogSearch.index(
        "Pec Deck",
        listOf("Contractor de pecho", "Mariposa", "Cable Fly en máquina"),
    )

    @Test
    fun findsByNameIgnoringCase() {
        assertTrue(CatalogSearch.matches(pecDeck, "PEC de"))
    }

    @Test
    fun findsByAlias() {
        assertTrue(CatalogSearch.matches(pecDeck, "mariposa"))
    }

    @Test
    fun findsAnAccentedAliasTypedWithoutTheAccent() {
        assertTrue(CatalogSearch.matches(pecDeck, "maquina"))
    }

    @Test
    fun findsAnUnaccentedNameTypedWithAnAccent() {
        assertTrue(CatalogSearch.matches(pecDeck, "péc"))
    }

    @Test
    fun findsWordsInAnyOrderAcrossNameAndAliases() {
        assertTrue(CatalogSearch.matches(pecDeck, "pecho deck"))
    }

    @Test
    fun doesNotFindWhenOneWordIsMissing() {
        assertFalse(CatalogSearch.matches(pecDeck, "pec remo"))
    }

    @Test
    fun aWordDoesNotMatchAcrossTwoAliases() {
        assertFalse(CatalogSearch.matches(pecDeck, "pechomariposa"))
    }

    @Test
    fun aBlankQueryFindsEverything() {
        assertTrue(CatalogSearch.matches(pecDeck, "   "))
    }

    @Test
    fun theEnyeIsNotAnAccentedN() {
        val index = CatalogSearch.index("Banco de piña", listOf("Caña"))

        assertTrue(CatalogSearch.matches(index, "PIÑA"))
        assertTrue(CatalogSearch.matches(index, "caña"))
        assertFalse(CatalogSearch.matches(index, "pina"))
        assertFalse(CatalogSearch.matches(index, "cana"))
    }

    @Test
    fun theDiaeresisIsIgnoredLikeAnAccent() {
        assertTrue(CatalogSearch.matches(CatalogSearch.index("Agarre de pingüino"), "pinguino"))
    }

    @Test
    fun punctuationCountsAsASpace() {
        assertEquals("chest supported row", CatalogSearch.normalize("  Chest-Supported   Row. "))
        assertTrue(CatalogSearch.matches(CatalogSearch.index("Chest-Supported Row"), "chest supported"))
    }
}
