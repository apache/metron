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

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class StellarLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		IN=1, LAMBDA_OP=2, DOUBLE_QUOTE=3, SINGLE_QUOTE=4, COMMA=5, PERIOD=6, 
		AND=7, OR=8, NOT=9, TRUE=10, FALSE=11, EQ=12, NEQ=13, LT=14, LTE=15, GT=16, 
		GTE=17, QUESTION=18, COLON=19, IF=20, THEN=21, ELSE=22, NULL=23, NAN=24, 
		MATCH=25, DEFAULT=26, MINUS=27, PLUS=28, DIV=29, MUL=30, LBRACE=31, RBRACE=32, 
		LBRACKET=33, RBRACKET=34, LPAREN=35, RPAREN=36, NIN=37, EXISTS=38, EXPONENT=39, 
		INT_LITERAL=40, DOUBLE_LITERAL=41, FLOAT_LITERAL=42, LONG_LITERAL=43, 
		IDENTIFIER=44, STRING_LITERAL=45, COMMENT=46, WS=47;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "MATCH", "DEFAULT", 
		"MINUS", "PLUS", "DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", 
		"LPAREN", "RPAREN", "NIN", "EXISTS", "EXPONENT", "INT_LITERAL", "DOUBLE_LITERAL", 
		"FLOAT_LITERAL", "LONG_LITERAL", "IDENTIFIER", "STRING_LITERAL", "COMMENT", 
		"WS", "ZERO", "FIRST_DIGIT", "DIGIT", "D", "E", "F", "L", "EOL", "IDENTIFIER_START", 
		"IDENTIFIER_MIDDLE", "IDENTIFIER_END"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'->'", "'\"'", "'''", "','", "'.'", null, null, null, null, 
		null, "'=='", "'!='", "'<'", "'<='", "'>'", "'>='", "'?'", "':'", null, 
		null, null, null, "'NaN'", null, null, "'-'", "'+'", "'/'", "'*'", "'{'", 
		"'}'", "'['", "']'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "MATCH", "DEFAULT", 
		"MINUS", "PLUS", "DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", 
		"LPAREN", "RPAREN", "NIN", "EXISTS", "EXPONENT", "INT_LITERAL", "DOUBLE_LITERAL", 
		"FLOAT_LITERAL", "LONG_LITERAL", "IDENTIFIER", "STRING_LITERAL", "COMMENT", 
		"WS"
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


	public StellarLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Stellar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\61\u01f6\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\3\2\3\2\3"+
		"\2\3\2\5\2|\n\2\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\5\b\u0091\n\b\3\t\3\t\3\t\3\t\3\t\3\t\5\t\u0099"+
		"\n\t\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u00a1\n\n\3\13\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\5\13\u00ab\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f"+
		"\5\f\u00b7\n\f\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\20\3\21"+
		"\3\21\3\22\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\25\3\25\5\25\u00d1"+
		"\n\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u00db\n\26\3\27\3\27"+
		"\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u00e5\n\27\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\5\30\u00ef\n\30\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32"+
		"\3\32\3\32\3\32\3\32\3\32\3\32\5\32\u00ff\n\32\3\33\3\33\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\5\33\u010f\n\33\3\34\3\34"+
		"\3\35\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3"+
		"&\3&\3&\3&\3&\3&\3&\3&\3&\3&\3&\3&\5&\u0131\n&\3\'\3\'\3\'\3\'\3\'\3\'"+
		"\3\'\3\'\3\'\3\'\3\'\3\'\5\'\u013f\n\'\3(\3(\3(\5(\u0144\n(\3(\6(\u0147"+
		"\n(\r(\16(\u0148\3)\5)\u014c\n)\3)\3)\5)\u0150\n)\3)\3)\7)\u0154\n)\f"+
		")\16)\u0157\13)\5)\u0159\n)\3*\3*\3*\7*\u015e\n*\f*\16*\u0161\13*\3*\5"+
		"*\u0164\n*\3*\5*\u0167\n*\3*\3*\6*\u016b\n*\r*\16*\u016c\3*\5*\u0170\n"+
		"*\3*\5*\u0173\n*\3*\3*\3*\5*\u0178\n*\3*\3*\5*\u017c\n*\3*\3*\5*\u0180"+
		"\n*\3+\3+\3+\7+\u0185\n+\f+\16+\u0188\13+\3+\5+\u018b\n+\3+\3+\3+\5+\u0190"+
		"\n+\3+\3+\6+\u0194\n+\r+\16+\u0195\3+\5+\u0199\n+\3+\3+\3+\3+\5+\u019f"+
		"\n+\3+\3+\5+\u01a3\n+\3,\3,\3,\3-\3-\3-\7-\u01ab\n-\f-\16-\u01ae\13-\3"+
		"-\3-\5-\u01b2\n-\3.\3.\3.\3.\7.\u01b8\n.\f.\16.\u01bb\13.\3.\3.\3.\3."+
		"\3.\3.\7.\u01c3\n.\f.\16.\u01c6\13.\3.\3.\5.\u01ca\n.\3/\3/\3/\3/\6/\u01d0"+
		"\n/\r/\16/\u01d1\3/\3/\5/\u01d6\n/\3/\3/\3\60\6\60\u01db\n\60\r\60\16"+
		"\60\u01dc\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65"+
		"\3\66\3\66\3\67\3\67\38\38\39\39\3:\3:\3;\3;\3\u01d1\2<\3\3\5\4\7\5\t"+
		"\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23"+
		"%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G"+
		"%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\2c\2e\2g\2i\2k\2m\2o\2q\2s\2u\2\3\2\16"+
		"\4\2))^^\7\2))^^ppttvv\4\2$$^^\7\2$$^^ppttvv\5\2\13\f\16\17\"\"\4\2FF"+
		"ff\4\2GGgg\4\2HHhh\4\2NNnn\6\2&&C\\aac|\b\2\60\60\62<C\\^^aac|\b\2\60"+
		"\60\62;C\\^^aac|\u021e\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2"+
		"\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25"+
		"\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2"+
		"\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2"+
		"\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3"+
		"\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2"+
		"\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2"+
		"Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3"+
		"\2\2\2\2_\3\2\2\2\3{\3\2\2\2\5}\3\2\2\2\7\u0080\3\2\2\2\t\u0082\3\2\2"+
		"\2\13\u0084\3\2\2\2\r\u0086\3\2\2\2\17\u0090\3\2\2\2\21\u0098\3\2\2\2"+
		"\23\u00a0\3\2\2\2\25\u00aa\3\2\2\2\27\u00b6\3\2\2\2\31\u00b8\3\2\2\2\33"+
		"\u00bb\3\2\2\2\35\u00be\3\2\2\2\37\u00c0\3\2\2\2!\u00c3\3\2\2\2#\u00c5"+
		"\3\2\2\2%\u00c8\3\2\2\2\'\u00ca\3\2\2\2)\u00d0\3\2\2\2+\u00da\3\2\2\2"+
		"-\u00e4\3\2\2\2/\u00ee\3\2\2\2\61\u00f0\3\2\2\2\63\u00fe\3\2\2\2\65\u010e"+
		"\3\2\2\2\67\u0110\3\2\2\29\u0112\3\2\2\2;\u0114\3\2\2\2=\u0116\3\2\2\2"+
		"?\u0118\3\2\2\2A\u011a\3\2\2\2C\u011c\3\2\2\2E\u011e\3\2\2\2G\u0120\3"+
		"\2\2\2I\u0122\3\2\2\2K\u0130\3\2\2\2M\u013e\3\2\2\2O\u0140\3\2\2\2Q\u0158"+
		"\3\2\2\2S\u017f\3\2\2\2U\u01a2\3\2\2\2W\u01a4\3\2\2\2Y\u01b1\3\2\2\2["+
		"\u01c9\3\2\2\2]\u01cb\3\2\2\2_\u01da\3\2\2\2a\u01e0\3\2\2\2c\u01e2\3\2"+
		"\2\2e\u01e4\3\2\2\2g\u01e6\3\2\2\2i\u01e8\3\2\2\2k\u01ea\3\2\2\2m\u01ec"+
		"\3\2\2\2o\u01ee\3\2\2\2q\u01f0\3\2\2\2s\u01f2\3\2\2\2u\u01f4\3\2\2\2w"+
		"x\7k\2\2x|\7p\2\2yz\7K\2\2z|\7P\2\2{w\3\2\2\2{y\3\2\2\2|\4\3\2\2\2}~\7"+
		"/\2\2~\177\7@\2\2\177\6\3\2\2\2\u0080\u0081\7$\2\2\u0081\b\3\2\2\2\u0082"+
		"\u0083\7)\2\2\u0083\n\3\2\2\2\u0084\u0085\7.\2\2\u0085\f\3\2\2\2\u0086"+
		"\u0087\7\60\2\2\u0087\16\3\2\2\2\u0088\u0089\7c\2\2\u0089\u008a\7p\2\2"+
		"\u008a\u0091\7f\2\2\u008b\u008c\7(\2\2\u008c\u0091\7(\2\2\u008d\u008e"+
		"\7C\2\2\u008e\u008f\7P\2\2\u008f\u0091\7F\2\2\u0090\u0088\3\2\2\2\u0090"+
		"\u008b\3\2\2\2\u0090\u008d\3\2\2\2\u0091\20\3\2\2\2\u0092\u0093\7q\2\2"+
		"\u0093\u0099\7t\2\2\u0094\u0095\7~\2\2\u0095\u0099\7~\2\2\u0096\u0097"+
		"\7Q\2\2\u0097\u0099\7T\2\2\u0098\u0092\3\2\2\2\u0098\u0094\3\2\2\2\u0098"+
		"\u0096\3\2\2\2\u0099\22\3\2\2\2\u009a\u009b\7p\2\2\u009b\u009c\7q\2\2"+
		"\u009c\u00a1\7v\2\2\u009d\u009e\7P\2\2\u009e\u009f\7Q\2\2\u009f\u00a1"+
		"\7V\2\2\u00a0\u009a\3\2\2\2\u00a0\u009d\3\2\2\2\u00a1\24\3\2\2\2\u00a2"+
		"\u00a3\7v\2\2\u00a3\u00a4\7t\2\2\u00a4\u00a5\7w\2\2\u00a5\u00ab\7g\2\2"+
		"\u00a6\u00a7\7V\2\2\u00a7\u00a8\7T\2\2\u00a8\u00a9\7W\2\2\u00a9\u00ab"+
		"\7G\2\2\u00aa\u00a2\3\2\2\2\u00aa\u00a6\3\2\2\2\u00ab\26\3\2\2\2\u00ac"+
		"\u00ad\7h\2\2\u00ad\u00ae\7c\2\2\u00ae\u00af\7n\2\2\u00af\u00b0\7u\2\2"+
		"\u00b0\u00b7\7g\2\2\u00b1\u00b2\7H\2\2\u00b2\u00b3\7C\2\2\u00b3\u00b4"+
		"\7N\2\2\u00b4\u00b5\7U\2\2\u00b5\u00b7\7G\2\2\u00b6\u00ac\3\2\2\2\u00b6"+
		"\u00b1\3\2\2\2\u00b7\30\3\2\2\2\u00b8\u00b9\7?\2\2\u00b9\u00ba\7?\2\2"+
		"\u00ba\32\3\2\2\2\u00bb\u00bc\7#\2\2\u00bc\u00bd\7?\2\2\u00bd\34\3\2\2"+
		"\2\u00be\u00bf\7>\2\2\u00bf\36\3\2\2\2\u00c0\u00c1\7>\2\2\u00c1\u00c2"+
		"\7?\2\2\u00c2 \3\2\2\2\u00c3\u00c4\7@\2\2\u00c4\"\3\2\2\2\u00c5\u00c6"+
		"\7@\2\2\u00c6\u00c7\7?\2\2\u00c7$\3\2\2\2\u00c8\u00c9\7A\2\2\u00c9&\3"+
		"\2\2\2\u00ca\u00cb\7<\2\2\u00cb(\3\2\2\2\u00cc\u00cd\7K\2\2\u00cd\u00d1"+
		"\7H\2\2\u00ce\u00cf\7k\2\2\u00cf\u00d1\7h\2\2\u00d0\u00cc\3\2\2\2\u00d0"+
		"\u00ce\3\2\2\2\u00d1*\3\2\2\2\u00d2\u00d3\7V\2\2\u00d3\u00d4\7J\2\2\u00d4"+
		"\u00d5\7G\2\2\u00d5\u00db\7P\2\2\u00d6\u00d7\7v\2\2\u00d7\u00d8\7j\2\2"+
		"\u00d8\u00d9\7g\2\2\u00d9\u00db\7p\2\2\u00da\u00d2\3\2\2\2\u00da\u00d6"+
		"\3\2\2\2\u00db,\3\2\2\2\u00dc\u00dd\7G\2\2\u00dd\u00de\7N\2\2\u00de\u00df"+
		"\7U\2\2\u00df\u00e5\7G\2\2\u00e0\u00e1\7g\2\2\u00e1\u00e2\7n\2\2\u00e2"+
		"\u00e3\7u\2\2\u00e3\u00e5\7g\2\2\u00e4\u00dc\3\2\2\2\u00e4\u00e0\3\2\2"+
		"\2\u00e5.\3\2\2\2\u00e6\u00e7\7p\2\2\u00e7\u00e8\7w\2\2\u00e8\u00e9\7"+
		"n\2\2\u00e9\u00ef\7n\2\2\u00ea\u00eb\7P\2\2\u00eb\u00ec\7W\2\2\u00ec\u00ed"+
		"\7N\2\2\u00ed\u00ef\7N\2\2\u00ee\u00e6\3\2\2\2\u00ee\u00ea\3\2\2\2\u00ef"+
		"\60\3\2\2\2\u00f0\u00f1\7P\2\2\u00f1\u00f2\7c\2\2\u00f2\u00f3\7P\2\2\u00f3"+
		"\62\3\2\2\2\u00f4\u00f5\7o\2\2\u00f5\u00f6\7c\2\2\u00f6\u00f7\7v\2\2\u00f7"+
		"\u00f8\7e\2\2\u00f8\u00ff\7j\2\2\u00f9\u00fa\7O\2\2\u00fa\u00fb\7C\2\2"+
		"\u00fb\u00fc\7V\2\2\u00fc\u00fd\7E\2\2\u00fd\u00ff\7J\2\2\u00fe\u00f4"+
		"\3\2\2\2\u00fe\u00f9\3\2\2\2\u00ff\64\3\2\2\2\u0100\u0101\7f\2\2\u0101"+
		"\u0102\7g\2\2\u0102\u0103\7h\2\2\u0103\u0104\7c\2\2\u0104\u0105\7w\2\2"+
		"\u0105\u0106\7n\2\2\u0106\u010f\7v\2\2\u0107\u0108\7F\2\2\u0108\u0109"+
		"\7G\2\2\u0109\u010a\7H\2\2\u010a\u010b\7C\2\2\u010b\u010c\7W\2\2\u010c"+
		"\u010d\7N\2\2\u010d\u010f\7V\2\2\u010e\u0100\3\2\2\2\u010e\u0107\3\2\2"+
		"\2\u010f\66\3\2\2\2\u0110\u0111\7/\2\2\u01118\3\2\2\2\u0112\u0113\7-\2"+
		"\2\u0113:\3\2\2\2\u0114\u0115\7\61\2\2\u0115<\3\2\2\2\u0116\u0117\7,\2"+
		"\2\u0117>\3\2\2\2\u0118\u0119\7}\2\2\u0119@\3\2\2\2\u011a\u011b\7\177"+
		"\2\2\u011bB\3\2\2\2\u011c\u011d\7]\2\2\u011dD\3\2\2\2\u011e\u011f\7_\2"+
		"\2\u011fF\3\2\2\2\u0120\u0121\7*\2\2\u0121H\3\2\2\2\u0122\u0123\7+\2\2"+
		"\u0123J\3\2\2\2\u0124\u0125\7p\2\2\u0125\u0126\7q\2\2\u0126\u0127\7v\2"+
		"\2\u0127\u0128\7\"\2\2\u0128\u0129\7k\2\2\u0129\u0131\7p\2\2\u012a\u012b"+
		"\7P\2\2\u012b\u012c\7Q\2\2\u012c\u012d\7V\2\2\u012d\u012e\7\"\2\2\u012e"+
		"\u012f\7K\2\2\u012f\u0131\7P\2\2\u0130\u0124\3\2\2\2\u0130\u012a\3\2\2"+
		"\2\u0131L\3\2\2\2\u0132\u0133\7g\2\2\u0133\u0134\7z\2\2\u0134\u0135\7"+
		"k\2\2\u0135\u0136\7u\2\2\u0136\u0137\7v\2\2\u0137\u013f\7u\2\2\u0138\u0139"+
		"\7G\2\2\u0139\u013a\7Z\2\2\u013a\u013b\7K\2\2\u013b\u013c\7U\2\2\u013c"+
		"\u013d\7V\2\2\u013d\u013f\7U\2\2\u013e\u0132\3\2\2\2\u013e\u0138\3\2\2"+
		"\2\u013fN\3\2\2\2\u0140\u0143\5i\65\2\u0141\u0144\59\35\2\u0142\u0144"+
		"\5\67\34\2\u0143\u0141\3\2\2\2\u0143\u0142\3\2\2\2\u0143\u0144\3\2\2\2"+
		"\u0144\u0146\3\2\2\2\u0145\u0147\5e\63\2\u0146\u0145\3\2\2\2\u0147\u0148"+
		"\3\2\2\2\u0148\u0146\3\2\2\2\u0148\u0149\3\2\2\2\u0149P\3\2\2\2\u014a"+
		"\u014c\5\67\34\2\u014b\u014a\3\2\2\2\u014b\u014c\3\2\2\2\u014c\u014d\3"+
		"\2\2\2\u014d\u0159\5a\61\2\u014e\u0150\5\67\34\2\u014f\u014e\3\2\2\2\u014f"+
		"\u0150\3\2\2\2\u0150\u0151\3\2\2\2\u0151\u0155\5c\62\2\u0152\u0154\5e"+
		"\63\2\u0153\u0152\3\2\2\2\u0154\u0157\3\2\2\2\u0155\u0153\3\2\2\2\u0155"+
		"\u0156\3\2\2\2\u0156\u0159\3\2\2\2\u0157\u0155\3\2\2\2\u0158\u014b\3\2"+
		"\2\2\u0158\u014f\3\2\2\2\u0159R\3\2\2\2\u015a\u015b\5Q)\2\u015b\u015f"+
		"\5\r\7\2\u015c\u015e\5e\63\2\u015d\u015c\3\2\2\2\u015e\u0161\3\2\2\2\u015f"+
		"\u015d\3\2\2\2\u015f\u0160\3\2\2\2\u0160\u0163\3\2\2\2\u0161\u015f\3\2"+
		"\2\2\u0162\u0164\5O(\2\u0163\u0162\3\2\2\2\u0163\u0164\3\2\2\2\u0164\u0166"+
		"\3\2\2\2\u0165\u0167\5g\64\2\u0166\u0165\3\2\2\2\u0166\u0167\3\2\2\2\u0167"+
		"\u0180\3\2\2\2\u0168\u016a\5\r\7\2\u0169\u016b\5e\63\2\u016a\u0169\3\2"+
		"\2\2\u016b\u016c\3\2\2\2\u016c\u016a\3\2\2\2\u016c\u016d\3\2\2\2\u016d"+
		"\u016f\3\2\2\2\u016e\u0170\5O(\2\u016f\u016e\3\2\2\2\u016f\u0170\3\2\2"+
		"\2\u0170\u0172\3\2\2\2\u0171\u0173\5g\64\2\u0172\u0171\3\2\2\2\u0172\u0173"+
		"\3\2\2\2\u0173\u0180\3\2\2\2\u0174\u0175\5Q)\2\u0175\u0177\5O(\2\u0176"+
		"\u0178\5g\64\2\u0177\u0176\3\2\2\2\u0177\u0178\3\2\2\2\u0178\u0180\3\2"+
		"\2\2\u0179\u017b\5Q)\2\u017a\u017c\5O(\2\u017b\u017a\3\2\2\2\u017b\u017c"+
		"\3\2\2\2\u017c\u017d\3\2\2\2\u017d\u017e\5g\64\2\u017e\u0180\3\2\2\2\u017f"+
		"\u015a\3\2\2\2\u017f\u0168\3\2\2\2\u017f\u0174\3\2\2\2\u017f\u0179\3\2"+
		"\2\2\u0180T\3\2\2\2\u0181\u0182\5Q)\2\u0182\u0186\5\r\7\2\u0183\u0185"+
		"\5e\63\2\u0184\u0183\3\2\2\2\u0185\u0188\3\2\2\2\u0186\u0184\3\2\2\2\u0186"+
		"\u0187\3\2\2\2\u0187\u018a\3\2\2\2\u0188\u0186\3\2\2\2\u0189\u018b\5O"+
		"(\2\u018a\u0189\3\2\2\2\u018a\u018b\3\2\2\2\u018b\u018c\3\2\2\2\u018c"+
		"\u018d\5k\66\2\u018d\u01a3\3\2\2\2\u018e\u0190\5\67\34\2\u018f\u018e\3"+
		"\2\2\2\u018f\u0190\3\2\2\2\u0190\u0191\3\2\2\2\u0191\u0193\5\r\7\2\u0192"+
		"\u0194\5e\63\2\u0193\u0192\3\2\2\2\u0194\u0195\3\2\2\2\u0195\u0193\3\2"+
		"\2\2\u0195\u0196\3\2\2\2\u0196\u0198\3\2\2\2\u0197\u0199\5O(\2\u0198\u0197"+
		"\3\2\2\2\u0198\u0199\3\2\2\2\u0199\u019a\3\2\2\2\u019a\u019b\5k\66\2\u019b"+
		"\u01a3\3\2\2\2\u019c\u019e\5Q)\2\u019d\u019f\5O(\2\u019e\u019d\3\2\2\2"+
		"\u019e\u019f\3\2\2\2\u019f\u01a0\3\2\2\2\u01a0\u01a1\5k\66\2\u01a1\u01a3"+
		"\3\2\2\2\u01a2\u0181\3\2\2\2\u01a2\u018f\3\2\2\2\u01a2\u019c\3\2\2\2\u01a3"+
		"V\3\2\2\2\u01a4\u01a5\5Q)\2\u01a5\u01a6\5m\67\2\u01a6X\3\2\2\2\u01a7\u01b2"+
		"\5q9\2\u01a8\u01ac\5q9\2\u01a9\u01ab\5s:\2\u01aa\u01a9\3\2\2\2\u01ab\u01ae"+
		"\3\2\2\2\u01ac\u01aa\3\2\2\2\u01ac\u01ad\3\2\2\2\u01ad\u01af\3\2\2\2\u01ae"+
		"\u01ac\3\2\2\2\u01af\u01b0\5u;\2\u01b0\u01b2\3\2\2\2\u01b1\u01a7\3\2\2"+
		"\2\u01b1\u01a8\3\2\2\2\u01b2Z\3\2\2\2\u01b3\u01b9\5\t\5\2\u01b4\u01b8"+
		"\n\2\2\2\u01b5\u01b6\7^\2\2\u01b6\u01b8\t\3\2\2\u01b7\u01b4\3\2\2\2\u01b7"+
		"\u01b5\3\2\2\2\u01b8\u01bb\3\2\2\2\u01b9\u01b7\3\2\2\2\u01b9\u01ba\3\2"+
		"\2\2\u01ba\u01bc\3\2\2\2\u01bb\u01b9\3\2\2\2\u01bc\u01bd\5\t\5\2\u01bd"+
		"\u01ca\3\2\2\2\u01be\u01c4\5\7\4\2\u01bf\u01c3\n\4\2\2\u01c0\u01c1\7^"+
		"\2\2\u01c1\u01c3\t\5\2\2\u01c2\u01bf\3\2\2\2\u01c2\u01c0\3\2\2\2\u01c3"+
		"\u01c6\3\2\2\2\u01c4\u01c2\3\2\2\2\u01c4\u01c5\3\2\2\2\u01c5\u01c7\3\2"+
		"\2\2\u01c6\u01c4\3\2\2\2\u01c7\u01c8\5\7\4\2\u01c8\u01ca\3\2\2\2\u01c9"+
		"\u01b3\3\2\2\2\u01c9\u01be\3\2\2\2\u01ca\\\3\2\2\2\u01cb\u01cc\7\61\2"+
		"\2\u01cc\u01cd\7\61\2\2\u01cd\u01cf\3\2\2\2\u01ce\u01d0\13\2\2\2\u01cf"+
		"\u01ce\3\2\2\2\u01d0\u01d1\3\2\2\2\u01d1\u01d2\3\2\2\2\u01d1\u01cf\3\2"+
		"\2\2\u01d2\u01d5\3\2\2\2\u01d3\u01d6\5o8\2\u01d4\u01d6\7\2\2\3\u01d5\u01d3"+
		"\3\2\2\2\u01d5\u01d4\3\2\2\2\u01d6\u01d7\3\2\2\2\u01d7\u01d8\b/\2\2\u01d8"+
		"^\3\2\2\2\u01d9\u01db\t\6\2\2\u01da\u01d9\3\2\2\2\u01db\u01dc\3\2\2\2"+
		"\u01dc\u01da\3\2\2\2\u01dc\u01dd\3\2\2\2\u01dd\u01de\3\2\2\2\u01de\u01df"+
		"\b\60\2\2\u01df`\3\2\2\2\u01e0\u01e1\7\62\2\2\u01e1b\3\2\2\2\u01e2\u01e3"+
		"\4\63;\2\u01e3d\3\2\2\2\u01e4\u01e5\4\62;\2\u01e5f\3\2\2\2\u01e6\u01e7"+
		"\t\7\2\2\u01e7h\3\2\2\2\u01e8\u01e9\t\b\2\2\u01e9j\3\2\2\2\u01ea\u01eb"+
		"\t\t\2\2\u01ebl\3\2\2\2\u01ec\u01ed\t\n\2\2\u01edn\3\2\2\2\u01ee\u01ef"+
		"\7\f\2\2\u01efp\3\2\2\2\u01f0\u01f1\t\13\2\2\u01f1r\3\2\2\2\u01f2\u01f3"+
		"\t\f\2\2\u01f3t\3\2\2\2\u01f4\u01f5\t\r\2\2\u01f5v\3\2\2\2\61\2{\u0090"+
		"\u0098\u00a0\u00aa\u00b6\u00d0\u00da\u00e4\u00ee\u00fe\u010e\u0130\u013e"+
		"\u0143\u0148\u014b\u014f\u0155\u0158\u015f\u0163\u0166\u016c\u016f\u0172"+
		"\u0177\u017b\u017f\u0186\u018a\u018f\u0195\u0198\u019e\u01a2\u01ac\u01b1"+
		"\u01b7\u01b9\u01c2\u01c4\u01c9\u01d1\u01d5\u01dc\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}