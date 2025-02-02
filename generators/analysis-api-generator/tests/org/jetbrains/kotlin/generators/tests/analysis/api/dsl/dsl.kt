/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.dsl

import org.jetbrains.kotlin.analysis.api.descriptors.test.KtFe10FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.FirFrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.binary.LibraryFrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.fir.utils.libraries.source.LibrarySourceFrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.getDefaultSuiteTestClassName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import kotlin.reflect.KClass

internal class FirAndFe10TestGroup(
    val suite: TestGroupSuite,
    val directory: String?,
    val testModuleKinds: List<TestModuleKind>,
)

internal sealed class TestModuleKind(val componentName: String) {
    object SOURCE : TestModuleKind("source")
    object LIBRARY : TestModuleKind("library")
    object LIBRARY_SOURCE : TestModuleKind("librarySource")

    companion object {
        val SOURCE_ONLY by lazy(LazyThreadSafetyMode.NONE) { listOf(SOURCE) }
        val SOURCE_AND_LIBRARY_SOURCE by lazy(LazyThreadSafetyMode.NONE) { listOf(SOURCE, LIBRARY_SOURCE) }
    }
}

internal enum class Frontend(val prefix: String) {
    FIR("Fir"),
    FE10("Fe10"),
}

internal fun FirAndFe10TestGroup.test(
    baseClass: KClass<*>,
    generateFe10: Boolean = true,
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit,
) {
    analysisApiTest(
        "analysis/analysis-api-fir/tests",
        Frontend.FIR,
        baseClass,
        init
    )
    if (generateFe10) {
        analysisApiTest(
            "analysis/analysis-api-fe10/tests",
            Frontend.FE10,
            baseClass,
            init
        )
    }
}


internal fun TestGroupSuite.test(
    baseClass: KClass<*>,
    addFe10: Boolean = true,
    testModuleKinds: List<TestModuleKind> = TestModuleKind.SOURCE_ONLY,
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit,
) {
    FirAndFe10TestGroup(this, directory = null, testModuleKinds).test(baseClass, addFe10, init)
}


internal fun TestGroupSuite.group(
    directory: String,
    init: FirAndFe10TestGroup.() -> Unit,
) {
    FirAndFe10TestGroup(this, directory, testModuleKinds = listOf(TestModuleKind.SOURCE)).init()
}


internal fun TestGroupSuite.component(
    directory: String,
    init: FirAndFe10TestGroup.() -> Unit,
) {
    group("components/$directory", init)
}

private fun FirAndFe10TestGroup.analysisApiTest(
    testRoot: String,
    frontend: Frontend,
    testClass: KClass<*>,
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit,
) {
    with(suite) {
        val fullTestPath = "analysis/analysis-api/testData" + directory?.let { "/$it" }.orEmpty()
        testGroup(testRoot, fullTestPath) {
            if (testModuleKinds.size == 1) {
                analysisApiTestClass(frontend, moduleKind = TestModuleKind.SOURCE, testClass, prefixNeeded = false, init)
            } else {
                testModuleKinds.forEach { component ->
                    analysisApiTestClass(frontend, component, testClass, prefixNeeded = true, init)
                }
            }
        }
    }
}

private fun TestGroup.analysisApiTestClass(
    frontend: Frontend,
    moduleKind: TestModuleKind,
    testClass: KClass<*>,
    prefixNeeded: Boolean,
    init: TestGroup.TestClass.(module: TestModuleKind) -> Unit
) {
    val fullPackage = getPackageName(frontend.prefix, testClass)

    val suiteTestClassName = buildString {
        append(fullPackage)
        append(frontend.prefix)
        moduleKind.componentName
            .capitalizeAsciiOnly()
            .takeIf { prefixNeeded }
            ?.let(::append)
        append(getDefaultSuiteTestClassName(testClass.java.simpleName))
    }

    getDefaultSuiteTestClassName(testClass.java.simpleName)

    val configurator = createConfigurator(frontend, moduleKind)

    testClass(
        testClass,
        suiteTestClassName = suiteTestClassName,
        useJunit4 = false
    ) {
        method(FrontendConfiguratorTestModel(configurator))
        init(moduleKind)
    }
}

private fun createConfigurator(
    frontend: Frontend,
    moduleKind: TestModuleKind
): KClass<out FrontendApiTestConfiguratorService> = when (frontend) {
    Frontend.FIR -> when (moduleKind) {
        TestModuleKind.SOURCE -> FirFrontendApiTestConfiguratorService::class
        TestModuleKind.LIBRARY -> LibraryFrontendApiTestConfiguratorService::class
        TestModuleKind.LIBRARY_SOURCE -> LibrarySourceFrontendApiTestConfiguratorService::class
    }
    Frontend.FE10 -> when (moduleKind) {
        TestModuleKind.SOURCE -> KtFe10FrontendApiTestConfiguratorService::class
        TestModuleKind.LIBRARY -> TODO("TestModuleKind.LIBRARY is unsupported for fe10")
        TestModuleKind.LIBRARY_SOURCE -> TODO("TestModuleKind.LIBRARY_SOURCE is unsupported for fe10")
    }
}


private fun getPackageName(prefix: String, testClass: KClass<*>): String {
    val basePrefix = "org.jetbrains.kotlin.analysis.api.${prefix.lowercase()}"
    val packagePrefix = testClass.java.name
        .substringAfter("org.jetbrains.kotlin.analysis.api.impl.base.test.")
        .substringBeforeLast('.', "")

    return if (packagePrefix.isEmpty()) "$basePrefix." else "$basePrefix.$packagePrefix."
}



