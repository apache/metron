// Generated from org/apache/metron/stellar/common/generated/Stellar.g4 by ANTLR 4.5
package org.apache.metron.stellar.common.generated;

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

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link StellarParser}.
 */
public interface StellarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link StellarParser#transformation}.
	 * @param ctx the parse tree
	 */
	void enterTransformation(StellarParser.TransformationContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#transformation}.
	 * @param ctx the parse tree
	 */
	void exitTransformation(StellarParser.TransformationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ConditionalExpr}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterConditionalExpr(StellarParser.ConditionalExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ConditionalExpr}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitConditionalExpr(StellarParser.ConditionalExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TransformationExpr}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterTransformationExpr(StellarParser.TransformationExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TransformationExpr}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitTransformationExpr(StellarParser.TransformationExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterArithExpression(StellarParser.ArithExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitArithExpression(StellarParser.ArithExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TransformationEntity}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterTransformationEntity(StellarParser.TransformationEntityContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TransformationEntity}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitTransformationEntity(StellarParser.TransformationEntityContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpression(StellarParser.ComparisonExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpression(StellarParser.ComparisonExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpression(StellarParser.LogicalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpression(StellarParser.LogicalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code InExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterInExpression(StellarParser.InExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code InExpression}
	 * labeled alternative in {@link StellarParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitInExpression(StellarParser.InExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#if_expr}.
	 * @param ctx the parse tree
	 */
	void enterIf_expr(StellarParser.If_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#if_expr}.
	 * @param ctx the parse tree
	 */
	void exitIf_expr(StellarParser.If_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#then_expr}.
	 * @param ctx the parse tree
	 */
	void enterThen_expr(StellarParser.Then_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#then_expr}.
	 * @param ctx the parse tree
	 */
	void exitThen_expr(StellarParser.Then_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#else_expr}.
	 * @param ctx the parse tree
	 */
	void enterElse_expr(StellarParser.Else_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#else_expr}.
	 * @param ctx the parse tree
	 */
	void exitElse_expr(StellarParser.Else_exprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TernaryFuncWithoutIf}
	 * labeled alternative in {@link StellarParser#conditional_expr}.
	 * @param ctx the parse tree
	 */
	void enterTernaryFuncWithoutIf(StellarParser.TernaryFuncWithoutIfContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TernaryFuncWithoutIf}
	 * labeled alternative in {@link StellarParser#conditional_expr}.
	 * @param ctx the parse tree
	 */
	void exitTernaryFuncWithoutIf(StellarParser.TernaryFuncWithoutIfContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TernaryFuncWithIf}
	 * labeled alternative in {@link StellarParser#conditional_expr}.
	 * @param ctx the parse tree
	 */
	void enterTernaryFuncWithIf(StellarParser.TernaryFuncWithIfContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TernaryFuncWithIf}
	 * labeled alternative in {@link StellarParser#conditional_expr}.
	 * @param ctx the parse tree
	 */
	void exitTernaryFuncWithIf(StellarParser.TernaryFuncWithIfContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalExpressionAnd}
	 * labeled alternative in {@link StellarParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpressionAnd(StellarParser.LogicalExpressionAndContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalExpressionAnd}
	 * labeled alternative in {@link StellarParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpressionAnd(StellarParser.LogicalExpressionAndContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalExpressionOr}
	 * labeled alternative in {@link StellarParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpressionOr(StellarParser.LogicalExpressionOrContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalExpressionOr}
	 * labeled alternative in {@link StellarParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpressionOr(StellarParser.LogicalExpressionOrContext ctx);
	/**
	 * Enter a parse tree produced by the {@code BoleanExpression}
	 * labeled alternative in {@link StellarParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void enterBoleanExpression(StellarParser.BoleanExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code BoleanExpression}
	 * labeled alternative in {@link StellarParser#logical_expr}.
	 * @param ctx the parse tree
	 */
	void exitBoleanExpression(StellarParser.BoleanExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#b_expr}.
	 * @param ctx the parse tree
	 */
	void enterB_expr(StellarParser.B_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#b_expr}.
	 * @param ctx the parse tree
	 */
	void exitB_expr(StellarParser.B_exprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code InExpressionStatement}
	 * labeled alternative in {@link StellarParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void enterInExpressionStatement(StellarParser.InExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code InExpressionStatement}
	 * labeled alternative in {@link StellarParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void exitInExpressionStatement(StellarParser.InExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NInExpressionStatement}
	 * labeled alternative in {@link StellarParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void enterNInExpressionStatement(StellarParser.NInExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NInExpressionStatement}
	 * labeled alternative in {@link StellarParser#in_expr}.
	 * @param ctx the parse tree
	 */
	void exitNInExpressionStatement(StellarParser.NInExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NotFunc}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterNotFunc(StellarParser.NotFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NotFunc}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitNotFunc(StellarParser.NotFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonExpressionParens}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpressionParens(StellarParser.ComparisonExpressionParensContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonExpressionParens}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpressionParens(StellarParser.ComparisonExpressionParensContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonExpressionWithOperator}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterComparisonExpressionWithOperator(StellarParser.ComparisonExpressionWithOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonExpressionWithOperator}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitComparisonExpressionWithOperator(StellarParser.ComparisonExpressionWithOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code operand}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void enterOperand(StellarParser.OperandContext ctx);
	/**
	 * Exit a parse tree produced by the {@code operand}
	 * labeled alternative in {@link StellarParser#comparison_expr}.
	 * @param ctx the parse tree
	 */
	void exitOperand(StellarParser.OperandContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#transformation_entity}.
	 * @param ctx the parse tree
	 */
	void enterTransformation_entity(StellarParser.Transformation_entityContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#transformation_entity}.
	 * @param ctx the parse tree
	 */
	void exitTransformation_entity(StellarParser.Transformation_entityContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ComparisonOp}
	 * labeled alternative in {@link StellarParser#comp_operator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOp(StellarParser.ComparisonOpContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ComparisonOp}
	 * labeled alternative in {@link StellarParser#comp_operator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOp(StellarParser.ComparisonOpContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#func_args}.
	 * @param ctx the parse tree
	 */
	void enterFunc_args(StellarParser.Func_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#func_args}.
	 * @param ctx the parse tree
	 */
	void exitFunc_args(StellarParser.Func_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#op_list}.
	 * @param ctx the parse tree
	 */
	void enterOp_list(StellarParser.Op_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#op_list}.
	 * @param ctx the parse tree
	 */
	void exitOp_list(StellarParser.Op_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#list_entity}.
	 * @param ctx the parse tree
	 */
	void enterList_entity(StellarParser.List_entityContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#list_entity}.
	 * @param ctx the parse tree
	 */
	void exitList_entity(StellarParser.List_entityContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#kv_list}.
	 * @param ctx the parse tree
	 */
	void enterKv_list(StellarParser.Kv_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#kv_list}.
	 * @param ctx the parse tree
	 */
	void exitKv_list(StellarParser.Kv_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#map_entity}.
	 * @param ctx the parse tree
	 */
	void enterMap_entity(StellarParser.Map_entityContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#map_entity}.
	 * @param ctx the parse tree
	 */
	void exitMap_entity(StellarParser.Map_entityContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithExpr_solo}
	 * labeled alternative in {@link StellarParser#arithmetic_expr}.
	 * @param ctx the parse tree
	 */
	void enterArithExpr_solo(StellarParser.ArithExpr_soloContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithExpr_solo}
	 * labeled alternative in {@link StellarParser#arithmetic_expr}.
	 * @param ctx the parse tree
	 */
	void exitArithExpr_solo(StellarParser.ArithExpr_soloContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithExpr_minus}
	 * labeled alternative in {@link StellarParser#arithmetic_expr}.
	 * @param ctx the parse tree
	 */
	void enterArithExpr_minus(StellarParser.ArithExpr_minusContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithExpr_minus}
	 * labeled alternative in {@link StellarParser#arithmetic_expr}.
	 * @param ctx the parse tree
	 */
	void exitArithExpr_minus(StellarParser.ArithExpr_minusContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithExpr_plus}
	 * labeled alternative in {@link StellarParser#arithmetic_expr}.
	 * @param ctx the parse tree
	 */
	void enterArithExpr_plus(StellarParser.ArithExpr_plusContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithExpr_plus}
	 * labeled alternative in {@link StellarParser#arithmetic_expr}.
	 * @param ctx the parse tree
	 */
	void exitArithExpr_plus(StellarParser.ArithExpr_plusContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithExpr_div}
	 * labeled alternative in {@link StellarParser#arithmetic_expr_mul}.
	 * @param ctx the parse tree
	 */
	void enterArithExpr_div(StellarParser.ArithExpr_divContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithExpr_div}
	 * labeled alternative in {@link StellarParser#arithmetic_expr_mul}.
	 * @param ctx the parse tree
	 */
	void exitArithExpr_div(StellarParser.ArithExpr_divContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithExpr_mul_solo}
	 * labeled alternative in {@link StellarParser#arithmetic_expr_mul}.
	 * @param ctx the parse tree
	 */
	void enterArithExpr_mul_solo(StellarParser.ArithExpr_mul_soloContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithExpr_mul_solo}
	 * labeled alternative in {@link StellarParser#arithmetic_expr_mul}.
	 * @param ctx the parse tree
	 */
	void exitArithExpr_mul_solo(StellarParser.ArithExpr_mul_soloContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithExpr_mul}
	 * labeled alternative in {@link StellarParser#arithmetic_expr_mul}.
	 * @param ctx the parse tree
	 */
	void enterArithExpr_mul(StellarParser.ArithExpr_mulContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithExpr_mul}
	 * labeled alternative in {@link StellarParser#arithmetic_expr_mul}.
	 * @param ctx the parse tree
	 */
	void exitArithExpr_mul(StellarParser.ArithExpr_mulContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TransformationFunc}
	 * labeled alternative in {@link StellarParser#functions}.
	 * @param ctx the parse tree
	 */
	void enterTransformationFunc(StellarParser.TransformationFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TransformationFunc}
	 * labeled alternative in {@link StellarParser#functions}.
	 * @param ctx the parse tree
	 */
	void exitTransformationFunc(StellarParser.TransformationFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NumericFunctions}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterNumericFunctions(StellarParser.NumericFunctionsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NumericFunctions}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitNumericFunctions(StellarParser.NumericFunctionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DoubleLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(StellarParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DoubleLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(StellarParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IntLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterIntLiteral(StellarParser.IntLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IntLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitIntLiteral(StellarParser.IntLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LongLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterLongLiteral(StellarParser.LongLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LongLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitLongLiteral(StellarParser.LongLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FloatLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterFloatLiteral(StellarParser.FloatLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FloatLiteral}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitFloatLiteral(StellarParser.FloatLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Variable}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterVariable(StellarParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Variable}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitVariable(StellarParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ParenArith}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterParenArith(StellarParser.ParenArithContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ParenArith}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitParenArith(StellarParser.ParenArithContext ctx);
	/**
	 * Enter a parse tree produced by the {@code condExpr}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void enterCondExpr(StellarParser.CondExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code condExpr}
	 * labeled alternative in {@link StellarParser#arithmetic_operands}.
	 * @param ctx the parse tree
	 */
	void exitCondExpr(StellarParser.CondExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LogicalConst}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterLogicalConst(StellarParser.LogicalConstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LogicalConst}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitLogicalConst(StellarParser.LogicalConstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LambdaWithArgsExpr}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterLambdaWithArgsExpr(StellarParser.LambdaWithArgsExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LambdaWithArgsExpr}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitLambdaWithArgsExpr(StellarParser.LambdaWithArgsExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code LambdaWithoutArgsExpr}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterLambdaWithoutArgsExpr(StellarParser.LambdaWithoutArgsExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code LambdaWithoutArgsExpr}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitLambdaWithoutArgsExpr(StellarParser.LambdaWithoutArgsExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ArithmeticOperands}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterArithmeticOperands(StellarParser.ArithmeticOperandsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ArithmeticOperands}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitArithmeticOperands(StellarParser.ArithmeticOperandsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(StellarParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(StellarParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code List}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterList(StellarParser.ListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code List}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitList(StellarParser.ListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code MapConst}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterMapConst(StellarParser.MapConstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code MapConst}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitMapConst(StellarParser.MapConstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code NullConst}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterNullConst(StellarParser.NullConstContext ctx);
	/**
	 * Exit a parse tree produced by the {@code NullConst}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitNullConst(StellarParser.NullConstContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ExistsFunc}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterExistsFunc(StellarParser.ExistsFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ExistsFunc}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitExistsFunc(StellarParser.ExistsFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code condExpr_paren}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterCondExpr_paren(StellarParser.CondExpr_parenContext ctx);
	/**
	 * Exit a parse tree produced by the {@code condExpr_paren}
	 * labeled alternative in {@link StellarParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitCondExpr_paren(StellarParser.CondExpr_parenContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#lambda_without_args}.
	 * @param ctx the parse tree
	 */
	void enterLambda_without_args(StellarParser.Lambda_without_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#lambda_without_args}.
	 * @param ctx the parse tree
	 */
	void exitLambda_without_args(StellarParser.Lambda_without_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#lambda_with_args}.
	 * @param ctx the parse tree
	 */
	void enterLambda_with_args(StellarParser.Lambda_with_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#lambda_with_args}.
	 * @param ctx the parse tree
	 */
	void exitLambda_with_args(StellarParser.Lambda_with_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#lambda_variables}.
	 * @param ctx the parse tree
	 */
	void enterLambda_variables(StellarParser.Lambda_variablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#lambda_variables}.
	 * @param ctx the parse tree
	 */
	void exitLambda_variables(StellarParser.Lambda_variablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#single_lambda_variable}.
	 * @param ctx the parse tree
	 */
	void enterSingle_lambda_variable(StellarParser.Single_lambda_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#single_lambda_variable}.
	 * @param ctx the parse tree
	 */
	void exitSingle_lambda_variable(StellarParser.Single_lambda_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link StellarParser#lambda_variable}.
	 * @param ctx the parse tree
	 */
	void enterLambda_variable(StellarParser.Lambda_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link StellarParser#lambda_variable}.
	 * @param ctx the parse tree
	 */
	void exitLambda_variable(StellarParser.Lambda_variableContext ctx);
}