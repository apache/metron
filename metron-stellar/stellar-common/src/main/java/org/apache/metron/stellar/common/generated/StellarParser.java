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

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class StellarParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		IN=1, LAMBDA_OP=2, DOUBLE_QUOTE=3, SINGLE_QUOTE=4, COMMA=5, PERIOD=6, 
		AND=7, OR=8, NOT=9, TRUE=10, FALSE=11, ASSIGN=12, COLON_ASSIGN=13, PLUSASSIGN=14, 
		MINUSASSIGN=15, DIVIDEASSIGN=16, MULTASSIGN=17, EQ=18, NEQ=19, LT=20, 
		LTE=21, GT=22, GTE=23, QUESTION=24, COLON=25, IF=26, THEN=27, ELSE=28, 
		NULL=29, NAN=30, MATCH=31, DEFAULT=32, MATCH_ACTION=33, MINUS=34, MINUSMINUS=35, 
		PLUS=36, PLUSPLUS=37, DIV=38, MUL=39, LBRACE=40, RBRACE=41, LBRACKET=42, 
		RBRACKET=43, LPAREN=44, RPAREN=45, NIN=46, EXISTS=47, EXPONENT=48, INT_LITERAL=49, 
		DOUBLE_LITERAL=50, FLOAT_LITERAL=51, LONG_LITERAL=52, IDENTIFIER=53, STRING_LITERAL=54, 
		COMMENT=55, WS=56;
	public static final int
		RULE_transformation = 0, RULE_transformation_expr = 1, RULE_assign_expr = 2, 
		RULE_pre_expr = 3, RULE_post_expr = 4, RULE_if_expr = 5, RULE_then_expr = 6, 
		RULE_else_expr = 7, RULE_conditional_expr = 8, RULE_logical_expr = 9, 
		RULE_b_expr = 10, RULE_in_expr = 11, RULE_comparison_expr = 12, RULE_transformation_entity = 13, 
		RULE_comp_operator = 14, RULE_func_args = 15, RULE_op_list = 16, RULE_list_entity = 17, 
		RULE_kv_list = 18, RULE_map_entity = 19, RULE_arithmetic_expr = 20, RULE_arithmetic_expr_mul = 21, 
		RULE_functions = 22, RULE_arithmetic_operands = 23, RULE_identifier_operand = 24, 
		RULE_default_operand = 25, RULE_lambda_without_args = 26, RULE_lambda_with_args = 27, 
		RULE_lambda_variables = 28, RULE_single_lambda_variable = 29, RULE_lambda_variable = 30, 
		RULE_match_expr = 31, RULE_match_clauses = 32, RULE_match_clause = 33, 
		RULE_match_clause_action = 34, RULE_match_clause_check = 35;
	public static final String[] ruleNames = {
		"transformation", "transformation_expr", "assign_expr", "pre_expr", "post_expr", 
		"if_expr", "then_expr", "else_expr", "conditional_expr", "logical_expr", 
		"b_expr", "in_expr", "comparison_expr", "transformation_entity", "comp_operator", 
		"func_args", "op_list", "list_entity", "kv_list", "map_entity", "arithmetic_expr", 
		"arithmetic_expr_mul", "functions", "arithmetic_operands", "identifier_operand", 
		"default_operand", "lambda_without_args", "lambda_with_args", "lambda_variables", 
		"single_lambda_variable", "lambda_variable", "match_expr", "match_clauses", 
		"match_clause", "match_clause_action", "match_clause_check"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'->'", "'\"'", "'''", "','", "'.'", null, null, null, null, 
		null, "'='", "':='", "'+='", "'-='", "'/='", "'*='", "'=='", "'!='", "'<'", 
		"'<='", "'>'", "'>='", "'?'", "':'", null, null, null, null, "'NaN'", 
		null, null, "'=>'", "'-'", "'--'", "'+'", "'++'", "'/'", "'*'", "'{'", 
		"'}'", "'['", "']'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "ASSIGN", "COLON_ASSIGN", "PLUSASSIGN", 
		"MINUSASSIGN", "DIVIDEASSIGN", "MULTASSIGN", "EQ", "NEQ", "LT", "LTE", 
		"GT", "GTE", "QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "NAN", 
		"MATCH", "DEFAULT", "MATCH_ACTION", "MINUS", "MINUSMINUS", "PLUS", "PLUSPLUS", 
		"DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", 
		"NIN", "EXISTS", "EXPONENT", "INT_LITERAL", "DOUBLE_LITERAL", "FLOAT_LITERAL", 
		"LONG_LITERAL", "IDENTIFIER", "STRING_LITERAL", "COMMENT", "WS"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Stellar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public StellarParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class TransformationContext extends ParserRuleContext {
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(StellarParser.EOF, 0); }
		public TransformationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transformation; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterTransformation(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitTransformation(this);
		}
	}

	public final TransformationContext transformation() throws RecognitionException {
		TransformationContext _localctx = new TransformationContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_transformation);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(72);
			transformation_expr();
			setState(73);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Transformation_exprContext extends ParserRuleContext {
		public Transformation_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transformation_expr; }
	 
		public Transformation_exprContext() { }
		public void copyFrom(Transformation_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ComparisonExpressionContext extends Transformation_exprContext {
		public Comparison_exprContext comparison_expr() {
			return getRuleContext(Comparison_exprContext.class,0);
		}
		public ComparisonExpressionContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterComparisonExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitComparisonExpression(this);
		}
	}
	public static class LogicalExpressionContext extends Transformation_exprContext {
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public LogicalExpressionContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLogicalExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLogicalExpression(this);
		}
	}
	public static class PostEprContext extends Transformation_exprContext {
		public Post_exprContext post_expr() {
			return getRuleContext(Post_exprContext.class,0);
		}
		public PostEprContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterPostEpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitPostEpr(this);
		}
	}
	public static class PreExprContext extends Transformation_exprContext {
		public Pre_exprContext pre_expr() {
			return getRuleContext(Pre_exprContext.class,0);
		}
		public PreExprContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterPreExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitPreExpr(this);
		}
	}
	public static class TransformationEntityContext extends Transformation_exprContext {
		public Transformation_entityContext transformation_entity() {
			return getRuleContext(Transformation_entityContext.class,0);
		}
		public TransformationEntityContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterTransformationEntity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitTransformationEntity(this);
		}
	}
	public static class InExpressionContext extends Transformation_exprContext {
		public In_exprContext in_expr() {
			return getRuleContext(In_exprContext.class,0);
		}
		public InExpressionContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterInExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitInExpression(this);
		}
	}
	public static class ArithExpressionContext extends Transformation_exprContext {
		public Arithmetic_exprContext arithmetic_expr() {
			return getRuleContext(Arithmetic_exprContext.class,0);
		}
		public ArithExpressionContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithExpression(this);
		}
	}
	public static class TransformationExprContext extends Transformation_exprContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public TransformationExprContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterTransformationExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitTransformationExpr(this);
		}
	}
	public static class ConditionalExprContext extends Transformation_exprContext {
		public Conditional_exprContext conditional_expr() {
			return getRuleContext(Conditional_exprContext.class,0);
		}
		public ConditionalExprContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterConditionalExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitConditionalExpr(this);
		}
	}
	public static class MatchExprContext extends Transformation_exprContext {
		public Match_exprContext match_expr() {
			return getRuleContext(Match_exprContext.class,0);
		}
		public MatchExprContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMatchExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMatchExpr(this);
		}
	}
	public static class AssignExprContext extends Transformation_exprContext {
		public Assign_exprContext assign_expr() {
			return getRuleContext(Assign_exprContext.class,0);
		}
		public AssignExprContext(Transformation_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterAssignExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitAssignExpr(this);
		}
	}

	public final Transformation_exprContext transformation_expr() throws RecognitionException {
		Transformation_exprContext _localctx = new Transformation_exprContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_transformation_expr);
		try {
			setState(89);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				_localctx = new ConditionalExprContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(75);
				conditional_expr();
				}
				break;
			case 2:
				_localctx = new TransformationExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(76);
				match(LPAREN);
				setState(77);
				transformation_expr();
				setState(78);
				match(RPAREN);
				}
				break;
			case 3:
				_localctx = new ArithExpressionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(80);
				arithmetic_expr(0);
				}
				break;
			case 4:
				_localctx = new TransformationEntityContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(81);
				transformation_entity();
				}
				break;
			case 5:
				_localctx = new ComparisonExpressionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(82);
				comparison_expr(0);
				}
				break;
			case 6:
				_localctx = new LogicalExpressionContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(83);
				logical_expr();
				}
				break;
			case 7:
				_localctx = new InExpressionContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(84);
				in_expr();
				}
				break;
			case 8:
				_localctx = new MatchExprContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(85);
				match_expr();
				}
				break;
			case 9:
				_localctx = new AssignExprContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(86);
				assign_expr();
				}
				break;
			case 10:
				_localctx = new PreExprContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(87);
				pre_expr();
				}
				break;
			case 11:
				_localctx = new PostEprContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(88);
				post_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Assign_exprContext extends ParserRuleContext {
		public Assign_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_assign_expr; }
	 
		public Assign_exprContext() { }
		public void copyFrom(Assign_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class AssignExpressionContext extends Assign_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode ASSIGN() { return getToken(StellarParser.ASSIGN, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public AssignExpressionContext(Assign_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterAssignExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitAssignExpression(this);
		}
	}
	public static class DivideAssignExpressionContext extends Assign_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode DIVIDEASSIGN() { return getToken(StellarParser.DIVIDEASSIGN, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public DivideAssignExpressionContext(Assign_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterDivideAssignExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitDivideAssignExpression(this);
		}
	}
	public static class PlusAssignExpressionContext extends Assign_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode PLUSASSIGN() { return getToken(StellarParser.PLUSASSIGN, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public PlusAssignExpressionContext(Assign_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterPlusAssignExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitPlusAssignExpression(this);
		}
	}
	public static class MultiAssignExpressionContext extends Assign_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode MULTASSIGN() { return getToken(StellarParser.MULTASSIGN, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public MultiAssignExpressionContext(Assign_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMultiAssignExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMultiAssignExpression(this);
		}
	}
	public static class ColonAssignExpressionContext extends Assign_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode COLON_ASSIGN() { return getToken(StellarParser.COLON_ASSIGN, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public ColonAssignExpressionContext(Assign_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterColonAssignExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitColonAssignExpression(this);
		}
	}
	public static class MinusAssignExpressionContext extends Assign_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode MINUSASSIGN() { return getToken(StellarParser.MINUSASSIGN, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public MinusAssignExpressionContext(Assign_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMinusAssignExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMinusAssignExpression(this);
		}
	}

	public final Assign_exprContext assign_expr() throws RecognitionException {
		Assign_exprContext _localctx = new Assign_exprContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_assign_expr);
		try {
			setState(109);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				_localctx = new AssignExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(91);
				match(IDENTIFIER);
				setState(92);
				match(ASSIGN);
				setState(93);
				transformation_expr();
				}
				break;
			case 2:
				_localctx = new ColonAssignExpressionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(94);
				match(IDENTIFIER);
				setState(95);
				match(COLON_ASSIGN);
				setState(96);
				transformation_expr();
				}
				break;
			case 3:
				_localctx = new PlusAssignExpressionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(97);
				match(IDENTIFIER);
				setState(98);
				match(PLUSASSIGN);
				setState(99);
				transformation_expr();
				}
				break;
			case 4:
				_localctx = new MinusAssignExpressionContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(100);
				match(IDENTIFIER);
				setState(101);
				match(MINUSASSIGN);
				setState(102);
				transformation_expr();
				}
				break;
			case 5:
				_localctx = new DivideAssignExpressionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(103);
				match(IDENTIFIER);
				setState(104);
				match(DIVIDEASSIGN);
				setState(105);
				transformation_expr();
				}
				break;
			case 6:
				_localctx = new MultiAssignExpressionContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(106);
				match(IDENTIFIER);
				setState(107);
				match(MULTASSIGN);
				setState(108);
				transformation_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Pre_exprContext extends ParserRuleContext {
		public Pre_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pre_expr; }
	 
		public Pre_exprContext() { }
		public void copyFrom(Pre_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class PreDecrementExpressionContext extends Pre_exprContext {
		public TerminalNode MINUSMINUS() { return getToken(StellarParser.MINUSMINUS, 0); }
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public PreDecrementExpressionContext(Pre_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterPreDecrementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitPreDecrementExpression(this);
		}
	}
	public static class PreIncrementExpressionContext extends Pre_exprContext {
		public TerminalNode PLUSPLUS() { return getToken(StellarParser.PLUSPLUS, 0); }
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public PreIncrementExpressionContext(Pre_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterPreIncrementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitPreIncrementExpression(this);
		}
	}

	public final Pre_exprContext pre_expr() throws RecognitionException {
		Pre_exprContext _localctx = new Pre_exprContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_pre_expr);
		try {
			setState(115);
			switch (_input.LA(1)) {
			case PLUSPLUS:
				_localctx = new PreIncrementExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(111);
				match(PLUSPLUS);
				setState(112);
				match(IDENTIFIER);
				}
				break;
			case MINUSMINUS:
				_localctx = new PreDecrementExpressionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(113);
				match(MINUSMINUS);
				setState(114);
				match(IDENTIFIER);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Post_exprContext extends ParserRuleContext {
		public Post_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_post_expr; }
	 
		public Post_exprContext() { }
		public void copyFrom(Post_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class PostIncrementExpressionContext extends Post_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode PLUSPLUS() { return getToken(StellarParser.PLUSPLUS, 0); }
		public PostIncrementExpressionContext(Post_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterPostIncrementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitPostIncrementExpression(this);
		}
	}
	public static class PostDecrementExpressionContext extends Post_exprContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode MINUSMINUS() { return getToken(StellarParser.MINUSMINUS, 0); }
		public PostDecrementExpressionContext(Post_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterPostDecrementExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitPostDecrementExpression(this);
		}
	}

	public final Post_exprContext post_expr() throws RecognitionException {
		Post_exprContext _localctx = new Post_exprContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_post_expr);
		try {
			setState(121);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				_localctx = new PostIncrementExpressionContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(117);
				match(IDENTIFIER);
				setState(118);
				match(PLUSPLUS);
				}
				break;
			case 2:
				_localctx = new PostDecrementExpressionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(119);
				match(IDENTIFIER);
				setState(120);
				match(MINUSMINUS);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class If_exprContext extends ParserRuleContext {
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public If_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_if_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterIf_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitIf_expr(this);
		}
	}

	public final If_exprContext if_expr() throws RecognitionException {
		If_exprContext _localctx = new If_exprContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_if_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(123);
			logical_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Then_exprContext extends ParserRuleContext {
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public Then_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_then_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterThen_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitThen_expr(this);
		}
	}

	public final Then_exprContext then_expr() throws RecognitionException {
		Then_exprContext _localctx = new Then_exprContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_then_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(125);
			transformation_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Else_exprContext extends ParserRuleContext {
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public Else_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_else_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterElse_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitElse_expr(this);
		}
	}

	public final Else_exprContext else_expr() throws RecognitionException {
		Else_exprContext _localctx = new Else_exprContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_else_expr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(127);
			transformation_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Conditional_exprContext extends ParserRuleContext {
		public Conditional_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_conditional_expr; }
	 
		public Conditional_exprContext() { }
		public void copyFrom(Conditional_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TernaryFuncWithoutIfContext extends Conditional_exprContext {
		public If_exprContext if_expr() {
			return getRuleContext(If_exprContext.class,0);
		}
		public TerminalNode QUESTION() { return getToken(StellarParser.QUESTION, 0); }
		public Then_exprContext then_expr() {
			return getRuleContext(Then_exprContext.class,0);
		}
		public TerminalNode COLON() { return getToken(StellarParser.COLON, 0); }
		public Else_exprContext else_expr() {
			return getRuleContext(Else_exprContext.class,0);
		}
		public TernaryFuncWithoutIfContext(Conditional_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterTernaryFuncWithoutIf(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitTernaryFuncWithoutIf(this);
		}
	}
	public static class TernaryFuncWithIfContext extends Conditional_exprContext {
		public TerminalNode IF() { return getToken(StellarParser.IF, 0); }
		public If_exprContext if_expr() {
			return getRuleContext(If_exprContext.class,0);
		}
		public TerminalNode THEN() { return getToken(StellarParser.THEN, 0); }
		public Then_exprContext then_expr() {
			return getRuleContext(Then_exprContext.class,0);
		}
		public TerminalNode ELSE() { return getToken(StellarParser.ELSE, 0); }
		public Else_exprContext else_expr() {
			return getRuleContext(Else_exprContext.class,0);
		}
		public TernaryFuncWithIfContext(Conditional_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterTernaryFuncWithIf(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitTernaryFuncWithIf(this);
		}
	}

	public final Conditional_exprContext conditional_expr() throws RecognitionException {
		Conditional_exprContext _localctx = new Conditional_exprContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_conditional_expr);
		try {
			setState(142);
			switch (_input.LA(1)) {
			case NOT:
			case TRUE:
			case FALSE:
			case NULL:
			case NAN:
			case LBRACE:
			case LBRACKET:
			case LPAREN:
			case EXISTS:
			case INT_LITERAL:
			case DOUBLE_LITERAL:
			case FLOAT_LITERAL:
			case LONG_LITERAL:
			case IDENTIFIER:
			case STRING_LITERAL:
				_localctx = new TernaryFuncWithoutIfContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(129);
				if_expr();
				setState(130);
				match(QUESTION);
				setState(131);
				then_expr();
				setState(132);
				match(COLON);
				setState(133);
				else_expr();
				}
				break;
			case IF:
				_localctx = new TernaryFuncWithIfContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(135);
				match(IF);
				setState(136);
				if_expr();
				setState(137);
				match(THEN);
				setState(138);
				then_expr();
				setState(139);
				match(ELSE);
				setState(140);
				else_expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Logical_exprContext extends ParserRuleContext {
		public Logical_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_logical_expr; }
	 
		public Logical_exprContext() { }
		public void copyFrom(Logical_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class LogicalExpressionAndContext extends Logical_exprContext {
		public B_exprContext b_expr() {
			return getRuleContext(B_exprContext.class,0);
		}
		public TerminalNode AND() { return getToken(StellarParser.AND, 0); }
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public LogicalExpressionAndContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLogicalExpressionAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLogicalExpressionAnd(this);
		}
	}
	public static class BoleanExpressionContext extends Logical_exprContext {
		public B_exprContext b_expr() {
			return getRuleContext(B_exprContext.class,0);
		}
		public BoleanExpressionContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterBoleanExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitBoleanExpression(this);
		}
	}
	public static class LogicalExpressionOrContext extends Logical_exprContext {
		public B_exprContext b_expr() {
			return getRuleContext(B_exprContext.class,0);
		}
		public TerminalNode OR() { return getToken(StellarParser.OR, 0); }
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public LogicalExpressionOrContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLogicalExpressionOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLogicalExpressionOr(this);
		}
	}

	public final Logical_exprContext logical_expr() throws RecognitionException {
		Logical_exprContext _localctx = new Logical_exprContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_logical_expr);
		try {
			setState(153);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				_localctx = new LogicalExpressionAndContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(144);
				b_expr();
				setState(145);
				match(AND);
				setState(146);
				logical_expr();
				}
				break;
			case 2:
				_localctx = new LogicalExpressionOrContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(148);
				b_expr();
				setState(149);
				match(OR);
				setState(150);
				logical_expr();
				}
				break;
			case 3:
				_localctx = new BoleanExpressionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(152);
				b_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class B_exprContext extends ParserRuleContext {
		public Comparison_exprContext comparison_expr() {
			return getRuleContext(Comparison_exprContext.class,0);
		}
		public In_exprContext in_expr() {
			return getRuleContext(In_exprContext.class,0);
		}
		public B_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_b_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterB_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitB_expr(this);
		}
	}

	public final B_exprContext b_expr() throws RecognitionException {
		B_exprContext _localctx = new B_exprContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_b_expr);
		try {
			setState(157);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(155);
				comparison_expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(156);
				in_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class In_exprContext extends ParserRuleContext {
		public In_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_in_expr; }
	 
		public In_exprContext() { }
		public void copyFrom(In_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NInExpressionStatementContext extends In_exprContext {
		public Identifier_operandContext identifier_operand() {
			return getRuleContext(Identifier_operandContext.class,0);
		}
		public TerminalNode NIN() { return getToken(StellarParser.NIN, 0); }
		public B_exprContext b_expr() {
			return getRuleContext(B_exprContext.class,0);
		}
		public NInExpressionStatementContext(In_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterNInExpressionStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitNInExpressionStatement(this);
		}
	}
	public static class InExpressionStatementContext extends In_exprContext {
		public Identifier_operandContext identifier_operand() {
			return getRuleContext(Identifier_operandContext.class,0);
		}
		public TerminalNode IN() { return getToken(StellarParser.IN, 0); }
		public B_exprContext b_expr() {
			return getRuleContext(B_exprContext.class,0);
		}
		public InExpressionStatementContext(In_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterInExpressionStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitInExpressionStatement(this);
		}
	}

	public final In_exprContext in_expr() throws RecognitionException {
		In_exprContext _localctx = new In_exprContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_in_expr);
		try {
			setState(167);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				_localctx = new InExpressionStatementContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(159);
				identifier_operand();
				setState(160);
				match(IN);
				setState(161);
				b_expr();
				}
				break;
			case 2:
				_localctx = new NInExpressionStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(163);
				identifier_operand();
				setState(164);
				match(NIN);
				setState(165);
				b_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Comparison_exprContext extends ParserRuleContext {
		public Comparison_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparison_expr; }
	 
		public Comparison_exprContext() { }
		public void copyFrom(Comparison_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NotFuncContext extends Comparison_exprContext {
		public TerminalNode NOT() { return getToken(StellarParser.NOT, 0); }
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public NotFuncContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterNotFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitNotFunc(this);
		}
	}
	public static class ComparisonExpressionParensContext extends Comparison_exprContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public ComparisonExpressionParensContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterComparisonExpressionParens(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitComparisonExpressionParens(this);
		}
	}
	public static class ComparisonExpressionWithOperatorContext extends Comparison_exprContext {
		public List<Comparison_exprContext> comparison_expr() {
			return getRuleContexts(Comparison_exprContext.class);
		}
		public Comparison_exprContext comparison_expr(int i) {
			return getRuleContext(Comparison_exprContext.class,i);
		}
		public Comp_operatorContext comp_operator() {
			return getRuleContext(Comp_operatorContext.class,0);
		}
		public ComparisonExpressionWithOperatorContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterComparisonExpressionWithOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitComparisonExpressionWithOperator(this);
		}
	}
	public static class OperandContext extends Comparison_exprContext {
		public Identifier_operandContext identifier_operand() {
			return getRuleContext(Identifier_operandContext.class,0);
		}
		public OperandContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterOperand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitOperand(this);
		}
	}

	public final Comparison_exprContext comparison_expr() throws RecognitionException {
		return comparison_expr(0);
	}

	private Comparison_exprContext comparison_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Comparison_exprContext _localctx = new Comparison_exprContext(_ctx, _parentState);
		Comparison_exprContext _prevctx = _localctx;
		int _startState = 24;
		enterRecursionRule(_localctx, 24, RULE_comparison_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				_localctx = new NotFuncContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(170);
				match(NOT);
				setState(171);
				match(LPAREN);
				setState(172);
				logical_expr();
				setState(173);
				match(RPAREN);
				}
				break;
			case 2:
				{
				_localctx = new ComparisonExpressionParensContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(175);
				match(LPAREN);
				setState(176);
				logical_expr();
				setState(177);
				match(RPAREN);
				}
				break;
			case 3:
				{
				_localctx = new OperandContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(179);
				identifier_operand();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(188);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ComparisonExpressionWithOperatorContext(new Comparison_exprContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_comparison_expr);
					setState(182);
					if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
					setState(183);
					comp_operator();
					setState(184);
					comparison_expr(5);
					}
					} 
				}
				setState(190);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Transformation_entityContext extends ParserRuleContext {
		public Identifier_operandContext identifier_operand() {
			return getRuleContext(Identifier_operandContext.class,0);
		}
		public Transformation_entityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_transformation_entity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterTransformation_entity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitTransformation_entity(this);
		}
	}

	public final Transformation_entityContext transformation_entity() throws RecognitionException {
		Transformation_entityContext _localctx = new Transformation_entityContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_transformation_entity);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(191);
			identifier_operand();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Comp_operatorContext extends ParserRuleContext {
		public Comp_operatorContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comp_operator; }
	 
		public Comp_operatorContext() { }
		public void copyFrom(Comp_operatorContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ComparisonOpContext extends Comp_operatorContext {
		public TerminalNode EQ() { return getToken(StellarParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(StellarParser.NEQ, 0); }
		public TerminalNode LT() { return getToken(StellarParser.LT, 0); }
		public TerminalNode LTE() { return getToken(StellarParser.LTE, 0); }
		public TerminalNode GT() { return getToken(StellarParser.GT, 0); }
		public TerminalNode GTE() { return getToken(StellarParser.GTE, 0); }
		public ComparisonOpContext(Comp_operatorContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterComparisonOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitComparisonOp(this);
		}
	}

	public final Comp_operatorContext comp_operator() throws RecognitionException {
		Comp_operatorContext _localctx = new Comp_operatorContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_comp_operator);
		int _la;
		try {
			_localctx = new ComparisonOpContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << EQ) | (1L << NEQ) | (1L << LT) | (1L << LTE) | (1L << GT) | (1L << GTE))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Func_argsContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Op_listContext op_list() {
			return getRuleContext(Op_listContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public Func_argsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_func_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterFunc_args(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitFunc_args(this);
		}
	}

	public final Func_argsContext func_args() throws RecognitionException {
		Func_argsContext _localctx = new Func_argsContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_func_args);
		try {
			setState(201);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(195);
				match(LPAREN);
				setState(196);
				op_list(0);
				setState(197);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(199);
				match(LPAREN);
				setState(200);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Op_listContext extends ParserRuleContext {
		public Identifier_operandContext identifier_operand() {
			return getRuleContext(Identifier_operandContext.class,0);
		}
		public Conditional_exprContext conditional_expr() {
			return getRuleContext(Conditional_exprContext.class,0);
		}
		public Comparison_exprContext comparison_expr() {
			return getRuleContext(Comparison_exprContext.class,0);
		}
		public Op_listContext op_list() {
			return getRuleContext(Op_listContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(StellarParser.COMMA, 0); }
		public Op_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_op_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterOp_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitOp_list(this);
		}
	}

	public final Op_listContext op_list() throws RecognitionException {
		return op_list(0);
	}

	private Op_listContext op_list(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Op_listContext _localctx = new Op_listContext(_ctx, _parentState);
		Op_listContext _prevctx = _localctx;
		int _startState = 32;
		enterRecursionRule(_localctx, 32, RULE_op_list, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(207);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				{
				setState(204);
				identifier_operand();
				}
				break;
			case 2:
				{
				setState(205);
				conditional_expr();
				}
				break;
			case 3:
				{
				setState(206);
				comparison_expr(0);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(220);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(218);
					switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
					case 1:
						{
						_localctx = new Op_listContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_op_list);
						setState(209);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(210);
						match(COMMA);
						setState(211);
						identifier_operand();
						}
						break;
					case 2:
						{
						_localctx = new Op_listContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_op_list);
						setState(212);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(213);
						match(COMMA);
						setState(214);
						conditional_expr();
						}
						break;
					case 3:
						{
						_localctx = new Op_listContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_op_list);
						setState(215);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(216);
						match(COMMA);
						setState(217);
						comparison_expr(0);
						}
						break;
					}
					} 
				}
				setState(222);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class List_entityContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(StellarParser.LBRACKET, 0); }
		public TerminalNode RBRACKET() { return getToken(StellarParser.RBRACKET, 0); }
		public Op_listContext op_list() {
			return getRuleContext(Op_listContext.class,0);
		}
		public List_entityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_list_entity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterList_entity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitList_entity(this);
		}
	}

	public final List_entityContext list_entity() throws RecognitionException {
		List_entityContext _localctx = new List_entityContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_list_entity);
		try {
			setState(229);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(223);
				match(LBRACKET);
				setState(224);
				match(RBRACKET);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(225);
				match(LBRACKET);
				setState(226);
				op_list(0);
				setState(227);
				match(RBRACKET);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Kv_listContext extends ParserRuleContext {
		public Identifier_operandContext identifier_operand() {
			return getRuleContext(Identifier_operandContext.class,0);
		}
		public TerminalNode COLON() { return getToken(StellarParser.COLON, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public Comparison_exprContext comparison_expr() {
			return getRuleContext(Comparison_exprContext.class,0);
		}
		public Kv_listContext kv_list() {
			return getRuleContext(Kv_listContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(StellarParser.COMMA, 0); }
		public Kv_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_kv_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterKv_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitKv_list(this);
		}
	}

	public final Kv_listContext kv_list() throws RecognitionException {
		return kv_list(0);
	}

	private Kv_listContext kv_list(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Kv_listContext _localctx = new Kv_listContext(_ctx, _parentState);
		Kv_listContext _prevctx = _localctx;
		int _startState = 36;
		enterRecursionRule(_localctx, 36, RULE_kv_list, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(240);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				{
				setState(232);
				identifier_operand();
				setState(233);
				match(COLON);
				setState(234);
				transformation_expr();
				}
				break;
			case 2:
				{
				setState(236);
				comparison_expr(0);
				setState(237);
				match(COLON);
				setState(238);
				transformation_expr();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(256);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(254);
					switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
					case 1:
						{
						_localctx = new Kv_listContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_kv_list);
						setState(242);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(243);
						match(COMMA);
						setState(244);
						identifier_operand();
						setState(245);
						match(COLON);
						setState(246);
						transformation_expr();
						}
						break;
					case 2:
						{
						_localctx = new Kv_listContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_kv_list);
						setState(248);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(249);
						match(COMMA);
						setState(250);
						comparison_expr(0);
						setState(251);
						match(COLON);
						setState(252);
						transformation_expr();
						}
						break;
					}
					} 
				}
				setState(258);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Map_entityContext extends ParserRuleContext {
		public TerminalNode LBRACE() { return getToken(StellarParser.LBRACE, 0); }
		public Kv_listContext kv_list() {
			return getRuleContext(Kv_listContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(StellarParser.RBRACE, 0); }
		public Map_entityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_map_entity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMap_entity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMap_entity(this);
		}
	}

	public final Map_entityContext map_entity() throws RecognitionException {
		Map_entityContext _localctx = new Map_entityContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_map_entity);
		try {
			setState(265);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(259);
				match(LBRACE);
				setState(260);
				kv_list(0);
				setState(261);
				match(RBRACE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(263);
				match(LBRACE);
				setState(264);
				match(RBRACE);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Arithmetic_exprContext extends ParserRuleContext {
		public Arithmetic_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmetic_expr; }
	 
		public Arithmetic_exprContext() { }
		public void copyFrom(Arithmetic_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ArithExpr_soloContext extends Arithmetic_exprContext {
		public Arithmetic_expr_mulContext arithmetic_expr_mul() {
			return getRuleContext(Arithmetic_expr_mulContext.class,0);
		}
		public ArithExpr_soloContext(Arithmetic_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithExpr_solo(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithExpr_solo(this);
		}
	}
	public static class ArithExpr_minusContext extends Arithmetic_exprContext {
		public Arithmetic_exprContext arithmetic_expr() {
			return getRuleContext(Arithmetic_exprContext.class,0);
		}
		public TerminalNode MINUS() { return getToken(StellarParser.MINUS, 0); }
		public Arithmetic_expr_mulContext arithmetic_expr_mul() {
			return getRuleContext(Arithmetic_expr_mulContext.class,0);
		}
		public ArithExpr_minusContext(Arithmetic_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithExpr_minus(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithExpr_minus(this);
		}
	}
	public static class ArithExpr_plusContext extends Arithmetic_exprContext {
		public Arithmetic_exprContext arithmetic_expr() {
			return getRuleContext(Arithmetic_exprContext.class,0);
		}
		public TerminalNode PLUS() { return getToken(StellarParser.PLUS, 0); }
		public Arithmetic_expr_mulContext arithmetic_expr_mul() {
			return getRuleContext(Arithmetic_expr_mulContext.class,0);
		}
		public ArithExpr_plusContext(Arithmetic_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithExpr_plus(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithExpr_plus(this);
		}
	}

	public final Arithmetic_exprContext arithmetic_expr() throws RecognitionException {
		return arithmetic_expr(0);
	}

	private Arithmetic_exprContext arithmetic_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Arithmetic_exprContext _localctx = new Arithmetic_exprContext(_ctx, _parentState);
		Arithmetic_exprContext _prevctx = _localctx;
		int _startState = 40;
		enterRecursionRule(_localctx, 40, RULE_arithmetic_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ArithExpr_soloContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(268);
			arithmetic_expr_mul(0);
			}
			_ctx.stop = _input.LT(-1);
			setState(278);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(276);
					switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
					case 1:
						{
						_localctx = new ArithExpr_plusContext(new Arithmetic_exprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr);
						setState(270);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(271);
						match(PLUS);
						setState(272);
						arithmetic_expr_mul(0);
						}
						break;
					case 2:
						{
						_localctx = new ArithExpr_minusContext(new Arithmetic_exprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr);
						setState(273);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(274);
						match(MINUS);
						setState(275);
						arithmetic_expr_mul(0);
						}
						break;
					}
					} 
				}
				setState(280);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class Arithmetic_expr_mulContext extends ParserRuleContext {
		public Arithmetic_expr_mulContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmetic_expr_mul; }
	 
		public Arithmetic_expr_mulContext() { }
		public void copyFrom(Arithmetic_expr_mulContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ArithExpr_divContext extends Arithmetic_expr_mulContext {
		public List<Arithmetic_expr_mulContext> arithmetic_expr_mul() {
			return getRuleContexts(Arithmetic_expr_mulContext.class);
		}
		public Arithmetic_expr_mulContext arithmetic_expr_mul(int i) {
			return getRuleContext(Arithmetic_expr_mulContext.class,i);
		}
		public TerminalNode DIV() { return getToken(StellarParser.DIV, 0); }
		public ArithExpr_divContext(Arithmetic_expr_mulContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithExpr_div(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithExpr_div(this);
		}
	}
	public static class ArithExpr_mul_soloContext extends Arithmetic_expr_mulContext {
		public Arithmetic_operandsContext arithmetic_operands() {
			return getRuleContext(Arithmetic_operandsContext.class,0);
		}
		public ArithExpr_mul_soloContext(Arithmetic_expr_mulContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithExpr_mul_solo(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithExpr_mul_solo(this);
		}
	}
	public static class ArithExpr_mulContext extends Arithmetic_expr_mulContext {
		public List<Arithmetic_expr_mulContext> arithmetic_expr_mul() {
			return getRuleContexts(Arithmetic_expr_mulContext.class);
		}
		public Arithmetic_expr_mulContext arithmetic_expr_mul(int i) {
			return getRuleContext(Arithmetic_expr_mulContext.class,i);
		}
		public TerminalNode MUL() { return getToken(StellarParser.MUL, 0); }
		public ArithExpr_mulContext(Arithmetic_expr_mulContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithExpr_mul(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithExpr_mul(this);
		}
	}

	public final Arithmetic_expr_mulContext arithmetic_expr_mul() throws RecognitionException {
		return arithmetic_expr_mul(0);
	}

	private Arithmetic_expr_mulContext arithmetic_expr_mul(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Arithmetic_expr_mulContext _localctx = new Arithmetic_expr_mulContext(_ctx, _parentState);
		Arithmetic_expr_mulContext _prevctx = _localctx;
		int _startState = 42;
		enterRecursionRule(_localctx, 42, RULE_arithmetic_expr_mul, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ArithExpr_mul_soloContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(282);
			arithmetic_operands();
			}
			_ctx.stop = _input.LT(-1);
			setState(292);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(290);
					switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
					case 1:
						{
						_localctx = new ArithExpr_mulContext(new Arithmetic_expr_mulContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr_mul);
						setState(284);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(285);
						match(MUL);
						setState(286);
						arithmetic_expr_mul(3);
						}
						break;
					case 2:
						{
						_localctx = new ArithExpr_divContext(new Arithmetic_expr_mulContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr_mul);
						setState(287);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(288);
						match(DIV);
						setState(289);
						arithmetic_expr_mul(2);
						}
						break;
					}
					} 
				}
				setState(294);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class FunctionsContext extends ParserRuleContext {
		public FunctionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_functions; }
	 
		public FunctionsContext() { }
		public void copyFrom(FunctionsContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TransformationFuncContext extends FunctionsContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public Func_argsContext func_args() {
			return getRuleContext(Func_argsContext.class,0);
		}
		public TransformationFuncContext(FunctionsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterTransformationFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitTransformationFunc(this);
		}
	}

	public final FunctionsContext functions() throws RecognitionException {
		FunctionsContext _localctx = new FunctionsContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_functions);
		try {
			_localctx = new TransformationFuncContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(295);
			match(IDENTIFIER);
			setState(296);
			func_args();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Arithmetic_operandsContext extends ParserRuleContext {
		public Arithmetic_operandsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arithmetic_operands; }
	 
		public Arithmetic_operandsContext() { }
		public void copyFrom(Arithmetic_operandsContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class VariableContext extends Arithmetic_operandsContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public VariableContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitVariable(this);
		}
	}
	public static class NumericFunctionsContext extends Arithmetic_operandsContext {
		public FunctionsContext functions() {
			return getRuleContext(FunctionsContext.class,0);
		}
		public NumericFunctionsContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterNumericFunctions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitNumericFunctions(this);
		}
	}
	public static class LongLiteralContext extends Arithmetic_operandsContext {
		public TerminalNode LONG_LITERAL() { return getToken(StellarParser.LONG_LITERAL, 0); }
		public LongLiteralContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLongLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLongLiteral(this);
		}
	}
	public static class FloatLiteralContext extends Arithmetic_operandsContext {
		public TerminalNode FLOAT_LITERAL() { return getToken(StellarParser.FLOAT_LITERAL, 0); }
		public FloatLiteralContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterFloatLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitFloatLiteral(this);
		}
	}
	public static class CondExprContext extends Arithmetic_operandsContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Conditional_exprContext conditional_expr() {
			return getRuleContext(Conditional_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public CondExprContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterCondExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitCondExpr(this);
		}
	}
	public static class ParenArithContext extends Arithmetic_operandsContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Arithmetic_exprContext arithmetic_expr() {
			return getRuleContext(Arithmetic_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public ParenArithContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterParenArith(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitParenArith(this);
		}
	}
	public static class IntLiteralContext extends Arithmetic_operandsContext {
		public TerminalNode INT_LITERAL() { return getToken(StellarParser.INT_LITERAL, 0); }
		public IntLiteralContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitIntLiteral(this);
		}
	}
	public static class NaNArithContext extends Arithmetic_operandsContext {
		public TerminalNode NAN() { return getToken(StellarParser.NAN, 0); }
		public NaNArithContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterNaNArith(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitNaNArith(this);
		}
	}
	public static class DoubleLiteralContext extends Arithmetic_operandsContext {
		public TerminalNode DOUBLE_LITERAL() { return getToken(StellarParser.DOUBLE_LITERAL, 0); }
		public DoubleLiteralContext(Arithmetic_operandsContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterDoubleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitDoubleLiteral(this);
		}
	}

	public final Arithmetic_operandsContext arithmetic_operands() throws RecognitionException {
		Arithmetic_operandsContext _localctx = new Arithmetic_operandsContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_arithmetic_operands);
		try {
			setState(313);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				_localctx = new NumericFunctionsContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(298);
				functions();
				}
				break;
			case 2:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(299);
				match(DOUBLE_LITERAL);
				}
				break;
			case 3:
				_localctx = new IntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(300);
				match(INT_LITERAL);
				}
				break;
			case 4:
				_localctx = new LongLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(301);
				match(LONG_LITERAL);
				}
				break;
			case 5:
				_localctx = new FloatLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(302);
				match(FLOAT_LITERAL);
				}
				break;
			case 6:
				_localctx = new VariableContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(303);
				match(IDENTIFIER);
				}
				break;
			case 7:
				_localctx = new NaNArithContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(304);
				match(NAN);
				}
				break;
			case 8:
				_localctx = new ParenArithContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(305);
				match(LPAREN);
				setState(306);
				arithmetic_expr(0);
				setState(307);
				match(RPAREN);
				}
				break;
			case 9:
				_localctx = new CondExprContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(309);
				match(LPAREN);
				setState(310);
				conditional_expr();
				setState(311);
				match(RPAREN);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Identifier_operandContext extends ParserRuleContext {
		public Identifier_operandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier_operand; }
	 
		public Identifier_operandContext() { }
		public void copyFrom(Identifier_operandContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ArithmeticOperandsContext extends Identifier_operandContext {
		public Arithmetic_exprContext arithmetic_expr() {
			return getRuleContext(Arithmetic_exprContext.class,0);
		}
		public ArithmeticOperandsContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterArithmeticOperands(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitArithmeticOperands(this);
		}
	}
	public static class LambdaWithArgsExprContext extends Identifier_operandContext {
		public Lambda_with_argsContext lambda_with_args() {
			return getRuleContext(Lambda_with_argsContext.class,0);
		}
		public LambdaWithArgsExprContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLambdaWithArgsExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLambdaWithArgsExpr(this);
		}
	}
	public static class StringLiteralContext extends Identifier_operandContext {
		public TerminalNode STRING_LITERAL() { return getToken(StellarParser.STRING_LITERAL, 0); }
		public StringLiteralContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitStringLiteral(this);
		}
	}
	public static class FuncContext extends Identifier_operandContext {
		public FunctionsContext functions() {
			return getRuleContext(FunctionsContext.class,0);
		}
		public FuncContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitFunc(this);
		}
	}
	public static class LambdaWithoutArgsExprContext extends Identifier_operandContext {
		public Lambda_without_argsContext lambda_without_args() {
			return getRuleContext(Lambda_without_argsContext.class,0);
		}
		public LambdaWithoutArgsExprContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLambdaWithoutArgsExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLambdaWithoutArgsExpr(this);
		}
	}
	public static class ListContext extends Identifier_operandContext {
		public List_entityContext list_entity() {
			return getRuleContext(List_entityContext.class,0);
		}
		public ListContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitList(this);
		}
	}
	public static class MapConstContext extends Identifier_operandContext {
		public Map_entityContext map_entity() {
			return getRuleContext(Map_entityContext.class,0);
		}
		public MapConstContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMapConst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMapConst(this);
		}
	}
	public static class LogicalConstContext extends Identifier_operandContext {
		public TerminalNode TRUE() { return getToken(StellarParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(StellarParser.FALSE, 0); }
		public LogicalConstContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLogicalConst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLogicalConst(this);
		}
	}
	public static class NullConstContext extends Identifier_operandContext {
		public TerminalNode NULL() { return getToken(StellarParser.NULL, 0); }
		public NullConstContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterNullConst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitNullConst(this);
		}
	}
	public static class ExistsFuncContext extends Identifier_operandContext {
		public TerminalNode EXISTS() { return getToken(StellarParser.EXISTS, 0); }
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public ExistsFuncContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterExistsFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitExistsFunc(this);
		}
	}
	public static class CondExpr_parenContext extends Identifier_operandContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Conditional_exprContext conditional_expr() {
			return getRuleContext(Conditional_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public CondExpr_parenContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterCondExpr_paren(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitCondExpr_paren(this);
		}
	}

	public final Identifier_operandContext identifier_operand() throws RecognitionException {
		Identifier_operandContext _localctx = new Identifier_operandContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_identifier_operand);
		int _la;
		try {
			setState(332);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				_localctx = new LogicalConstContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(315);
				_la = _input.LA(1);
				if ( !(_la==TRUE || _la==FALSE) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
				break;
			case 2:
				_localctx = new LambdaWithArgsExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(316);
				lambda_with_args();
				}
				break;
			case 3:
				_localctx = new LambdaWithoutArgsExprContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(317);
				lambda_without_args();
				}
				break;
			case 4:
				_localctx = new ArithmeticOperandsContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(318);
				arithmetic_expr(0);
				}
				break;
			case 5:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(319);
				match(STRING_LITERAL);
				}
				break;
			case 6:
				_localctx = new ListContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(320);
				list_entity();
				}
				break;
			case 7:
				_localctx = new MapConstContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(321);
				map_entity();
				}
				break;
			case 8:
				_localctx = new NullConstContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(322);
				match(NULL);
				}
				break;
			case 9:
				_localctx = new ExistsFuncContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(323);
				match(EXISTS);
				setState(324);
				match(LPAREN);
				setState(325);
				match(IDENTIFIER);
				setState(326);
				match(RPAREN);
				}
				break;
			case 10:
				_localctx = new CondExpr_parenContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(327);
				match(LPAREN);
				setState(328);
				conditional_expr();
				setState(329);
				match(RPAREN);
				}
				break;
			case 11:
				_localctx = new FuncContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(331);
				functions();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Default_operandContext extends ParserRuleContext {
		public Default_operandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_default_operand; }
	 
		public Default_operandContext() { }
		public void copyFrom(Default_operandContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class DefaultContext extends Default_operandContext {
		public TerminalNode DEFAULT() { return getToken(StellarParser.DEFAULT, 0); }
		public DefaultContext(Default_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterDefault(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitDefault(this);
		}
	}

	public final Default_operandContext default_operand() throws RecognitionException {
		Default_operandContext _localctx = new Default_operandContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_default_operand);
		try {
			_localctx = new DefaultContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(334);
			match(DEFAULT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Lambda_without_argsContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public TerminalNode LAMBDA_OP() { return getToken(StellarParser.LAMBDA_OP, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public Lambda_without_argsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambda_without_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLambda_without_args(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLambda_without_args(this);
		}
	}

	public final Lambda_without_argsContext lambda_without_args() throws RecognitionException {
		Lambda_without_argsContext _localctx = new Lambda_without_argsContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_lambda_without_args);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(336);
			match(LPAREN);
			setState(337);
			match(RPAREN);
			setState(338);
			match(LAMBDA_OP);
			setState(339);
			transformation_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Lambda_with_argsContext extends ParserRuleContext {
		public TerminalNode LPAREN() { return getToken(StellarParser.LPAREN, 0); }
		public Lambda_variablesContext lambda_variables() {
			return getRuleContext(Lambda_variablesContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(StellarParser.RPAREN, 0); }
		public TerminalNode LAMBDA_OP() { return getToken(StellarParser.LAMBDA_OP, 0); }
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public Single_lambda_variableContext single_lambda_variable() {
			return getRuleContext(Single_lambda_variableContext.class,0);
		}
		public Lambda_with_argsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambda_with_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLambda_with_args(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLambda_with_args(this);
		}
	}

	public final Lambda_with_argsContext lambda_with_args() throws RecognitionException {
		Lambda_with_argsContext _localctx = new Lambda_with_argsContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_lambda_with_args);
		try {
			setState(351);
			switch (_input.LA(1)) {
			case LPAREN:
				enterOuterAlt(_localctx, 1);
				{
				setState(341);
				match(LPAREN);
				setState(342);
				lambda_variables();
				setState(343);
				match(RPAREN);
				setState(344);
				match(LAMBDA_OP);
				setState(345);
				transformation_expr();
				}
				break;
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(347);
				single_lambda_variable();
				setState(348);
				match(LAMBDA_OP);
				setState(349);
				transformation_expr();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Lambda_variablesContext extends ParserRuleContext {
		public List<Lambda_variableContext> lambda_variable() {
			return getRuleContexts(Lambda_variableContext.class);
		}
		public Lambda_variableContext lambda_variable(int i) {
			return getRuleContext(Lambda_variableContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(StellarParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(StellarParser.COMMA, i);
		}
		public Lambda_variablesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambda_variables; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLambda_variables(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLambda_variables(this);
		}
	}

	public final Lambda_variablesContext lambda_variables() throws RecognitionException {
		Lambda_variablesContext _localctx = new Lambda_variablesContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_lambda_variables);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(353);
			lambda_variable();
			setState(358);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(354);
				match(COMMA);
				setState(355);
				lambda_variable();
				}
				}
				setState(360);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Single_lambda_variableContext extends ParserRuleContext {
		public Lambda_variableContext lambda_variable() {
			return getRuleContext(Lambda_variableContext.class,0);
		}
		public Single_lambda_variableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_single_lambda_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterSingle_lambda_variable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitSingle_lambda_variable(this);
		}
	}

	public final Single_lambda_variableContext single_lambda_variable() throws RecognitionException {
		Single_lambda_variableContext _localctx = new Single_lambda_variableContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_single_lambda_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(361);
			lambda_variable();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Lambda_variableContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(StellarParser.IDENTIFIER, 0); }
		public Lambda_variableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lambda_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterLambda_variable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitLambda_variable(this);
		}
	}

	public final Lambda_variableContext lambda_variable() throws RecognitionException {
		Lambda_variableContext _localctx = new Lambda_variableContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_lambda_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(363);
			match(IDENTIFIER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Match_exprContext extends ParserRuleContext {
		public Match_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_expr; }
	 
		public Match_exprContext() { }
		public void copyFrom(Match_exprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MatchClausesContext extends Match_exprContext {
		public TerminalNode MATCH() { return getToken(StellarParser.MATCH, 0); }
		public TerminalNode LBRACE() { return getToken(StellarParser.LBRACE, 0); }
		public Match_clausesContext match_clauses() {
			return getRuleContext(Match_clausesContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(StellarParser.COMMA, 0); }
		public TerminalNode DEFAULT() { return getToken(StellarParser.DEFAULT, 0); }
		public TerminalNode MATCH_ACTION() { return getToken(StellarParser.MATCH_ACTION, 0); }
		public Match_clause_actionContext match_clause_action() {
			return getRuleContext(Match_clause_actionContext.class,0);
		}
		public TerminalNode RBRACE() { return getToken(StellarParser.RBRACE, 0); }
		public MatchClausesContext(Match_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMatchClauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMatchClauses(this);
		}
	}

	public final Match_exprContext match_expr() throws RecognitionException {
		Match_exprContext _localctx = new Match_exprContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_match_expr);
		try {
			_localctx = new MatchClausesContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(365);
			match(MATCH);
			setState(366);
			match(LBRACE);
			setState(367);
			match_clauses();
			setState(368);
			match(COMMA);
			setState(369);
			match(DEFAULT);
			setState(370);
			match(MATCH_ACTION);
			setState(371);
			match_clause_action();
			setState(372);
			match(RBRACE);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Match_clausesContext extends ParserRuleContext {
		public List<Match_clauseContext> match_clause() {
			return getRuleContexts(Match_clauseContext.class);
		}
		public Match_clauseContext match_clause(int i) {
			return getRuleContext(Match_clauseContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(StellarParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(StellarParser.COMMA, i);
		}
		public Match_clausesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_clauses; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMatch_clauses(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMatch_clauses(this);
		}
	}

	public final Match_clausesContext match_clauses() throws RecognitionException {
		Match_clausesContext _localctx = new Match_clausesContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_match_clauses);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(374);
			match_clause();
			setState(379);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(375);
					match(COMMA);
					setState(376);
					match_clause();
					}
					} 
				}
				setState(381);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Match_clauseContext extends ParserRuleContext {
		public Match_clause_checkContext match_clause_check() {
			return getRuleContext(Match_clause_checkContext.class,0);
		}
		public TerminalNode MATCH_ACTION() { return getToken(StellarParser.MATCH_ACTION, 0); }
		public Match_clause_actionContext match_clause_action() {
			return getRuleContext(Match_clause_actionContext.class,0);
		}
		public Match_clauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMatch_clause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMatch_clause(this);
		}
	}

	public final Match_clauseContext match_clause() throws RecognitionException {
		Match_clauseContext _localctx = new Match_clauseContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_match_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(382);
			match_clause_check();
			setState(383);
			match(MATCH_ACTION);
			setState(384);
			match_clause_action();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Match_clause_actionContext extends ParserRuleContext {
		public Match_clause_actionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_clause_action; }
	 
		public Match_clause_actionContext() { }
		public void copyFrom(Match_clause_actionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MatchClauseActionContext extends Match_clause_actionContext {
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public MatchClauseActionContext(Match_clause_actionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMatchClauseAction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMatchClauseAction(this);
		}
	}

	public final Match_clause_actionContext match_clause_action() throws RecognitionException {
		Match_clause_actionContext _localctx = new Match_clause_actionContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_match_clause_action);
		try {
			_localctx = new MatchClauseActionContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(386);
			transformation_expr();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Match_clause_checkContext extends ParserRuleContext {
		public Match_clause_checkContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_clause_check; }
	 
		public Match_clause_checkContext() { }
		public void copyFrom(Match_clause_checkContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class MatchClauseCheckExprContext extends Match_clause_checkContext {
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public Conditional_exprContext conditional_expr() {
			return getRuleContext(Conditional_exprContext.class,0);
		}
		public MatchClauseCheckExprContext(Match_clause_checkContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterMatchClauseCheckExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitMatchClauseCheckExpr(this);
		}
	}

	public final Match_clause_checkContext match_clause_check() throws RecognitionException {
		Match_clause_checkContext _localctx = new Match_clause_checkContext(_ctx, getState());
		enterRule(_localctx, 70, RULE_match_clause_check);
		try {
			setState(390);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				_localctx = new MatchClauseCheckExprContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(388);
				logical_expr();
				}
				break;
			case 2:
				_localctx = new MatchClauseCheckExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(389);
				conditional_expr();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 12:
			return comparison_expr_sempred((Comparison_exprContext)_localctx, predIndex);
		case 16:
			return op_list_sempred((Op_listContext)_localctx, predIndex);
		case 18:
			return kv_list_sempred((Kv_listContext)_localctx, predIndex);
		case 20:
			return arithmetic_expr_sempred((Arithmetic_exprContext)_localctx, predIndex);
		case 21:
			return arithmetic_expr_mul_sempred((Arithmetic_expr_mulContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean comparison_expr_sempred(Comparison_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 4);
		}
		return true;
	}
	private boolean op_list_sempred(Op_listContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 5);
		case 2:
			return precpred(_ctx, 3);
		case 3:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean kv_list_sempred(Kv_listContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4:
			return precpred(_ctx, 2);
		case 5:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean arithmetic_expr_sempred(Arithmetic_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 6:
			return precpred(_ctx, 2);
		case 7:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean arithmetic_expr_mul_sempred(Arithmetic_expr_mulContext _localctx, int predIndex) {
		switch (predIndex) {
		case 8:
			return precpred(_ctx, 2);
		case 9:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3:\u018b\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3\\\n\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4p\n\4\3\5\3\5\3\5\3\5\5\5"+
		"v\n\5\3\6\3\6\3\6\3\6\5\6|\n\6\3\7\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\n"+
		"\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u0091\n\n\3\13\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\5\13\u009c\n\13\3\f\3\f\5\f\u00a0\n\f\3\r\3"+
		"\r\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u00aa\n\r\3\16\3\16\3\16\3\16\3\16\3\16"+
		"\3\16\3\16\3\16\3\16\3\16\5\16\u00b7\n\16\3\16\3\16\3\16\3\16\7\16\u00bd"+
		"\n\16\f\16\16\16\u00c0\13\16\3\17\3\17\3\20\3\20\3\21\3\21\3\21\3\21\3"+
		"\21\3\21\5\21\u00cc\n\21\3\22\3\22\3\22\3\22\5\22\u00d2\n\22\3\22\3\22"+
		"\3\22\3\22\3\22\3\22\3\22\3\22\3\22\7\22\u00dd\n\22\f\22\16\22\u00e0\13"+
		"\22\3\23\3\23\3\23\3\23\3\23\3\23\5\23\u00e8\n\23\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\5\24\u00f3\n\24\3\24\3\24\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\7\24\u0101\n\24\f\24\16\24\u0104\13\24"+
		"\3\25\3\25\3\25\3\25\3\25\3\25\5\25\u010c\n\25\3\26\3\26\3\26\3\26\3\26"+
		"\3\26\3\26\3\26\3\26\7\26\u0117\n\26\f\26\16\26\u011a\13\26\3\27\3\27"+
		"\3\27\3\27\3\27\3\27\3\27\3\27\3\27\7\27\u0125\n\27\f\27\16\27\u0128\13"+
		"\27\3\30\3\30\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3"+
		"\31\3\31\3\31\3\31\3\31\5\31\u013c\n\31\3\32\3\32\3\32\3\32\3\32\3\32"+
		"\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\3\32\5\32\u014f\n\32"+
		"\3\33\3\33\3\34\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35"+
		"\3\35\3\35\3\35\5\35\u0162\n\35\3\36\3\36\3\36\7\36\u0167\n\36\f\36\16"+
		"\36\u016a\13\36\3\37\3\37\3 \3 \3!\3!\3!\3!\3!\3!\3!\3!\3!\3\"\3\"\3\""+
		"\7\"\u017c\n\"\f\"\16\"\u017f\13\"\3#\3#\3#\3#\3$\3$\3%\3%\5%\u0189\n"+
		"%\3%\2\7\32\"&*,&\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62"+
		"\64\668:<>@BDFH\2\4\3\2\24\31\3\2\f\r\u01a4\2J\3\2\2\2\4[\3\2\2\2\6o\3"+
		"\2\2\2\bu\3\2\2\2\n{\3\2\2\2\f}\3\2\2\2\16\177\3\2\2\2\20\u0081\3\2\2"+
		"\2\22\u0090\3\2\2\2\24\u009b\3\2\2\2\26\u009f\3\2\2\2\30\u00a9\3\2\2\2"+
		"\32\u00b6\3\2\2\2\34\u00c1\3\2\2\2\36\u00c3\3\2\2\2 \u00cb\3\2\2\2\"\u00d1"+
		"\3\2\2\2$\u00e7\3\2\2\2&\u00f2\3\2\2\2(\u010b\3\2\2\2*\u010d\3\2\2\2,"+
		"\u011b\3\2\2\2.\u0129\3\2\2\2\60\u013b\3\2\2\2\62\u014e\3\2\2\2\64\u0150"+
		"\3\2\2\2\66\u0152\3\2\2\28\u0161\3\2\2\2:\u0163\3\2\2\2<\u016b\3\2\2\2"+
		">\u016d\3\2\2\2@\u016f\3\2\2\2B\u0178\3\2\2\2D\u0180\3\2\2\2F\u0184\3"+
		"\2\2\2H\u0188\3\2\2\2JK\5\4\3\2KL\7\2\2\3L\3\3\2\2\2M\\\5\22\n\2NO\7."+
		"\2\2OP\5\4\3\2PQ\7/\2\2Q\\\3\2\2\2R\\\5*\26\2S\\\5\34\17\2T\\\5\32\16"+
		"\2U\\\5\24\13\2V\\\5\30\r\2W\\\5@!\2X\\\5\6\4\2Y\\\5\b\5\2Z\\\5\n\6\2"+
		"[M\3\2\2\2[N\3\2\2\2[R\3\2\2\2[S\3\2\2\2[T\3\2\2\2[U\3\2\2\2[V\3\2\2\2"+
		"[W\3\2\2\2[X\3\2\2\2[Y\3\2\2\2[Z\3\2\2\2\\\5\3\2\2\2]^\7\67\2\2^_\7\16"+
		"\2\2_p\5\4\3\2`a\7\67\2\2ab\7\17\2\2bp\5\4\3\2cd\7\67\2\2de\7\20\2\2e"+
		"p\5\4\3\2fg\7\67\2\2gh\7\21\2\2hp\5\4\3\2ij\7\67\2\2jk\7\22\2\2kp\5\4"+
		"\3\2lm\7\67\2\2mn\7\23\2\2np\5\4\3\2o]\3\2\2\2o`\3\2\2\2oc\3\2\2\2of\3"+
		"\2\2\2oi\3\2\2\2ol\3\2\2\2p\7\3\2\2\2qr\7\'\2\2rv\7\67\2\2st\7%\2\2tv"+
		"\7\67\2\2uq\3\2\2\2us\3\2\2\2v\t\3\2\2\2wx\7\67\2\2x|\7\'\2\2yz\7\67\2"+
		"\2z|\7%\2\2{w\3\2\2\2{y\3\2\2\2|\13\3\2\2\2}~\5\24\13\2~\r\3\2\2\2\177"+
		"\u0080\5\4\3\2\u0080\17\3\2\2\2\u0081\u0082\5\4\3\2\u0082\21\3\2\2\2\u0083"+
		"\u0084\5\f\7\2\u0084\u0085\7\32\2\2\u0085\u0086\5\16\b\2\u0086\u0087\7"+
		"\33\2\2\u0087\u0088\5\20\t\2\u0088\u0091\3\2\2\2\u0089\u008a\7\34\2\2"+
		"\u008a\u008b\5\f\7\2\u008b\u008c\7\35\2\2\u008c\u008d\5\16\b\2\u008d\u008e"+
		"\7\36\2\2\u008e\u008f\5\20\t\2\u008f\u0091\3\2\2\2\u0090\u0083\3\2\2\2"+
		"\u0090\u0089\3\2\2\2\u0091\23\3\2\2\2\u0092\u0093\5\26\f\2\u0093\u0094"+
		"\7\t\2\2\u0094\u0095\5\24\13\2\u0095\u009c\3\2\2\2\u0096\u0097\5\26\f"+
		"\2\u0097\u0098\7\n\2\2\u0098\u0099\5\24\13\2\u0099\u009c\3\2\2\2\u009a"+
		"\u009c\5\26\f\2\u009b\u0092\3\2\2\2\u009b\u0096\3\2\2\2\u009b\u009a\3"+
		"\2\2\2\u009c\25\3\2\2\2\u009d\u00a0\5\32\16\2\u009e\u00a0\5\30\r\2\u009f"+
		"\u009d\3\2\2\2\u009f\u009e\3\2\2\2\u00a0\27\3\2\2\2\u00a1\u00a2\5\62\32"+
		"\2\u00a2\u00a3\7\3\2\2\u00a3\u00a4\5\26\f\2\u00a4\u00aa\3\2\2\2\u00a5"+
		"\u00a6\5\62\32\2\u00a6\u00a7\7\60\2\2\u00a7\u00a8\5\26\f\2\u00a8\u00aa"+
		"\3\2\2\2\u00a9\u00a1\3\2\2\2\u00a9\u00a5\3\2\2\2\u00aa\31\3\2\2\2\u00ab"+
		"\u00ac\b\16\1\2\u00ac\u00ad\7\13\2\2\u00ad\u00ae\7.\2\2\u00ae\u00af\5"+
		"\24\13\2\u00af\u00b0\7/\2\2\u00b0\u00b7\3\2\2\2\u00b1\u00b2\7.\2\2\u00b2"+
		"\u00b3\5\24\13\2\u00b3\u00b4\7/\2\2\u00b4\u00b7\3\2\2\2\u00b5\u00b7\5"+
		"\62\32\2\u00b6\u00ab\3\2\2\2\u00b6\u00b1\3\2\2\2\u00b6\u00b5\3\2\2\2\u00b7"+
		"\u00be\3\2\2\2\u00b8\u00b9\f\6\2\2\u00b9\u00ba\5\36\20\2\u00ba\u00bb\5"+
		"\32\16\7\u00bb\u00bd\3\2\2\2\u00bc\u00b8\3\2\2\2\u00bd\u00c0\3\2\2\2\u00be"+
		"\u00bc\3\2\2\2\u00be\u00bf\3\2\2\2\u00bf\33\3\2\2\2\u00c0\u00be\3\2\2"+
		"\2\u00c1\u00c2\5\62\32\2\u00c2\35\3\2\2\2\u00c3\u00c4\t\2\2\2\u00c4\37"+
		"\3\2\2\2\u00c5\u00c6\7.\2\2\u00c6\u00c7\5\"\22\2\u00c7\u00c8\7/\2\2\u00c8"+
		"\u00cc\3\2\2\2\u00c9\u00ca\7.\2\2\u00ca\u00cc\7/\2\2\u00cb\u00c5\3\2\2"+
		"\2\u00cb\u00c9\3\2\2\2\u00cc!\3\2\2\2\u00cd\u00ce\b\22\1\2\u00ce\u00d2"+
		"\5\62\32\2\u00cf\u00d2\5\22\n\2\u00d0\u00d2\5\32\16\2\u00d1\u00cd\3\2"+
		"\2\2\u00d1\u00cf\3\2\2\2\u00d1\u00d0\3\2\2\2\u00d2\u00de\3\2\2\2\u00d3"+
		"\u00d4\f\7\2\2\u00d4\u00d5\7\7\2\2\u00d5\u00dd\5\62\32\2\u00d6\u00d7\f"+
		"\5\2\2\u00d7\u00d8\7\7\2\2\u00d8\u00dd\5\22\n\2\u00d9\u00da\f\3\2\2\u00da"+
		"\u00db\7\7\2\2\u00db\u00dd\5\32\16\2\u00dc\u00d3\3\2\2\2\u00dc\u00d6\3"+
		"\2\2\2\u00dc\u00d9\3\2\2\2\u00dd\u00e0\3\2\2\2\u00de\u00dc\3\2\2\2\u00de"+
		"\u00df\3\2\2\2\u00df#\3\2\2\2\u00e0\u00de\3\2\2\2\u00e1\u00e2\7,\2\2\u00e2"+
		"\u00e8\7-\2\2\u00e3\u00e4\7,\2\2\u00e4\u00e5\5\"\22\2\u00e5\u00e6\7-\2"+
		"\2\u00e6\u00e8\3\2\2\2\u00e7\u00e1\3\2\2\2\u00e7\u00e3\3\2\2\2\u00e8%"+
		"\3\2\2\2\u00e9\u00ea\b\24\1\2\u00ea\u00eb\5\62\32\2\u00eb\u00ec\7\33\2"+
		"\2\u00ec\u00ed\5\4\3\2\u00ed\u00f3\3\2\2\2\u00ee\u00ef\5\32\16\2\u00ef"+
		"\u00f0\7\33\2\2\u00f0\u00f1\5\4\3\2\u00f1\u00f3\3\2\2\2\u00f2\u00e9\3"+
		"\2\2\2\u00f2\u00ee\3\2\2\2\u00f3\u0102\3\2\2\2\u00f4\u00f5\f\4\2\2\u00f5"+
		"\u00f6\7\7\2\2\u00f6\u00f7\5\62\32\2\u00f7\u00f8\7\33\2\2\u00f8\u00f9"+
		"\5\4\3\2\u00f9\u0101\3\2\2\2\u00fa\u00fb\f\3\2\2\u00fb\u00fc\7\7\2\2\u00fc"+
		"\u00fd\5\32\16\2\u00fd\u00fe\7\33\2\2\u00fe\u00ff\5\4\3\2\u00ff\u0101"+
		"\3\2\2\2\u0100\u00f4\3\2\2\2\u0100\u00fa\3\2\2\2\u0101\u0104\3\2\2\2\u0102"+
		"\u0100\3\2\2\2\u0102\u0103\3\2\2\2\u0103\'\3\2\2\2\u0104\u0102\3\2\2\2"+
		"\u0105\u0106\7*\2\2\u0106\u0107\5&\24\2\u0107\u0108\7+\2\2\u0108\u010c"+
		"\3\2\2\2\u0109\u010a\7*\2\2\u010a\u010c\7+\2\2\u010b\u0105\3\2\2\2\u010b"+
		"\u0109\3\2\2\2\u010c)\3\2\2\2\u010d\u010e\b\26\1\2\u010e\u010f\5,\27\2"+
		"\u010f\u0118\3\2\2\2\u0110\u0111\f\4\2\2\u0111\u0112\7&\2\2\u0112\u0117"+
		"\5,\27\2\u0113\u0114\f\3\2\2\u0114\u0115\7$\2\2\u0115\u0117\5,\27\2\u0116"+
		"\u0110\3\2\2\2\u0116\u0113\3\2\2\2\u0117\u011a\3\2\2\2\u0118\u0116\3\2"+
		"\2\2\u0118\u0119\3\2\2\2\u0119+\3\2\2\2\u011a\u0118\3\2\2\2\u011b\u011c"+
		"\b\27\1\2\u011c\u011d\5\60\31\2\u011d\u0126\3\2\2\2\u011e\u011f\f\4\2"+
		"\2\u011f\u0120\7)\2\2\u0120\u0125\5,\27\5\u0121\u0122\f\3\2\2\u0122\u0123"+
		"\7(\2\2\u0123\u0125\5,\27\4\u0124\u011e\3\2\2\2\u0124\u0121\3\2\2\2\u0125"+
		"\u0128\3\2\2\2\u0126\u0124\3\2\2\2\u0126\u0127\3\2\2\2\u0127-\3\2\2\2"+
		"\u0128\u0126\3\2\2\2\u0129\u012a\7\67\2\2\u012a\u012b\5 \21\2\u012b/\3"+
		"\2\2\2\u012c\u013c\5.\30\2\u012d\u013c\7\64\2\2\u012e\u013c\7\63\2\2\u012f"+
		"\u013c\7\66\2\2\u0130\u013c\7\65\2\2\u0131\u013c\7\67\2\2\u0132\u013c"+
		"\7 \2\2\u0133\u0134\7.\2\2\u0134\u0135\5*\26\2\u0135\u0136\7/\2\2\u0136"+
		"\u013c\3\2\2\2\u0137\u0138\7.\2\2\u0138\u0139\5\22\n\2\u0139\u013a\7/"+
		"\2\2\u013a\u013c\3\2\2\2\u013b\u012c\3\2\2\2\u013b\u012d\3\2\2\2\u013b"+
		"\u012e\3\2\2\2\u013b\u012f\3\2\2\2\u013b\u0130\3\2\2\2\u013b\u0131\3\2"+
		"\2\2\u013b\u0132\3\2\2\2\u013b\u0133\3\2\2\2\u013b\u0137\3\2\2\2\u013c"+
		"\61\3\2\2\2\u013d\u014f\t\3\2\2\u013e\u014f\58\35\2\u013f\u014f\5\66\34"+
		"\2\u0140\u014f\5*\26\2\u0141\u014f\78\2\2\u0142\u014f\5$\23\2\u0143\u014f"+
		"\5(\25\2\u0144\u014f\7\37\2\2\u0145\u0146\7\61\2\2\u0146\u0147\7.\2\2"+
		"\u0147\u0148\7\67\2\2\u0148\u014f\7/\2\2\u0149\u014a\7.\2\2\u014a\u014b"+
		"\5\22\n\2\u014b\u014c\7/\2\2\u014c\u014f\3\2\2\2\u014d\u014f\5.\30\2\u014e"+
		"\u013d\3\2\2\2\u014e\u013e\3\2\2\2\u014e\u013f\3\2\2\2\u014e\u0140\3\2"+
		"\2\2\u014e\u0141\3\2\2\2\u014e\u0142\3\2\2\2\u014e\u0143\3\2\2\2\u014e"+
		"\u0144\3\2\2\2\u014e\u0145\3\2\2\2\u014e\u0149\3\2\2\2\u014e\u014d\3\2"+
		"\2\2\u014f\63\3\2\2\2\u0150\u0151\7\"\2\2\u0151\65\3\2\2\2\u0152\u0153"+
		"\7.\2\2\u0153\u0154\7/\2\2\u0154\u0155\7\4\2\2\u0155\u0156\5\4\3\2\u0156"+
		"\67\3\2\2\2\u0157\u0158\7.\2\2\u0158\u0159\5:\36\2\u0159\u015a\7/\2\2"+
		"\u015a\u015b\7\4\2\2\u015b\u015c\5\4\3\2\u015c\u0162\3\2\2\2\u015d\u015e"+
		"\5<\37\2\u015e\u015f\7\4\2\2\u015f\u0160\5\4\3\2\u0160\u0162\3\2\2\2\u0161"+
		"\u0157\3\2\2\2\u0161\u015d\3\2\2\2\u01629\3\2\2\2\u0163\u0168\5> \2\u0164"+
		"\u0165\7\7\2\2\u0165\u0167\5> \2\u0166\u0164\3\2\2\2\u0167\u016a\3\2\2"+
		"\2\u0168\u0166\3\2\2\2\u0168\u0169\3\2\2\2\u0169;\3\2\2\2\u016a\u0168"+
		"\3\2\2\2\u016b\u016c\5> \2\u016c=\3\2\2\2\u016d\u016e\7\67\2\2\u016e?"+
		"\3\2\2\2\u016f\u0170\7!\2\2\u0170\u0171\7*\2\2\u0171\u0172\5B\"\2\u0172"+
		"\u0173\7\7\2\2\u0173\u0174\7\"\2\2\u0174\u0175\7#\2\2\u0175\u0176\5F$"+
		"\2\u0176\u0177\7+\2\2\u0177A\3\2\2\2\u0178\u017d\5D#\2\u0179\u017a\7\7"+
		"\2\2\u017a\u017c\5D#\2\u017b\u0179\3\2\2\2\u017c\u017f\3\2\2\2\u017d\u017b"+
		"\3\2\2\2\u017d\u017e\3\2\2\2\u017eC\3\2\2\2\u017f\u017d\3\2\2\2\u0180"+
		"\u0181\5H%\2\u0181\u0182\7#\2\2\u0182\u0183\5F$\2\u0183E\3\2\2\2\u0184"+
		"\u0185\5\4\3\2\u0185G\3\2\2\2\u0186\u0189\5\24\13\2\u0187\u0189\5\22\n"+
		"\2\u0188\u0186\3\2\2\2\u0188\u0187\3\2\2\2\u0189I\3\2\2\2\37[ou{\u0090"+
		"\u009b\u009f\u00a9\u00b6\u00be\u00cb\u00d1\u00dc\u00de\u00e7\u00f2\u0100"+
		"\u0102\u010b\u0116\u0118\u0124\u0126\u013b\u014e\u0161\u0168\u017d\u0188";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}