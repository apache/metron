// Generated from org/apache/metron/common/transformation/generated/Transformation.g4 by ANTLR 4.5
package org.apache.metron.common.transformation.generated;

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
 * {@link TransformationParser}.
 */
public interface TransformationListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TransformationParser#transformation}.
	 * @param ctx the parse tree
	 */
	void enterTransformation(TransformationParser.TransformationContext ctx);
	/**
	 * Exit a parse tree produced by {@link TransformationParser#transformation}.
	 * @param ctx the parse tree
	 */
	void exitTransformation(TransformationParser.TransformationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TransformationExpr}
	 * labeled alternative in {@link TransformationParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterTransformationExpr(TransformationParser.TransformationExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TransformationExpr}
	 * labeled alternative in {@link TransformationParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitTransformationExpr(TransformationParser.TransformationExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TransformationEntity}
	 * labeled alternative in {@link TransformationParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void enterTransformationEntity(TransformationParser.TransformationEntityContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TransformationEntity}
	 * labeled alternative in {@link TransformationParser#transformation_expr}.
	 * @param ctx the parse tree
	 */
	void exitTransformationEntity(TransformationParser.TransformationEntityContext ctx);
	/**
	 * Enter a parse tree produced by {@link TransformationParser#transformation_entity}.
	 * @param ctx the parse tree
	 */
	void enterTransformation_entity(TransformationParser.Transformation_entityContext ctx);
	/**
	 * Exit a parse tree produced by {@link TransformationParser#transformation_entity}.
	 * @param ctx the parse tree
	 */
	void exitTransformation_entity(TransformationParser.Transformation_entityContext ctx);
	/**
	 * Enter a parse tree produced by {@link TransformationParser#func_args}.
	 * @param ctx the parse tree
	 */
	void enterFunc_args(TransformationParser.Func_argsContext ctx);
	/**
	 * Exit a parse tree produced by {@link TransformationParser#func_args}.
	 * @param ctx the parse tree
	 */
	void exitFunc_args(TransformationParser.Func_argsContext ctx);
	/**
	 * Enter a parse tree produced by {@link TransformationParser#op_list}.
	 * @param ctx the parse tree
	 */
	void enterOp_list(TransformationParser.Op_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link TransformationParser#op_list}.
	 * @param ctx the parse tree
	 */
	void exitOp_list(TransformationParser.Op_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link TransformationParser#list_entity}.
	 * @param ctx the parse tree
	 */
	void enterList_entity(TransformationParser.List_entityContext ctx);
	/**
	 * Exit a parse tree produced by {@link TransformationParser#list_entity}.
	 * @param ctx the parse tree
	 */
	void exitList_entity(TransformationParser.List_entityContext ctx);
	/**
	 * Enter a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(TransformationParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code StringLiteral}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(TransformationParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code IntegerLiteral}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterIntegerLiteral(TransformationParser.IntegerLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code IntegerLiteral}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitIntegerLiteral(TransformationParser.IntegerLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code DoubleLiteral}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterDoubleLiteral(TransformationParser.DoubleLiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code DoubleLiteral}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitDoubleLiteral(TransformationParser.DoubleLiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Variable}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterVariable(TransformationParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Variable}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitVariable(TransformationParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code TransformationFunc}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterTransformationFunc(TransformationParser.TransformationFuncContext ctx);
	/**
	 * Exit a parse tree produced by the {@code TransformationFunc}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitTransformationFunc(TransformationParser.TransformationFuncContext ctx);
	/**
	 * Enter a parse tree produced by the {@code List}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void enterList(TransformationParser.ListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code List}
	 * labeled alternative in {@link TransformationParser#identifier_operand}.
	 * @param ctx the parse tree
	 */
	void exitList(TransformationParser.ListContext ctx);
}