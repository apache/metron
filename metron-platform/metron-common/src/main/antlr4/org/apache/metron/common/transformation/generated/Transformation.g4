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

grammar Transformation;

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

LBRACKET : '[';
RBRACKET : ']';
LPAREN : '(' ;
RPAREN : ')' ;

INT_LITERAL     : '0'..'9'+ ;
DOUBLE_LITERAL  : '0'..'9'+'.''0'..'9'+ ;
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

transformation_expr
  : LPAREN transformation_expr RPAREN #TransformationExpr
  | transformation_entity #TransformationEntity
  ;

transformation_entity
  : identifier_operand 
  ;

func_args : op_list
          ;
op_list : identifier_operand
        | op_list COMMA identifier_operand
        ;
list_entity : LBRACKET op_list RBRACKET;

identifier_operand : STRING_LITERAL # StringLiteral
                   | INT_LITERAL #IntegerLiteral
                   | DOUBLE_LITERAL #DoubleLiteral
                   | IDENTIFIER     # Variable
                   | IDENTIFIER LPAREN func_args RPAREN #TransformationFunc
                   | list_entity #List
                   ;
