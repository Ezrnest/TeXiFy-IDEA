package nl.hannahsten.texifyidea.run.compiler

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import nl.hannahsten.texifyidea.run.latex.LatexDistributionType
import nl.hannahsten.texifyidea.run.latex.LatexRunConfiguration
import nl.hannahsten.texifyidea.settings.sdk.LatexSdkUtil
import nl.hannahsten.texifyidea.util.LatexmkRcFileFinder
import nl.hannahsten.texifyidea.util.runCommand
import nl.hannahsten.texifyidea.util.splitWhitespace

/**
 * @author Hannah Schellekens, Sten Wessel
 */
@Suppress("unused", "DuplicatedCode")
enum class LatexCompiler(private val displayName: String, val executableName: String) {

    PDFLATEX("pdfLaTeX", "pdflatex") {

        override fun createCommand(
            runConfig: LatexRunConfiguration,
            auxilPath: String?,
            outputPath: String?,
            moduleRoot: VirtualFile?,
            moduleRoots: Array<VirtualFile>
        ): MutableList<String> {
            // For now only support custom executable for TeX Live
            // At least avoids prepending a full path to a supposed TeX Live executable when in fact it will be prepended by a docker command
            val executable = LatexSdkUtil.getExecutableName(executableName, runConfig.project)
            val command = mutableListOf(runConfig.compilerPath ?: executable)

            command.add("-file-line-error")
            command.add("-interaction=nonstopmode")
            command.add("-synctex=1")
            command.add("-output-format=${runConfig.outputFormat.name.toLowerCase()}")

            command.add("-output-directory=$outputPath")

            // -aux-directory only exists on MiKTeX
            if (auxilPath != null && runConfig.getLatexDistributionType().isMiktex()) {
                command.add("-aux-directory=$auxilPath")
            }

            // Prepend root paths to the input search path
            if (runConfig.getLatexDistributionType().isMiktex()) {
                moduleRoots.forEach {
                    command.add("-include-directory=${it.path}")
                }
            }

            return command
        }
    },

    LUALATEX("LuaLaTeX", "lualatex") {

        override fun createCommand(
            runConfig: LatexRunConfiguration,
            auxilPath: String?,
            outputPath: String?,
            moduleRoot: VirtualFile?,
            moduleRoots: Array<VirtualFile>
        ): MutableList<String> {
            val command = mutableListOf(runConfig.compilerPath ?: LatexSdkUtil.getExecutableName(executableName, runConfig.project))

            // Some commands are the same as for pdflatex
            command.add("-file-line-error")
            command.add("-interaction=nonstopmode")
            command.add("-synctex=1")
            command.add("-output-format=${runConfig.outputFormat.name.toLowerCase()}")

            command.add("-output-directory=$outputPath")

            // Note that lualatex has no -aux-directory
            return command
        }
    },

    LATEXMK("Latexmk", "latexmk") {

        override val includesBibtex = true

        override val includesMakeindex = true

        override val handlesNumberOfCompiles = true

        override val outputFormats = arrayOf(Format.DEFAULT, Format.PDF, Format.DVI)

        override fun createCommand(
            runConfig: LatexRunConfiguration,
            auxilPath: String?,
            outputPath: String?,
            moduleRoot: VirtualFile?,
            moduleRoots: Array<VirtualFile>
        ): MutableList<String> {
            val command = mutableListOf(runConfig.compilerPath ?: LatexSdkUtil.getExecutableName(executableName, runConfig.project))

            val isLatexmkRcFilePresent = LatexmkRcFileFinder.isLatexmkRcFilePresent(runConfig)

            // If it is present, assume that it will handle everything (command line options would overwrite latexmkrc options)
            if (!isLatexmkRcFilePresent) {
                // Adding the -pdf flag makes latexmk run with pdflatex, which is definitely preferred over running with just latex
                command.add("-pdf")
                command.add("-file-line-error")
                command.add("-interaction=nonstopmode")
                command.add("-synctex=1")
            }

            if (runConfig.outputFormat != Format.DEFAULT) {
                command.add("-output-format=${runConfig.outputFormat.name.toLowerCase()}")
            }

            command.add("-output-directory=$outputPath")

            if (auxilPath != null && runConfig.getLatexDistributionType().isMiktex()) {
                command.add("-aux-directory=$auxilPath")
            }

            // -include-directory does not work with latexmk
            return command
        }
    },

