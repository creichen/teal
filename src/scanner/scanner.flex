package lang.ast; // The generated scanner will belong to the package lang.ast

import lang.ast.LangParser.Terminals; // The terminals are implicitly defined in the parser
import lang.ast.LangParser.SyntaxError;

%%

// define the signature for the generated scanner
%public
%final
%class LangScanner
%extends beaver.Scanner

// the interface between the scanner and the parser is the nextToken() method
%type beaver.Symbol
%function nextToken

// store line and column information in the tokens
%line
%column

// this code will be inlined in the body of the generated scanner class
%{
  private beaver.Symbol sym(short id) {
    return new beaver.Symbol(id, yyline + 1, yycolumn + 1, yylength(), yytext());
  }
%}

// macros
LineTerminator = \r|\n|\r\n
WhiteSpace = [ ] | \t | \f | \n | \r
ID = [a-zA-Z][a-zA-Z0-9_]*
IntLiteral = [0-9] | [1-9][0-9]*
SingleLineComment = "//" [^\r\n]* {LineTerminator}?
MultiLineComment = "/*" ~"*/"


%%

// discard whitespace information
{WhiteSpace}  { }
// discard comments
{SingleLineComment} { }
{MultiLineComment} { }

// token definitions
","           { return sym(Terminals.COMMA); }
";"           { return sym(Terminals.SEMICOLON); }
"("           { return sym(Terminals.LPAREN); }
")"           { return sym(Terminals.RPAREN); }
"{"           { return sym(Terminals.LBRACE); }
"}"           { return sym(Terminals.RBRACE); }
"while"       { return sym(Terminals.WHILE); }
"if"          { return sym(Terminals.IF); }
"else"        { return sym(Terminals.ELSE); }
"break"       { return sym(Terminals.BREAK); }
"continue"    { return sym(Terminals.CONTINUE); }
"return"      { return sym(Terminals.RETURN); }
"+"           { return sym(Terminals.PLUS); }
"-"           { return sym(Terminals.MINUS); }
"*"           { return sym(Terminals.STAR); }
"/"           { return sym(Terminals.SLASH); }
"%"           { return sym(Terminals.PERCENT); }
"=="          { return sym(Terminals.EQ); }
"="           { return sym(Terminals.ASSIGN); }
"<"           { return sym(Terminals.LT); }
">"           { return sym(Terminals.GT); }
">="          { return sym(Terminals.GTE); }
"<="          { return sym(Terminals.LTE); }
{IntLiteral}  { return sym(Terminals.INT_LITERAL); }
{ID}          { return sym(Terminals.ID); }
<<EOF>>       { return sym(Terminals.EOF); }



/* error fallback */
[^]           { throw new SyntaxError("Illegal character <"+yytext()+">"); }
