package nl.hannahsten.texifyidea.inspections.latex.codematurity

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import nl.hannahsten.texifyidea.inspections.AbstractTexifyWholeFileRegexBasedInspection

/**
 * @author Hannah Schellekens
 */
class LatexPrimitiveEquationInspection : AbstractTexifyWholeFileRegexBasedInspection(
    inspectionId = "PrimitiveEquation",
    regex = """
        \$\$((?:[^$]|\$(?!\$))+)\$\$
    """.trimIndent().toRegex()
) {

    override fun errorMessage(matcher: MatchResult): String {
        return "Use '\\[..\\]' instead of primitive TeX display math."
    }

    override fun quickFixName(matcher: MatchResult): String {
        return "Replace with '\\[..\\]'"
    }

    override fun getReplacement(match: MatchResult, project: Project, problemDescriptor: ProblemDescriptor): String {
        return "\\[${match.groupValues[1]}\\]"
    }
}