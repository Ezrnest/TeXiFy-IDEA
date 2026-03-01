package nl.hannahsten.texifyidea.run.latex.ui.fragments

import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import nl.hannahsten.texifyidea.TexifyBundle
import nl.hannahsten.texifyidea.run.latex.PdfViewerStepOptions
import nl.hannahsten.texifyidea.run.latex.StepUiOptionIds
import nl.hannahsten.texifyidea.run.pdfviewer.PdfViewer
import java.awt.event.ItemEvent

internal class LatexViewerStepFragmentedEditor(
    initialStep: PdfViewerStepOptions = PdfViewerStepOptions(),
) : AbstractStepFragmentedEditor<PdfViewerStepOptions>(initialStep) {

    private val pdfViewer = ComboBox(PdfViewer.availableViewers.toTypedArray())
    private val pdfViewerRow = LabeledComponent.create(pdfViewer, TexifyBundle.message("run.step.ui.field.pdf.viewer"))

    private val requireFocus = JBCheckBox(TexifyBundle.message("run.latex.settings.allow.viewer.focus"))
    private val viewerCommand = JBTextField()
    private val viewerCommandRow = LabeledComponent.create(viewerCommand, TexifyBundle.message("run.step.ui.field.custom.viewer.command"))

    init {
        pdfViewer.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                updateRequireFocusEnabled()
                fireEditorStateChanged()
            }
        }
    }

    override fun createFragments(): Collection<SettingsEditorFragment<PdfViewerStepOptions, *>> {
        val headerFragment = CommonParameterFragments.createHeader<PdfViewerStepOptions>(TexifyBundle.message("run.step.ui.header.pdf.viewer"))

        val viewerFragment = stepFragment(
            id = "step.viewer.type",
            name = TexifyBundle.message("run.step.ui.field.pdf.viewer"),
            component = pdfViewerRow,
            reset = { step, component ->
                component.component.selectedItem = PdfViewer.availableViewers.firstOrNull { it.name == step.pdfViewerName }
                    ?: PdfViewer.firstAvailableViewer
                updateRequireFocusEnabled()
            },
            apply = { step, component ->
                step.pdfViewerName = (component.component.selectedItem as? PdfViewer ?: PdfViewer.firstAvailableViewer).name
            },
            initiallyVisible = { true },
            removable = false,
            hint = TexifyBundle.message("run.step.ui.hint.pdf.viewer"),
        )

        val focusFragment = stepFragment(
            id = StepUiOptionIds.VIEWER_REQUIRE_FOCUS,
            name = TexifyBundle.message("run.step.ui.field.require.focus"),
            component = requireFocus,
            reset = { step, component ->
                component.isSelected = step.requireFocus
                updateRequireFocusEnabled()
            },
            apply = { step, component ->
                step.requireFocus = component.isSelected
            },
            initiallyVisible = { step -> !step.requireFocus },
            removable = true,
            hint = TexifyBundle.message("run.step.ui.hint.require.focus"),
            actionHint = TexifyBundle.message("run.step.ui.action.set.require.focus"),
        )

        val commandFragment = stepFragment(
            id = StepUiOptionIds.VIEWER_COMMAND,
            name = TexifyBundle.message("run.step.ui.field.custom.viewer.command"),
            component = viewerCommandRow,
            reset = { step, component -> component.component.text = step.customViewerCommand.orEmpty() },
            apply = { step, component -> step.customViewerCommand = component.component.text.ifBlank { null } },
            initiallyVisible = { step -> !step.customViewerCommand.isNullOrBlank() },
            removable = true,
            hint = TexifyBundle.message("run.step.ui.hint.custom.viewer.command"),
            actionHint = TexifyBundle.message("run.step.ui.action.set.custom.viewer.command"),
        )

        return listOf(
            headerFragment,
            viewerFragment,
            focusFragment,
            commandFragment,
        )
    }

    private fun updateRequireFocusEnabled() {
        val selectedViewer = pdfViewer.selectedItem as? PdfViewer
        val supported = selectedViewer?.let { it.isForwardSearchSupported && it.isFocusSupported } ?: false
        requireFocus.isEnabled = supported
    }
}
