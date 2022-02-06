package nl.hannahsten.texifyidea.inspections.latex.redundancy

import nl.hannahsten.texifyidea.file.LatexFileType
import nl.hannahsten.texifyidea.inspections.TexifyInspectionTestBase

class LatexCommandAlreadyDefinedInspectionTest : TexifyInspectionTestBase(LatexCommandAlreadyDefinedInspection()) {

    fun testWarning() {
        myFixture.configureByText(
            LatexFileType,
            """
            <warning descr="Command may already be defined in a LaTeX package">\newcommand{\cite}{\citeauthor}</warning>
            
            <warning descr="Command may already be defined in a LaTeX package">\def</warning>\citeauthor\cite
            
            \newcommand{\notexists}{}
            """.trimIndent()
        )
        myFixture.checkHighlighting()
    }
}