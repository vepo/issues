grammar TicketQuery;

options {
    package = 'dev.vepo.issues.ticket.search.query.antlr';
}

query
    : expression (ORDER BY sortField=IDENTIFIER sortDir=(ASC | DESC)?)? EOF
    ;

expression
    : expression AND expression   # AndExpr
    | expression OR expression    # OrExpr
    | '(' expression ')'          # ParenExpr
    | clause                      # ClauseExpr
    ;

clause
    : field=IDENTIFIER op=(EQ | NEQ | GT | LT | GTE | LTE | TILDE) value=literal       # CompareClause
    | field=IDENTIFIER IN '(' literals ')'                                             # InClause
    | field=IDENTIFIER NOT IN '(' literals ')'                                         # NotInClause
    | field=IDENTIFIER IS EMPTY                                                          # IsEmptyClause
    | field=IDENTIFIER IS NOT EMPTY                                                      # IsNotEmptyClause
    ;

literal
    : STRING
    | NUMBER
    | CURRENTUSER '(' ')'
    | ME '(' ')'
    ;

literals
    : literal (',' literal)*
    ;

EQ   : '=' ;
NEQ  : '!=' ;
GT   : '>' ;
LT   : '<' ;
GTE  : '>=' ;
LTE  : '<=' ;
TILDE: '~' ;
AND  : 'AND' ;
OR   : 'OR' ;
IN   : 'IN' ;
NOT  : 'NOT' ;
IS   : 'IS' ;
EMPTY: 'EMPTY' ;
ORDER: 'ORDER' ;
BY   : 'BY' ;
ASC  : 'ASC' ;
DESC : 'DESC' ;
CURRENTUSER : 'currentUser' ;
ME   : 'me' ;

STRING
    : '"' (~["\\] | '\\' .)* '"'
    ;

NUMBER
    : [0-9]+ ('.' [0-9]+)?
    ;

IDENTIFIER
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
