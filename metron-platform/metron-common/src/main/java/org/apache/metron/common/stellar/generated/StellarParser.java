// Generated from org/apache/metron/common/stellar/generated/Stellar.g4 by ANTLR 4.5
package org.apache.metron.common.stellar.generated;

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
		IN=1, REFERENCE_OP=2, DOUBLE_QUOTE=3, SINGLE_QUOTE=4, COMMA=5, PERIOD=6, 
		AND=7, OR=8, NOT=9, TRUE=10, FALSE=11, EQ=12, NEQ=13, LT=14, LTE=15, GT=16, 
		GTE=17, QUESTION=18, COLON=19, IF=20, THEN=21, ELSE=22, NULL=23, MINUS=24, 
		PLUS=25, DIV=26, MUL=27, LBRACE=28, RBRACE=29, LBRACKET=30, RBRACKET=31, 
		LPAREN=32, RPAREN=33, NIN=34, EXISTS=35, EXPONENT=36, INT_LITERAL=37, 
		DOUBLE_LITERAL=38, FLOAT_LITERAL=39, LONG_LITERAL=40, IDENTIFIER=41, STRING_LITERAL=42, 
		COMMENT=43, WS=44;
	public static final int
		RULE_transformation = 0, RULE_transformation_expr = 1, RULE_conditional_expr = 2, 
		RULE_logical_expr = 3, RULE_b_expr = 4, RULE_in_expr = 5, RULE_comparison_expr = 6, 
		RULE_transformation_entity = 7, RULE_comp_operator = 8, RULE_func_args = 9, 
		RULE_op_list = 10, RULE_list_entity = 11, RULE_kv_list = 12, RULE_map_entity = 13, 
		RULE_arithmetic_expr = 14, RULE_arithmetic_expr_mul = 15, RULE_functions = 16, 
		RULE_arithmetic_operands = 17, RULE_identifier_operand = 18, RULE_ref_expr = 19;
	public static final String[] ruleNames = {
		"transformation", "transformation_expr", "conditional_expr", "logical_expr", 
		"b_expr", "in_expr", "comparison_expr", "transformation_entity", "comp_operator", 
		"func_args", "op_list", "list_entity", "kv_list", "map_entity", "arithmetic_expr", 
		"arithmetic_expr_mul", "functions", "arithmetic_operands", "identifier_operand", 
		"ref_expr"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'&'", "'\"'", "'''", "','", "'.'", null, null, null, null, 
		null, "'=='", "'!='", "'<'", "'<='", "'>'", "'>='", "'?'", "':'", null, 
		null, null, null, "'-'", "'+'", "'/'", "'*'", "'{'", "'}'", "'['", "']'", 
		"'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "IN", "REFERENCE_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "MINUS", "PLUS", "DIV", 
		"MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", 
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
			setState(40);
			transformation_expr();
			setState(41);
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

	public final Transformation_exprContext transformation_expr() throws RecognitionException {
		Transformation_exprContext _localctx = new Transformation_exprContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_transformation_expr);
		try {
			setState(53);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				_localctx = new ConditionalExprContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(43);
				conditional_expr();
				}
				break;
			case 2:
				_localctx = new TransformationExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(44);
				match(LPAREN);
				setState(45);
				transformation_expr();
				setState(46);
				match(RPAREN);
				}
				break;
			case 3:
				_localctx = new ArithExpressionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(48);
				arithmetic_expr(0);
				}
				break;
			case 4:
				_localctx = new TransformationEntityContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(49);
				transformation_entity();
				}
				break;
			case 5:
				_localctx = new ComparisonExpressionContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(50);
				comparison_expr(0);
				}
				break;
			case 6:
				_localctx = new LogicalExpressionContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(51);
				logical_expr();
				}
				break;
			case 7:
				_localctx = new InExpressionContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(52);
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
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public TerminalNode QUESTION() { return getToken(StellarParser.QUESTION, 0); }
		public List<Transformation_exprContext> transformation_expr() {
			return getRuleContexts(Transformation_exprContext.class);
		}
		public Transformation_exprContext transformation_expr(int i) {
			return getRuleContext(Transformation_exprContext.class,i);
		}
		public TerminalNode COLON() { return getToken(StellarParser.COLON, 0); }
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
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public TerminalNode THEN() { return getToken(StellarParser.THEN, 0); }
		public List<Transformation_exprContext> transformation_expr() {
			return getRuleContexts(Transformation_exprContext.class);
		}
		public Transformation_exprContext transformation_expr(int i) {
			return getRuleContext(Transformation_exprContext.class,i);
		}
		public TerminalNode ELSE() { return getToken(StellarParser.ELSE, 0); }
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
		enterRule(_localctx, 4, RULE_conditional_expr);
		try {
			setState(68);
			switch (_input.LA(1)) {
			case REFERENCE_OP:
			case NOT:
			case TRUE:
			case FALSE:
			case NULL:
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
				setState(55);
				logical_expr();
				setState(56);
				match(QUESTION);
				setState(57);
				transformation_expr();
				setState(58);
				match(COLON);
				setState(59);
				transformation_expr();
				}
				break;
			case IF:
				_localctx = new TernaryFuncWithIfContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(61);
				match(IF);
				setState(62);
				logical_expr();
				setState(63);
				match(THEN);
				setState(64);
				transformation_expr();
				setState(65);
				match(ELSE);
				setState(66);
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
		enterRule(_localctx, 6, RULE_logical_expr);
		try {
			setState(79);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				_localctx = new LogicalExpressionAndContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(70);
				b_expr();
				setState(71);
				match(AND);
				setState(72);
				logical_expr();
				}
				break;
			case 2:
				_localctx = new LogicalExpressionOrContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(74);
				b_expr();
				setState(75);
				match(OR);
				setState(76);
				logical_expr();
				}
				break;
			case 3:
				_localctx = new BoleanExpressionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(78);
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
		enterRule(_localctx, 8, RULE_b_expr);
		try {
			setState(83);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(81);
				comparison_expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(82);
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
		enterRule(_localctx, 10, RULE_in_expr);
		try {
			setState(93);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				_localctx = new InExpressionStatementContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(85);
				identifier_operand();
				setState(86);
				match(IN);
				setState(87);
				b_expr();
				}
				break;
			case 2:
				_localctx = new NInExpressionStatementContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(89);
				identifier_operand();
				setState(90);
				match(NIN);
				setState(91);
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
		int _startState = 12;
		enterRecursionRule(_localctx, 12, RULE_comparison_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(106);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				_localctx = new NotFuncContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(96);
				match(NOT);
				setState(97);
				match(LPAREN);
				setState(98);
				logical_expr();
				setState(99);
				match(RPAREN);
				}
				break;
			case 2:
				{
				_localctx = new ComparisonExpressionParensContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(101);
				match(LPAREN);
				setState(102);
				logical_expr();
				setState(103);
				match(RPAREN);
				}
				break;
			case 3:
				{
				_localctx = new OperandContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(105);
				identifier_operand();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(114);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ComparisonExpressionWithOperatorContext(new Comparison_exprContext(_parentctx, _parentState));
					pushNewRecursionContext(_localctx, _startState, RULE_comparison_expr);
					setState(108);
					if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
					setState(109);
					comp_operator();
					setState(110);
					comparison_expr(5);
					}
					} 
				}
				setState(116);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
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
		enterRule(_localctx, 14, RULE_transformation_entity);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(117);
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
		enterRule(_localctx, 16, RULE_comp_operator);
		int _la;
		try {
			_localctx = new ComparisonOpContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
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
		enterRule(_localctx, 18, RULE_func_args);
		try {
			setState(127);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(121);
				match(LPAREN);
				setState(122);
				op_list(0);
				setState(123);
				match(RPAREN);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(125);
				match(LPAREN);
				setState(126);
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
		int _startState = 20;
		enterRecursionRule(_localctx, 20, RULE_op_list, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(132);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(130);
				identifier_operand();
				}
				break;
			case 2:
				{
				setState(131);
				conditional_expr();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(142);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(140);
					switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
					case 1:
						{
						_localctx = new Op_listContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_op_list);
						setState(134);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(135);
						match(COMMA);
						setState(136);
						identifier_operand();
						}
						break;
					case 2:
						{
						_localctx = new Op_listContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_op_list);
						setState(137);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(138);
						match(COMMA);
						setState(139);
						conditional_expr();
						}
						break;
					}
					} 
				}
				setState(144);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,10,_ctx);
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
		enterRule(_localctx, 22, RULE_list_entity);
		try {
			setState(151);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(145);
				match(LBRACKET);
				setState(146);
				match(RBRACKET);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(147);
				match(LBRACKET);
				setState(148);
				op_list(0);
				setState(149);
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
		int _startState = 24;
		enterRecursionRule(_localctx, 24, RULE_kv_list, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(154);
			identifier_operand();
			setState(155);
			match(COLON);
			setState(156);
			transformation_expr();
			}
			_ctx.stop = _input.LT(-1);
			setState(166);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Kv_listContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_kv_list);
					setState(158);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(159);
					match(COMMA);
					setState(160);
					identifier_operand();
					setState(161);
					match(COLON);
					setState(162);
					transformation_expr();
					}
					} 
				}
				setState(168);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
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
		enterRule(_localctx, 26, RULE_map_entity);
		try {
			setState(175);
			switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(169);
				match(LBRACE);
				setState(170);
				kv_list(0);
				setState(171);
				match(RBRACE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(173);
				match(LBRACE);
				setState(174);
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
		int _startState = 28;
		enterRecursionRule(_localctx, 28, RULE_arithmetic_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ArithExpr_soloContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(178);
			arithmetic_expr_mul(0);
			}
			_ctx.stop = _input.LT(-1);
			setState(188);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(186);
					switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
					case 1:
						{
						_localctx = new ArithExpr_plusContext(new Arithmetic_exprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr);
						setState(180);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(181);
						match(PLUS);
						setState(182);
						arithmetic_expr_mul(0);
						}
						break;
					case 2:
						{
						_localctx = new ArithExpr_minusContext(new Arithmetic_exprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr);
						setState(183);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(184);
						match(MINUS);
						setState(185);
						arithmetic_expr_mul(0);
						}
						break;
					}
					} 
				}
				setState(190);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
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
		int _startState = 30;
		enterRecursionRule(_localctx, 30, RULE_arithmetic_expr_mul, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			_localctx = new ArithExpr_mul_soloContext(_localctx);
			_ctx = _localctx;
			_prevctx = _localctx;

			setState(192);
			arithmetic_operands();
			}
			_ctx.stop = _input.LT(-1);
			setState(202);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,17,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(200);
					switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
					case 1:
						{
						_localctx = new ArithExpr_mulContext(new Arithmetic_expr_mulContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr_mul);
						setState(194);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(195);
						match(MUL);
						setState(196);
						arithmetic_expr_mul(3);
						}
						break;
					case 2:
						{
						_localctx = new ArithExpr_divContext(new Arithmetic_expr_mulContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_arithmetic_expr_mul);
						setState(197);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(198);
						match(DIV);
						setState(199);
						arithmetic_expr_mul(2);
						}
						break;
					}
					} 
				}
				setState(204);
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
		enterRule(_localctx, 32, RULE_functions);
		try {
			_localctx = new TransformationFuncContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(205);
			match(IDENTIFIER);
			setState(206);
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
		enterRule(_localctx, 34, RULE_arithmetic_operands);
		try {
			setState(222);
			switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
			case 1:
				_localctx = new NumericFunctionsContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(208);
				functions();
				}
				break;
			case 2:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(209);
				match(DOUBLE_LITERAL);
				}
				break;
			case 3:
				_localctx = new IntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(210);
				match(INT_LITERAL);
				}
				break;
			case 4:
				_localctx = new LongLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(211);
				match(LONG_LITERAL);
				}
				break;
			case 5:
				_localctx = new FloatLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(212);
				match(FLOAT_LITERAL);
				}
				break;
			case 6:
				_localctx = new VariableContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(213);
				match(IDENTIFIER);
				}
				break;
			case 7:
				_localctx = new ParenArithContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(214);
				match(LPAREN);
				setState(215);
				arithmetic_expr(0);
				setState(216);
				match(RPAREN);
				}
				break;
			case 8:
				_localctx = new CondExprContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(218);
				match(LPAREN);
				setState(219);
				conditional_expr();
				setState(220);
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
	public static class DereferenceExprContext extends Identifier_operandContext {
		public TerminalNode REFERENCE_OP() { return getToken(StellarParser.REFERENCE_OP, 0); }
		public Ref_exprContext ref_expr() {
			return getRuleContext(Ref_exprContext.class,0);
		}
		public DereferenceExprContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterDereferenceExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitDereferenceExpr(this);
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
		enterRule(_localctx, 36, RULE_identifier_operand);
		int _la;
		try {
			setState(240);
			switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
			case 1:
				_localctx = new LogicalConstContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(224);
				_la = _input.LA(1);
				if ( !(_la==TRUE || _la==FALSE) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
				break;
			case 2:
				_localctx = new DereferenceExprContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(225);
				match(REFERENCE_OP);
				setState(226);
				ref_expr();
				}
				break;
			case 3:
				_localctx = new ArithmeticOperandsContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(227);
				arithmetic_expr(0);
				}
				break;
			case 4:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(228);
				match(STRING_LITERAL);
				}
				break;
			case 5:
				_localctx = new ListContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(229);
				list_entity();
				}
				break;
			case 6:
				_localctx = new MapConstContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(230);
				map_entity();
				}
				break;
			case 7:
				_localctx = new NullConstContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(231);
				match(NULL);
				}
				break;
			case 8:
				_localctx = new ExistsFuncContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(232);
				match(EXISTS);
				setState(233);
				match(LPAREN);
				setState(234);
				match(IDENTIFIER);
				setState(235);
				match(RPAREN);
				}
				break;
			case 9:
				_localctx = new CondExpr_parenContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(236);
				match(LPAREN);
				setState(237);
				conditional_expr();
				setState(238);
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

	public static class Ref_exprContext extends ParserRuleContext {
		public Transformation_exprContext transformation_expr() {
			return getRuleContext(Transformation_exprContext.class,0);
		}
		public Ref_exprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ref_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).enterRef_expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof StellarListener ) ((StellarListener)listener).exitRef_expr(this);
		}
	}

	public final Ref_exprContext ref_expr() throws RecognitionException {
		Ref_exprContext _localctx = new Ref_exprContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_ref_expr);
		try {
			setState(244);
			switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(243);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 6:
			return comparison_expr_sempred((Comparison_exprContext)_localctx, predIndex);
		case 10:
			return op_list_sempred((Op_listContext)_localctx, predIndex);
		case 12:
			return kv_list_sempred((Kv_listContext)_localctx, predIndex);
		case 14:
			return arithmetic_expr_sempred((Arithmetic_exprContext)_localctx, predIndex);
		case 15:
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
			return precpred(_ctx, 3);
		case 2:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean kv_list_sempred(Kv_listContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean arithmetic_expr_sempred(Arithmetic_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4:
			return precpred(_ctx, 2);
		case 5:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean arithmetic_expr_mul_sempred(Arithmetic_expr_mulContext _localctx, int predIndex) {
		switch (predIndex) {
		case 6:
			return precpred(_ctx, 2);
		case 7:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3.\u00f9\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\5\38\n\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\5\4G\n\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5R\n\5\3\6\3\6\5\6"+
		"V\n\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7`\n\7\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\5\bm\n\b\3\b\3\b\3\b\3\b\7\bs\n\b\f\b\16\bv\13\b"+
		"\3\t\3\t\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u0082\n\13\3\f\3\f"+
		"\3\f\5\f\u0087\n\f\3\f\3\f\3\f\3\f\3\f\3\f\7\f\u008f\n\f\f\f\16\f\u0092"+
		"\13\f\3\r\3\r\3\r\3\r\3\r\3\r\5\r\u009a\n\r\3\16\3\16\3\16\3\16\3\16\3"+
		"\16\3\16\3\16\3\16\3\16\3\16\7\16\u00a7\n\16\f\16\16\16\u00aa\13\16\3"+
		"\17\3\17\3\17\3\17\3\17\3\17\5\17\u00b2\n\17\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\20\7\20\u00bd\n\20\f\20\16\20\u00c0\13\20\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\21\3\21\7\21\u00cb\n\21\f\21\16\21\u00ce\13"+
		"\21\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3"+
		"\23\3\23\3\23\3\23\5\23\u00e1\n\23\3\24\3\24\3\24\3\24\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\3\24\5\24\u00f3\n\24\3\25\3\25"+
		"\5\25\u00f7\n\25\3\25\2\7\16\26\32\36 \26\2\4\6\b\n\f\16\20\22\24\26\30"+
		"\32\34\36 \"$&(\2\4\3\2\16\23\3\2\f\r\u010d\2*\3\2\2\2\4\67\3\2\2\2\6"+
		"F\3\2\2\2\bQ\3\2\2\2\nU\3\2\2\2\f_\3\2\2\2\16l\3\2\2\2\20w\3\2\2\2\22"+
		"y\3\2\2\2\24\u0081\3\2\2\2\26\u0086\3\2\2\2\30\u0099\3\2\2\2\32\u009b"+
		"\3\2\2\2\34\u00b1\3\2\2\2\36\u00b3\3\2\2\2 \u00c1\3\2\2\2\"\u00cf\3\2"+
		"\2\2$\u00e0\3\2\2\2&\u00f2\3\2\2\2(\u00f6\3\2\2\2*+\5\4\3\2+,\7\2\2\3"+
		",\3\3\2\2\2-8\5\6\4\2./\7\"\2\2/\60\5\4\3\2\60\61\7#\2\2\618\3\2\2\2\62"+
		"8\5\36\20\2\638\5\20\t\2\648\5\16\b\2\658\5\b\5\2\668\5\f\7\2\67-\3\2"+
		"\2\2\67.\3\2\2\2\67\62\3\2\2\2\67\63\3\2\2\2\67\64\3\2\2\2\67\65\3\2\2"+
		"\2\67\66\3\2\2\28\5\3\2\2\29:\5\b\5\2:;\7\24\2\2;<\5\4\3\2<=\7\25\2\2"+
		"=>\5\4\3\2>G\3\2\2\2?@\7\26\2\2@A\5\b\5\2AB\7\27\2\2BC\5\4\3\2CD\7\30"+
		"\2\2DE\5\4\3\2EG\3\2\2\2F9\3\2\2\2F?\3\2\2\2G\7\3\2\2\2HI\5\n\6\2IJ\7"+
		"\t\2\2JK\5\b\5\2KR\3\2\2\2LM\5\n\6\2MN\7\n\2\2NO\5\b\5\2OR\3\2\2\2PR\5"+
		"\n\6\2QH\3\2\2\2QL\3\2\2\2QP\3\2\2\2R\t\3\2\2\2SV\5\16\b\2TV\5\f\7\2U"+
		"S\3\2\2\2UT\3\2\2\2V\13\3\2\2\2WX\5&\24\2XY\7\3\2\2YZ\5\n\6\2Z`\3\2\2"+
		"\2[\\\5&\24\2\\]\7$\2\2]^\5\n\6\2^`\3\2\2\2_W\3\2\2\2_[\3\2\2\2`\r\3\2"+
		"\2\2ab\b\b\1\2bc\7\13\2\2cd\7\"\2\2de\5\b\5\2ef\7#\2\2fm\3\2\2\2gh\7\""+
		"\2\2hi\5\b\5\2ij\7#\2\2jm\3\2\2\2km\5&\24\2la\3\2\2\2lg\3\2\2\2lk\3\2"+
		"\2\2mt\3\2\2\2no\f\6\2\2op\5\22\n\2pq\5\16\b\7qs\3\2\2\2rn\3\2\2\2sv\3"+
		"\2\2\2tr\3\2\2\2tu\3\2\2\2u\17\3\2\2\2vt\3\2\2\2wx\5&\24\2x\21\3\2\2\2"+
		"yz\t\2\2\2z\23\3\2\2\2{|\7\"\2\2|}\5\26\f\2}~\7#\2\2~\u0082\3\2\2\2\177"+
		"\u0080\7\"\2\2\u0080\u0082\7#\2\2\u0081{\3\2\2\2\u0081\177\3\2\2\2\u0082"+
		"\25\3\2\2\2\u0083\u0084\b\f\1\2\u0084\u0087\5&\24\2\u0085\u0087\5\6\4"+
		"\2\u0086\u0083\3\2\2\2\u0086\u0085\3\2\2\2\u0087\u0090\3\2\2\2\u0088\u0089"+
		"\f\5\2\2\u0089\u008a\7\7\2\2\u008a\u008f\5&\24\2\u008b\u008c\f\3\2\2\u008c"+
		"\u008d\7\7\2\2\u008d\u008f\5\6\4\2\u008e\u0088\3\2\2\2\u008e\u008b\3\2"+
		"\2\2\u008f\u0092\3\2\2\2\u0090\u008e\3\2\2\2\u0090\u0091\3\2\2\2\u0091"+
		"\27\3\2\2\2\u0092\u0090\3\2\2\2\u0093\u0094\7 \2\2\u0094\u009a\7!\2\2"+
		"\u0095\u0096\7 \2\2\u0096\u0097\5\26\f\2\u0097\u0098\7!\2\2\u0098\u009a"+
		"\3\2\2\2\u0099\u0093\3\2\2\2\u0099\u0095\3\2\2\2\u009a\31\3\2\2\2\u009b"+
		"\u009c\b\16\1\2\u009c\u009d\5&\24\2\u009d\u009e\7\25\2\2\u009e\u009f\5"+
		"\4\3\2\u009f\u00a8\3\2\2\2\u00a0\u00a1\f\3\2\2\u00a1\u00a2\7\7\2\2\u00a2"+
		"\u00a3\5&\24\2\u00a3\u00a4\7\25\2\2\u00a4\u00a5\5\4\3\2\u00a5\u00a7\3"+
		"\2\2\2\u00a6\u00a0\3\2\2\2\u00a7\u00aa\3\2\2\2\u00a8\u00a6\3\2\2\2\u00a8"+
		"\u00a9\3\2\2\2\u00a9\33\3\2\2\2\u00aa\u00a8\3\2\2\2\u00ab\u00ac\7\36\2"+
		"\2\u00ac\u00ad\5\32\16\2\u00ad\u00ae\7\37\2\2\u00ae\u00b2\3\2\2\2\u00af"+
		"\u00b0\7\36\2\2\u00b0\u00b2\7\37\2\2\u00b1\u00ab\3\2\2\2\u00b1\u00af\3"+
		"\2\2\2\u00b2\35\3\2\2\2\u00b3\u00b4\b\20\1\2\u00b4\u00b5\5 \21\2\u00b5"+
		"\u00be\3\2\2\2\u00b6\u00b7\f\4\2\2\u00b7\u00b8\7\33\2\2\u00b8\u00bd\5"+
		" \21\2\u00b9\u00ba\f\3\2\2\u00ba\u00bb\7\32\2\2\u00bb\u00bd\5 \21\2\u00bc"+
		"\u00b6\3\2\2\2\u00bc\u00b9\3\2\2\2\u00bd\u00c0\3\2\2\2\u00be\u00bc\3\2"+
		"\2\2\u00be\u00bf\3\2\2\2\u00bf\37\3\2\2\2\u00c0\u00be\3\2\2\2\u00c1\u00c2"+
		"\b\21\1\2\u00c2\u00c3\5$\23\2\u00c3\u00cc\3\2\2\2\u00c4\u00c5\f\4\2\2"+
		"\u00c5\u00c6\7\35\2\2\u00c6\u00cb\5 \21\5\u00c7\u00c8\f\3\2\2\u00c8\u00c9"+
		"\7\34\2\2\u00c9\u00cb\5 \21\4\u00ca\u00c4\3\2\2\2\u00ca\u00c7\3\2\2\2"+
		"\u00cb\u00ce\3\2\2\2\u00cc\u00ca\3\2\2\2\u00cc\u00cd\3\2\2\2\u00cd!\3"+
		"\2\2\2\u00ce\u00cc\3\2\2\2\u00cf\u00d0\7+\2\2\u00d0\u00d1\5\24\13\2\u00d1"+
		"#\3\2\2\2\u00d2\u00e1\5\"\22\2\u00d3\u00e1\7(\2\2\u00d4\u00e1\7\'\2\2"+
		"\u00d5\u00e1\7*\2\2\u00d6\u00e1\7)\2\2\u00d7\u00e1\7+\2\2\u00d8\u00d9"+
		"\7\"\2\2\u00d9\u00da\5\36\20\2\u00da\u00db\7#\2\2\u00db\u00e1\3\2\2\2"+
		"\u00dc\u00dd\7\"\2\2\u00dd\u00de\5\6\4\2\u00de\u00df\7#\2\2\u00df\u00e1"+
		"\3\2\2\2\u00e0\u00d2\3\2\2\2\u00e0\u00d3\3\2\2\2\u00e0\u00d4\3\2\2\2\u00e0"+
		"\u00d5\3\2\2\2\u00e0\u00d6\3\2\2\2\u00e0\u00d7\3\2\2\2\u00e0\u00d8\3\2"+
		"\2\2\u00e0\u00dc\3\2\2\2\u00e1%\3\2\2\2\u00e2\u00f3\t\3\2\2\u00e3\u00e4"+
		"\7\4\2\2\u00e4\u00f3\5(\25\2\u00e5\u00f3\5\36\20\2\u00e6\u00f3\7,\2\2"+
		"\u00e7\u00f3\5\30\r\2\u00e8\u00f3\5\34\17\2\u00e9\u00f3\7\31\2\2\u00ea"+
		"\u00eb\7%\2\2\u00eb\u00ec\7\"\2\2\u00ec\u00ed\7+\2\2\u00ed\u00f3\7#\2"+
		"\2\u00ee\u00ef\7\"\2\2\u00ef\u00f0\5\6\4\2\u00f0\u00f1\7#\2\2\u00f1\u00f3"+
		"\3\2\2\2\u00f2\u00e2\3\2\2\2\u00f2\u00e3\3\2\2\2\u00f2\u00e5\3\2\2\2\u00f2"+
		"\u00e6\3\2\2\2\u00f2\u00e7\3\2\2\2\u00f2\u00e8\3\2\2\2\u00f2\u00e9\3\2"+
		"\2\2\u00f2\u00ea\3\2\2\2\u00f2\u00ee\3\2\2\2\u00f3\'\3\2\2\2\u00f4\u00f7"+
		"\3\2\2\2\u00f5\u00f7\5\4\3\2\u00f6\u00f4\3\2\2\2\u00f6\u00f5\3\2\2\2\u00f7"+
		")\3\2\2\2\27\67FQU_lt\u0081\u0086\u008e\u0090\u0099\u00a8\u00b1\u00bc"+
		"\u00be\u00ca\u00cc\u00e0\u00f2\u00f6";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}