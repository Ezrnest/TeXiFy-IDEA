package nl.hannahsten.texifyidea.run.latexmk

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.util.io.awaitExit
import nl.hannahsten.texifyidea.TexifyBundle
import com.intellij.util.execution.ParametersListUtil
import nl.hannahsten.texifyidea.run.compiler.LatexCompiler
import nl.hannahsten.texifyidea.run.latex.LatexPathResolver
import nl.hannahsten.texifyidea.run.latex.LatexRunConfiguration
import nl.hannahsten.texifyidea.settings.sdk.LatexSdkUtil
import nl.hannahsten.texifyidea.util.runInBackgroundWithoutProgress
import java.nio.file.Path

object LatexmkCleanUtil {

    enum class Mode(val label: String) {
        CLEAN("Clean auxiliary files"),
        CLEAN_ALL("Clean all generated files"),
    }

    fun run(project: Project, runConfig: LatexRunConfiguration, mode: Mode) {
        if (runConfig.compiler != LatexCompiler.LATEXMK) {
            Notification("LaTeX", "Latexmk clean failed", "Selected run configuration is not using latexmk.", NotificationType.ERROR).notify(project)
            return
        }

        val mainFile = runConfig.mainFile
        if (mainFile == null) {
            Notification(
                TexifyBundle.message("notification.group.latex"),
                TexifyBundle.message("run.latexmk.clean.failed.title"),
                TexifyBundle.message("run.latexmk.clean.failed.no.main.file"),
                NotificationType.ERROR
            ).notify(project)
            return
        }

        val command = buildCleanCommand(runConfig, mode == Mode.CLEAN_ALL)
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

    private fun buildCleanCommand(runConfig: LatexRunConfiguration, cleanAll: Boolean): List<String>? {
        val mainFile = runConfig.mainFile ?: return null
        val distributionType = runConfig.getLatexDistributionType()
        val executable = runConfig.compilerPath ?: LatexSdkUtil.getExecutableName(
            LatexCompiler.LATEXMK.executableName,
            runConfig.project,
            runConfig.getLatexSdk(),
            distributionType,
        )

        val command = mutableListOf(executable)
        val compilerArguments = runConfig.buildLatexmkArguments()
        if (compilerArguments.isNotBlank()) {
            command += ParametersListUtil.parse(compilerArguments)
        }

        val outputPath = LatexPathResolver.resolveOutputDir(runConfig)?.path ?: mainFile.parent.path
        command += "-outdir=$outputPath"

        val auxPath = LatexPathResolver.resolveAuxDir(runConfig)?.path
        if (auxPath != null && auxPath != outputPath) {
            command += "-auxdir=$auxPath"
        }

        command += if (cleanAll) "-C" else "-c"
        command += if (runConfig.hasDefaultWorkingDirectory()) mainFile.name else mainFile.path
        return command
    }
}
