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
		IN=1, REFERENCE_OP=2, DOUBLE_QUOTE=3, SINGLE_QUOTE=4, COMMA=5, PERIOD=6, 
		AND=7, OR=8, NOT=9, TRUE=10, FALSE=11, EQ=12, NEQ=13, LT=14, LTE=15, GT=16, 
		GTE=17, QUESTION=18, COLON=19, IF=20, THEN=21, ELSE=22, NULL=23, MINUS=24, 
		PLUS=25, DIV=26, MUL=27, LBRACE=28, RBRACE=29, LBRACKET=30, RBRACKET=31, 
		LPAREN=32, RPAREN=33, NIN=34, EXISTS=35, EXPONENT=36, INT_LITERAL=37, 
		DOUBLE_LITERAL=38, FLOAT_LITERAL=39, LONG_LITERAL=40, IDENTIFIER=41, STRING_LITERAL=42, 
		COMMENT=43, WS=44;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"IN", "REFERENCE_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "MINUS", "PLUS", "DIV", 
		"MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", 
		"NIN", "EXISTS", "EXPONENT", "INT_LITERAL", "DOUBLE_LITERAL", "FLOAT_LITERAL", 
		"LONG_LITERAL", "IDENTIFIER", "STRING_LITERAL", "COMMENT", "WS", "ZERO", 
		"FIRST_DIGIT", "DIGIT", "SCHAR", "D", "E", "F", "L", "EOL", "IDENTIFIER_START", 
		"IDENTIFIER_MIDDLE", "IDENTIFIER_END"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2.\u01cf\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\3\2\3\2\3\2\3\2\5\2x\n\2"+
		"\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\5\b\u008c\n\b\3\t\3\t\3\t\3\t\3\t\3\t\5\t\u0094\n\t\3\n\3\n\3\n\3\n"+
		"\3\n\3\n\5\n\u009c\n\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u00a6"+
		"\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\5\f\u00b2\n\f\3\r\3\r\3"+
		"\r\3\16\3\16\3\16\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\22\3\22\3\22\3"+
		"\23\3\23\3\24\3\24\3\25\3\25\3\25\3\25\5\25\u00cc\n\25\3\26\3\26\3\26"+
		"\3\26\3\26\3\26\3\26\3\26\5\26\u00d6\n\26\3\27\3\27\3\27\3\27\3\27\3\27"+
		"\3\27\3\27\5\27\u00e0\n\27\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\5\30"+
		"\u00ea\n\30\3\31\3\31\3\32\3\32\3\33\3\33\3\34\3\34\3\35\3\35\3\36\3\36"+
		"\3\37\3\37\3 \3 \3!\3!\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\5#"+
		"\u010c\n#\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\5$\u011a\n$\3%\3%\3%\5%"+
		"\u011f\n%\3%\6%\u0122\n%\r%\16%\u0123\3&\5&\u0127\n&\3&\3&\5&\u012b\n"+
		"&\3&\3&\7&\u012f\n&\f&\16&\u0132\13&\5&\u0134\n&\3\'\3\'\3\'\7\'\u0139"+
		"\n\'\f\'\16\'\u013c\13\'\3\'\5\'\u013f\n\'\3\'\5\'\u0142\n\'\3\'\3\'\6"+
		"\'\u0146\n\'\r\'\16\'\u0147\3\'\5\'\u014b\n\'\3\'\5\'\u014e\n\'\3\'\3"+
		"\'\3\'\5\'\u0153\n\'\3\'\3\'\5\'\u0157\n\'\3\'\3\'\5\'\u015b\n\'\3(\3"+
		"(\3(\7(\u0160\n(\f(\16(\u0163\13(\3(\5(\u0166\n(\3(\3(\3(\5(\u016b\n("+
		"\3(\3(\6(\u016f\n(\r(\16(\u0170\3(\5(\u0174\n(\3(\3(\3(\3(\5(\u017a\n"+
		"(\3(\3(\5(\u017e\n(\3)\3)\3)\3*\3*\3*\7*\u0186\n*\f*\16*\u0189\13*\3*"+
		"\3*\5*\u018d\n*\3+\3+\7+\u0191\n+\f+\16+\u0194\13+\3+\3+\3+\3+\7+\u019a"+
		"\n+\f+\16+\u019d\13+\3+\3+\5+\u01a1\n+\3,\3,\3,\3,\6,\u01a7\n,\r,\16,"+
		"\u01a8\3,\3,\5,\u01ad\n,\3,\3,\3-\6-\u01b2\n-\r-\16-\u01b3\3-\3-\3.\3"+
		".\3/\3/\3\60\3\60\3\61\3\61\3\62\3\62\3\63\3\63\3\64\3\64\3\65\3\65\3"+
		"\66\3\66\3\67\3\67\38\38\39\39\3\u01a8\2:\3\3\5\4\7\5\t\6\13\7\r\b\17"+
		"\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+"+
		"\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G%I&K\'M(O)Q*S+"+
		"U,W-Y.[\2]\2_\2a\2c\2e\2g\2i\2k\2m\2o\2q\2\3\2\13\5\2\13\f\16\17\"\"\7"+
		"\2\f\f\17\17$$))^^\4\2FFff\4\2GGgg\4\2HHhh\4\2NNnn\6\2&&C\\aac|\b\2\60"+
		"\60\62<C\\^^aac|\b\2\60\60\62;C\\^^aac|\u01f2\2\3\3\2\2\2\2\5\3\2\2\2"+
		"\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3"+
		"\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2"+
		"\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2"+
		"\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63\3\2"+
		"\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2?\3\2"+
		"\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2\2\2K\3\2\2\2"+
		"\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2W\3\2\2\2\2Y"+
		"\3\2\2\2\3w\3\2\2\2\5y\3\2\2\2\7{\3\2\2\2\t}\3\2\2\2\13\177\3\2\2\2\r"+
		"\u0081\3\2\2\2\17\u008b\3\2\2\2\21\u0093\3\2\2\2\23\u009b\3\2\2\2\25\u00a5"+
		"\3\2\2\2\27\u00b1\3\2\2\2\31\u00b3\3\2\2\2\33\u00b6\3\2\2\2\35\u00b9\3"+
		"\2\2\2\37\u00bb\3\2\2\2!\u00be\3\2\2\2#\u00c0\3\2\2\2%\u00c3\3\2\2\2\'"+
		"\u00c5\3\2\2\2)\u00cb\3\2\2\2+\u00d5\3\2\2\2-\u00df\3\2\2\2/\u00e9\3\2"+
		"\2\2\61\u00eb\3\2\2\2\63\u00ed\3\2\2\2\65\u00ef\3\2\2\2\67\u00f1\3\2\2"+
		"\29\u00f3\3\2\2\2;\u00f5\3\2\2\2=\u00f7\3\2\2\2?\u00f9\3\2\2\2A\u00fb"+
		"\3\2\2\2C\u00fd\3\2\2\2E\u010b\3\2\2\2G\u0119\3\2\2\2I\u011b\3\2\2\2K"+
		"\u0133\3\2\2\2M\u015a\3\2\2\2O\u017d\3\2\2\2Q\u017f\3\2\2\2S\u018c\3\2"+
		"\2\2U\u01a0\3\2\2\2W\u01a2\3\2\2\2Y\u01b1\3\2\2\2[\u01b7\3\2\2\2]\u01b9"+
		"\3\2\2\2_\u01bb\3\2\2\2a\u01bd\3\2\2\2c\u01bf\3\2\2\2e\u01c1\3\2\2\2g"+
		"\u01c3\3\2\2\2i\u01c5\3\2\2\2k\u01c7\3\2\2\2m\u01c9\3\2\2\2o\u01cb\3\2"+
		"\2\2q\u01cd\3\2\2\2st\7k\2\2tx\7p\2\2uv\7K\2\2vx\7P\2\2ws\3\2\2\2wu\3"+
		"\2\2\2x\4\3\2\2\2yz\7(\2\2z\6\3\2\2\2{|\7$\2\2|\b\3\2\2\2}~\7)\2\2~\n"+
		"\3\2\2\2\177\u0080\7.\2\2\u0080\f\3\2\2\2\u0081\u0082\7\60\2\2\u0082\16"+
		"\3\2\2\2\u0083\u0084\7c\2\2\u0084\u0085\7p\2\2\u0085\u008c\7f\2\2\u0086"+
		"\u0087\7(\2\2\u0087\u008c\7(\2\2\u0088\u0089\7C\2\2\u0089\u008a\7P\2\2"+
		"\u008a\u008c\7F\2\2\u008b\u0083\3\2\2\2\u008b\u0086\3\2\2\2\u008b\u0088"+
		"\3\2\2\2\u008c\20\3\2\2\2\u008d\u008e\7q\2\2\u008e\u0094\7t\2\2\u008f"+
		"\u0090\7~\2\2\u0090\u0094\7~\2\2\u0091\u0092\7Q\2\2\u0092\u0094\7T\2\2"+
		"\u0093\u008d\3\2\2\2\u0093\u008f\3\2\2\2\u0093\u0091\3\2\2\2\u0094\22"+
		"\3\2\2\2\u0095\u0096\7p\2\2\u0096\u0097\7q\2\2\u0097\u009c\7v\2\2\u0098"+
		"\u0099\7P\2\2\u0099\u009a\7Q\2\2\u009a\u009c\7V\2\2\u009b\u0095\3\2\2"+
		"\2\u009b\u0098\3\2\2\2\u009c\24\3\2\2\2\u009d\u009e\7v\2\2\u009e\u009f"+
		"\7t\2\2\u009f\u00a0\7w\2\2\u00a0\u00a6\7g\2\2\u00a1\u00a2\7V\2\2\u00a2"+
		"\u00a3\7T\2\2\u00a3\u00a4\7W\2\2\u00a4\u00a6\7G\2\2\u00a5\u009d\3\2\2"+
		"\2\u00a5\u00a1\3\2\2\2\u00a6\26\3\2\2\2\u00a7\u00a8\7h\2\2\u00a8\u00a9"+
		"\7c\2\2\u00a9\u00aa\7n\2\2\u00aa\u00ab\7u\2\2\u00ab\u00b2\7g\2\2\u00ac"+
		"\u00ad\7H\2\2\u00ad\u00ae\7C\2\2\u00ae\u00af\7N\2\2\u00af\u00b0\7U\2\2"+
		"\u00b0\u00b2\7G\2\2\u00b1\u00a7\3\2\2\2\u00b1\u00ac\3\2\2\2\u00b2\30\3"+
		"\2\2\2\u00b3\u00b4\7?\2\2\u00b4\u00b5\7?\2\2\u00b5\32\3\2\2\2\u00b6\u00b7"+
		"\7#\2\2\u00b7\u00b8\7?\2\2\u00b8\34\3\2\2\2\u00b9\u00ba\7>\2\2\u00ba\36"+
		"\3\2\2\2\u00bb\u00bc\7>\2\2\u00bc\u00bd\7?\2\2\u00bd \3\2\2\2\u00be\u00bf"+
		"\7@\2\2\u00bf\"\3\2\2\2\u00c0\u00c1\7@\2\2\u00c1\u00c2\7?\2\2\u00c2$\3"+
		"\2\2\2\u00c3\u00c4\7A\2\2\u00c4&\3\2\2\2\u00c5\u00c6\7<\2\2\u00c6(\3\2"+
		"\2\2\u00c7\u00c8\7K\2\2\u00c8\u00cc\7H\2\2\u00c9\u00ca\7k\2\2\u00ca\u00cc"+
		"\7h\2\2\u00cb\u00c7\3\2\2\2\u00cb\u00c9\3\2\2\2\u00cc*\3\2\2\2\u00cd\u00ce"+
		"\7V\2\2\u00ce\u00cf\7J\2\2\u00cf\u00d0\7G\2\2\u00d0\u00d6\7P\2\2\u00d1"+
		"\u00d2\7v\2\2\u00d2\u00d3\7j\2\2\u00d3\u00d4\7g\2\2\u00d4\u00d6\7p\2\2"+
		"\u00d5\u00cd\3\2\2\2\u00d5\u00d1\3\2\2\2\u00d6,\3\2\2\2\u00d7\u00d8\7"+
		"G\2\2\u00d8\u00d9\7N\2\2\u00d9\u00da\7U\2\2\u00da\u00e0\7G\2\2\u00db\u00dc"+
		"\7g\2\2\u00dc\u00dd\7n\2\2\u00dd\u00de\7u\2\2\u00de\u00e0\7g\2\2\u00df"+
		"\u00d7\3\2\2\2\u00df\u00db\3\2\2\2\u00e0.\3\2\2\2\u00e1\u00e2\7p\2\2\u00e2"+
		"\u00e3\7w\2\2\u00e3\u00e4\7n\2\2\u00e4\u00ea\7n\2\2\u00e5\u00e6\7P\2\2"+
		"\u00e6\u00e7\7W\2\2\u00e7\u00e8\7N\2\2\u00e8\u00ea\7N\2\2\u00e9\u00e1"+
		"\3\2\2\2\u00e9\u00e5\3\2\2\2\u00ea\60\3\2\2\2\u00eb\u00ec\7/\2\2\u00ec"+
		"\62\3\2\2\2\u00ed\u00ee\7-\2\2\u00ee\64\3\2\2\2\u00ef\u00f0\7\61\2\2\u00f0"+
		"\66\3\2\2\2\u00f1\u00f2\7,\2\2\u00f28\3\2\2\2\u00f3\u00f4\7}\2\2\u00f4"+
		":\3\2\2\2\u00f5\u00f6\7\177\2\2\u00f6<\3\2\2\2\u00f7\u00f8\7]\2\2\u00f8"+
		">\3\2\2\2\u00f9\u00fa\7_\2\2\u00fa@\3\2\2\2\u00fb\u00fc\7*\2\2\u00fcB"+
		"\3\2\2\2\u00fd\u00fe\7+\2\2\u00feD\3\2\2\2\u00ff\u0100\7p\2\2\u0100\u0101"+
		"\7q\2\2\u0101\u0102\7v\2\2\u0102\u0103\7\"\2\2\u0103\u0104\7k\2\2\u0104"+
		"\u010c\7p\2\2\u0105\u0106\7P\2\2\u0106\u0107\7Q\2\2\u0107\u0108\7V\2\2"+
		"\u0108\u0109\7\"\2\2\u0109\u010a\7K\2\2\u010a\u010c\7P\2\2\u010b\u00ff"+
		"\3\2\2\2\u010b\u0105\3\2\2\2\u010cF\3\2\2\2\u010d\u010e\7g\2\2\u010e\u010f"+
		"\7z\2\2\u010f\u0110\7k\2\2\u0110\u0111\7u\2\2\u0111\u0112\7v\2\2\u0112"+
		"\u011a\7u\2\2\u0113\u0114\7G\2\2\u0114\u0115\7Z\2\2\u0115\u0116\7K\2\2"+
		"\u0116\u0117\7U\2\2\u0117\u0118\7V\2\2\u0118\u011a\7U\2\2\u0119\u010d"+
		"\3\2\2\2\u0119\u0113\3\2\2\2\u011aH\3\2\2\2\u011b\u011e\5e\63\2\u011c"+
		"\u011f\5\63\32\2\u011d\u011f\5\61\31\2\u011e\u011c\3\2\2\2\u011e\u011d"+
		"\3\2\2\2\u011e\u011f\3\2\2\2\u011f\u0121\3\2\2\2\u0120\u0122\5_\60\2\u0121"+
		"\u0120\3\2\2\2\u0122\u0123\3\2\2\2\u0123\u0121\3\2\2\2\u0123\u0124\3\2"+
		"\2\2\u0124J\3\2\2\2\u0125\u0127\5\61\31\2\u0126\u0125\3\2\2\2\u0126\u0127"+
		"\3\2\2\2\u0127\u0128\3\2\2\2\u0128\u0134\5[.\2\u0129\u012b\5\61\31\2\u012a"+
		"\u0129\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u012c\3\2\2\2\u012c\u0130\5]"+
		"/\2\u012d\u012f\5_\60\2\u012e\u012d\3\2\2\2\u012f\u0132\3\2\2\2\u0130"+
		"\u012e\3\2\2\2\u0130\u0131\3\2\2\2\u0131\u0134\3\2\2\2\u0132\u0130\3\2"+
		"\2\2\u0133\u0126\3\2\2\2\u0133\u012a\3\2\2\2\u0134L\3\2\2\2\u0135\u0136"+
		"\5K&\2\u0136\u013a\5\r\7\2\u0137\u0139\5_\60\2\u0138\u0137\3\2\2\2\u0139"+
		"\u013c\3\2\2\2\u013a\u0138\3\2\2\2\u013a\u013b\3\2\2\2\u013b\u013e\3\2"+
		"\2\2\u013c\u013a\3\2\2\2\u013d\u013f\5I%\2\u013e\u013d\3\2\2\2\u013e\u013f"+
		"\3\2\2\2\u013f\u0141\3\2\2\2\u0140\u0142\5c\62\2\u0141\u0140\3\2\2\2\u0141"+
		"\u0142\3\2\2\2\u0142\u015b\3\2\2\2\u0143\u0145\5\r\7\2\u0144\u0146\5_"+
		"\60\2\u0145\u0144\3\2\2\2\u0146\u0147\3\2\2\2\u0147\u0145\3\2\2\2\u0147"+
		"\u0148\3\2\2\2\u0148\u014a\3\2\2\2\u0149\u014b\5I%\2\u014a\u0149\3\2\2"+
		"\2\u014a\u014b\3\2\2\2\u014b\u014d\3\2\2\2\u014c\u014e\5c\62\2\u014d\u014c"+
		"\3\2\2\2\u014d\u014e\3\2\2\2\u014e\u015b\3\2\2\2\u014f\u0150\5K&\2\u0150"+
		"\u0152\5I%\2\u0151\u0153\5c\62\2\u0152\u0151\3\2\2\2\u0152\u0153\3\2\2"+
		"\2\u0153\u015b\3\2\2\2\u0154\u0156\5K&\2\u0155\u0157\5I%\2\u0156\u0155"+
		"\3\2\2\2\u0156\u0157\3\2\2\2\u0157\u0158\3\2\2\2\u0158\u0159\5c\62\2\u0159"+
		"\u015b\3\2\2\2\u015a\u0135\3\2\2\2\u015a\u0143\3\2\2\2\u015a\u014f\3\2"+
		"\2\2\u015a\u0154\3\2\2\2\u015bN\3\2\2\2\u015c\u015d\5K&\2\u015d\u0161"+
		"\5\r\7\2\u015e\u0160\5_\60\2\u015f\u015e\3\2\2\2\u0160\u0163\3\2\2\2\u0161"+
		"\u015f\3\2\2\2\u0161\u0162\3\2\2\2\u0162\u0165\3\2\2\2\u0163\u0161\3\2"+
		"\2\2\u0164\u0166\5I%\2\u0165\u0164\3\2\2\2\u0165\u0166\3\2\2\2\u0166\u0167"+
		"\3\2\2\2\u0167\u0168\5g\64\2\u0168\u017e\3\2\2\2\u0169\u016b\5\61\31\2"+
		"\u016a\u0169\3\2\2\2\u016a\u016b\3\2\2\2\u016b\u016c\3\2\2\2\u016c\u016e"+
		"\5\r\7\2\u016d\u016f\5_\60\2\u016e\u016d\3\2\2\2\u016f\u0170\3\2\2\2\u0170"+
		"\u016e\3\2\2\2\u0170\u0171\3\2\2\2\u0171\u0173\3\2\2\2\u0172\u0174\5I"+
		"%\2\u0173\u0172\3\2\2\2\u0173\u0174\3\2\2\2\u0174\u0175\3\2\2\2\u0175"+
		"\u0176\5g\64\2\u0176\u017e\3\2\2\2\u0177\u0179\5K&\2\u0178\u017a\5I%\2"+
		"\u0179\u0178\3\2\2\2\u0179\u017a\3\2\2\2\u017a\u017b\3\2\2\2\u017b\u017c"+
		"\5g\64\2\u017c\u017e\3\2\2\2\u017d\u015c\3\2\2\2\u017d\u016a\3\2\2\2\u017d"+
		"\u0177\3\2\2\2\u017eP\3\2\2\2\u017f\u0180\5K&\2\u0180\u0181\5i\65\2\u0181"+
		"R\3\2\2\2\u0182\u018d\5m\67\2\u0183\u0187\5m\67\2\u0184\u0186\5o8\2\u0185"+
		"\u0184\3\2\2\2\u0186\u0189\3\2\2\2\u0187\u0185\3\2\2\2\u0187\u0188\3\2"+
		"\2\2\u0188\u018a\3\2\2\2\u0189\u0187\3\2\2\2\u018a\u018b\5q9\2\u018b\u018d"+
		"\3\2\2\2\u018c\u0182\3\2\2\2\u018c\u0183\3\2\2\2\u018dT\3\2\2\2\u018e"+
		"\u0192\5\7\4\2\u018f\u0191\5a\61\2\u0190\u018f\3\2\2\2\u0191\u0194\3\2"+
		"\2\2\u0192\u0190\3\2\2\2\u0192\u0193\3\2\2\2\u0193\u0195\3\2\2\2\u0194"+
		"\u0192\3\2\2\2\u0195\u0196\5\7\4\2\u0196\u01a1\3\2\2\2\u0197\u019b\5\t"+
		"\5\2\u0198\u019a\5a\61\2\u0199\u0198\3\2\2\2\u019a\u019d\3\2\2\2\u019b"+
		"\u0199\3\2\2\2\u019b\u019c\3\2\2\2\u019c\u019e\3\2\2\2\u019d\u019b\3\2"+
		"\2\2\u019e\u019f\5\t\5\2\u019f\u01a1\3\2\2\2\u01a0\u018e\3\2\2\2\u01a0"+
		"\u0197\3\2\2\2\u01a1V\3\2\2\2\u01a2\u01a3\7\61\2\2\u01a3\u01a4\7\61\2"+
		"\2\u01a4\u01a6\3\2\2\2\u01a5\u01a7\13\2\2\2\u01a6\u01a5\3\2\2\2\u01a7"+
		"\u01a8\3\2\2\2\u01a8\u01a9\3\2\2\2\u01a8\u01a6\3\2\2\2\u01a9\u01ac\3\2"+
		"\2\2\u01aa\u01ad\5k\66\2\u01ab\u01ad\7\2\2\3\u01ac\u01aa\3\2\2\2\u01ac"+
		"\u01ab\3\2\2\2\u01ad\u01ae\3\2\2\2\u01ae\u01af\b,\2\2\u01afX\3\2\2\2\u01b0"+
		"\u01b2\t\2\2\2\u01b1\u01b0\3\2\2\2\u01b2\u01b3\3\2\2\2\u01b3\u01b1\3\2"+
		"\2\2\u01b3\u01b4\3\2\2\2\u01b4\u01b5\3\2\2\2\u01b5\u01b6\b-\2\2\u01b6"+
		"Z\3\2\2\2\u01b7\u01b8\7\62\2\2\u01b8\\\3\2\2\2\u01b9\u01ba\4\63;\2\u01ba"+
		"^\3\2\2\2\u01bb\u01bc\4\62;\2\u01bc`\3\2\2\2\u01bd\u01be\n\3\2\2\u01be"+
		"b\3\2\2\2\u01bf\u01c0\t\4\2\2\u01c0d\3\2\2\2\u01c1\u01c2\t\5\2\2\u01c2"+
		"f\3\2\2\2\u01c3\u01c4\t\6\2\2\u01c4h\3\2\2\2\u01c5\u01c6\t\7\2\2\u01c6"+
		"j\3\2\2\2\u01c7\u01c8\7\f\2\2\u01c8l\3\2\2\2\u01c9\u01ca\t\b\2\2\u01ca"+
		"n\3\2\2\2\u01cb\u01cc\t\t\2\2\u01ccp\3\2\2\2\u01cd\u01ce\t\n\2\2\u01ce"+
		"r\3\2\2\2-\2w\u008b\u0093\u009b\u00a5\u00b1\u00cb\u00d5\u00df\u00e9\u010b"+
		"\u0119\u011e\u0123\u0126\u012a\u0130\u0133\u013a\u013e\u0141\u0147\u014a"+
		"\u014d\u0152\u0156\u015a\u0161\u0165\u016a\u0170\u0173\u0179\u017d\u0187"+
		"\u018c\u0192\u019b\u01a0\u01a8\u01ac\u01b3\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}