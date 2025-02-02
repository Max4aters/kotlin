/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils.libraries.source

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.TestKtLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.projectModuleProvider
import org.jetbrains.kotlin.analysis.api.impl.base.test.utils.libraries.TestLibraryUtils
import org.jetbrains.kotlin.analysis.api.impl.base.test.utils.libraries.compiledLibraryProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based.registerTestServices
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.test.frontend.fir.getAnalyzerServices
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.PreAnalysisHandler
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import java.nio.file.Path


class LibrarySourceModuleRegistrarPreAnalysisHandler(
    testServices: TestServices
) : PreAnalysisHandler(testServices) {
    private val moduleInfoProvider = testServices.projectModuleProvider

    override fun preprocessModuleStructure(moduleStructure: TestModuleStructure) {
        val testModule = moduleStructure.modules.single()
        val project = testServices.compilerConfigurationProvider.getProject(testModule)
        val (libraryJar, librarySourcesJar) = testServices.compiledLibraryProvider.getCompiledLibrary(testModule)

        val libraryKtModule = KtTestLibraryModule(project, testModule, libraryJar)

        val librarySourceKtFiles = TestLibraryUtils
            .getAllVirtualFilesFromJar(librarySourcesJar)
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? KtFile }

        val sourcesKtModule = TestKtLibrarySourceModule(
            project,
            testModule,
            librarySourceKtFiles.toSet(),
            testServices,
            libraryKtModule
        )
        libraryKtModule.librarySources = sourcesKtModule

        moduleInfoProvider.registerModuleInfo(testModule, sourcesKtModule)
        (project as MockProject).registerTestServices(testModule, librarySourceKtFiles, testServices)
    }
}

private class KtTestLibraryModule(
    override val project: Project,
    private val testModule: TestModule,
    private val jar: Path
) : KtLibraryModule {
    override val directRegularDependencies: List<KtModule> get() = emptyList()
    override val directRefinementDependencies: List<KtModule> get() = emptyList()
    override val directFriendDependencies: List<KtModule> get() = emptyList()

    override val contentScope: GlobalSearchScope by lazy {
        GlobalSearchScope.filesScope(project, TestLibraryUtils.getAllVirtualFilesFromJar(jar))
    }

    override fun getBinaryRoots(): Collection<Path> = listOf(jar)
    override val libraryName: String get() = testModule.name

    override lateinit var librarySources: KtLibrarySourceModule

    override val platform: TargetPlatform get() = testModule.targetPlatform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = testModule.targetPlatform.getAnalyzerServices()
}