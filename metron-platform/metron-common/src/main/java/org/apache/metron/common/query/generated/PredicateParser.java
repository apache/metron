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

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class PredicateParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		AND=1, OR=2, NOT=3, TRUE=4, FALSE=5, EQ=6, NEQ=7, LT=8, LTE=9, GT=10, 
		GTE=11, COMMA=12, LBRACKET=13, RBRACKET=14, LPAREN=15, RPAREN=16, IN=17, 
		NIN=18, EXISTS=19, INT_LITERAL=20, DOUBLE_LITERAL=21, IDENTIFIER=22, STRING_LITERAL=23, 
		SEMI=24, COMMENT=25, WS=26;
	public static final int
		RULE_single_rule = 0, RULE_logical_expr = 1, RULE_comparison_expr = 2, 
		RULE_logical_entity = 3, RULE_list_entity = 4, RULE_func_args = 5, RULE_op_list = 6, 
		RULE_t_func = 7, RULE_identifier_operand = 8, RULE_comparison_operand = 9, 
		RULE_comp_operator = 10;
	public static final String[] ruleNames = {
		"single_rule", "logical_expr", "comparison_expr", "logical_entity", "list_entity", 
		"func_args", "op_list", "t_func", "identifier_operand", "comparison_operand", 
		"comp_operator"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, null, "'=='", "'!='", "'<'", "'<='", "'>'", 
		"'>='", "','", "'['", "']'", "'('", "')'", "'in'", "'not in'", "'exists'", 
		null, null, null, null, "';'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", 
		"GTE", "COMMA", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "IN", "NIN", 
		"EXISTS", "INT_LITERAL", "DOUBLE_LITERAL", "IDENTIFIER", "STRING_LITERAL", 
		"SEMI", "COMMENT", "WS"
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
	public String getGrammarFileName() { return "Predicate.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public PredicateParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class Single_ruleContext extends ParserRuleContext {
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public TerminalNode EOF() { return getToken(PredicateParser.EOF, 0); }
		public Single_ruleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_single_rule; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterSingle_rule(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitSingle_rule(this);
		}
	}

	public final Single_ruleContext single_rule() throws RecognitionException {
		Single_ruleContext _localctx = new Single_ruleContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_single_rule);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(22);
			logical_expr(0);
			setState(23);
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
	public static class LogicalEntityContext extends Logical_exprContext {
		public Logical_entityContext logical_entity() {
			return getRuleContext(Logical_entityContext.class,0);
		}
		public LogicalEntityContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalEntity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalEntity(this);
		}
	}
	public static class ComparisonExpressionContext extends Logical_exprContext {
		public Comparison_exprContext comparison_expr() {
			return getRuleContext(Comparison_exprContext.class,0);
		}
		public ComparisonExpressionContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterComparisonExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitComparisonExpression(this);
		}
	}
	public static class LogicalExpressionInParenContext extends Logical_exprContext {
		public TerminalNode LPAREN() { return getToken(PredicateParser.LPAREN, 0); }
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(PredicateParser.RPAREN, 0); }
		public LogicalExpressionInParenContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalExpressionInParen(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalExpressionInParen(this);
		}
	}
	public static class NotFuncContext extends Logical_exprContext {
		public TerminalNode NOT() { return getToken(PredicateParser.NOT, 0); }
		public TerminalNode LPAREN() { return getToken(PredicateParser.LPAREN, 0); }
		public Logical_exprContext logical_expr() {
			return getRuleContext(Logical_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(PredicateParser.RPAREN, 0); }
		public NotFuncContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterNotFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitNotFunc(this);
		}
	}
	public static class LogicalExpressionAndContext extends Logical_exprContext {
		public List<Logical_exprContext> logical_expr() {
			return getRuleContexts(Logical_exprContext.class);
		}
		public Logical_exprContext logical_expr(int i) {
			return getRuleContext(Logical_exprContext.class,i);
		}
		public TerminalNode AND() { return getToken(PredicateParser.AND, 0); }
		public LogicalExpressionAndContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalExpressionAnd(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalExpressionAnd(this);
		}
	}
	public static class LogicalExpressionOrContext extends Logical_exprContext {
		public List<Logical_exprContext> logical_expr() {
			return getRuleContexts(Logical_exprContext.class);
		}
		public Logical_exprContext logical_expr(int i) {
			return getRuleContext(Logical_exprContext.class,i);
		}
		public TerminalNode OR() { return getToken(PredicateParser.OR, 0); }
		public LogicalExpressionOrContext(Logical_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalExpressionOr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalExpressionOr(this);
		}
	}

	public final Logical_exprContext logical_expr() throws RecognitionException {
		return logical_expr(0);
	}

	private Logical_exprContext logical_expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Logical_exprContext _localctx = new Logical_exprContext(_ctx, _parentState);
		Logical_exprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_logical_expr, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(37);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				{
				_localctx = new ComparisonExpressionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(26);
				comparison_expr();
				}
				break;
			case 2:
				{
				_localctx = new LogicalExpressionInParenContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(27);
				match(LPAREN);
				setState(28);
				logical_expr(0);
				setState(29);
				match(RPAREN);
				}
				break;
			case 3:
				{
				_localctx = new NotFuncContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(31);
				match(NOT);
				setState(32);
				match(LPAREN);
				setState(33);
				logical_expr(0);
				setState(34);
				match(RPAREN);
				}
				break;
			case 4:
				{
				_localctx = new LogicalEntityContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(36);
				logical_entity();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(47);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(45);
					switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
					case 1:
						{
						_localctx = new LogicalExpressionAndContext(new Logical_exprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_logical_expr);
						setState(39);
						if (!(precpred(_ctx, 6))) throw new FailedPredicateException(this, "precpred(_ctx, 6)");
						setState(40);
						match(AND);
						setState(41);
						logical_expr(7);
						}
						break;
					case 2:
						{
						_localctx = new LogicalExpressionOrContext(new Logical_exprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_logical_expr);
						setState(42);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(43);
						match(OR);
						setState(44);
						logical_expr(6);
						}
						break;
					}
					} 
				}
				setState(49);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
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
	public static class ComparisonExpressionParensContext extends Comparison_exprContext {
		public TerminalNode LPAREN() { return getToken(PredicateParser.LPAREN, 0); }
		public Comparison_exprContext comparison_expr() {
			return getRuleContext(Comparison_exprContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(PredicateParser.RPAREN, 0); }
		public ComparisonExpressionParensContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterComparisonExpressionParens(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitComparisonExpressionParens(this);
		}
	}
	public static class InExpressionContext extends Comparison_exprContext {
		public List<Identifier_operandContext> identifier_operand() {
			return getRuleContexts(Identifier_operandContext.class);
		}
		public Identifier_operandContext identifier_operand(int i) {
			return getRuleContext(Identifier_operandContext.class,i);
		}
		public TerminalNode IN() { return getToken(PredicateParser.IN, 0); }
		public InExpressionContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterInExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitInExpression(this);
		}
	}
	public static class ComparisonExpressionWithOperatorContext extends Comparison_exprContext {
		public List<Comparison_operandContext> comparison_operand() {
			return getRuleContexts(Comparison_operandContext.class);
		}
		public Comparison_operandContext comparison_operand(int i) {
			return getRuleContext(Comparison_operandContext.class,i);
		}
		public Comp_operatorContext comp_operator() {
			return getRuleContext(Comp_operatorContext.class,0);
		}
		public ComparisonExpressionWithOperatorContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterComparisonExpressionWithOperator(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitComparisonExpressionWithOperator(this);
		}
	}
	public static class NInExpressionContext extends Comparison_exprContext {
		public List<Identifier_operandContext> identifier_operand() {
			return getRuleContexts(Identifier_operandContext.class);
		}
		public Identifier_operandContext identifier_operand(int i) {
			return getRuleContext(Identifier_operandContext.class,i);
		}
		public TerminalNode NIN() { return getToken(PredicateParser.NIN, 0); }
		public NInExpressionContext(Comparison_exprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterNInExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitNInExpression(this);
		}
	}

	public final Comparison_exprContext comparison_expr() throws RecognitionException {
		Comparison_exprContext _localctx = new Comparison_exprContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_comparison_expr);
		try {
			setState(66);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				_localctx = new ComparisonExpressionWithOperatorContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(50);
				comparison_operand();
				setState(51);
				comp_operator();
				setState(52);
				comparison_operand();
				}
				break;
			case 2:
				_localctx = new InExpressionContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(54);
				identifier_operand();
				setState(55);
				match(IN);
				setState(56);
				identifier_operand();
				}
				break;
			case 3:
				_localctx = new NInExpressionContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(58);
				identifier_operand();
				setState(59);
				match(NIN);
				setState(60);
				identifier_operand();
				}
				break;
			case 4:
				_localctx = new ComparisonExpressionParensContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(62);
				match(LPAREN);
				setState(63);
				comparison_expr();
				setState(64);
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

	public static class Logical_entityContext extends ParserRuleContext {
		public Logical_entityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_logical_entity; }
	 
		public Logical_entityContext() { }
		public void copyFrom(Logical_entityContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class LogicalFuncContext extends Logical_entityContext {
		public TerminalNode IDENTIFIER() { return getToken(PredicateParser.IDENTIFIER, 0); }
		public TerminalNode LPAREN() { return getToken(PredicateParser.LPAREN, 0); }
		public Func_argsContext func_args() {
			return getRuleContext(Func_argsContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(PredicateParser.RPAREN, 0); }
		public LogicalFuncContext(Logical_entityContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalFunc(this);
		}
	}
	public static class LogicalConstContext extends Logical_entityContext {
		public TerminalNode TRUE() { return getToken(PredicateParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(PredicateParser.FALSE, 0); }
		public LogicalConstContext(Logical_entityContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalConst(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalConst(this);
		}
	}
	public static class ExistsFuncContext extends Logical_entityContext {
		public TerminalNode EXISTS() { return getToken(PredicateParser.EXISTS, 0); }
		public TerminalNode LPAREN() { return getToken(PredicateParser.LPAREN, 0); }
		public TerminalNode IDENTIFIER() { return getToken(PredicateParser.IDENTIFIER, 0); }
		public TerminalNode RPAREN() { return getToken(PredicateParser.RPAREN, 0); }
		public ExistsFuncContext(Logical_entityContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterExistsFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitExistsFunc(this);
		}
	}

	public final Logical_entityContext logical_entity() throws RecognitionException {
		Logical_entityContext _localctx = new Logical_entityContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_logical_entity);
		int _la;
		try {
			setState(78);
			switch (_input.LA(1)) {
			case TRUE:
			case FALSE:
				_localctx = new LogicalConstContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(68);
				_la = _input.LA(1);
				if ( !(_la==TRUE || _la==FALSE) ) {
				_errHandler.recoverInline(this);
				} else {
					consume();
				}
				}
				break;
			case EXISTS:
				_localctx = new ExistsFuncContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(69);
				match(EXISTS);
				setState(70);
				match(LPAREN);
				setState(71);
				match(IDENTIFIER);
				setState(72);
				match(RPAREN);
				}
				break;
			case IDENTIFIER:
				_localctx = new LogicalFuncContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(73);
				match(IDENTIFIER);
				setState(74);
				match(LPAREN);
				setState(75);
				func_args();
				setState(76);
				match(RPAREN);
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

	public static class List_entityContext extends ParserRuleContext {
		public TerminalNode LBRACKET() { return getToken(PredicateParser.LBRACKET, 0); }
		public Op_listContext op_list() {
			return getRuleContext(Op_listContext.class,0);
		}
		public TerminalNode RBRACKET() { return getToken(PredicateParser.RBRACKET, 0); }
		public List_entityContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_list_entity; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterList_entity(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitList_entity(this);
		}
	}

	public final List_entityContext list_entity() throws RecognitionException {
		List_entityContext _localctx = new List_entityContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_list_entity);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(80);
			match(LBRACKET);
			setState(81);
			op_list(0);
			setState(82);
			match(RBRACKET);
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
		public Op_listContext op_list() {
			return getRuleContext(Op_listContext.class,0);
		}
		public Func_argsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_func_args; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterFunc_args(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitFunc_args(this);
		}
	}

	public final Func_argsContext func_args() throws RecognitionException {
		Func_argsContext _localctx = new Func_argsContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_func_args);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			op_list(0);
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
		public Op_listContext op_list() {
			return getRuleContext(Op_listContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(PredicateParser.COMMA, 0); }
		public Op_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_op_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterOp_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitOp_list(this);
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
		int _startState = 12;
		enterRecursionRule(_localctx, 12, RULE_op_list, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(87);
			identifier_operand();
			}
			_ctx.stop = _input.LT(-1);
			setState(94);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Op_listContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_op_list);
					setState(89);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(90);
					match(COMMA);
					setState(91);
					identifier_operand();
					}
					} 
				}
				setState(96);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
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

	public static class T_funcContext extends ParserRuleContext {
		public T_funcContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_t_func; }
	 
		public T_funcContext() { }
		public void copyFrom(T_funcContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TransformationFuncContext extends T_funcContext {
		public TerminalNode IDENTIFIER() { return getToken(PredicateParser.IDENTIFIER, 0); }
		public TerminalNode LPAREN() { return getToken(PredicateParser.LPAREN, 0); }
		public Func_argsContext func_args() {
			return getRuleContext(Func_argsContext.class,0);
		}
		public TerminalNode RPAREN() { return getToken(PredicateParser.RPAREN, 0); }
		public TransformationFuncContext(T_funcContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterTransformationFunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitTransformationFunc(this);
		}
	}

	public final T_funcContext t_func() throws RecognitionException {
		T_funcContext _localctx = new T_funcContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_t_func);
		try {
			_localctx = new TransformationFuncContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(97);
			match(IDENTIFIER);
			setState(98);
			match(LPAREN);
			setState(99);
			func_args();
			setState(100);
			match(RPAREN);
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
	public static class LogicalVariableContext extends Identifier_operandContext {
		public TerminalNode IDENTIFIER() { return getToken(PredicateParser.IDENTIFIER, 0); }
		public LogicalVariableContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalVariable(this);
		}
	}
	public static class StringLiteralContext extends Identifier_operandContext {
		public TerminalNode STRING_LITERAL() { return getToken(PredicateParser.STRING_LITERAL, 0); }
		public StringLiteralContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitStringLiteral(this);
		}
	}
	public static class ListContext extends Identifier_operandContext {
		public List_entityContext list_entity() {
			return getRuleContext(List_entityContext.class,0);
		}
		public ListContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitList(this);
		}
	}
	public static class DoubleLiteralContext extends Identifier_operandContext {
		public TerminalNode DOUBLE_LITERAL() { return getToken(PredicateParser.DOUBLE_LITERAL, 0); }
		public DoubleLiteralContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterDoubleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitDoubleLiteral(this);
		}
	}
	public static class Id_tfuncContext extends Identifier_operandContext {
		public T_funcContext t_func() {
			return getRuleContext(T_funcContext.class,0);
		}
		public Id_tfuncContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterId_tfunc(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitId_tfunc(this);
		}
	}
	public static class IntegerLiteralContext extends Identifier_operandContext {
		public TerminalNode INT_LITERAL() { return getToken(PredicateParser.INT_LITERAL, 0); }
		public IntegerLiteralContext(Identifier_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterIntegerLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitIntegerLiteral(this);
		}
	}

	public final Identifier_operandContext identifier_operand() throws RecognitionException {
		Identifier_operandContext _localctx = new Identifier_operandContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_identifier_operand);
		try {
			setState(108);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(102);
				match(STRING_LITERAL);
				}
				break;
			case 2:
				_localctx = new LogicalVariableContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(103);
				match(IDENTIFIER);
				}
				break;
			case 3:
				_localctx = new Id_tfuncContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(104);
				t_func();
				}
				break;
			case 4:
				_localctx = new IntegerLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(105);
				match(INT_LITERAL);
				}
				break;
			case 5:
				_localctx = new DoubleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(106);
				match(DOUBLE_LITERAL);
				}
				break;
			case 6:
				_localctx = new ListContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(107);
				list_entity();
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

	public static class Comparison_operandContext extends ParserRuleContext {
		public Comparison_operandContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comparison_operand; }
	 
		public Comparison_operandContext() { }
		public void copyFrom(Comparison_operandContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class LogicalConstComparisonContext extends Comparison_operandContext {
		public Logical_entityContext logical_entity() {
			return getRuleContext(Logical_entityContext.class,0);
		}
		public LogicalConstComparisonContext(Comparison_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterLogicalConstComparison(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitLogicalConstComparison(this);
		}
	}
	public static class IdentifierOperandContext extends Comparison_operandContext {
		public Identifier_operandContext identifier_operand() {
			return getRuleContext(Identifier_operandContext.class,0);
		}
		public IdentifierOperandContext(Comparison_operandContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterIdentifierOperand(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitIdentifierOperand(this);
		}
	}

	public final Comparison_operandContext comparison_operand() throws RecognitionException {
		Comparison_operandContext _localctx = new Comparison_operandContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_comparison_operand);
		try {
			setState(112);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				_localctx = new IdentifierOperandContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(110);
				identifier_operand();
				}
				break;
			case 2:
				_localctx = new LogicalConstComparisonContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(111);
				logical_entity();
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
		public TerminalNode EQ() { return getToken(PredicateParser.EQ, 0); }
		public TerminalNode NEQ() { return getToken(PredicateParser.NEQ, 0); }
		public TerminalNode LT() { return getToken(PredicateParser.LT, 0); }
		public TerminalNode LTE() { return getToken(PredicateParser.LTE, 0); }
		public TerminalNode GT() { return getToken(PredicateParser.GT, 0); }
		public TerminalNode GTE() { return getToken(PredicateParser.GTE, 0); }
		public ComparisonOpContext(Comp_operatorContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).enterComparisonOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof PredicateListener ) ((PredicateListener)listener).exitComparisonOp(this);
		}
	}

	public final Comp_operatorContext comp_operator() throws RecognitionException {
		Comp_operatorContext _localctx = new Comp_operatorContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_comp_operator);
		int _la;
		try {
			_localctx = new ComparisonOpContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(114);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return logical_expr_sempred((Logical_exprContext)_localctx, predIndex);
		case 6:
			return op_list_sempred((Op_listContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean logical_expr_sempred(Logical_exprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 6);
		case 1:
			return precpred(_ctx, 5);
		}
		return true;
	}
	private boolean op_list_sempred(Op_listContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\34w\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3"+
		"(\n\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3\60\n\3\f\3\16\3\63\13\3\3\4\3\4\3\4"+
		"\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4E\n\4\3\5\3\5"+
		"\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5Q\n\5\3\6\3\6\3\6\3\6\3\7\3\7\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\7\b_\n\b\f\b\16\bb\13\b\3\t\3\t\3\t\3\t\3\t\3\n\3"+
		"\n\3\n\3\n\3\n\3\n\5\no\n\n\3\13\3\13\5\13s\n\13\3\f\3\f\3\f\2\4\4\16"+
		"\r\2\4\6\b\n\f\16\20\22\24\26\2\4\3\2\6\7\3\2\b\r|\2\30\3\2\2\2\4\'\3"+
		"\2\2\2\6D\3\2\2\2\bP\3\2\2\2\nR\3\2\2\2\fV\3\2\2\2\16X\3\2\2\2\20c\3\2"+
		"\2\2\22n\3\2\2\2\24r\3\2\2\2\26t\3\2\2\2\30\31\5\4\3\2\31\32\7\2\2\3\32"+
		"\3\3\2\2\2\33\34\b\3\1\2\34(\5\6\4\2\35\36\7\21\2\2\36\37\5\4\3\2\37 "+
		"\7\22\2\2 (\3\2\2\2!\"\7\5\2\2\"#\7\21\2\2#$\5\4\3\2$%\7\22\2\2%(\3\2"+
		"\2\2&(\5\b\5\2\'\33\3\2\2\2\'\35\3\2\2\2\'!\3\2\2\2\'&\3\2\2\2(\61\3\2"+
		"\2\2)*\f\b\2\2*+\7\3\2\2+\60\5\4\3\t,-\f\7\2\2-.\7\4\2\2.\60\5\4\3\b/"+
		")\3\2\2\2/,\3\2\2\2\60\63\3\2\2\2\61/\3\2\2\2\61\62\3\2\2\2\62\5\3\2\2"+
		"\2\63\61\3\2\2\2\64\65\5\24\13\2\65\66\5\26\f\2\66\67\5\24\13\2\67E\3"+
		"\2\2\289\5\22\n\29:\7\23\2\2:;\5\22\n\2;E\3\2\2\2<=\5\22\n\2=>\7\24\2"+
		"\2>?\5\22\n\2?E\3\2\2\2@A\7\21\2\2AB\5\6\4\2BC\7\22\2\2CE\3\2\2\2D\64"+
		"\3\2\2\2D8\3\2\2\2D<\3\2\2\2D@\3\2\2\2E\7\3\2\2\2FQ\t\2\2\2GH\7\25\2\2"+
		"HI\7\21\2\2IJ\7\30\2\2JQ\7\22\2\2KL\7\30\2\2LM\7\21\2\2MN\5\f\7\2NO\7"+
		"\22\2\2OQ\3\2\2\2PF\3\2\2\2PG\3\2\2\2PK\3\2\2\2Q\t\3\2\2\2RS\7\17\2\2"+
		"ST\5\16\b\2TU\7\20\2\2U\13\3\2\2\2VW\5\16\b\2W\r\3\2\2\2XY\b\b\1\2YZ\5"+
		"\22\n\2Z`\3\2\2\2[\\\f\3\2\2\\]\7\16\2\2]_\5\22\n\2^[\3\2\2\2_b\3\2\2"+
		"\2`^\3\2\2\2`a\3\2\2\2a\17\3\2\2\2b`\3\2\2\2cd\7\30\2\2de\7\21\2\2ef\5"+
		"\f\7\2fg\7\22\2\2g\21\3\2\2\2ho\7\31\2\2io\7\30\2\2jo\5\20\t\2ko\7\26"+
		"\2\2lo\7\27\2\2mo\5\n\6\2nh\3\2\2\2ni\3\2\2\2nj\3\2\2\2nk\3\2\2\2nl\3"+
		"\2\2\2nm\3\2\2\2o\23\3\2\2\2ps\5\22\n\2qs\5\b\5\2rp\3\2\2\2rq\3\2\2\2"+
		"s\25\3\2\2\2tu\t\3\2\2u\27\3\2\2\2\n\'/\61DP`nr";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}