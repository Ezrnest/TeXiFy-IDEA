package nl.hannahsten.texifyidea.psi

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import nl.hannahsten.texifyidea.parser.LatexParser
import nl.hannahsten.texifyidea.util.magic.EnvironmentMagic

@Suppress("FunctionName")
class LatexParserUtil : GeneratedParserUtilBase() {

    companion object {

        /**
         * Remap tokens inside verbatim environments to raw text.
         * Requires the lexer to be in a proper state before and after the environment.
         */
        @JvmStatic
        fun injection_env_content(builder: PsiBuilder, level: Int, rawText: Parser): Boolean {
            // This might be optimized by handling the tokens incrementally
            val beginText = builder.originalText.subSequence(
                builder.latestDoneMarker?.startOffset ?: return true,
                builder.latestDoneMarker?.endOffset ?: return true
            )
            val nameStart = beginText.indexOf('{') + 1
            val nameEnd = beginText.indexOf('}')
            if (nameStart >= nameEnd) return false

            val env = beginText.subSequence(nameStart, nameEnd).toString()

            if (!EnvironmentMagic.isProbablyVerbatim(env)) return false

            val startIndex = builder.currentOffset
            // Exclude the last newline, so it will stay a whitespace,
            // otherwise the formatter (LatexSpacingRules) will insert a
            // newline too much between environment content and \end
            val endIndex = builder.originalText.indexOf("\\end{$env}", startIndex) - 1

            // If there is nothing to remap, for example because there are only newlines, return false
            if (endIndex < startIndex) return false

            // Only remap \end and whitespace tokens, other ones are already raw text by the lexer
            // This makes sure the the optional argument of a verbatim environment is not by mistake also remapped to raw text
            // \end is remapped because the lexer only knows afterwards whether it ended the environment or not, and whitespace is remapped because this allows keeping the last whitespace for the formatter
            builder.setTokenTypeRemapper { token, start, end, _ ->
                if (startIndex <= start && end <= endIndex &&
                    (token == LatexTypes.END_TOKEN || token == LatexTypes.BEGIN_TOKEN || token == LatexTypes.OPEN_BRACE || token == LatexTypes.OPEN_BRACE || token == com.intellij.psi.TokenType.WHITE_SPACE)
                ) {
                    LatexTypes.RAW_TEXT_TOKEN
                }
                else {
                    token
                }
            }

            rawText.parse(builder, level)

            builder.setTokenTypeRemapper(null)

            return true
        }

        private fun hasPrecedingLinebreak(builder: PsiBuilder, startingOffset: Int): Boolean {
            // Get the raw text between the current position and the last consumed token
            val currentOffset = builder.currentOffset
            val precedingText = builder.originalText.subSequence(startingOffset, currentOffset)
            return precedingText.contains("\n") || precedingText.contains("\r")
        }

        @JvmStatic
        fun parseBeginCommand(builder: PsiBuilder, level: Int): Boolean {
            /*
            The `begin` command is something like:
            \begin{environment}[optional parameters]{}
            ---
            Latex allows whitespace between the `\begin` command, the environment name and the parameters.
            However, it would cause misinterpretation as we may have
            \begin{equation}
                [x+y]^2
            \end{equation}
            where `[]` are not parameters of the `begin` command, but rather part of the equation.

            Therefore, we require the following:
            - Whitespaces (including linebreaks) between `\begin` and the environment name
            - No linebreaks between the environment name and the rest of the parameters
             */
            // Expect BEGIN_TOKEN (\begin)
            if (builder.tokenType != LatexTypes.BEGIN_TOKEN) {
                return false
            }
            builder.advanceLexer()
            if (!LatexParser.parameter(builder, level + 1)) {
                return false // If the environment name is not present, drop the marker
            }

            val initialOffset = builder.currentOffset
            // Parse parameters only if they immediately follow without whitespace
            while (true) {
                val startingOffset = builder.latestDoneMarker?.endOffset ?: initialOffset
                val nextToken = builder.lookAhead(0)
                if (nextToken != LatexTypes.OPEN_BRACKET && nextToken != LatexTypes.OPEN_BRACE && nextToken != LatexTypes.OPEN_PAREN && nextToken != LatexTypes.ANGLE_PARAM) {
                    break // No more parameters
                }
                // Check for whitespace before the parameter
                if (hasPrecedingLinebreak(builder, startingOffset)) {
                    break // Stop parsing parameters if whitespace is found
                }
                if (!LatexParser.parameter(builder, level + 1)) {
                    break // Stop parsing parameters if parsing fails, for example, unclosed braces
                }
            }
            return true
        }
    }
}
