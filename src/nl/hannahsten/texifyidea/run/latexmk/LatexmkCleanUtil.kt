package nl.hannahsten.texifyidea.run.latexmk

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.util.io.awaitExit
import nl.hannahsten.texifyidea.TexifyBundle
import nl.hannahsten.texifyidea.util.runInBackgroundWithoutProgress
import java.nio.file.Path

object LatexmkCleanUtil {

    enum class Mode(val label: String) {
        CLEAN("Clean auxiliary files"),
        CLEAN_ALL("Clean all generated files"),
    }

    fun run(project: Project, runConfig: LatexmkRunConfiguration, mode: Mode) {
        val mainFile = runConfig.resolveMainFileIfNeeded()
        if (mainFile == null) {
            Notification(
                TexifyBundle.message("notification.group.latex"),
                TexifyBundle.message("run.latexmk.clean.failed.title"),
                TexifyBundle.message("run.latexmk.clean.failed.no.main.file"),
                NotificationType.ERROR
            ).notify(project)
            return
        }

        val command = LatexmkCommandBuilder.buildCleanCommand(runConfig, mode == Mode.CLEAN_ALL)
        if (command == null) {
            Notification(
                TexifyBundle.message("notification.group.latex"),
                TexifyBundle.message("run.latexmk.clean.failed.title"),
                TexifyBundle.message("run.latexmk.clean.failed.command.not.built"),
                NotificationType.ERROR
            ).notify(project)
            return
        }

        runInBackgroundWithoutProgress {
            val workingDirectoryPath = runConfig.getResolvedWorkingDirectory() ?: Path.of(mainFile.parent.path)
            val envVariables = runConfig.environmentVariables.envs.let { envs ->
                if (!runConfig.expandMacrosEnvVariables) {
                    envs
                }
                else {
                    val configurator = ProgramParametersConfigurator()
                    envs.mapValues { configurator.expandPathAndMacros(it.value, null, project) }
                }
            }

            runCatching {
                val process = GeneralCommandLine(command)
                    .withWorkingDirectory(workingDirectoryPath)
                    .withEnvironment(envVariables)
                    .toProcessBuilder()
                    .redirectErrorStream(true)
                    .start()

                process.inputReader().readText()
                val exitCode = process.awaitExit()

                if (exitCode == 0) {
                    val modeText = when (mode) {
                        Mode.CLEAN -> TexifyBundle.message("run.latexmk.clean.mode.auxiliary")
                        Mode.CLEAN_ALL -> TexifyBundle.message("run.latexmk.clean.mode.all.generated")
                    }
                    Notification(
                        TexifyBundle.message("notification.group.latex"),
                        TexifyBundle.message("run.latexmk.clean.completed.title"),
                        TexifyBundle.message("run.latexmk.clean.completed.message", modeText, mainFile.name),
                        NotificationType.INFORMATION
                    ).notify(project)
                }
                else {
                    Notification(
                        TexifyBundle.message("notification.group.latex"),
                        TexifyBundle.message("run.latexmk.clean.failed.title"),
                        TexifyBundle.message("run.latexmk.clean.failed.exit.code", exitCode),
                        NotificationType.ERROR
                    ).notify(project)
                }
            }.onFailure {
                Notification(
                    TexifyBundle.message("notification.group.latex"),
                    TexifyBundle.message("run.latexmk.clean.failed.title"),
                    it.message ?: TexifyBundle.message("run.error.unknown"),
                    NotificationType.ERROR
                ).notify(project)
            }
        }
    }
}
