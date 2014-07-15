/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.nativebinaries.plugins
import org.gradle.api.Incubating
import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.language.base.internal.LanguageRegistry
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.nativebinaries.ProjectNativeBinary
import org.gradle.nativebinaries.ProjectNativeExecutableBinary
import org.gradle.nativebinaries.ProjectSharedLibraryBinary
import org.gradle.nativebinaries.ProjectStaticLibraryBinary
import org.gradle.nativebinaries.internal.ProjectNativeBinaryInternal
import org.gradle.nativebinaries.language.internal.CreateSourceTransformTask
import org.gradle.nativebinaries.tasks.CreateStaticLibrary
import org.gradle.nativebinaries.tasks.InstallExecutable
import org.gradle.nativebinaries.tasks.LinkExecutable
import org.gradle.nativebinaries.tasks.LinkSharedLibrary
import org.gradle.nativebinaries.test.ProjectNativeTestSuiteBinary
import org.gradle.nativebinaries.toolchain.internal.ToolChainInternal
import org.gradle.nativebinaries.toolchain.internal.plugins.StandardToolChainsPlugin
import org.gradle.runtime.base.BinaryContainer
/**
 * A plugin that creates tasks used for constructing native binaries.
 */
@Incubating
public class NativeComponentPlugin implements Plugin<ProjectInternal> {

    public void apply(final ProjectInternal project) {
        project.plugins.apply(NativeComponentModelPlugin.class);
        project.plugins.apply(StandardToolChainsPlugin)

        final LanguageRegistry languages = project.getExtensions().getByType(LanguageRegistry.class);
        final BinaryContainer binaries = project.getExtensions().getByType(BinaryContainer.class);

        // TODO:DAZ Make a model rule
        // TODO:DAZ Apply to jvm binaries/languages too
        languages.all { language ->
            binaries.withType(ProjectNativeBinary) { binary ->
                final CreateSourceTransformTask createRule = new CreateSourceTransformTask(language);
                createRule.createCompileTasksForBinary(project.getTasks(), binary);
            }
        }

        binaries.withType(ProjectNativeBinary) { ProjectNativeBinaryInternal binary ->
            binary.conventionMapping.buildable = { isBuildableBinary(binary) }
            createTasks(project, binary)
        }
    }

    static boolean isBuildableBinary(ProjectNativeBinaryInternal binary) {
        final chain = binary.toolChain as ToolChainInternal
        chain.select(binary.getTargetPlatform()).available
    }

    def createTasks(ProjectInternal project, ProjectNativeBinaryInternal binary) {
        def builderTask
        if (binary instanceof ProjectNativeExecutableBinary || binary instanceof ProjectNativeTestSuiteBinary) {
            builderTask = createLinkExecutableTask(project, binary)
            binary.tasks.add createInstallTask(project, binary);
        } else if (binary instanceof ProjectSharedLibraryBinary) {
            builderTask = createLinkSharedLibraryTask(project, binary)
        } else if (binary instanceof ProjectStaticLibraryBinary) {
            builderTask = createStaticLibraryTask(project, binary)
        } else {
            throw new RuntimeException("Not a valid binary type for building: " + binary)
        }
        binary.tasks.add builderTask
        binary.builtBy builderTask
    }

    private LinkExecutable createLinkExecutableTask(ProjectInternal project, def executable) {
        def binary = executable as ProjectNativeBinaryInternal
        LinkExecutable linkTask = project.task(binary.namingScheme.getTaskName("link"), type: LinkExecutable) {
             description = "Links ${executable}"
         }

        linkTask.toolChain = binary.toolChain
        linkTask.targetPlatform = executable.targetPlatform

        linkTask.lib { binary.libs*.linkFiles }

        linkTask.conventionMapping.outputFile = { executable.executableFile }
        linkTask.linkerArgs = binary.linker.args
        return linkTask
    }

    private LinkSharedLibrary createLinkSharedLibraryTask(ProjectInternal project, ProjectSharedLibraryBinary sharedLibrary) {
        def binary = sharedLibrary as ProjectNativeBinaryInternal
        LinkSharedLibrary linkTask = project.task(binary.namingScheme.getTaskName("link"), type: LinkSharedLibrary) {
             description = "Links ${sharedLibrary}"
         }

        linkTask.toolChain = binary.toolChain
        linkTask.targetPlatform = binary.targetPlatform

        linkTask.lib { binary.libs*.linkFiles }

        linkTask.conventionMapping.outputFile = { sharedLibrary.sharedLibraryFile }
        linkTask.conventionMapping.installName = { sharedLibrary.sharedLibraryFile.name }
        linkTask.linkerArgs = binary.linker.args
        return linkTask
    }

    private CreateStaticLibrary createStaticLibraryTask(ProjectInternal project, ProjectStaticLibraryBinary staticLibrary) {
        def binary = staticLibrary as ProjectNativeBinaryInternal
        CreateStaticLibrary task = project.task(binary.namingScheme.getTaskName("create"), type: CreateStaticLibrary) {
             description = "Creates ${staticLibrary}"
         }

        task.toolChain = binary.toolChain
        task.targetPlatform = staticLibrary.targetPlatform
        task.conventionMapping.outputFile = { staticLibrary.staticLibraryFile }
        task.staticLibArgs = binary.staticLibArchiver.args
        return task
    }

    def createInstallTask(ProjectInternal project, def executable) {
        def binary = executable as ProjectNativeBinaryInternal
        InstallExecutable installTask = project.task(binary.namingScheme.getTaskName("install"), type: InstallExecutable) {
            description = "Installs a development image of $executable"
            group = LifecycleBasePlugin.BUILD_GROUP
        }

        installTask.toolChain = binary.toolChain
        installTask.conventionMapping.destinationDir = { project.file("${project.buildDir}/install/${binary.namingScheme.outputDirectoryBase}") }

        installTask.conventionMapping.executable = { executable.executableFile }
        installTask.lib { binary.libs*.runtimeFiles }

        installTask.dependsOn(executable)
        return installTask
    }
}