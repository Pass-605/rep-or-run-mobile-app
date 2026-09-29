package app.reporrun.buildlogic

import kotlin.test.Test
import kotlin.test.assertEquals

class ModuleDependencyRulesTest {
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
    fun ignoresModulesThatAreNotFeatures() {
        val dependencies = setOf(":core:data", ":core:ai", ":feature:plan")

        assertEquals(emptyList(), ModuleDependencyRules.violations(":composeApp", dependencies))
        assertEquals(emptyList(), ModuleDependencyRules.violations(":core:data", dependencies))
    }

    @Test
    fun doesNotTreatModulesWithFeaturePrefixInNameAsFeatures() {
        assertEquals(emptyList(), ModuleDependencyRules.violations(":core:featureflags", setOf(":core:ai")))
    }

    @Test
    fun checksEveryModuleOfSampleGraph() {
        val graph = mapOf(
            ":composeApp" to setOf(":feature:plan", ":feature:gym", ":core:data"),
            ":core:data" to setOf(":core:domain", ":core:database", ":core:ai"),
            ":feature:plan" to setOf(":core:domain", ":core:ui"),
            ":feature:gym" to setOf(":core:domain", ":core:network", ":feature:plan"),
        )

        val violations = graph
            .mapValues { (module, dependencies) -> ModuleDependencyRules.violations(module, dependencies) }
            .filterValues { it.isNotEmpty() }

        assertEquals(mapOf(":feature:gym" to listOf(":core:network", ":feature:plan")), violations)
    }
}
