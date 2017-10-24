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
		AS=25, MATCH=26, DEFAULT=27, MINUS=28, PLUS=29, DIV=30, MUL=31, LBRACE=32, 
		RBRACE=33, LBRACKET=34, RBRACKET=35, LPAREN=36, RPAREN=37, NIN=38, EXISTS=39, 
		EXPONENT=40, INT_LITERAL=41, DOUBLE_LITERAL=42, FLOAT_LITERAL=43, LONG_LITERAL=44, 
		IDENTIFIER=45, STRING_LITERAL=46, COMMENT=47, WS=48;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "AS", "MATCH", 
		"DEFAULT", "MINUS", "PLUS", "DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", 
		"RBRACKET", "LPAREN", "RPAREN", "NIN", "EXISTS", "EXPONENT", "INT_LITERAL", 
		"DOUBLE_LITERAL", "FLOAT_LITERAL", "LONG_LITERAL", "IDENTIFIER", "STRING_LITERAL", 
		"COMMENT", "WS", "ZERO", "FIRST_DIGIT", "DIGIT", "D", "E", "F", "L", "EOL", 
		"IDENTIFIER_START", "IDENTIFIER_MIDDLE", "IDENTIFIER_END"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'->'", "'\"'", "'''", "','", "'.'", null, null, null, null, 
		null, "'=='", "'!='", "'<'", "'<='", "'>'", "'>='", "'?'", "':'", null, 
		null, null, null, "'NaN'", null, null, null, "'-'", "'+'", "'/'", "'*'", 
		"'{'", "'}'", "'['", "']'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "AS", "MATCH", 
		"DEFAULT", "MINUS", "PLUS", "DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", 
		"RBRACKET", "LPAREN", "RPAREN", "NIN", "EXISTS", "EXPONENT", "INT_LITERAL", 
		"DOUBLE_LITERAL", "FLOAT_LITERAL", "LONG_LITERAL", "IDENTIFIER", "STRING_LITERAL", 
		"COMMENT", "WS"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\62\u01fe\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t"+
		" \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t"+
		"+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64"+
		"\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\3\2"+
		"\3\2\3\2\3\2\5\2~\n\2\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u0093\n\b\3\t\3\t\3\t\3\t\3\t\3\t\5\t"+
		"\u009b\n\t\3\n\3\n\3\n\3\n\3\n\3\n\5\n\u00a3\n\n\3\13\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\5\13\u00ad\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f"+
		"\3\f\5\f\u00b9\n\f\3\r\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\20"+
		"\3\21\3\21\3\22\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25\3\25\3\25\5\25"+
		"\u00d3\n\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u00dd\n\26\3"+
		"\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\5\27\u00e7\n\27\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\5\30\u00f1\n\30\3\31\3\31\3\31\3\31\3\32\3\32"+
		"\3\32\3\32\5\32\u00fb\n\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33"+
		"\3\33\5\33\u0107\n\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\3\34\3\34\5\34\u0117\n\34\3\35\3\35\3\36\3\36\3\37\3\37\3 "+
		"\3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3%\3%\3&\3&\3\'\3\'\3\'\3\'\3\'\3\'\3\'"+
		"\3\'\3\'\3\'\3\'\3\'\5\'\u0139\n\'\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3"+
		"(\5(\u0147\n(\3)\3)\3)\5)\u014c\n)\3)\6)\u014f\n)\r)\16)\u0150\3*\5*\u0154"+
		"\n*\3*\3*\5*\u0158\n*\3*\3*\7*\u015c\n*\f*\16*\u015f\13*\5*\u0161\n*\3"+
		"+\3+\3+\7+\u0166\n+\f+\16+\u0169\13+\3+\5+\u016c\n+\3+\5+\u016f\n+\3+"+
		"\3+\6+\u0173\n+\r+\16+\u0174\3+\5+\u0178\n+\3+\5+\u017b\n+\3+\3+\3+\5"+
		"+\u0180\n+\3+\3+\5+\u0184\n+\3+\3+\5+\u0188\n+\3,\3,\3,\7,\u018d\n,\f"+
		",\16,\u0190\13,\3,\5,\u0193\n,\3,\3,\3,\5,\u0198\n,\3,\3,\6,\u019c\n,"+
		"\r,\16,\u019d\3,\5,\u01a1\n,\3,\3,\3,\3,\5,\u01a7\n,\3,\3,\5,\u01ab\n"+
		",\3-\3-\3-\3.\3.\3.\7.\u01b3\n.\f.\16.\u01b6\13.\3.\3.\5.\u01ba\n.\3/"+
		"\3/\3/\3/\7/\u01c0\n/\f/\16/\u01c3\13/\3/\3/\3/\3/\3/\3/\7/\u01cb\n/\f"+
		"/\16/\u01ce\13/\3/\3/\5/\u01d2\n/\3\60\3\60\3\60\3\60\6\60\u01d8\n\60"+
		"\r\60\16\60\u01d9\3\60\3\60\5\60\u01de\n\60\3\60\3\60\3\61\6\61\u01e3"+
		"\n\61\r\61\16\61\u01e4\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3"+
		"\65\3\66\3\66\3\67\3\67\38\38\39\39\3:\3:\3;\3;\3<\3<\3\u01d9\2=\3\3\5"+
		"\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21"+
		"!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!"+
		"A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\2e\2g\2i\2k\2m\2o\2q\2s\2"+
		"u\2w\2\3\2\16\4\2))^^\7\2))^^ppttvv\4\2$$^^\7\2$$^^ppttvv\5\2\13\f\16"+
		"\17\"\"\4\2FFff\4\2GGgg\4\2HHhh\4\2NNnn\6\2&&C\\aac|\b\2\60\60\62<C\\"+
		"^^aac|\b\2\60\60\62;C\\^^aac|\u0227\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2"+
		"\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23"+
		"\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2"+
		"\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2"+
		"\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3"+
		"\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2"+
		"\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2"+
		"\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2["+
		"\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\3}\3\2\2\2\5\177\3\2\2\2\7\u0082"+
		"\3\2\2\2\t\u0084\3\2\2\2\13\u0086\3\2\2\2\r\u0088\3\2\2\2\17\u0092\3\2"+
		"\2\2\21\u009a\3\2\2\2\23\u00a2\3\2\2\2\25\u00ac\3\2\2\2\27\u00b8\3\2\2"+
		"\2\31\u00ba\3\2\2\2\33\u00bd\3\2\2\2\35\u00c0\3\2\2\2\37\u00c2\3\2\2\2"+
		"!\u00c5\3\2\2\2#\u00c7\3\2\2\2%\u00ca\3\2\2\2\'\u00cc\3\2\2\2)\u00d2\3"+
		"\2\2\2+\u00dc\3\2\2\2-\u00e6\3\2\2\2/\u00f0\3\2\2\2\61\u00f2\3\2\2\2\63"+
		"\u00fa\3\2\2\2\65\u0106\3\2\2\2\67\u0116\3\2\2\29\u0118\3\2\2\2;\u011a"+
		"\3\2\2\2=\u011c\3\2\2\2?\u011e\3\2\2\2A\u0120\3\2\2\2C\u0122\3\2\2\2E"+
		"\u0124\3\2\2\2G\u0126\3\2\2\2I\u0128\3\2\2\2K\u012a\3\2\2\2M\u0138\3\2"+
		"\2\2O\u0146\3\2\2\2Q\u0148\3\2\2\2S\u0160\3\2\2\2U\u0187\3\2\2\2W\u01aa"+
		"\3\2\2\2Y\u01ac\3\2\2\2[\u01b9\3\2\2\2]\u01d1\3\2\2\2_\u01d3\3\2\2\2a"+
		"\u01e2\3\2\2\2c\u01e8\3\2\2\2e\u01ea\3\2\2\2g\u01ec\3\2\2\2i\u01ee\3\2"+
		"\2\2k\u01f0\3\2\2\2m\u01f2\3\2\2\2o\u01f4\3\2\2\2q\u01f6\3\2\2\2s\u01f8"+
		"\3\2\2\2u\u01fa\3\2\2\2w\u01fc\3\2\2\2yz\7k\2\2z~\7p\2\2{|\7K\2\2|~\7"+
		"P\2\2}y\3\2\2\2}{\3\2\2\2~\4\3\2\2\2\177\u0080\7/\2\2\u0080\u0081\7@\2"+
		"\2\u0081\6\3\2\2\2\u0082\u0083\7$\2\2\u0083\b\3\2\2\2\u0084\u0085\7)\2"+
		"\2\u0085\n\3\2\2\2\u0086\u0087\7.\2\2\u0087\f\3\2\2\2\u0088\u0089\7\60"+
		"\2\2\u0089\16\3\2\2\2\u008a\u008b\7c\2\2\u008b\u008c\7p\2\2\u008c\u0093"+
		"\7f\2\2\u008d\u008e\7(\2\2\u008e\u0093\7(\2\2\u008f\u0090\7C\2\2\u0090"+
		"\u0091\7P\2\2\u0091\u0093\7F\2\2\u0092\u008a\3\2\2\2\u0092\u008d\3\2\2"+
		"\2\u0092\u008f\3\2\2\2\u0093\20\3\2\2\2\u0094\u0095\7q\2\2\u0095\u009b"+
		"\7t\2\2\u0096\u0097\7~\2\2\u0097\u009b\7~\2\2\u0098\u0099\7Q\2\2\u0099"+
		"\u009b\7T\2\2\u009a\u0094\3\2\2\2\u009a\u0096\3\2\2\2\u009a\u0098\3\2"+
		"\2\2\u009b\22\3\2\2\2\u009c\u009d\7p\2\2\u009d\u009e\7q\2\2\u009e\u00a3"+
		"\7v\2\2\u009f\u00a0\7P\2\2\u00a0\u00a1\7Q\2\2\u00a1\u00a3\7V\2\2\u00a2"+
		"\u009c\3\2\2\2\u00a2\u009f\3\2\2\2\u00a3\24\3\2\2\2\u00a4\u00a5\7v\2\2"+
		"\u00a5\u00a6\7t\2\2\u00a6\u00a7\7w\2\2\u00a7\u00ad\7g\2\2\u00a8\u00a9"+
		"\7V\2\2\u00a9\u00aa\7T\2\2\u00aa\u00ab\7W\2\2\u00ab\u00ad\7G\2\2\u00ac"+
		"\u00a4\3\2\2\2\u00ac\u00a8\3\2\2\2\u00ad\26\3\2\2\2\u00ae\u00af\7h\2\2"+
		"\u00af\u00b0\7c\2\2\u00b0\u00b1\7n\2\2\u00b1\u00b2\7u\2\2\u00b2\u00b9"+
		"\7g\2\2\u00b3\u00b4\7H\2\2\u00b4\u00b5\7C\2\2\u00b5\u00b6\7N\2\2\u00b6"+
		"\u00b7\7U\2\2\u00b7\u00b9\7G\2\2\u00b8\u00ae\3\2\2\2\u00b8\u00b3\3\2\2"+
		"\2\u00b9\30\3\2\2\2\u00ba\u00bb\7?\2\2\u00bb\u00bc\7?\2\2\u00bc\32\3\2"+
		"\2\2\u00bd\u00be\7#\2\2\u00be\u00bf\7?\2\2\u00bf\34\3\2\2\2\u00c0\u00c1"+
		"\7>\2\2\u00c1\36\3\2\2\2\u00c2\u00c3\7>\2\2\u00c3\u00c4\7?\2\2\u00c4 "+
		"\3\2\2\2\u00c5\u00c6\7@\2\2\u00c6\"\3\2\2\2\u00c7\u00c8\7@\2\2\u00c8\u00c9"+
		"\7?\2\2\u00c9$\3\2\2\2\u00ca\u00cb\7A\2\2\u00cb&\3\2\2\2\u00cc\u00cd\7"+
		"<\2\2\u00cd(\3\2\2\2\u00ce\u00cf\7K\2\2\u00cf\u00d3\7H\2\2\u00d0\u00d1"+
		"\7k\2\2\u00d1\u00d3\7h\2\2\u00d2\u00ce\3\2\2\2\u00d2\u00d0\3\2\2\2\u00d3"+
		"*\3\2\2\2\u00d4\u00d5\7V\2\2\u00d5\u00d6\7J\2\2\u00d6\u00d7\7G\2\2\u00d7"+
		"\u00dd\7P\2\2\u00d8\u00d9\7v\2\2\u00d9\u00da\7j\2\2\u00da\u00db\7g\2\2"+
		"\u00db\u00dd\7p\2\2\u00dc\u00d4\3\2\2\2\u00dc\u00d8\3\2\2\2\u00dd,\3\2"+
		"\2\2\u00de\u00df\7G\2\2\u00df\u00e0\7N\2\2\u00e0\u00e1\7U\2\2\u00e1\u00e7"+
		"\7G\2\2\u00e2\u00e3\7g\2\2\u00e3\u00e4\7n\2\2\u00e4\u00e5\7u\2\2\u00e5"+
		"\u00e7\7g\2\2\u00e6\u00de\3\2\2\2\u00e6\u00e2\3\2\2\2\u00e7.\3\2\2\2\u00e8"+
		"\u00e9\7p\2\2\u00e9\u00ea\7w\2\2\u00ea\u00eb\7n\2\2\u00eb\u00f1\7n\2\2"+
		"\u00ec\u00ed\7P\2\2\u00ed\u00ee\7W\2\2\u00ee\u00ef\7N\2\2\u00ef\u00f1"+
		"\7N\2\2\u00f0\u00e8\3\2\2\2\u00f0\u00ec\3\2\2\2\u00f1\60\3\2\2\2\u00f2"+
		"\u00f3\7P\2\2\u00f3\u00f4\7c\2\2\u00f4\u00f5\7P\2\2\u00f5\62\3\2\2\2\u00f6"+
		"\u00f7\7c\2\2\u00f7\u00fb\7u\2\2\u00f8\u00f9\7C\2\2\u00f9\u00fb\7U\2\2"+
		"\u00fa\u00f6\3\2\2\2\u00fa\u00f8\3\2\2\2\u00fb\64\3\2\2\2\u00fc\u00fd"+
		"\7o\2\2\u00fd\u00fe\7c\2\2\u00fe\u00ff\7v\2\2\u00ff\u0100\7e\2\2\u0100"+
		"\u0107\7j\2\2\u0101\u0102\7O\2\2\u0102\u0103\7C\2\2\u0103\u0104\7V\2\2"+
		"\u0104\u0105\7E\2\2\u0105\u0107\7J\2\2\u0106\u00fc\3\2\2\2\u0106\u0101"+
		"\3\2\2\2\u0107\66\3\2\2\2\u0108\u0109\7f\2\2\u0109\u010a\7g\2\2\u010a"+
		"\u010b\7h\2\2\u010b\u010c\7c\2\2\u010c\u010d\7w\2\2\u010d\u010e\7n\2\2"+
		"\u010e\u0117\7v\2\2\u010f\u0110\7F\2\2\u0110\u0111\7G\2\2\u0111\u0112"+
		"\7H\2\2\u0112\u0113\7C\2\2\u0113\u0114\7W\2\2\u0114\u0115\7N\2\2\u0115"+
		"\u0117\7V\2\2\u0116\u0108\3\2\2\2\u0116\u010f\3\2\2\2\u01178\3\2\2\2\u0118"+
		"\u0119\7/\2\2\u0119:\3\2\2\2\u011a\u011b\7-\2\2\u011b<\3\2\2\2\u011c\u011d"+
		"\7\61\2\2\u011d>\3\2\2\2\u011e\u011f\7,\2\2\u011f@\3\2\2\2\u0120\u0121"+
		"\7}\2\2\u0121B\3\2\2\2\u0122\u0123\7\177\2\2\u0123D\3\2\2\2\u0124\u0125"+
		"\7]\2\2\u0125F\3\2\2\2\u0126\u0127\7_\2\2\u0127H\3\2\2\2\u0128\u0129\7"+
		"*\2\2\u0129J\3\2\2\2\u012a\u012b\7+\2\2\u012bL\3\2\2\2\u012c\u012d\7p"+
		"\2\2\u012d\u012e\7q\2\2\u012e\u012f\7v\2\2\u012f\u0130\7\"\2\2\u0130\u0131"+
		"\7k\2\2\u0131\u0139\7p\2\2\u0132\u0133\7P\2\2\u0133\u0134\7Q\2\2\u0134"+
		"\u0135\7V\2\2\u0135\u0136\7\"\2\2\u0136\u0137\7K\2\2\u0137\u0139\7P\2"+
		"\2\u0138\u012c\3\2\2\2\u0138\u0132\3\2\2\2\u0139N\3\2\2\2\u013a\u013b"+
		"\7g\2\2\u013b\u013c\7z\2\2\u013c\u013d\7k\2\2\u013d\u013e\7u\2\2\u013e"+
		"\u013f\7v\2\2\u013f\u0147\7u\2\2\u0140\u0141\7G\2\2\u0141\u0142\7Z\2\2"+
		"\u0142\u0143\7K\2\2\u0143\u0144\7U\2\2\u0144\u0145\7V\2\2\u0145\u0147"+
		"\7U\2\2\u0146\u013a\3\2\2\2\u0146\u0140\3\2\2\2\u0147P\3\2\2\2\u0148\u014b"+
		"\5k\66\2\u0149\u014c\5;\36\2\u014a\u014c\59\35\2\u014b\u0149\3\2\2\2\u014b"+
		"\u014a\3\2\2\2\u014b\u014c\3\2\2\2\u014c\u014e\3\2\2\2\u014d\u014f\5g"+
		"\64\2\u014e\u014d\3\2\2\2\u014f\u0150\3\2\2\2\u0150\u014e\3\2\2\2\u0150"+
		"\u0151\3\2\2\2\u0151R\3\2\2\2\u0152\u0154\59\35\2\u0153\u0152\3\2\2\2"+
		"\u0153\u0154\3\2\2\2\u0154\u0155\3\2\2\2\u0155\u0161\5c\62\2\u0156\u0158"+
		"\59\35\2\u0157\u0156\3\2\2\2\u0157\u0158\3\2\2\2\u0158\u0159\3\2\2\2\u0159"+
		"\u015d\5e\63\2\u015a\u015c\5g\64\2\u015b\u015a\3\2\2\2\u015c\u015f\3\2"+
		"\2\2\u015d\u015b\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u0161\3\2\2\2\u015f"+
		"\u015d\3\2\2\2\u0160\u0153\3\2\2\2\u0160\u0157\3\2\2\2\u0161T\3\2\2\2"+
		"\u0162\u0163\5S*\2\u0163\u0167\5\r\7\2\u0164\u0166\5g\64\2\u0165\u0164"+
		"\3\2\2\2\u0166\u0169\3\2\2\2\u0167\u0165\3\2\2\2\u0167\u0168\3\2\2\2\u0168"+
		"\u016b\3\2\2\2\u0169\u0167\3\2\2\2\u016a\u016c\5Q)\2\u016b\u016a\3\2\2"+
		"\2\u016b\u016c\3\2\2\2\u016c\u016e\3\2\2\2\u016d\u016f\5i\65\2\u016e\u016d"+
		"\3\2\2\2\u016e\u016f\3\2\2\2\u016f\u0188\3\2\2\2\u0170\u0172\5\r\7\2\u0171"+
		"\u0173\5g\64\2\u0172\u0171\3\2\2\2\u0173\u0174\3\2\2\2\u0174\u0172\3\2"+
		"\2\2\u0174\u0175\3\2\2\2\u0175\u0177\3\2\2\2\u0176\u0178\5Q)\2\u0177\u0176"+
		"\3\2\2\2\u0177\u0178\3\2\2\2\u0178\u017a\3\2\2\2\u0179\u017b\5i\65\2\u017a"+
		"\u0179\3\2\2\2\u017a\u017b\3\2\2\2\u017b\u0188\3\2\2\2\u017c\u017d\5S"+
		"*\2\u017d\u017f\5Q)\2\u017e\u0180\5i\65\2\u017f\u017e\3\2\2\2\u017f\u0180"+
		"\3\2\2\2\u0180\u0188\3\2\2\2\u0181\u0183\5S*\2\u0182\u0184\5Q)\2\u0183"+
		"\u0182\3\2\2\2\u0183\u0184\3\2\2\2\u0184\u0185\3\2\2\2\u0185\u0186\5i"+
		"\65\2\u0186\u0188\3\2\2\2\u0187\u0162\3\2\2\2\u0187\u0170\3\2\2\2\u0187"+
		"\u017c\3\2\2\2\u0187\u0181\3\2\2\2\u0188V\3\2\2\2\u0189\u018a\5S*\2\u018a"+
		"\u018e\5\r\7\2\u018b\u018d\5g\64\2\u018c\u018b\3\2\2\2\u018d\u0190\3\2"+
		"\2\2\u018e\u018c\3\2\2\2\u018e\u018f\3\2\2\2\u018f\u0192\3\2\2\2\u0190"+
		"\u018e\3\2\2\2\u0191\u0193\5Q)\2\u0192\u0191\3\2\2\2\u0192\u0193\3\2\2"+
		"\2\u0193\u0194\3\2\2\2\u0194\u0195\5m\67\2\u0195\u01ab\3\2\2\2\u0196\u0198"+
		"\59\35\2\u0197\u0196\3\2\2\2\u0197\u0198\3\2\2\2\u0198\u0199\3\2\2\2\u0199"+
		"\u019b\5\r\7\2\u019a\u019c\5g\64\2\u019b\u019a\3\2\2\2\u019c\u019d\3\2"+
		"\2\2\u019d\u019b\3\2\2\2\u019d\u019e\3\2\2\2\u019e\u01a0\3\2\2\2\u019f"+
		"\u01a1\5Q)\2\u01a0\u019f\3\2\2\2\u01a0\u01a1\3\2\2\2\u01a1\u01a2\3\2\2"+
		"\2\u01a2\u01a3\5m\67\2\u01a3\u01ab\3\2\2\2\u01a4\u01a6\5S*\2\u01a5\u01a7"+
		"\5Q)\2\u01a6\u01a5\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7\u01a8\3\2\2\2\u01a8"+
		"\u01a9\5m\67\2\u01a9\u01ab\3\2\2\2\u01aa\u0189\3\2\2\2\u01aa\u0197\3\2"+
		"\2\2\u01aa\u01a4\3\2\2\2\u01abX\3\2\2\2\u01ac\u01ad\5S*\2\u01ad\u01ae"+
		"\5o8\2\u01aeZ\3\2\2\2\u01af\u01ba\5s:\2\u01b0\u01b4\5s:\2\u01b1\u01b3"+
		"\5u;\2\u01b2\u01b1\3\2\2\2\u01b3\u01b6\3\2\2\2\u01b4\u01b2\3\2\2\2\u01b4"+
		"\u01b5\3\2\2\2\u01b5\u01b7\3\2\2\2\u01b6\u01b4\3\2\2\2\u01b7\u01b8\5w"+
		"<\2\u01b8\u01ba\3\2\2\2\u01b9\u01af\3\2\2\2\u01b9\u01b0\3\2\2\2\u01ba"+
		"\\\3\2\2\2\u01bb\u01c1\5\t\5\2\u01bc\u01c0\n\2\2\2\u01bd\u01be\7^\2\2"+
		"\u01be\u01c0\t\3\2\2\u01bf\u01bc\3\2\2\2\u01bf\u01bd\3\2\2\2\u01c0\u01c3"+
		"\3\2\2\2\u01c1\u01bf\3\2\2\2\u01c1\u01c2\3\2\2\2\u01c2\u01c4\3\2\2\2\u01c3"+
		"\u01c1\3\2\2\2\u01c4\u01c5\5\t\5\2\u01c5\u01d2\3\2\2\2\u01c6\u01cc\5\7"+
		"\4\2\u01c7\u01cb\n\4\2\2\u01c8\u01c9\7^\2\2\u01c9\u01cb\t\5\2\2\u01ca"+
		"\u01c7\3\2\2\2\u01ca\u01c8\3\2\2\2\u01cb\u01ce\3\2\2\2\u01cc\u01ca\3\2"+
		"\2\2\u01cc\u01cd\3\2\2\2\u01cd\u01cf\3\2\2\2\u01ce\u01cc\3\2\2\2\u01cf"+
		"\u01d0\5\7\4\2\u01d0\u01d2\3\2\2\2\u01d1\u01bb\3\2\2\2\u01d1\u01c6\3\2"+
		"\2\2\u01d2^\3\2\2\2\u01d3\u01d4\7\61\2\2\u01d4\u01d5\7\61\2\2\u01d5\u01d7"+
		"\3\2\2\2\u01d6\u01d8\13\2\2\2\u01d7\u01d6\3\2\2\2\u01d8\u01d9\3\2\2\2"+
		"\u01d9\u01da\3\2\2\2\u01d9\u01d7\3\2\2\2\u01da\u01dd\3\2\2\2\u01db\u01de"+
		"\5q9\2\u01dc\u01de\7\2\2\3\u01dd\u01db\3\2\2\2\u01dd\u01dc\3\2\2\2\u01de"+
		"\u01df\3\2\2\2\u01df\u01e0\b\60\2\2\u01e0`\3\2\2\2\u01e1\u01e3\t\6\2\2"+
		"\u01e2\u01e1\3\2\2\2\u01e3\u01e4\3\2\2\2\u01e4\u01e2\3\2\2\2\u01e4\u01e5"+
		"\3\2\2\2\u01e5\u01e6\3\2\2\2\u01e6\u01e7\b\61\2\2\u01e7b\3\2\2\2\u01e8"+
		"\u01e9\7\62\2\2\u01e9d\3\2\2\2\u01ea\u01eb\4\63;\2\u01ebf\3\2\2\2\u01ec"+
		"\u01ed\4\62;\2\u01edh\3\2\2\2\u01ee\u01ef\t\7\2\2\u01efj\3\2\2\2\u01f0"+
		"\u01f1\t\b\2\2\u01f1l\3\2\2\2\u01f2\u01f3\t\t\2\2\u01f3n\3\2\2\2\u01f4"+
		"\u01f5\t\n\2\2\u01f5p\3\2\2\2\u01f6\u01f7\7\f\2\2\u01f7r\3\2\2\2\u01f8"+
		"\u01f9\t\13\2\2\u01f9t\3\2\2\2\u01fa\u01fb\t\f\2\2\u01fbv\3\2\2\2\u01fc"+
		"\u01fd\t\r\2\2\u01fdx\3\2\2\2\62\2}\u0092\u009a\u00a2\u00ac\u00b8\u00d2"+
		"\u00dc\u00e6\u00f0\u00fa\u0106\u0116\u0138\u0146\u014b\u0150\u0153\u0157"+
		"\u015d\u0160\u0167\u016b\u016e\u0174\u0177\u017a\u017f\u0183\u0187\u018e"+
		"\u0192\u0197\u019d\u01a0\u01a6\u01aa\u01b4\u01b9\u01bf\u01c1\u01ca\u01cc"+
		"\u01d1\u01d9\u01dd\u01e4\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}