    XELATEX("XeLaTeX", "xelatex") {

        override val outputFormats = arrayOf(Format.PDF, Format.XDV)

        override fun createCommand(
            runConfig: LatexRunConfiguration,
            auxilPath: String?,
            outputPath: String?,
            moduleRoot: VirtualFile?,
            moduleRoots: Array<VirtualFile>
        ): MutableList<String> {
            val command = mutableListOf(runConfig.compilerPath ?: LatexSdkUtil.getExecutableName(executableName, runConfig.project))

            // As usual, available command line options can be viewed with xelatex --help
            // On TeX Live, installing collection-xetex should be sufficient to get xelatex
            command.add("-file-line-error")
            command.add("-interaction=nonstopmode")
            command.add("-synctex=1")

            if (runConfig.outputFormat == Format.XDV) {
                command.add("-no-pdf")
            }

            command.add("-output-directory=$outputPath")

            if (auxilPath != null && runConfig.getLatexDistributionType().isMiktex()) {
                command.add("-aux-directory=$auxilPath")
            }

            // Prepend root paths to the input search path
            if (runConfig.getLatexDistributionType().isMiktex()) {
                moduleRoots.forEach {
                    command.add("-include-directory=${it.path}")
                }
            }

            return command
        }
    },

    TEXLIVEONFLY("Texliveonfly", "texliveonfly") {

        override val outputFormats = arrayOf(Format.PDF)

        override fun createCommand(
            runConfig: LatexRunConfiguration,
            auxilPath: String?,
            outputPath: String?,
            moduleRoot: VirtualFile?,
            moduleRoots: Array<VirtualFile>
        ): MutableList<String> {
            val command = mutableListOf(runConfig.compilerPath ?: LatexSdkUtil.getExecutableName(executableName, runConfig.project))

            // texliveonfly is a Python script which calls other compilers (by default pdflatex), main feature is downloading packages automatically
            // commands can be passed to those compilers with the arguments flag, however apparently IntelliJ cannot handle quotes so we cannot pass multiple arguments to pdflatex.
            // Fortunately, -synctex=1 and -interaction=nonstopmode are on by default in texliveonfly
            // Since adding one will work without any quotes, we choose the output directory.
            if (outputPath != null) {
                command.add("--arguments=--output-directory=$outputPath")
            }

            return command
        }
    },

    TECTONIC("Tectonic", "tectonic") {

        override val includesBibtex = true

        override val handlesNumberOfCompiles = true

        override val outputFormats = arrayOf(Format.PDF, Format.HTML, Format.XDV, Format.AUX)

        override fun createCommand(
            runConfig: LatexRunConfiguration,
            auxilPath: String?,
            outputPath: String?,
            moduleRoot: VirtualFile?,
            moduleRoots: Array<VirtualFile>
        ): MutableList<String> {

            // The available command line arguments can be found at https://github.com/tectonic-typesetting/tectonic/blob/d7a8497c90deb08b5e5792a11d6e8b082f53bbb7/src/bin/tectonic.rs#L158
            val command = mutableListOf(runConfig.compilerPath ?: LatexSdkUtil.getExecutableName(executableName, runConfig.project))

            command.add("--synctex")

            command.add("--outfmt=${runConfig.outputFormat.name.toLowerCase()}")

            if (outputPath != null) {
                command.add("--outdir=$outputPath")
            }

            return command
        }
    };

    /**
     * Convert Windows paths to WSL paths.
     */
    private fun String.toPath(runConfig: LatexRunConfiguration): String =
        if (runConfig.getLatexDistributionType() == LatexDistributionType.WSL_TEXLIVE) {
            runCommand("wsl", "wslpath", "-a", this) ?: this
        }
        else this

