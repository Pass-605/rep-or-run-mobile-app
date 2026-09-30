package app.reporrun.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModuleDependencyRulesTest {
    // ---------------------------------------------------------------- feature:*

    @Test
    fun acceptsFeatureWithAllowedDependencies() {
        val dependencies = setOf(":core:domain", ":core:model", ":core:designsystem", ":core:ui")

        assertEquals(emptyList(), ModuleDependencyRules.violations(":feature:plan", dependencies))
    }

    @Test
    fun rejectsFeatureDependingOnDataLayerModules() {
        val dependencies = setOf(":core:domain", ":core:data", ":core:database", ":core:network", ":core:ai")

        assertEquals(
            listOf(":core:ai", ":core:data", ":core:database", ":core:network"),
            ModuleDependencyRules.violations(":feature:plan", dependencies),
        )
    }

    @Test
    fun rejectsFeatureDependingOnAnotherFeature() {
        val dependencies = setOf(":core:model", ":feature:gym")

        assertEquals(listOf(":feature:gym"), ModuleDependencyRules.violations(":feature:plan", dependencies))
    }

    @Test
    fun rejectsFeatureDependingOnACoreModuleThatIsNotListed() {
        // La seccion 3 lista lo permitido, no lo prohibido: core:pdf no esta, luego no vale.
        assertEquals(listOf(":core:pdf"), ModuleDependencyRules.violations(":feature:plan", setOf(":core:pdf")))
    }

    // ---------------------------------------------------------------- nucleo

    @Test
    fun acceptsDomainDependingOnlyOnModel() {
        assertEquals(emptyList(), ModuleDependencyRules.violations(":core:domain", setOf(":core:model")))
    }

    @Test
    fun rejectsDomainDependingOnAnythingElse() {
        // La regla de mas valor: el dominio se mantiene puro.
        assertEquals(
            listOf(":core:data", ":core:network"),
            ModuleDependencyRules.violations(":core:domain", setOf(":core:model", ":core:data", ":core:network")),
        )
    }

    @Test
    fun rejectsModelDependingOnAnything() {
        assertEquals(listOf(":core:domain"), ModuleDependencyRules.violations(":core:model", setOf(":core:domain")))
    }

    @Test
    fun acceptsModelWithNoDependencies() {
        assertEquals(emptyList(), ModuleDependencyRules.violations(":core:model", emptySet()))
    }

    @Test
    fun acceptsAiWithItsAllowedDependencies() {
        val dependencies = setOf(":core:network", ":core:domain", ":core:model")

        assertEquals(emptyList(), ModuleDependencyRules.violations(":core:ai", dependencies))
    }

    @Test
    fun rejectsAiDependingOnData() {
        assertEquals(
            listOf(":core:data"),
            ModuleDependencyRules.violations(":core:ai", setOf(":core:domain", ":core:data")),
        )
    }

    @Test
    fun acceptsDataWithItsAllowedDependencies() {
        val dependencies = setOf(":core:domain", ":core:model", ":core:database", ":core:datastore", ":core:ai")

        assertEquals(emptyList(), ModuleDependencyRules.violations(":core:data", dependencies))
    }

    @Test
    fun rejectsDataDependingOnAFeature() {
        assertEquals(
            listOf(":feature:plan"),
            ModuleDependencyRules.violations(":core:data", setOf(":core:domain", ":feature:plan")),
        )
    }

    // ---------------------------------------------------------------- sin regla

    @Test
    fun ignoresModulesWithoutARule() {
        // La seccion 3 no restringe composeApp ni androidApp: se dejan pasar a proposito.
        assertNull(ModuleDependencyRules.allowedFor(":composeApp"))
        assertEquals(
            emptyList(),
            ModuleDependencyRules.violations(":composeApp", setOf(":core:data", ":core:ai", ":feature:plan")),
        )
    }

    @Test
    fun doesNotTreatModulesWithFeaturePrefixInNameAsFeatures() {
        assertEquals(emptyList(), ModuleDependencyRules.violations(":core:featureflags", setOf(":core:ai")))
    }

    // ---------------------------------------------------------------- grafo completo

    @Test
    fun checksEveryModuleOfSampleGraph() {
        val graph = mapOf(
            ":composeApp" to setOf(":feature:plan", ":feature:gym", ":core:data"),
            ":core:data" to setOf(":core:domain", ":core:database", ":core:ai"),
            ":core:domain" to setOf(":core:model", ":core:database"),
            ":core:model" to emptySet(),
            ":feature:plan" to setOf(":core:domain", ":core:ui"),
            ":feature:gym" to setOf(":core:domain", ":core:network", ":feature:plan"),
        )

        val violations = graph
            .mapValues { (module, dependencies) -> ModuleDependencyRules.violations(module, dependencies) }
            .filterValues { it.isNotEmpty() }

        assertEquals(
            mapOf(
                ":core:domain" to listOf(":core:database"),
                ":feature:gym" to listOf(":core:network", ":feature:plan"),
            ),
            violations,
        )
    }
}
