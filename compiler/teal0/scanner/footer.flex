// these patterns match only if nothing else matches
<<EOF>>       { return sym(Terminals.EOF); }

/* error fallback */
[^]           { throw new SyntaxError(
	      	      yyline + 1, yycolumn, yyline + 1, yycolumn + 1,
	      	      "Illegal character <"+yytext()+">"); }
