// Header for the Teal scanner specification
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
Identifier = [a-zA-Z_][a-zA-Z0-9_]*
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
