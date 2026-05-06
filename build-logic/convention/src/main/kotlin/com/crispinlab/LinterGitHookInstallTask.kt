package com.crispinlab

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

internal sealed class LinterGitHookInstallTask(
    private val hookName: String,
    private val gradleTask: String
) : DefaultTask() {
    @TaskAction
    fun installHook() {
        val gitDir: File = project.rootProject.file(".git")
        if (!gitDir.isDirectory) {
            logger.lifecycle("Skipping $hookName hook install: .git directory not found")
            return
        }

        val hookFile = File(gitDir, "hooks/$hookName")
        hookFile.parentFile.mkdirs()
        hookFile.writeText(createHookScript())

        runCatching { hookFile.setExecutable(true) }
            .onFailure { logger.warn("Could not set executable permission for $hookFile") }
    }

    private fun createHookScript(): String =
        """
        #!/bin/sh
        set -e
        ./gradlew $gradleTask
        """.trimIndent()
}

internal abstract class InstallPreCommitHookTask :
    LinterGitHookInstallTask(
        hookName = "pre-commit",
        gradleTask = "lintKotlin"
    )

internal abstract class InstallPrePushHookTask :
    LinterGitHookInstallTask(
        hookName = "pre-push",
        gradleTask = "test"
    )
