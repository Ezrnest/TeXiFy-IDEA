package nl.hannahsten.texifyidea.grammar;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import nl.hannahsten.texifyidea.psi.LatexTypes;

import java.util.ArrayDeque;
import java.util.Deque;
%%

%{
  private Deque<Integer> stack = new ArrayDeque<>();


  public void yypushState(int newState) {
    stack.push(yystate());
    yybegin(newState);
  }

  public void yypopState() {
    yybegin(stack.pop());
  }


  public LatexLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class LatexLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%xstate VERBATIM_INLINE, VERBATIM_ENV
%state COMMAND_WATING_ARGUMENT

//%state

BACK_SLASH = \\
WHITE_SPACE= [ \t\n\x0B\f\r]+
SPECIAL_CHAR = [#$%&~_\^{}\\]

COMMENT_LINE=%[^\r\n]*

INLINE_VERBATIM = \\verb
VERBATIM_DELIMITER=[^ :*\s]

VERBATIM_COMMANDS = verbatim|lstlisting|lstinputlisting|minted|mintedinput

COMMAND_NAME = [a-zA-Z]+*?

COMMAND = \\({COMMAND_NAME} | .)

PLAIN_TEXT = [^\\{}#%&~_\^]+

%%


{COMMENT_LINE}    { return LatexTypes.COMMENT_TOKEN; }


\\begin { return LatexTypes.BEGIN_TOKEN; }
\\end   { return LatexTypes.END_TOKEN; }




{COMMAND}    { return LatexTypes.N_COMMAND_IDENTIFIER; }



//\{    { return LatexTypes.OPEN_BRACE; }
//\}    { return LatexTypes.CLOSE_BRACE; }

{PLAIN_TEXT} { return LatexTypes.PLAIN_TEXT; }

{WHITE_SPACE}           { return TokenType.WHITE_SPACE; }
{SPECIAL_CHAR}          { return LatexTypes.SPECIAL_CHAR; }
[^]                     { return com.intellij.psi.TokenType.BAD_CHARACTER; }