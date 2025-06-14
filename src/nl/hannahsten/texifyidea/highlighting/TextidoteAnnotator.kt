package nl.hannahsten.texifyidea.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.util.execution.ParametersListUtil
import nl.hannahsten.texifyidea.settings.TexifySettings
import nl.hannahsten.texifyidea.util.runCommandWithExitCode
import java.nio.file.Path
import kotlin.io.path.Path

@JvmInline value class FileName(val value: String)

@JvmInline value class StartLine(val value: Int)

@JvmInline value class StartColumn(val value: Int)

@JvmInline value class EndLine(val value: Int)

@JvmInline value class EndColumn(val value: Int)

data class TextidoteAnnotatorInitialInfo(
    val fileName: String,
    val workingDirectory: Path,
    val project: Project,
    val document: Document,
)

data class TextidoteWarning(
    val fileName: FileName,
    val startLine: StartLine,
    val startColumn: StartColumn,
    val endLine: EndLine,
    val endColumn: EndColumn,
    val message: String,
)

data class TextidoteAnnotationResult(
    val warnings: List<TextidoteWarning>,
    val document: Document,
)

class TextidoteAnnotator : DumbAware, ExternalAnnotator<TextidoteAnnotatorInitialInfo, TextidoteAnnotationResult>() {

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): TextidoteAnnotatorInitialInfo? {
        if (file.containingDirectory == null) return null

        return TextidoteAnnotatorInitialInfo(
            file.virtualFile.name,
            Path(file.containingDirectory?.virtualFile?.path ?: return null),
            file.project,
            editor.document,
        )
    }

    override fun doAnnotate(collectedInfo: TextidoteAnnotatorInitialInfo?): TextidoteAnnotationResult? {
        if (collectedInfo == null) return null
        if (!TexifySettings.getInstance().enableTextidote) {
            return TextidoteAnnotationResult(emptyList(), collectedInfo.document)
        }

        val arguments = TexifySettings.getInstance().textidoteOptions
        val command = listOf("textidote") + ParametersListUtil.parse(arguments) + listOf(collectedInfo.fileName)
        val (output, exitCode) = runCommandWithExitCode(
            *command.toTypedArray(),
            workingDirectory = collectedInfo.workingDirectory,
            timeout = 10,
            returnExceptionMessage = true
        )

        // Since the user has explicitly enabled this inspection, we should raise an error if we cannot actually run textidote
        if (output == null) {
            Notification("LaTeX", "Could not run textidote", NotificationType.ERROR).notify(collectedInfo.project)
            return TextidoteAnnotationResult(emptyList(), collectedInfo.document)
        }

        // Example output:
        // main.tex(L8C16-L8C16): This sentence does not start with an uppercase letter.. Suggestions: [Blubblub] (39) "    \crefrange{blub}{blub}"
        val warnings =
            """(?<file>[^(\n]+)\(L(?<line1>\d+)C(?<column1>\d+)-L(?<line2>\d+)C(?<column2>\d+)\):\s*(?<message>[^"]+)"""".toRegex()
                .findAll(output)
                .filter { match -> match.groups["file"]?.value == collectedInfo.fileName }
                .mapNotNull { match ->
                    val line1 = match.groups["line1"]?.value?.toInt() ?: return@mapNotNull null
                    val column1 = match.groups["column1"]?.value?.toInt() ?: return@mapNotNull null
                    val line2 = match.groups["line2"]?.value?.toInt() ?: return@mapNotNull null
                    val column2 = match.groups["column2"]?.value?.toInt() ?: return@mapNotNull null
                    val message = match.groups["message"]?.value ?: return@mapNotNull null

                    TextidoteWarning(FileName(collectedInfo.fileName), StartLine(line1), StartColumn(column1), EndLine(line2), EndColumn(column2), message)
                }.toList()

        // Exit code is nonzero when warnings are found
        if (warnings.isEmpty() && exitCode != 0) {
            Notification(
                "LaTeX",
                "There was a problem getting Textidote results: $output",
                NotificationType.ERROR
            ).notify(collectedInfo.project)
        }

        return TextidoteAnnotationResult(warnings, collectedInfo.document)
    }

    override fun apply(file: PsiFile, annotationResult: TextidoteAnnotationResult?, holder: AnnotationHolder) {
        if (annotationResult == null) return
        for (warning in annotationResult.warnings) {
            val document = annotationResult.document

            // Don't show obsolete warnings out of range
            if (warning.endLine.value > document.lineCount) {
                continue
            }

            // In Textidote, everything is 1-based, and here everything is 0-based
            val lineStartOffset1 = document.getLineStartOffset(warning.startLine.value - 1)
            val lineStartOffset2 = document.getLineStartOffset(warning.endLine.value - 1)

            val startOffset = lineStartOffset1 + warning.startColumn.value - 1
            val endOffset = lineStartOffset2 + warning.endColumn.value - 1

            // Check if computed range falls in the document range.
            if (startOffset < document.textLength && endOffset < document.textLength) {
                holder.newAnnotation(HighlightSeverity.WARNING, warning.message + " (Textidote)")
                    .range(TextRange(startOffset, endOffset))
                    .create()
            }
        }

        super.apply(file, annotationResult, holder)
    }
}