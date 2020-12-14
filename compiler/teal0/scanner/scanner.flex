// token definitions
"fun"         { return sym(Terminals.FUN); }
"var"         { return sym(Terminals.VAR); }
"while"       { return sym(Terminals.WHILE); }
"if"          { return sym(Terminals.IF); }
"else"        { return sym(Terminals.ELSE); }
"return"      { return sym(Terminals.RETURN); }
"import"      { return sym(Terminals.IMPORT); }
"not"         { return sym(Terminals.NOT); }
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

// literals
{IntLiteral}
{
   String text = yytext();
   Long data = Long.parseLong(text);
   return new beaver.Symbol(Terminals.INTEGER_LITERAL, yyline + 1, yycolumn, yylength(), data);
}
{HexLiteral}
{
   String text = yytext();
   Long data = Long.parseLong(text.substring(2), 16);
   return new beaver.Symbol(Terminals.INTEGER_LITERAL, yyline + 1, yycolumn, yylength(), data);
}
{StringLiteral}
{
   String text = yytext();
   String data = text.substring(1, text.length() - 1);
   return new beaver.Symbol(Terminals.STRING_LITERAL, yyline + 1, yycolumn, yylength() - 2, data);
}
