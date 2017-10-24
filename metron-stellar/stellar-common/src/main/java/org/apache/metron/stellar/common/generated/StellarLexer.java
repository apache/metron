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
		MINUS=25, PLUS=26, DIV=27, MUL=28, LBRACE=29, RBRACE=30, LBRACKET=31, 
		RBRACKET=32, LPAREN=33, RPAREN=34, NIN=35, EXISTS=36, EXPONENT=37, INT_LITERAL=38, 
		DOUBLE_LITERAL=39, FLOAT_LITERAL=40, LONG_LITERAL=41, IDENTIFIER=42, STRING_LITERAL=43, 
		COMMENT=44, WS=45;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "MINUS", "PLUS", 
		"DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", 
		"NIN", "EXISTS", "EXPONENT", "INT_LITERAL", "DOUBLE_LITERAL", "FLOAT_LITERAL", 
		"LONG_LITERAL", "IDENTIFIER", "STRING_LITERAL", "COMMENT", "WS", "ZERO", 
		"FIRST_DIGIT", "DIGIT", "D", "E", "F", "L", "EOL", "IDENTIFIER_START", 
		"IDENTIFIER_MIDDLE", "IDENTIFIER_END"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'->'", "'\"'", "'''", "','", "'.'", null, null, null, null, 
		null, "'=='", "'!='", "'<'", "'<='", "'>'", "'>='", "'?'", "':'", null, 
		null, null, null, "'NaN'", "'-'", "'+'", "'/'", "'*'", "'{'", "'}'", "'['", 
		"']'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "MINUS", "PLUS", 
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2/\u01d6\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\3\2\3\2\3\2\3\2\5\2x\n\2"+
		"\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\3\b\5\b\u008d\n\b\3\t\3\t\3\t\3\t\3\t\3\t\5\t\u0095\n\t\3\n\3\n\3\n"+
		"\3\n\3\n\3\n\5\n\u009d\n\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13"+
		"\u00a7\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\5\f\u00b3\n\f\3\r"+
		"\3\r\3\r\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3"+
		"\22\3\23\3\23\3\24\3\24\3\25\3\25\3\25\3\25\5\25\u00cd\n\25\3\26\3\26"+
		"\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u00d7\n\26\3\27\3\27\3\27\3\27\3\27"+
		"\3\27\3\27\3\27\5\27\u00e1\n\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\5\30\u00eb\n\30\3\31\3\31\3\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35"+
		"\3\35\3\36\3\36\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3#\3$\3$\3$\3$\3$\3$"+
		"\3$\3$\3$\3$\3$\3$\5$\u0111\n$\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\5%"+
		"\u011f\n%\3&\3&\3&\5&\u0124\n&\3&\6&\u0127\n&\r&\16&\u0128\3\'\5\'\u012c"+
		"\n\'\3\'\3\'\5\'\u0130\n\'\3\'\3\'\7\'\u0134\n\'\f\'\16\'\u0137\13\'\5"+
		"\'\u0139\n\'\3(\3(\3(\7(\u013e\n(\f(\16(\u0141\13(\3(\5(\u0144\n(\3(\5"+
		"(\u0147\n(\3(\3(\6(\u014b\n(\r(\16(\u014c\3(\5(\u0150\n(\3(\5(\u0153\n"+
		"(\3(\3(\3(\5(\u0158\n(\3(\3(\5(\u015c\n(\3(\3(\5(\u0160\n(\3)\3)\3)\7"+
		")\u0165\n)\f)\16)\u0168\13)\3)\5)\u016b\n)\3)\3)\3)\5)\u0170\n)\3)\3)"+
		"\6)\u0174\n)\r)\16)\u0175\3)\5)\u0179\n)\3)\3)\3)\3)\5)\u017f\n)\3)\3"+
		")\5)\u0183\n)\3*\3*\3*\3+\3+\3+\7+\u018b\n+\f+\16+\u018e\13+\3+\3+\5+"+
		"\u0192\n+\3,\3,\3,\3,\7,\u0198\n,\f,\16,\u019b\13,\3,\3,\3,\3,\3,\3,\7"+
		",\u01a3\n,\f,\16,\u01a6\13,\3,\3,\5,\u01aa\n,\3-\3-\3-\3-\6-\u01b0\n-"+
		"\r-\16-\u01b1\3-\3-\5-\u01b6\n-\3-\3-\3.\6.\u01bb\n.\r.\16.\u01bc\3.\3"+
		".\3/\3/\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3"+
		"\66\3\66\3\67\3\67\38\38\39\39\3\u01b1\2:\3\3\5\4\7\5\t\6\13\7\r\b\17"+
		"\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+"+
		"\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+"+
		"U,W-Y.[/]\2_\2a\2c\2e\2g\2i\2k\2m\2o\2q\2\3\2\16\4\2))^^\7\2))^^ppttv"+
		"v\4\2$$^^\7\2$$^^ppttvv\5\2\13\f\16\17\"\"\4\2FFff\4\2GGgg\4\2HHhh\4\2"+
		"NNnn\6\2&&C\\aac|\b\2\60\60\62<C\\^^aac|\b\2\60\60\62;C\\^^aac|\u01fc"+
		"\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2"+
		"\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2"+
		"\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2"+
		"\2\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2"+
		"\2\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3"+
		"\2\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2"+
		"\2\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2"+
		"U\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\3w\3\2\2\2\5y\3\2\2\2\7|\3"+
		"\2\2\2\t~\3\2\2\2\13\u0080\3\2\2\2\r\u0082\3\2\2\2\17\u008c\3\2\2\2\21"+
		"\u0094\3\2\2\2\23\u009c\3\2\2\2\25\u00a6\3\2\2\2\27\u00b2\3\2\2\2\31\u00b4"+
		"\3\2\2\2\33\u00b7\3\2\2\2\35\u00ba\3\2\2\2\37\u00bc\3\2\2\2!\u00bf\3\2"+
		"\2\2#\u00c1\3\2\2\2%\u00c4\3\2\2\2\'\u00c6\3\2\2\2)\u00cc\3\2\2\2+\u00d6"+
		"\3\2\2\2-\u00e0\3\2\2\2/\u00ea\3\2\2\2\61\u00ec\3\2\2\2\63\u00f0\3\2\2"+
		"\2\65\u00f2\3\2\2\2\67\u00f4\3\2\2\29\u00f6\3\2\2\2;\u00f8\3\2\2\2=\u00fa"+
		"\3\2\2\2?\u00fc\3\2\2\2A\u00fe\3\2\2\2C\u0100\3\2\2\2E\u0102\3\2\2\2G"+
		"\u0110\3\2\2\2I\u011e\3\2\2\2K\u0120\3\2\2\2M\u0138\3\2\2\2O\u015f\3\2"+
		"\2\2Q\u0182\3\2\2\2S\u0184\3\2\2\2U\u0191\3\2\2\2W\u01a9\3\2\2\2Y\u01ab"+
		"\3\2\2\2[\u01ba\3\2\2\2]\u01c0\3\2\2\2_\u01c2\3\2\2\2a\u01c4\3\2\2\2c"+
		"\u01c6\3\2\2\2e\u01c8\3\2\2\2g\u01ca\3\2\2\2i\u01cc\3\2\2\2k\u01ce\3\2"+
		"\2\2m\u01d0\3\2\2\2o\u01d2\3\2\2\2q\u01d4\3\2\2\2st\7k\2\2tx\7p\2\2uv"+
		"\7K\2\2vx\7P\2\2ws\3\2\2\2wu\3\2\2\2x\4\3\2\2\2yz\7/\2\2z{\7@\2\2{\6\3"+
		"\2\2\2|}\7$\2\2}\b\3\2\2\2~\177\7)\2\2\177\n\3\2\2\2\u0080\u0081\7.\2"+
		"\2\u0081\f\3\2\2\2\u0082\u0083\7\60\2\2\u0083\16\3\2\2\2\u0084\u0085\7"+
		"c\2\2\u0085\u0086\7p\2\2\u0086\u008d\7f\2\2\u0087\u0088\7(\2\2\u0088\u008d"+
		"\7(\2\2\u0089\u008a\7C\2\2\u008a\u008b\7P\2\2\u008b\u008d\7F\2\2\u008c"+
		"\u0084\3\2\2\2\u008c\u0087\3\2\2\2\u008c\u0089\3\2\2\2\u008d\20\3\2\2"+
		"\2\u008e\u008f\7q\2\2\u008f\u0095\7t\2\2\u0090\u0091\7~\2\2\u0091\u0095"+
		"\7~\2\2\u0092\u0093\7Q\2\2\u0093\u0095\7T\2\2\u0094\u008e\3\2\2\2\u0094"+
		"\u0090\3\2\2\2\u0094\u0092\3\2\2\2\u0095\22\3\2\2\2\u0096\u0097\7p\2\2"+
		"\u0097\u0098\7q\2\2\u0098\u009d\7v\2\2\u0099\u009a\7P\2\2\u009a\u009b"+
		"\7Q\2\2\u009b\u009d\7V\2\2\u009c\u0096\3\2\2\2\u009c\u0099\3\2\2\2\u009d"+
		"\24\3\2\2\2\u009e\u009f\7v\2\2\u009f\u00a0\7t\2\2\u00a0\u00a1\7w\2\2\u00a1"+
		"\u00a7\7g\2\2\u00a2\u00a3\7V\2\2\u00a3\u00a4\7T\2\2\u00a4\u00a5\7W\2\2"+
		"\u00a5\u00a7\7G\2\2\u00a6\u009e\3\2\2\2\u00a6\u00a2\3\2\2\2\u00a7\26\3"+
		"\2\2\2\u00a8\u00a9\7h\2\2\u00a9\u00aa\7c\2\2\u00aa\u00ab\7n\2\2\u00ab"+
		"\u00ac\7u\2\2\u00ac\u00b3\7g\2\2\u00ad\u00ae\7H\2\2\u00ae\u00af\7C\2\2"+
		"\u00af\u00b0\7N\2\2\u00b0\u00b1\7U\2\2\u00b1\u00b3\7G\2\2\u00b2\u00a8"+
		"\3\2\2\2\u00b2\u00ad\3\2\2\2\u00b3\30\3\2\2\2\u00b4\u00b5\7?\2\2\u00b5"+
		"\u00b6\7?\2\2\u00b6\32\3\2\2\2\u00b7\u00b8\7#\2\2\u00b8\u00b9\7?\2\2\u00b9"+
		"\34\3\2\2\2\u00ba\u00bb\7>\2\2\u00bb\36\3\2\2\2\u00bc\u00bd\7>\2\2\u00bd"+
		"\u00be\7?\2\2\u00be \3\2\2\2\u00bf\u00c0\7@\2\2\u00c0\"\3\2\2\2\u00c1"+
		"\u00c2\7@\2\2\u00c2\u00c3\7?\2\2\u00c3$\3\2\2\2\u00c4\u00c5\7A\2\2\u00c5"+
		"&\3\2\2\2\u00c6\u00c7\7<\2\2\u00c7(\3\2\2\2\u00c8\u00c9\7K\2\2\u00c9\u00cd"+
		"\7H\2\2\u00ca\u00cb\7k\2\2\u00cb\u00cd\7h\2\2\u00cc\u00c8\3\2\2\2\u00cc"+
		"\u00ca\3\2\2\2\u00cd*\3\2\2\2\u00ce\u00cf\7V\2\2\u00cf\u00d0\7J\2\2\u00d0"+
		"\u00d1\7G\2\2\u00d1\u00d7\7P\2\2\u00d2\u00d3\7v\2\2\u00d3\u00d4\7j\2\2"+
		"\u00d4\u00d5\7g\2\2\u00d5\u00d7\7p\2\2\u00d6\u00ce\3\2\2\2\u00d6\u00d2"+
		"\3\2\2\2\u00d7,\3\2\2\2\u00d8\u00d9\7G\2\2\u00d9\u00da\7N\2\2\u00da\u00db"+
		"\7U\2\2\u00db\u00e1\7G\2\2\u00dc\u00dd\7g\2\2\u00dd\u00de\7n\2\2\u00de"+
		"\u00df\7u\2\2\u00df\u00e1\7g\2\2\u00e0\u00d8\3\2\2\2\u00e0\u00dc\3\2\2"+
		"\2\u00e1.\3\2\2\2\u00e2\u00e3\7p\2\2\u00e3\u00e4\7w\2\2\u00e4\u00e5\7"+
		"n\2\2\u00e5\u00eb\7n\2\2\u00e6\u00e7\7P\2\2\u00e7\u00e8\7W\2\2\u00e8\u00e9"+
		"\7N\2\2\u00e9\u00eb\7N\2\2\u00ea\u00e2\3\2\2\2\u00ea\u00e6\3\2\2\2\u00eb"+
		"\60\3\2\2\2\u00ec\u00ed\7P\2\2\u00ed\u00ee\7c\2\2\u00ee\u00ef\7P\2\2\u00ef"+
		"\62\3\2\2\2\u00f0\u00f1\7/\2\2\u00f1\64\3\2\2\2\u00f2\u00f3\7-\2\2\u00f3"+
		"\66\3\2\2\2\u00f4\u00f5\7\61\2\2\u00f58\3\2\2\2\u00f6\u00f7\7,\2\2\u00f7"+
		":\3\2\2\2\u00f8\u00f9\7}\2\2\u00f9<\3\2\2\2\u00fa\u00fb\7\177\2\2\u00fb"+
		">\3\2\2\2\u00fc\u00fd\7]\2\2\u00fd@\3\2\2\2\u00fe\u00ff\7_\2\2\u00ffB"+
		"\3\2\2\2\u0100\u0101\7*\2\2\u0101D\3\2\2\2\u0102\u0103\7+\2\2\u0103F\3"+
		"\2\2\2\u0104\u0105\7p\2\2\u0105\u0106\7q\2\2\u0106\u0107\7v\2\2\u0107"+
		"\u0108\7\"\2\2\u0108\u0109\7k\2\2\u0109\u0111\7p\2\2\u010a\u010b\7P\2"+
		"\2\u010b\u010c\7Q\2\2\u010c\u010d\7V\2\2\u010d\u010e\7\"\2\2\u010e\u010f"+
		"\7K\2\2\u010f\u0111\7P\2\2\u0110\u0104\3\2\2\2\u0110\u010a\3\2\2\2\u0111"+
		"H\3\2\2\2\u0112\u0113\7g\2\2\u0113\u0114\7z\2\2\u0114\u0115\7k\2\2\u0115"+
		"\u0116\7u\2\2\u0116\u0117\7v\2\2\u0117\u011f\7u\2\2\u0118\u0119\7G\2\2"+
		"\u0119\u011a\7Z\2\2\u011a\u011b\7K\2\2\u011b\u011c\7U\2\2\u011c\u011d"+
		"\7V\2\2\u011d\u011f\7U\2\2\u011e\u0112\3\2\2\2\u011e\u0118\3\2\2\2\u011f"+
		"J\3\2\2\2\u0120\u0123\5e\63\2\u0121\u0124\5\65\33\2\u0122\u0124\5\63\32"+
		"\2\u0123\u0121\3\2\2\2\u0123\u0122\3\2\2\2\u0123\u0124\3\2\2\2\u0124\u0126"+
		"\3\2\2\2\u0125\u0127\5a\61\2\u0126\u0125\3\2\2\2\u0127\u0128\3\2\2\2\u0128"+
		"\u0126\3\2\2\2\u0128\u0129\3\2\2\2\u0129L\3\2\2\2\u012a\u012c\5\63\32"+
		"\2\u012b\u012a\3\2\2\2\u012b\u012c\3\2\2\2\u012c\u012d\3\2\2\2\u012d\u0139"+
		"\5]/\2\u012e\u0130\5\63\32\2\u012f\u012e\3\2\2\2\u012f\u0130\3\2\2\2\u0130"+
		"\u0131\3\2\2\2\u0131\u0135\5_\60\2\u0132\u0134\5a\61\2\u0133\u0132\3\2"+
		"\2\2\u0134\u0137\3\2\2\2\u0135\u0133\3\2\2\2\u0135\u0136\3\2\2\2\u0136"+
		"\u0139\3\2\2\2\u0137\u0135\3\2\2\2\u0138\u012b\3\2\2\2\u0138\u012f\3\2"+
		"\2\2\u0139N\3\2\2\2\u013a\u013b\5M\'\2\u013b\u013f\5\r\7\2\u013c\u013e"+
		"\5a\61\2\u013d\u013c\3\2\2\2\u013e\u0141\3\2\2\2\u013f\u013d\3\2\2\2\u013f"+
		"\u0140\3\2\2\2\u0140\u0143\3\2\2\2\u0141\u013f\3\2\2\2\u0142\u0144\5K"+
		"&\2\u0143\u0142\3\2\2\2\u0143\u0144\3\2\2\2\u0144\u0146\3\2\2\2\u0145"+
		"\u0147\5c\62\2\u0146\u0145\3\2\2\2\u0146\u0147\3\2\2\2\u0147\u0160\3\2"+
		"\2\2\u0148\u014a\5\r\7\2\u0149\u014b\5a\61\2\u014a\u0149\3\2\2\2\u014b"+
		"\u014c\3\2\2\2\u014c\u014a\3\2\2\2\u014c\u014d\3\2\2\2\u014d\u014f\3\2"+
		"\2\2\u014e\u0150\5K&\2\u014f\u014e\3\2\2\2\u014f\u0150\3\2\2\2\u0150\u0152"+
		"\3\2\2\2\u0151\u0153\5c\62\2\u0152\u0151\3\2\2\2\u0152\u0153\3\2\2\2\u0153"+
		"\u0160\3\2\2\2\u0154\u0155\5M\'\2\u0155\u0157\5K&\2\u0156\u0158\5c\62"+
		"\2\u0157\u0156\3\2\2\2\u0157\u0158\3\2\2\2\u0158\u0160\3\2\2\2\u0159\u015b"+
		"\5M\'\2\u015a\u015c\5K&\2\u015b\u015a\3\2\2\2\u015b\u015c\3\2\2\2\u015c"+
		"\u015d\3\2\2\2\u015d\u015e\5c\62\2\u015e\u0160\3\2\2\2\u015f\u013a\3\2"+
		"\2\2\u015f\u0148\3\2\2\2\u015f\u0154\3\2\2\2\u015f\u0159\3\2\2\2\u0160"+
		"P\3\2\2\2\u0161\u0162\5M\'\2\u0162\u0166\5\r\7\2\u0163\u0165\5a\61\2\u0164"+
		"\u0163\3\2\2\2\u0165\u0168\3\2\2\2\u0166\u0164\3\2\2\2\u0166\u0167\3\2"+
		"\2\2\u0167\u016a\3\2\2\2\u0168\u0166\3\2\2\2\u0169\u016b\5K&\2\u016a\u0169"+
		"\3\2\2\2\u016a\u016b\3\2\2\2\u016b\u016c\3\2\2\2\u016c\u016d\5g\64\2\u016d"+
		"\u0183\3\2\2\2\u016e\u0170\5\63\32\2\u016f\u016e\3\2\2\2\u016f\u0170\3"+
		"\2\2\2\u0170\u0171\3\2\2\2\u0171\u0173\5\r\7\2\u0172\u0174\5a\61\2\u0173"+
		"\u0172\3\2\2\2\u0174\u0175\3\2\2\2\u0175\u0173\3\2\2\2\u0175\u0176\3\2"+
		"\2\2\u0176\u0178\3\2\2\2\u0177\u0179\5K&\2\u0178\u0177\3\2\2\2\u0178\u0179"+
		"\3\2\2\2\u0179\u017a\3\2\2\2\u017a\u017b\5g\64\2\u017b\u0183\3\2\2\2\u017c"+
		"\u017e\5M\'\2\u017d\u017f\5K&\2\u017e\u017d\3\2\2\2\u017e\u017f\3\2\2"+
		"\2\u017f\u0180\3\2\2\2\u0180\u0181\5g\64\2\u0181\u0183\3\2\2\2\u0182\u0161"+
		"\3\2\2\2\u0182\u016f\3\2\2\2\u0182\u017c\3\2\2\2\u0183R\3\2\2\2\u0184"+
		"\u0185\5M\'\2\u0185\u0186\5i\65\2\u0186T\3\2\2\2\u0187\u0192\5m\67\2\u0188"+
		"\u018c\5m\67\2\u0189\u018b\5o8\2\u018a\u0189\3\2\2\2\u018b\u018e\3\2\2"+
		"\2\u018c\u018a\3\2\2\2\u018c\u018d\3\2\2\2\u018d\u018f\3\2\2\2\u018e\u018c"+
		"\3\2\2\2\u018f\u0190\5q9\2\u0190\u0192\3\2\2\2\u0191\u0187\3\2\2\2\u0191"+
		"\u0188\3\2\2\2\u0192V\3\2\2\2\u0193\u0199\5\t\5\2\u0194\u0198\n\2\2\2"+
		"\u0195\u0196\7^\2\2\u0196\u0198\t\3\2\2\u0197\u0194\3\2\2\2\u0197\u0195"+
		"\3\2\2\2\u0198\u019b\3\2\2\2\u0199\u0197\3\2\2\2\u0199\u019a\3\2\2\2\u019a"+
		"\u019c\3\2\2\2\u019b\u0199\3\2\2\2\u019c\u019d\5\t\5\2\u019d\u01aa\3\2"+
		"\2\2\u019e\u01a4\5\7\4\2\u019f\u01a3\n\4\2\2\u01a0\u01a1\7^\2\2\u01a1"+
		"\u01a3\t\5\2\2\u01a2\u019f\3\2\2\2\u01a2\u01a0\3\2\2\2\u01a3\u01a6\3\2"+
		"\2\2\u01a4\u01a2\3\2\2\2\u01a4\u01a5\3\2\2\2\u01a5\u01a7\3\2\2\2\u01a6"+
		"\u01a4\3\2\2\2\u01a7\u01a8\5\7\4\2\u01a8\u01aa\3\2\2\2\u01a9\u0193\3\2"+
		"\2\2\u01a9\u019e\3\2\2\2\u01aaX\3\2\2\2\u01ab\u01ac\7\61\2\2\u01ac\u01ad"+
		"\7\61\2\2\u01ad\u01af\3\2\2\2\u01ae\u01b0\13\2\2\2\u01af\u01ae\3\2\2\2"+
		"\u01b0\u01b1\3\2\2\2\u01b1\u01b2\3\2\2\2\u01b1\u01af\3\2\2\2\u01b2\u01b5"+
		"\3\2\2\2\u01b3\u01b6\5k\66\2\u01b4\u01b6\7\2\2\3\u01b5\u01b3\3\2\2\2\u01b5"+
		"\u01b4\3\2\2\2\u01b6\u01b7\3\2\2\2\u01b7\u01b8\b-\2\2\u01b8Z\3\2\2\2\u01b9"+
		"\u01bb\t\6\2\2\u01ba\u01b9\3\2\2\2\u01bb\u01bc\3\2\2\2\u01bc\u01ba\3\2"+
		"\2\2\u01bc\u01bd\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\u01bf\b.\2\2\u01bf"+
		"\\\3\2\2\2\u01c0\u01c1\7\62\2\2\u01c1^\3\2\2\2\u01c2\u01c3\4\63;\2\u01c3"+
		"`\3\2\2\2\u01c4\u01c5\4\62;\2\u01c5b\3\2\2\2\u01c6\u01c7\t\7\2\2\u01c7"+
		"d\3\2\2\2\u01c8\u01c9\t\b\2\2\u01c9f\3\2\2\2\u01ca\u01cb\t\t\2\2\u01cb"+
		"h\3\2\2\2\u01cc\u01cd\t\n\2\2\u01cdj\3\2\2\2\u01ce\u01cf\7\f\2\2\u01cf"+
		"l\3\2\2\2\u01d0\u01d1\t\13\2\2\u01d1n\3\2\2\2\u01d2\u01d3\t\f\2\2\u01d3"+
		"p\3\2\2\2\u01d4\u01d5\t\r\2\2\u01d5r\3\2\2\2/\2w\u008c\u0094\u009c\u00a6"+
		"\u00b2\u00cc\u00d6\u00e0\u00ea\u0110\u011e\u0123\u0128\u012b\u012f\u0135"+
		"\u0138\u013f\u0143\u0146\u014c\u014f\u0152\u0157\u015b\u015f\u0166\u016a"+
		"\u016f\u0175\u0178\u017e\u0182\u018c\u0191\u0197\u0199\u01a2\u01a4\u01a9"+
		"\u01b1\u01b5\u01bc\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}