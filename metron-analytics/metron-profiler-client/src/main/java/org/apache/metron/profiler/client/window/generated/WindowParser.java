// Generated from org\apache\metron\profiler\client\window\generated\Window.g4 by ANTLR 4.5
package org.apache.metron.profiler.client.window.generated;

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
public class WindowParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMA=1, COLON=2, WINDOW=3, INCLUDE=4, EXCLUDE=5, FROM=6, EVERY=7, TO=8, 
		AGO=9, NUMBER=10, IDENTIFIER=11, DAY_SPECIFIER=12, TIME_UNIT=13, WS=14;
	public static final int
		RULE_window = 0, RULE_window_expression = 1, RULE_excluding_specifier = 2, 
		RULE_including_specifier = 3, RULE_specifier = 4, RULE_specifier_arg_list = 5, 
		RULE_day_specifier = 6, RULE_identifier = 7, RULE_specifier_list = 8, 
		RULE_duration = 9, RULE_skip_distance = 10, RULE_window_width = 11, RULE_time_interval = 12, 
		RULE_time_amount = 13, RULE_time_unit = 14;
	public static final String[] ruleNames = {
		"window", "window_expression", "excluding_specifier", "including_specifier", 
		"specifier", "specifier_arg_list", "day_specifier", "identifier", "specifier_list", 
		"duration", "skip_distance", "window_width", "time_interval", "time_amount", 
		"time_unit"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "','", "':'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "COMMA", "COLON", "WINDOW", "INCLUDE", "EXCLUDE", "FROM", "EVERY", 
		"TO", "AGO", "NUMBER", "IDENTIFIER", "DAY_SPECIFIER", "TIME_UNIT", "WS"
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
	public String getGrammarFileName() { return "Window.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public WindowParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class WindowContext extends ParserRuleContext {
		public Window_expressionContext window_expression() {
			return getRuleContext(Window_expressionContext.class,0);
		}
		public TerminalNode EOF() { return getToken(WindowParser.EOF, 0); }
		public WindowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_window; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitWindow(this);
		}
	}

	public final WindowContext window() throws RecognitionException {
		WindowContext _localctx = new WindowContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_window);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(30);
			window_expression();
			setState(31);
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

	public static class Window_expressionContext extends ParserRuleContext {
		public Window_expressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_window_expression; }
	 
		public Window_expressionContext() { }
		public void copyFrom(Window_expressionContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class RepeatingWindowContext extends Window_expressionContext {
		public Window_widthContext window_width() {
			return getRuleContext(Window_widthContext.class,0);
		}
		public Skip_distanceContext skip_distance() {
			return getRuleContext(Skip_distanceContext.class,0);
		}
		public DurationContext duration() {
			return getRuleContext(DurationContext.class,0);
		}
		public Including_specifierContext including_specifier() {
			return getRuleContext(Including_specifierContext.class,0);
		}
		public Excluding_specifierContext excluding_specifier() {
			return getRuleContext(Excluding_specifierContext.class,0);
		}
		public RepeatingWindowContext(Window_expressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterRepeatingWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitRepeatingWindow(this);
		}
	}
	public static class DenseWindowContext extends Window_expressionContext {
		public DurationContext duration() {
			return getRuleContext(DurationContext.class,0);
		}
		public DenseWindowContext(Window_expressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterDenseWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitDenseWindow(this);
		}
	}
	public static class NonRepeatingWindowContext extends Window_expressionContext {
		public Window_widthContext window_width() {
			return getRuleContext(Window_widthContext.class,0);
		}
		public Including_specifierContext including_specifier() {
			return getRuleContext(Including_specifierContext.class,0);
		}
		public Excluding_specifierContext excluding_specifier() {
			return getRuleContext(Excluding_specifierContext.class,0);
		}
		public NonRepeatingWindowContext(Window_expressionContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterNonRepeatingWindow(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitNonRepeatingWindow(this);
		}
	}

	public final Window_expressionContext window_expression() throws RecognitionException {
		Window_expressionContext _localctx = new Window_expressionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_window_expression);
		int _la;
		try {
			setState(50);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				_localctx = new NonRepeatingWindowContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(33);
				window_width();
				setState(35);
				_la = _input.LA(1);
				if (_la==INCLUDE) {
					{
					setState(34);
					including_specifier();
					}
				}

				setState(38);
				_la = _input.LA(1);
				if (_la==EXCLUDE) {
					{
					setState(37);
					excluding_specifier();
					}
				}

				}
				break;
			case 2:
				_localctx = new RepeatingWindowContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(40);
				window_width();
				setState(41);
				skip_distance();
				setState(42);
				duration();
				setState(44);
				_la = _input.LA(1);
				if (_la==INCLUDE) {
					{
					setState(43);
					including_specifier();
					}
				}

				setState(47);
				_la = _input.LA(1);
				if (_la==EXCLUDE) {
					{
					setState(46);
					excluding_specifier();
					}
				}

				}
				break;
			case 3:
				_localctx = new DenseWindowContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(49);
				duration();
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

	public static class Excluding_specifierContext extends ParserRuleContext {
		public TerminalNode EXCLUDE() { return getToken(WindowParser.EXCLUDE, 0); }
		public Specifier_listContext specifier_list() {
			return getRuleContext(Specifier_listContext.class,0);
		}
		public Excluding_specifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_excluding_specifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterExcluding_specifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitExcluding_specifier(this);
		}
	}

	public final Excluding_specifierContext excluding_specifier() throws RecognitionException {
		Excluding_specifierContext _localctx = new Excluding_specifierContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_excluding_specifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(52);
			match(EXCLUDE);
			setState(53);
			specifier_list(0);
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

	public static class Including_specifierContext extends ParserRuleContext {
		public TerminalNode INCLUDE() { return getToken(WindowParser.INCLUDE, 0); }
		public Specifier_listContext specifier_list() {
			return getRuleContext(Specifier_listContext.class,0);
		}
		public Including_specifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_including_specifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterIncluding_specifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitIncluding_specifier(this);
		}
	}

	public final Including_specifierContext including_specifier() throws RecognitionException {
		Including_specifierContext _localctx = new Including_specifierContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_including_specifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(55);
			match(INCLUDE);
			setState(56);
			specifier_list(0);
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

	public static class SpecifierContext extends ParserRuleContext {
		public Day_specifierContext day_specifier() {
			return getRuleContext(Day_specifierContext.class,0);
		}
		public Specifier_arg_listContext specifier_arg_list() {
			return getRuleContext(Specifier_arg_listContext.class,0);
		}
		public SpecifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_specifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterSpecifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitSpecifier(this);
		}
	}

	public final SpecifierContext specifier() throws RecognitionException {
		SpecifierContext _localctx = new SpecifierContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_specifier);
		try {
			setState(62);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(58);
				day_specifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(59);
				day_specifier();
				setState(60);
				specifier_arg_list();
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

	public static class Specifier_arg_listContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Specifier_arg_listContext specifier_arg_list() {
			return getRuleContext(Specifier_arg_listContext.class,0);
		}
		public Specifier_arg_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_specifier_arg_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterSpecifier_arg_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitSpecifier_arg_list(this);
		}
	}

	public final Specifier_arg_listContext specifier_arg_list() throws RecognitionException {
		Specifier_arg_listContext _localctx = new Specifier_arg_listContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_specifier_arg_list);
		try {
			setState(68);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(64);
				identifier();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(65);
				identifier();
				setState(66);
				specifier_arg_list();
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

	public static class Day_specifierContext extends ParserRuleContext {
		public TerminalNode DAY_SPECIFIER() { return getToken(WindowParser.DAY_SPECIFIER, 0); }
		public Day_specifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_day_specifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterDay_specifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitDay_specifier(this);
		}
	}

	public final Day_specifierContext day_specifier() throws RecognitionException {
		Day_specifierContext _localctx = new Day_specifierContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_day_specifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(70);
			match(DAY_SPECIFIER);
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

	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(WindowParser.NUMBER, 0); }
		public TerminalNode IDENTIFIER() { return getToken(WindowParser.IDENTIFIER, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitIdentifier(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(72);
			_la = _input.LA(1);
			if ( !(_la==NUMBER || _la==IDENTIFIER) ) {
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

	public static class Specifier_listContext extends ParserRuleContext {
		public SpecifierContext specifier() {
			return getRuleContext(SpecifierContext.class,0);
		}
		public Specifier_listContext specifier_list() {
			return getRuleContext(Specifier_listContext.class,0);
		}
		public TerminalNode COMMA() { return getToken(WindowParser.COMMA, 0); }
		public Specifier_listContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_specifier_list; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterSpecifier_list(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitSpecifier_list(this);
		}
	}

	public final Specifier_listContext specifier_list() throws RecognitionException {
		return specifier_list(0);
	}

	private Specifier_listContext specifier_list(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		Specifier_listContext _localctx = new Specifier_listContext(_ctx, _parentState);
		Specifier_listContext _prevctx = _localctx;
		int _startState = 16;
		enterRecursionRule(_localctx, 16, RULE_specifier_list, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			{
			setState(75);
			specifier();
			}
			_ctx.stop = _input.LT(-1);
			setState(82);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new Specifier_listContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_specifier_list);
					setState(77);
					if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
					setState(78);
					match(COMMA);
					setState(79);
					specifier();
					}
					} 
				}
				setState(84);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
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

	public static class DurationContext extends ParserRuleContext {
		public DurationContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_duration; }
	 
		public DurationContext() { }
		public void copyFrom(DurationContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class FromToDurationContext extends DurationContext {
		public TerminalNode FROM() { return getToken(WindowParser.FROM, 0); }
		public List<Time_intervalContext> time_interval() {
			return getRuleContexts(Time_intervalContext.class);
		}
		public Time_intervalContext time_interval(int i) {
			return getRuleContext(Time_intervalContext.class,i);
		}
		public TerminalNode TO() { return getToken(WindowParser.TO, 0); }
		public List<TerminalNode> AGO() { return getTokens(WindowParser.AGO); }
		public TerminalNode AGO(int i) {
			return getToken(WindowParser.AGO, i);
		}
		public FromToDurationContext(DurationContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterFromToDuration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitFromToDuration(this);
		}
	}
	public static class FromDurationContext extends DurationContext {
		public TerminalNode FROM() { return getToken(WindowParser.FROM, 0); }
		public Time_intervalContext time_interval() {
			return getRuleContext(Time_intervalContext.class,0);
		}
		public TerminalNode AGO() { return getToken(WindowParser.AGO, 0); }
		public FromDurationContext(DurationContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterFromDuration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitFromDuration(this);
		}
	}

	public final DurationContext duration() throws RecognitionException {
		DurationContext _localctx = new DurationContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_duration);
		int _la;
		try {
			setState(100);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				_localctx = new FromToDurationContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(85);
				match(FROM);
				setState(86);
				time_interval();
				setState(88);
				_la = _input.LA(1);
				if (_la==AGO) {
					{
					setState(87);
					match(AGO);
					}
				}

				setState(90);
				match(TO);
				setState(91);
				time_interval();
				setState(93);
				_la = _input.LA(1);
				if (_la==AGO) {
					{
					setState(92);
					match(AGO);
					}
				}

				}
				break;
			case 2:
				_localctx = new FromDurationContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(95);
				match(FROM);
				setState(96);
				time_interval();
				setState(98);
				_la = _input.LA(1);
				if (_la==AGO) {
					{
					setState(97);
					match(AGO);
					}
				}

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

	public static class Skip_distanceContext extends ParserRuleContext {
		public Skip_distanceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_skip_distance; }
	 
		public Skip_distanceContext() { }
		public void copyFrom(Skip_distanceContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class SkipDistanceContext extends Skip_distanceContext {
		public TerminalNode EVERY() { return getToken(WindowParser.EVERY, 0); }
		public Time_intervalContext time_interval() {
			return getRuleContext(Time_intervalContext.class,0);
		}
		public SkipDistanceContext(Skip_distanceContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterSkipDistance(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitSkipDistance(this);
		}
	}

	public final Skip_distanceContext skip_distance() throws RecognitionException {
		Skip_distanceContext _localctx = new Skip_distanceContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_skip_distance);
		try {
			_localctx = new SkipDistanceContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(102);
			match(EVERY);
			setState(103);
			time_interval();
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

	public static class Window_widthContext extends ParserRuleContext {
		public Window_widthContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_window_width; }
	 
		public Window_widthContext() { }
		public void copyFrom(Window_widthContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class WindowWidthContext extends Window_widthContext {
		public Time_intervalContext time_interval() {
			return getRuleContext(Time_intervalContext.class,0);
		}
		public TerminalNode WINDOW() { return getToken(WindowParser.WINDOW, 0); }
		public WindowWidthContext(Window_widthContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterWindowWidth(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitWindowWidth(this);
		}
	}

	public final Window_widthContext window_width() throws RecognitionException {
		Window_widthContext _localctx = new Window_widthContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_window_width);
		int _la;
		try {
			_localctx = new WindowWidthContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(105);
			time_interval();
			setState(107);
			_la = _input.LA(1);
			if (_la==WINDOW) {
				{
				setState(106);
				match(WINDOW);
				}
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

	public static class Time_intervalContext extends ParserRuleContext {
		public Time_intervalContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_time_interval; }
	 
		public Time_intervalContext() { }
		public void copyFrom(Time_intervalContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TimeIntervalContext extends Time_intervalContext {
		public Time_amountContext time_amount() {
			return getRuleContext(Time_amountContext.class,0);
		}
		public Time_unitContext time_unit() {
			return getRuleContext(Time_unitContext.class,0);
		}
		public TimeIntervalContext(Time_intervalContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterTimeInterval(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitTimeInterval(this);
		}
	}

	public final Time_intervalContext time_interval() throws RecognitionException {
		Time_intervalContext _localctx = new Time_intervalContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_time_interval);
		try {
			_localctx = new TimeIntervalContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(109);
			time_amount();
			setState(110);
			time_unit();
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

	public static class Time_amountContext extends ParserRuleContext {
		public Time_amountContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_time_amount; }
	 
		public Time_amountContext() { }
		public void copyFrom(Time_amountContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TimeAmountContext extends Time_amountContext {
		public TerminalNode NUMBER() { return getToken(WindowParser.NUMBER, 0); }
		public TimeAmountContext(Time_amountContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterTimeAmount(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitTimeAmount(this);
		}
	}

	public final Time_amountContext time_amount() throws RecognitionException {
		Time_amountContext _localctx = new Time_amountContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_time_amount);
		try {
			_localctx = new TimeAmountContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(112);
			match(NUMBER);
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

	public static class Time_unitContext extends ParserRuleContext {
		public Time_unitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_time_unit; }
	 
		public Time_unitContext() { }
		public void copyFrom(Time_unitContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class TimeUnitContext extends Time_unitContext {
		public TerminalNode TIME_UNIT() { return getToken(WindowParser.TIME_UNIT, 0); }
		public TimeUnitContext(Time_unitContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).enterTimeUnit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof WindowListener ) ((WindowListener)listener).exitTimeUnit(this);
		}
	}

	public final Time_unitContext time_unit() throws RecognitionException {
		Time_unitContext _localctx = new Time_unitContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_time_unit);
		try {
			_localctx = new TimeUnitContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(114);
			match(TIME_UNIT);
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
		case 8:
			return specifier_list_sempred((Specifier_listContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean specifier_list_sempred(Specifier_listContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\20w\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\4"+
		"\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\3\2\3\2\3\2\3\3\3\3\5\3&"+
		"\n\3\3\3\5\3)\n\3\3\3\3\3\3\3\3\3\5\3/\n\3\3\3\5\3\62\n\3\3\3\5\3\65\n"+
		"\3\3\4\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\5\6A\n\6\3\7\3\7\3\7\3\7\5"+
		"\7G\n\7\3\b\3\b\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\7\nS\n\n\f\n\16\nV\13"+
		"\n\3\13\3\13\3\13\5\13[\n\13\3\13\3\13\3\13\5\13`\n\13\3\13\3\13\3\13"+
		"\5\13e\n\13\5\13g\n\13\3\f\3\f\3\f\3\r\3\r\5\rn\n\r\3\16\3\16\3\16\3\17"+
		"\3\17\3\20\3\20\3\20\2\3\22\21\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36"+
		"\2\3\3\2\f\ru\2 \3\2\2\2\4\64\3\2\2\2\6\66\3\2\2\2\b9\3\2\2\2\n@\3\2\2"+
		"\2\fF\3\2\2\2\16H\3\2\2\2\20J\3\2\2\2\22L\3\2\2\2\24f\3\2\2\2\26h\3\2"+
		"\2\2\30k\3\2\2\2\32o\3\2\2\2\34r\3\2\2\2\36t\3\2\2\2 !\5\4\3\2!\"\7\2"+
		"\2\3\"\3\3\2\2\2#%\5\30\r\2$&\5\b\5\2%$\3\2\2\2%&\3\2\2\2&(\3\2\2\2\'"+
		")\5\6\4\2(\'\3\2\2\2()\3\2\2\2)\65\3\2\2\2*+\5\30\r\2+,\5\26\f\2,.\5\24"+
		"\13\2-/\5\b\5\2.-\3\2\2\2./\3\2\2\2/\61\3\2\2\2\60\62\5\6\4\2\61\60\3"+
		"\2\2\2\61\62\3\2\2\2\62\65\3\2\2\2\63\65\5\24\13\2\64#\3\2\2\2\64*\3\2"+
		"\2\2\64\63\3\2\2\2\65\5\3\2\2\2\66\67\7\7\2\2\678\5\22\n\28\7\3\2\2\2"+
		"9:\7\6\2\2:;\5\22\n\2;\t\3\2\2\2<A\5\16\b\2=>\5\16\b\2>?\5\f\7\2?A\3\2"+
		"\2\2@<\3\2\2\2@=\3\2\2\2A\13\3\2\2\2BG\5\20\t\2CD\5\20\t\2DE\5\f\7\2E"+
		"G\3\2\2\2FB\3\2\2\2FC\3\2\2\2G\r\3\2\2\2HI\7\16\2\2I\17\3\2\2\2JK\t\2"+
		"\2\2K\21\3\2\2\2LM\b\n\1\2MN\5\n\6\2NT\3\2\2\2OP\f\3\2\2PQ\7\3\2\2QS\5"+
		"\n\6\2RO\3\2\2\2SV\3\2\2\2TR\3\2\2\2TU\3\2\2\2U\23\3\2\2\2VT\3\2\2\2W"+
		"X\7\b\2\2XZ\5\32\16\2Y[\7\13\2\2ZY\3\2\2\2Z[\3\2\2\2[\\\3\2\2\2\\]\7\n"+
		"\2\2]_\5\32\16\2^`\7\13\2\2_^\3\2\2\2_`\3\2\2\2`g\3\2\2\2ab\7\b\2\2bd"+
		"\5\32\16\2ce\7\13\2\2dc\3\2\2\2de\3\2\2\2eg\3\2\2\2fW\3\2\2\2fa\3\2\2"+
		"\2g\25\3\2\2\2hi\7\t\2\2ij\5\32\16\2j\27\3\2\2\2km\5\32\16\2ln\7\5\2\2"+
		"ml\3\2\2\2mn\3\2\2\2n\31\3\2\2\2op\5\34\17\2pq\5\36\20\2q\33\3\2\2\2r"+
		"s\7\f\2\2s\35\3\2\2\2tu\7\17\2\2u\37\3\2\2\2\17%(.\61\64@FTZ_dfm";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}