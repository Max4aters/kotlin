/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.psi.KtFile

internal class ProjectStructureProviderByCompilerConfiguration(
    private val compilerConfig: CompilerConfiguration,
    project: Project,
    ktFiles: List<KtFile>,
) : ProjectStructureProvider() {
    private val sourceFiles: Set<String> by lazy {
        compilerConfig.javaSourceRoots
    }

    private val sourceModule = KtSourceModuleByCompilerConfiguration(compilerConfig, project, ktFiles)

    override fun getKtModuleForKtElement(element: PsiElement): KtModule {
        return if (element.containingFile.virtualFile.path in sourceFiles)
            sourceModule
        else
        // Assume classes in class path are in a single module
            sourceModule.directRegularDependencies.single()
    }
}
