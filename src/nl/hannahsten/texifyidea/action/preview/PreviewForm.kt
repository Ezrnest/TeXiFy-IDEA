package nl.hannahsten.texifyidea.action.preview

import nl.hannahsten.texifyidea.ui.ImagePanel
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.JTextArea

/**
 * @author Sergei Izmailov
 */
class PreviewForm {

    var panel: JPanel? = null
    private var equationArea: JTextArea? = null
    private var outputArea: JTextArea? = null
    private var equationPanel: ImagePanel? = null
    private var latexOutputTab: JPanel? = null
    private var equationTab: JPanel? = null
    private var tabbedPane: JTabbedPane? = null

    fun setEquation(equation: String) {
        equationArea!!.text = equation
    }

    fun setPreview(image: BufferedImage, latexOutput: String) {
        equationPanel!!.setImage(image)
        outputArea!!.text = latexOutput
        tabbedPane!!.selectedIndex = tabbedPane!!.indexOfComponent(equationTab)
    }

    fun setLatexErrorMessage(errorMessage: String, showJlatexmathHint: Boolean = false) {
        outputArea!!.text = errorMessage
        if (showJlatexmathHint) {
            outputArea!!.text += "\n\nNote: if something is not supported by JLaTeXMath, you can use \n%! Begin preamble = math ... %! End preamble = math\nmagic comments to specify any dependencies and use the inkscape-based previewer. \nSee https://hannah-sten.github.io/TeXiFy-IDEA/tool-windows.html#equation-preview"
        }
        equationPanel!!.clearImage()
        tabbedPane!!.selectedIndex = tabbedPane!!.indexOfComponent(latexOutputTab)
    }

    fun clear() {
        outputArea!!.text = ""
        equationArea!!.text = ""
        equationPanel!!.clearImage()
    }
}
