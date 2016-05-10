// Generated from org/apache/metron/common/query/generated/Predicate.g4 by ANTLR 4.5
package org.apache.metron.common.query.generated;

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

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PredicateParser}.
 */
public interface PredicateListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PredicateParser#single_rule}.
	 * @param ctx the parse tree
	 */
	void enterSingle_rule(PredicateParser.Single_ruleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PredicateParser#single_rule}.
	 * @param ctx the parse tree
	 */
	void exitSingle_rule(PredicateParser.Single_ruleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalEntity}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalEntity(PredicateParser.LogicalEntityContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalEntity}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalEntity(PredicateParser.LogicalEntityContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonExpression}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpression(PredicateParser.ComparisonExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonExpression}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpression(PredicateParser.ComparisonExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalExpressionInParen}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpressionInParen(PredicateParser.LogicalExpressionInParenContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalExpressionInParen}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpressionInParen(PredicateParser.LogicalExpressionInParenContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotFunc}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterNotFunc(PredicateParser.NotFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotFunc}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitNotFunc(PredicateParser.NotFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalExpressionAnd}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpressionAnd(PredicateParser.LogicalExpressionAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalExpressionAnd}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpressionAnd(PredicateParser.LogicalExpressionAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalExpressionOr}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpressionOr(PredicateParser.LogicalExpressionOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalExpressionOr}
	 * labeled alternative in {@link PredicateParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpressionOr(PredicateParser.LogicalExpressionOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonExpressionWithOperator}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpressionWithOperator(PredicateParser.ComparisonExpressionWithOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonExpressionWithOperator}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpressionWithOperator(PredicateParser.ComparisonExpressionWithOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code InExpression}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterInExpression(PredicateParser.InExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code InExpression}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitInExpression(PredicateParser.InExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NInExpression}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterNInExpression(PredicateParser.NInExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NInExpression}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitNInExpression(PredicateParser.NInExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonExpressionParens}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpressionParens(PredicateParser.ComparisonExpressionParensContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonExpressionParens}
	 * labeled alternative in {@link PredicateParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpressionParens(PredicateParser.ComparisonExpressionParensContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalConst}
	 * labeled alternative in {@link PredicateParser#logical_entity}.
	 * @param ctx the parse tree
	 */
	void enterLogicalConst(PredicateParser.LogicalConstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalConst}
	 * labeled alternative in {@link PredicateParser#logical_entity}.
	 * @param ctx the parse tree
	 */
	void exitLogicalConst(PredicateParser.LogicalConstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ExistsFunc}
	 * labeled alternative in {@link PredicateParser#logical_entity}.
	 * @param ctx the parse tree
	 */
	void enterExistsFunc(PredicateParser.ExistsFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ExistsFunc}
	 * labeled alternative in {@link PredicateParser#logical_entity}.
	 * @param ctx the parse tree
	 */
	void exitExistsFunc(PredicateParser.ExistsFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalFunc}
	 * labeled alternative in {@link PredicateParser#logical_entity}.
	 * @param ctx the parse tree
	 */
	void enterLogicalFunc(PredicateParser.LogicalFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalFunc}
	 * labeled alternative in {@link PredicateParser#logical_entity}.
	 * @param ctx the parse tree
	 */
	void exitLogicalFunc(PredicateParser.LogicalFuncContext ctx);
	/**
	 * Enter a parse tree produced by {@link PredicateParser#list_entity}.
	 * @param ctx the parse tree
	 */
	void enterList_entity(PredicateParser.List_entityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PredicateParser#list_entity}.
	 * @param ctx the parse tree
	 */
	void exitList_entity(PredicateParser.List_entityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PredicateParser#func_args}.
	 * @param ctx the parse tree
	 */
	void enterFunc_args(PredicateParser.Func_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PredicateParser#func_args}.
	 * @param ctx the parse tree
	 */
	void exitFunc_args(PredicateParser.Func_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PredicateParser#op_list}.
	 * @param ctx the parse tree
	 */
	void enterOp_list(PredicateParser.Op_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PredicateParser#op_list}.
	 * @param ctx the parse tree
	 */
	void exitOp_list(PredicateParser.Op_listContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link PredicateParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(PredicateParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link PredicateParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(PredicateParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalVariable}
	 * labeled alternative in {@link PredicateParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterLogicalVariable(PredicateParser.LogicalVariableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalVariable}
	 * labeled alternative in {@link PredicateParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitLogicalVariable(PredicateParser.LogicalVariableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringFunc}
	 * labeled alternative in {@link PredicateParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterStringFunc(PredicateParser.StringFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringFunc}
	 * labeled alternative in {@link PredicateParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitStringFunc(PredicateParser.StringFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IdentifierOperand}
	 * labeled alternative in {@link PredicateParser#comparison_operand}.
	 * @param ctx the parse tree
	 */
	void enterIdentifierOperand(PredicateParser.IdentifierOperandContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IdentifierOperand}
	 * labeled alternative in {@link PredicateParser#comparison_operand}.
	 * @param ctx the parse tree
	 */
	void exitIdentifierOperand(PredicateParser.IdentifierOperandContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalConstComparison}
	 * labeled alternative in {@link PredicateParser#comparison_operand}.
	 * @param ctx the parse tree
	 */
	void enterLogicalConstComparison(PredicateParser.LogicalConstComparisonContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalConstComparison}
	 * labeled alternative in {@link PredicateParser#comparison_operand}.
	 * @param ctx the parse tree
	 */
	void exitLogicalConstComparison(PredicateParser.LogicalConstComparisonContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonOp}
	 * labeled alternative in {@link PredicateParser#comp_operator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOp(PredicateParser.ComparisonOpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonOp}
	 * labeled alternative in {@link PredicateParser#comp_operator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOp(PredicateParser.ComparisonOpContext ctx);
}