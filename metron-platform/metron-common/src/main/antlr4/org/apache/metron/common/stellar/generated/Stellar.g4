/**
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
/**
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

COMMA : ',';
AND : 'and'
    | '&&'
    | 'AND'
    ;
OR  : 'or'
    | '||'
    | 'OR';

NOT : 'not'
    | 'NOT';

TRUE  : 'true'
      | 'TRUE' ;

FALSE : 'false'
      | 'FALSE';

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
IN : 'in'
   ;
NIN : 'not in'
   ;
EXISTS : 'exists' | 'EXISTS';
INT_LITERAL     : MINUS? '0'..'9'+ ;
DOUBLE_LITERAL  : MINUS? '0'..'9'+'.''0'..'9'+ ;
IDENTIFIER : [a-zA-Z_][a-zA-Z_\.0-9]* ;
fragment SCHAR:  ~['"\\\r\n];
STRING_LITERAL : '"' SCHAR* '"'
               | '\'' SCHAR* '\'' ;


// COMMENT and WS are stripped from the output token stream by sending
// to a different channel 'skip'

COMMENT : '//' .+? ('\n'|EOF) -> skip ;

WS : [ \r\t\u000C\n]+ -> skip ;


/* Parser rules */

transformation : transformation_expr EOF;

transformation_expr:
   conditional_expr #ConditionalExpr
  |  LPAREN transformation_expr RPAREN #TransformationExpr

  | arithmetic_expr               # ArithExpression
  | transformation_entity #TransformationEntity
  | comparison_expr               # ComparisonExpression
  ;
conditional_expr :  comparison_expr QUESTION transformation_expr COLON transformation_expr #TernaryFuncWithoutIf
                 | IF comparison_expr THEN transformation_expr ELSE transformation_expr #TernaryFuncWithIf
                 ;

comparison_expr : comparison_operand comp_operator comparison_operand # ComparisonExpressionWithOperator
                | identifier_operand IN identifier_operand #InExpression
                | identifier_operand NIN identifier_operand #NInExpression
                | comparison_expr AND comparison_expr #LogicalExpressionAnd
                | comparison_expr OR comparison_expr #LogicalExpressionOr
                | NOT LPAREN comparison_expr RPAREN #NotFunc
                | LPAREN comparison_expr RPAREN # ComparisonExpressionParens
                | identifier_operand #operand
                ;
comparison_operand : identifier_operand #IdentifierOperand
                   ;
transformation_entity : identifier_operand
  ;
comp_operator : (EQ | NEQ | LT | LTE | GT | GTE) # ComparisonOp
              ;
arith_operator_addition : (PLUS | MINUS) # ArithOp_plus
               ;
arith_operator_mul : (MUL | DIV) # ArithOp_mul
               ;
func_args : LPAREN op_list RPAREN
          | LPAREN RPAREN
          ;
op_list : identifier_operand
        | op_list COMMA identifier_operand
        ;
list_entity : LBRACKET op_list RBRACKET
            | LBRACKET RBRACKET;

kv_list : identifier_operand COLON transformation_expr
        | kv_list COMMA identifier_operand ':' transformation_expr
        ;

map_entity : LBRACE kv_list RBRACE
           | LBRACE RBRACE;

arithmetic_expr: arithmetic_expr_mul #ArithExpr_solo
               | arithmetic_expr PLUS arithmetic_expr_mul #ArithExpr_plus
               | arithmetic_expr MINUS arithmetic_expr_mul #ArithExpr_minus
                ;
arithmetic_expr_mul : arithmetic_operands #ArithExpr_mul_solo
                    | arithmetic_expr_mul MUL arithmetic_expr_mul #ArithExpr_mul
                    | arithmetic_expr_mul DIV arithmetic_expr_mul #ArithExpr_div
                    ;

functions : IDENTIFIER func_args #TransformationFunc
          ;
arithmetic_operands : functions #NumericFunctions
                    | DOUBLE_LITERAL #DoubleLiteral
                    | INT_LITERAL #IntLiteral
                    | IDENTIFIER #Variable
                    | LPAREN arithmetic_expr RPAREN #ParenArith
                    ;
identifier_operand : (TRUE | FALSE) # LogicalConst
                   | arithmetic_expr #ArithmeticOperands
                   | STRING_LITERAL # StringLiteral
                   | list_entity #List
                   | map_entity #MapConst
                   | NULL #NullConst
                   | EXISTS LPAREN IDENTIFIER RPAREN #ExistsFunc
                   ;
