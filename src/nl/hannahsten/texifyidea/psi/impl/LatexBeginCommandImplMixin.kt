package nl.hannahsten.texifyidea.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import nl.hannahsten.texifyidea.psi.LatexBeginCommand
import nl.hannahsten.texifyidea.psi.LatexCommandWithParams
import nl.hannahsten.texifyidea.psi.LatexKeyValValue
import nl.hannahsten.texifyidea.psi.LatexOptionalKeyValKey
import nl.hannahsten.texifyidea.psi.LatexParameter
import nl.hannahsten.texifyidea.util.parser.getOptionalParameterMapFromParameters

abstract class LatexBeginCommandImplMixin(node: ASTNode) : LatexBeginCommand, ASTWrapperPsiElement(node) {

    override fun getOptionalParameterMap() = getOptionalParameterMapFromParameters(this.parameterList)

    override fun getRequiredParameters() = nl.hannahsten.texifyidea.util.parser.getRequiredParameters(this.parameterList)

    override val parameterList: List<LatexParameter>
        get() = children.filterIsInstance<LatexParameter>().toList()
}

abstract class LatexEndCommandImplMixin(node: ASTNode) : LatexCommandWithParams, ASTWrapperPsiElement(node) {

    override fun getOptionalParameterMap(): Map<LatexOptionalKeyValKey, LatexKeyValValue?> {
        return emptyMap()
    }

    override fun getRequiredParameters() = nl.hannahsten.texifyidea.util.parser.getRequiredParameters(this.parameterList)

    override val parameterList: List<LatexParameter>
        get() = children.filterIsInstance<LatexParameter>().toList()
}