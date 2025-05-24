package nl.hannahsten.texifyidea.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import nl.hannahsten.texifyidea.psi.LatexBeginCommand
import nl.hannahsten.texifyidea.psi.LatexBeginEndCommand
import nl.hannahsten.texifyidea.psi.LatexCommandWithParams
import nl.hannahsten.texifyidea.psi.LatexEndCommand
import nl.hannahsten.texifyidea.psi.LatexKeyValValue
import nl.hannahsten.texifyidea.psi.LatexOptionalKeyValKey
import nl.hannahsten.texifyidea.psi.LatexParameter
import nl.hannahsten.texifyidea.util.parser.getOptionalParameterMapFromParameters

abstract class LatexBeginCommandImplMixin(node: ASTNode) : LatexBeginCommand, ASTWrapperPsiElement(node) {

    override val parameterList: List<LatexParameter>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, LatexParameter::class.java)
}

abstract class LatexEndCommandImplMixin(node: ASTNode) : LatexCommandWithParams, LatexEndCommand, ASTWrapperPsiElement(node) {

    override fun getOptionalParameterMap(): Map<LatexOptionalKeyValKey, LatexKeyValValue?> {
        return emptyMap()
    }
}