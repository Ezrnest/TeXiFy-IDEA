package nl.hannahsten.texifyidea.grammar;

import com.intellij.lexer.FlexLexer;
import java.util.*;

import com.intellij.psi.tree.IElementType;
import nl.hannahsten.texifyidea.util.magic.EnvironmentMagic;

import static nl.hannahsten.texifyidea.psi.LatexTypes.*;

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

%state COMMENT

NEWLINE = [^\r\n]
BACK_SLASH = "\\"
WHITE_SPACE= [ \t\n\x0B\f\r]+
SPECIAL_CHAR = [#$%&~_\^{}\\]

%%


"%"    { yybegin(COMMENT); return LatexTypes.COMMENT_START; }
<COMMENT>{
      {NEWLINE}        { yybegin(YYINITIAL); return LatexTypes.NEWLINE; }
      .*               { return LatexTypes.COMMENT; }
}

{WHITE_SPACE}           { return com.intellij.psi.TokenType.WHITE_SPACE; }
[^]                     { return com.intellij.psi.TokenType.BAD_CHARACTER; }

\{    { return LatexTypes.LBRACE; }
\}    { return LatexTypes.RBRACE; }
