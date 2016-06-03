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

grammar Predicate;

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
COMMA : ',';

LBRACKET : '[';
RBRACKET : ']';
LPAREN : '(' ;
RPAREN : ')' ;

IN : 'in'
   ;
NIN : 'not in'
   ;
EXISTS : 'exists';
INT_LITERAL     : '0'..'9'+ ;
DOUBLE_LITERAL  : '0'..'9'+'.''0'..'9'+ ;

IDENTIFIER : [a-zA-Z_][a-zA-Z_\.0-9]* ;
fragment SCHAR:  ~['"\\\r\n];
STRING_LITERAL : '"' SCHAR* '"'
               | '\'' SCHAR* '\'' ;
SEMI : ';' ;


// COMMENT and WS are stripped from the output token stream by sending
// to a different channel 'skip'

COMMENT : '//' .+? ('\n'|EOF) -> skip ;

WS : [ \r\t\u000C\n]+ -> skip ;


/* Parser rules */

single_rule : logical_expr EOF;

logical_expr
 : logical_expr AND logical_expr # LogicalExpressionAnd
 | logical_expr OR logical_expr  # LogicalExpressionOr
 | comparison_expr               # ComparisonExpression
 | LPAREN logical_expr RPAREN    # LogicalExpressionInParen
 | NOT LPAREN logical_expr RPAREN #NotFunc
 | logical_entity                # LogicalEntity
 ;

comparison_expr : comparison_operand comp_operator comparison_operand # ComparisonExpressionWithOperator
                | identifier_operand IN identifier_operand #InExpression
                | identifier_operand NIN identifier_operand #NInExpression
                | LPAREN comparison_expr RPAREN # ComparisonExpressionParens
                ;

logical_entity : (TRUE | FALSE) # LogicalConst
               | EXISTS LPAREN IDENTIFIER RPAREN #ExistsFunc
               | IDENTIFIER LPAREN func_args RPAREN #LogicalFunc
               ;

list_entity : LBRACKET op_list RBRACKET
            ;
func_args : op_list
          ;
op_list : identifier_operand
        | op_list COMMA identifier_operand
        ;

t_func : IDENTIFIER LPAREN func_args RPAREN #TransformationFunc
       ;

identifier_operand : STRING_LITERAL # StringLiteral
                   | IDENTIFIER     # LogicalVariable
                   | t_func #id_tfunc
                   | INT_LITERAL #IntegerLiteral
                   | DOUBLE_LITERAL #DoubleLiteral
                   | list_entity #List
                   ;

comparison_operand : identifier_operand #IdentifierOperand
                   | logical_entity # LogicalConstComparison
                   ;

comp_operator : (EQ | NEQ | LT | LTE | GT | GTE) # ComparisonOp
              ;
