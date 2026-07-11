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
    : field=fieldRef op=(EQ | NEQ | GT | LT | GTE | LTE | TILDE) value=literal       # CompareClause
    | field=fieldRef IN '(' literals ')'                                             # InClause
    | field=fieldRef NOT IN '(' literals ')'                                         # NotInClause
    | field=fieldRef IS EMPTY                                                          # IsEmptyClause
    | field=fieldRef IS NOT EMPTY                                                      # IsNotEmptyClause
    ;

fieldRef
    : CUSTOM_FIELD
    | IDENTIFIER
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

CUSTOM_FIELD
    : 'cf.' [a-zA-Z_][a-zA-Z0-9_]*
    ;

IDENTIFIER
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
