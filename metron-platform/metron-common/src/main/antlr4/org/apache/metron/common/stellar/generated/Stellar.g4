/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

grammar Stellar;

@header {
//CHECKSTYLE:OFF
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
}

/* Lexical rules */
IN : 'in' | 'IN';
LAMBDA_OP : '->';
DOUBLE_QUOTE : '"';
SINGLE_QUOTE : '\'';
COMMA : ',';
PERIOD : '.';

AND : 'and' | '&&' | 'AND';
OR : 'or' | '||' | 'OR';
NOT : 'not' | 'NOT';
TRUE : 'true' | 'TRUE';
FALSE : 'false' | 'FALSE';

EQ : '==' ;
NEQ : '!=' ;
LT : '<';
LTE : '<=';
GT : '>';
GTE : '>=';
QUESTION : '?' ;
COLON : ':' ;
IF : 'IF' | 'if';
THEN : 'THEN' | 'then';
ELSE : 'ELSE' | 'else';
NULL : 'null' | 'NULL';

MINUS : '-';
PLUS : '+';
DIV : '/';
MUL : '*';
LBRACE : '{';
RBRACE : '}';
LBRACKET : '[';
RBRACKET : ']';
LPAREN : '(' ;
RPAREN : ')' ;
NIN : 'not in' | 'NOT IN';
EXISTS : 'exists' | 'EXISTS';
EXPONENT : E ( PLUS|MINUS )? DIGIT+;
INT_LITERAL :
  MINUS? ZERO
  | MINUS? FIRST_DIGIT DIGIT*
  ;
DOUBLE_LITERAL :
  INT_LITERAL PERIOD DIGIT* EXPONENT? D?
  | PERIOD DIGIT+ EXPONENT? D?
  | INT_LITERAL EXPONENT D?
  | INT_LITERAL EXPONENT? D
  ;
FLOAT_LITERAL :
  INT_LITERAL PERIOD DIGIT* EXPONENT? F
  | MINUS? PERIOD DIGIT+ EXPONENT? F
  | INT_LITERAL EXPONENT? F
  ;
LONG_LITERAL : INT_LITERAL L;
IDENTIFIER : IDENTIFIER_START
           | IDENTIFIER_START IDENTIFIER_MIDDLE* IDENTIFIER_END
           ;

STRING_LITERAL :
  DOUBLE_QUOTE SCHAR* DOUBLE_QUOTE
  | SINGLE_QUOTE SCHAR* SINGLE_QUOTE
  ;

// COMMENT and WS are stripped from the output token stream by sending
// to a different channel 'skip'

COMMENT : '//' .+? (EOL|EOF) -> skip;

WS : [ \r\t\u000C\n]+ -> skip;

fragment ZERO: '0';
fragment FIRST_DIGIT: '1'..'9';
fragment DIGIT: '0'..'9';
fragment SCHAR:  ~['"\\\r\n];
fragment D: ('d'|'D');
fragment E: ('e'|'E');
fragment F: ('f'|'F');
fragment L: ('l'|'L');
fragment EOL : '\n';
fragment IDENTIFIER_START : [a-zA-Z_$];
fragment IDENTIFIER_MIDDLE: [a-zA-Z_\.:0-9];
//identifiers can't end with a colon, it screws up maps and lambda variables.
//the following (x,y:x == 'foo') doesn't parse because y:x is considered a variable
fragment IDENTIFIER_END: [a-zA-Z_\.0-9];

/* Parser rules */

transformation : transformation_expr EOF;

transformation_expr:
   conditional_expr #ConditionalExpr
  | LPAREN transformation_expr RPAREN #TransformationExpr
  | arithmetic_expr # ArithExpression
  | transformation_entity #TransformationEntity
  | comparison_expr # ComparisonExpression
  | logical_expr #LogicalExpression
  | in_expr #InExpression
  ;

conditional_expr :
  logical_expr QUESTION transformation_expr COLON transformation_expr #TernaryFuncWithoutIf
  | IF logical_expr THEN transformation_expr ELSE transformation_expr #TernaryFuncWithIf
  ;

logical_expr:
  b_expr AND logical_expr #LogicalExpressionAnd
  | b_expr OR logical_expr #LogicalExpressionOr
  | b_expr #BoleanExpression
  ;

b_expr:
  comparison_expr
  | in_expr
  ;

in_expr:
  identifier_operand IN b_expr #InExpressionStatement
  | identifier_operand NIN b_expr #NInExpressionStatement
  ;

comparison_expr :
  comparison_expr comp_operator comparison_expr #ComparisonExpressionWithOperator
  | NOT LPAREN logical_expr RPAREN #NotFunc
  | LPAREN logical_expr RPAREN #ComparisonExpressionParens
  | identifier_operand #operand
  ;

transformation_entity : identifier_operand;

comp_operator : (EQ | NEQ | LT | LTE | GT | GTE) # ComparisonOp;

func_args :
  LPAREN op_list RPAREN
  | LPAREN RPAREN
  ;

op_list :
  identifier_operand
  | op_list COMMA identifier_operand
  | conditional_expr
  | op_list COMMA conditional_expr
  ;

list_entity :
  LBRACKET RBRACKET
  | LBRACKET op_list RBRACKET
  ;

kv_list :
  identifier_operand COLON transformation_expr
  | kv_list COMMA identifier_operand COLON transformation_expr
  ;

map_entity :
  LBRACE kv_list RBRACE
  | LBRACE RBRACE
  ;

arithmetic_expr:
  arithmetic_expr_mul #ArithExpr_solo
  | arithmetic_expr PLUS arithmetic_expr_mul #ArithExpr_plus
  | arithmetic_expr MINUS arithmetic_expr_mul #ArithExpr_minus
  ;

arithmetic_expr_mul :
  arithmetic_operands #ArithExpr_mul_solo
  | arithmetic_expr_mul MUL arithmetic_expr_mul #ArithExpr_mul
  | arithmetic_expr_mul DIV arithmetic_expr_mul #ArithExpr_div
  ;

functions : IDENTIFIER func_args #TransformationFunc;

arithmetic_operands :
  functions #NumericFunctions
  | DOUBLE_LITERAL #DoubleLiteral
  | INT_LITERAL #IntLiteral
  | LONG_LITERAL #LongLiteral
  | FLOAT_LITERAL #FloatLiteral
  | IDENTIFIER #Variable
  | LPAREN arithmetic_expr RPAREN #ParenArith
  | LPAREN conditional_expr RPAREN #condExpr
  ;


identifier_operand :
  (TRUE | FALSE) #LogicalConst
  | lambda_with_args  #LambdaWithArgsExpr
  | lambda_without_args  #LambdaWithoutArgsExpr
  | arithmetic_expr #ArithmeticOperands
  | STRING_LITERAL # StringLiteral
  | list_entity #List
  | map_entity #MapConst
  | NULL #NullConst
  | EXISTS LPAREN IDENTIFIER RPAREN #ExistsFunc
  | LPAREN conditional_expr RPAREN #condExpr_paren
  ;


lambda_without_args:
  LPAREN RPAREN LAMBDA_OP transformation_expr
  ;

lambda_with_args :
  LPAREN lambda_variables RPAREN LAMBDA_OP transformation_expr
  | single_lambda_variable LAMBDA_OP transformation_expr
  ;

lambda_variables :
  lambda_variable (COMMA lambda_variable)*
  ;

single_lambda_variable :
  lambda_variable;

lambda_variable:
  IDENTIFIER
  ;