    /**
     * Get the execution command for the latex compiler.
     *
     * @param runConfig
     *          The run configuration object to get the command for.
     * @param project
     *          The current project.
     */
    fun getCommand(runConfig: LatexRunConfiguration, project: Project): List<String>? {
        val rootManager = ProjectRootManager.getInstance(project)
        val fileIndex = rootManager.fileIndex
        val mainFile = runConfig.mainFile ?: return null
        val moduleRoot = fileIndex.getContentRootForFile(mainFile)
        // For now we disable module roots with Docker
        // Could be improved by mounting them to the right directory
        val moduleRoots = if (runConfig.getLatexDistributionType() != LatexDistributionType.DOCKER_MIKTEX) {
            rootManager.contentSourceRoots
        }
        else {
            emptyArray()
        }

        // If we used /miktex/work/out, an out directory would appear in the src folder on the host system
        val dockerOutputDir = "/miktex/out"
        val dockerAuxilDir = "/miktex/auxil"
        val outputPath = if (runConfig.getLatexDistributionType() != LatexDistributionType.DOCKER_MIKTEX) {
            runConfig.outputPath.getAndCreatePath()?.path?.toPath(runConfig)
        }
        else {
            dockerOutputDir
        }

        val auxilPath = if (runConfig.getLatexDistributionType() != LatexDistributionType.DOCKER_MIKTEX) {
            runConfig.auxilPath.getAndCreatePath()?.path?.toPath(runConfig)
        }
        else {
            dockerAuxilDir
        }

        var command = createCommand(
            runConfig,
            auxilPath,
            outputPath,
            moduleRoot,
            moduleRoots
        )

        if (runConfig.getLatexDistributionType() == LatexDistributionType.WSL_TEXLIVE) {
            command = mutableListOf("bash", "-ic", GeneralCommandLine(command).commandLineString)
        }

        if (runConfig.getLatexDistributionType() == LatexDistributionType.DOCKER_MIKTEX) {
            createDockerCommand(runConfig, dockerAuxilDir, dockerOutputDir, mainFile, command)
        }

        // Custom compiler arguments specified by the user
        runConfig.compilerArguments?.let { arguments ->
            arguments.splitWhitespace()
                .dropLastWhile { it.isEmpty() }
                .forEach { command.add(it) }
        }

        if (runConfig.getLatexDistributionType() == LatexDistributionType.WSL_TEXLIVE) {
            command[command.size - 1] = command.last() + " ${mainFile.path.toPath(runConfig)}"
        }
        else {
            command.add(mainFile.name)
        }

        return command
    }

    @Suppress("SameParameterValue")
    private fun createDockerCommand(runConfig: LatexRunConfiguration, dockerAuxilDir: String, dockerOutputDir: String, mainFile: VirtualFile, command: MutableList<String>) {
        // See https://hub.docker.com/r/miktex/miktex
        "docker volume create --name miktex".runCommand()

        val parameterList = mutableListOf(
            "docker", // Could be improved by getting executable name based on SDK
            "run",
            "--rm",
            "-v",
            "miktex:/miktex/.miktex",
            "-v",
            "${mainFile.parent.path}:/miktex/work"
        )

        // Avoid mounting the mainfile parent also to /miktex/work/out,
        // because there may be a good reason to make the output directory the same as the source directory
        if (runConfig.outputPath.getAndCreatePath() != mainFile.parent) {
            parameterList.addAll(listOf("-v", "${runConfig.outputPath.getAndCreatePath()?.path}:$dockerOutputDir"))
        }

        if (runConfig.auxilPath.getAndCreatePath() != mainFile.parent) {
            parameterList.addAll(listOf("-v", "${runConfig.auxilPath.getAndCreatePath()}:$dockerAuxilDir"))
        }

        parameterList.add("docker.pkg.github.com/hannah-sten/texify-idea/miktex:latest")

        command.addAll(0, parameterList)
    }

    /**
     * Create the command to execute to use the compiler.
     *
     * @param runConfig LaTeX run configuration which initiated the action of creating this command.
     * @param moduleRoot Module root.
     * @param moduleRoots List of source roots.
     *
     * @return The command to be executed.
     */
    protected open fun createCommand(
        runConfig: LatexRunConfiguration,
        auxilPath: String?,
        outputPath: String?,
        moduleRoot: VirtualFile?,
        moduleRoots: Array<VirtualFile>
    ): MutableList<String> = error("Not implemented for $this")

    /**
     * Whether the compiler includes running bibtex/biber.
     */
    open val includesBibtex = false

    /**
     * Whether the compiler includes running index programs.
     */
    open val includesMakeindex = false

    /**
     * Whether the compiler automatically determines the number of compiles needed.
     */
    open val handlesNumberOfCompiles = false

    /**
     * List of output formats supported by this compiler.
     */
    open val outputFormats: Array<Format> = arrayOf(Format.PDF, Format.DVI)

    override fun toString() = this.displayName

    companion object {

        fun byExecutableName(exe: String): LatexCompiler {
            return values().firstOrNull {
                it.executableName.equals(exe, true)
            } ?: PDFLATEX
        }
    }

    /**
     * @author Hannah Schellekens
     */
    enum class Format {

        DEFAULT, // Means: don't overwite the default, e.g. a default from the latexmkrc, i.e. don't add any command line parameters
        PDF,
        DVI,
        HTML,
        XDV,
        AUX;

        companion object {

            fun byNameIgnoreCase(name: String?): Format {
                return values().firstOrNull {
                    it.name.equals(name, ignoreCase = true)
                } ?: PDF
            }
        }
    }
}
