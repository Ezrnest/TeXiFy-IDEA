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

  public boolean yypopState(int currentState){
      if(yystate() == currentState){
          yybegin(stack.pop());
          return true;
      }
      return false;
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
%state COMMAND_WAITING_ARGUMENT
%state WAITING_ARGUMENT
%state INLINE_MATH

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

NORMAL_PLAIN_TEXT = [^\\{}#%&$]+



INLINE_MATH_START = ("$" | "\\(")
INLINE_MATH_END = ("$" | "\\)")

%%


{COMMENT_LINE}    { return LatexTypes.COMMENT_TOKEN; }


"\\begin" {
//          yybegin(WAITING_ARGUMENT);
          return LatexTypes.BEGIN_TOKEN;
      }
"\\end"   { return LatexTypes.END_TOKEN; }




<INLINE_MATH>{
{INLINE_MATH_END} {
          yypopState();
          return LatexTypes.INLINE_MATH_END;
      }
}
{INLINE_MATH_START} {
      yypushState(INLINE_MATH);
      return LatexTypes.INLINE_MATH_START;
}

"{"   {
          yypushState(YYINITIAL);
          return LatexTypes.OPEN_BRACE;
      }

"}"    {
      yypopState();
      return LatexTypes.CLOSE_BRACE;
}

{COMMAND}    {
//          yybegin(WAITING_ARGUMENT);
          return LatexTypes.N_COMMAND_IDENTIFIER;
      }

//<WAITING_ARGUMENT>{
//"{" { return LatexTypes.OPEN_BRACE;}
//.   { yybegin(YYINITIAL);}
//}

"&" { return LatexTypes.SPECIAL_CHAR;}
"\\" { return LatexTypes.SPECIAL_CHAR;}
"#" {return LatexTypes.SPECIAL_CHAR;}
"&" {return LatexTypes.SPECIAL_CHAR;}

{WHITE_SPACE}           { return TokenType.WHITE_SPACE; }

{NORMAL_PLAIN_TEXT} { return LatexTypes.PLAIN_TEXT; }

//{SPECIAL_CHAR}          { return LatexTypes.SPECIAL_CHAR; }



[^]                     { return com.intellij.psi.TokenType.BAD_CHARACTER; }