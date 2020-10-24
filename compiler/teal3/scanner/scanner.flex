package lang.ast; // The generated scanner will belong to the package lang.ast

import lang.ast.TEALParser.Terminals; // The terminals are implicitly defined in the parser
import lang.ast.TEALParser.SyntaxError;

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
ID = [a-zA-Z_][a-zA-Z0-9_]*
IntLiteral = "-"?(0|[1-9][0-9]*)
HexLiteral = "-"?"0x"[0-9]+
SingleLineComment = "//" [^\r\n]* {LineTerminator}?
MultiLineComment = "/*" ~"*/"
StringLiteral = \"([^\"]|\\\")*\"


%%

// discard whitespace information
{WhiteSpace}  { }
// discard comments
{SingleLineComment} { }
{MultiLineComment} { }

// token definitions
"fun"         { return sym(Terminals.FUN); }
"var"         { return sym(Terminals.VAR); }
"qualifier"   { return sym(Terminals.QUALIFIER); }
"type"        { return sym(Terminals.TYPE); }
"class"       { return sym(Terminals.CLASS); }
"while"       { return sym(Terminals.WHILE); }
"if"          { return sym(Terminals.IF); }
"else"        { return sym(Terminals.ELSE); }
"for"         { return sym(Terminals.FOR); }
"return"      { return sym(Terminals.RETURN); }
"assert"      { return sym(Terminals.ASSERT); }
"import"      { return sym(Terminals.IMPORT); }
"self"        { return sym(Terminals.SELF); }
"not"         { return sym(Terminals.NOT); }
"in"          { return sym(Terminals.IN); }
"and"         { return sym(Terminals.AND); }
"or"          { return sym(Terminals.OR); }
"null"        { return sym(Terminals.NULL); }
"new"         { return sym(Terminals.NEW); }
"::"          { return sym(Terminals.DOUBLE_COLON); }
"+"           { return sym(Terminals.PLUS); }
"-"           { return sym(Terminals.MINUS); }
"*"           { return sym(Terminals.STAR); }
"/"           { return sym(Terminals.SLASH); }
"%"           { return sym(Terminals.PERCENT); }
"=="          { return sym(Terminals.EQEQ); }
":="          { return sym(Terminals.ASSIGN); }
"!="          { return sym(Terminals.NEQ); }
">="          { return sym(Terminals.GTE); }
"<="          { return sym(Terminals.LTE); }
"<:"          { return sym(Terminals.SUBTYPE); }
"="           { return sym(Terminals.EQ); }
"<"           { return sym(Terminals.LT); }
">"           { return sym(Terminals.GT); }
","           { return sym(Terminals.COMMA); }
";"           { return sym(Terminals.SEMICOLON); }
"("           { return sym(Terminals.LPAREN); }
")"           { return sym(Terminals.RPAREN); }
"{"           { return sym(Terminals.LBRACE); }
"}"           { return sym(Terminals.RBRACE); }
"["           { return sym(Terminals.LBRACK); }
"]"           { return sym(Terminals.RBRACK); }
":"           { return sym(Terminals.COLON); }
"."           { return sym(Terminals.DOT); }

{IntLiteral}
{
   String text = yytext();
   Long data = Long.parseLong(text);
   return new beaver.Symbol(Terminals.INTEGER_LITERAL, yyline, yycolumn, yylength(), data);
}
{HexLiteral}
{
   String text = yytext();
   Long data = Long.parseLong(text.substring(2), 16);
   return new beaver.Symbol(Terminals.INTEGER_LITERAL, yyline, yycolumn, yylength(), data);
}
{StringLiteral}
{
   String text = yytext();
   String data = text.substring(1, text.length() - 1);
   return new beaver.Symbol(Terminals.STRING_LITERAL, yyline + 1, yycolumn + 1, yylength() - 2, data);
}
{ID}          { return sym(Terminals.ID); }



<<EOF>>       { return sym(Terminals.EOF); }



/* error fallback */
[^]           { throw new SyntaxError("Illegal character <"+yytext()+">"); }
