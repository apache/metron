// Generated from org/apache/metron/common/stellar/generated/Stellar.g4 by ANTLR 4.5
package org.apache.metron.common.stellar.generated;

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
		COMMA=1, AND=2, OR=3, NOT=4, TRUE=5, FALSE=6, EQ=7, NEQ=8, LT=9, LTE=10, 
		GT=11, GTE=12, QUESTION=13, COLON=14, IF=15, MINUS=16, PLUS=17, DIV=18, 
		MUL=19, LBRACKET=20, RBRACKET=21, LPAREN=22, RPAREN=23, IN=24, NIN=25, 
		EXISTS=26, INT_LITERAL=27, DOUBLE_LITERAL=28, IDENTIFIER=29, STRING_LITERAL=30, 
		COMMENT=31, WS=32;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"COMMA", "AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", 
		"GT", "GTE", "QUESTION", "COLON", "IF", "MINUS", "PLUS", "DIV", "MUL", 
		"LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "IN", "NIN", "EXISTS", "INT_LITERAL", 
		"DOUBLE_LITERAL", "IDENTIFIER", "SCHAR", "STRING_LITERAL", "COMMENT", 
		"WS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "','", null, null, null, null, null, "'=='", "'!='", "'<'", "'<='", 
		"'>'", "'>='", null, null, null, "'-'", "'+'", "'/'", "'*'", "'['", "']'", 
		"'('", "')'", "'in'", "'not in'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "COMMA", "AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", 
		"LTE", "GT", "GTE", "QUESTION", "COLON", "IF", "MINUS", "PLUS", "DIV", 
		"MUL", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "IN", "NIN", "EXISTS", 
		"INT_LITERAL", "DOUBLE_LITERAL", "IDENTIFIER", "STRING_LITERAL", "COMMENT", 
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\"\u0110\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3P\n\3\3\4\3\4\3"+
		"\4\3\4\3\4\3\4\5\4X\n\4\3\5\3\5\3\5\3\5\3\5\3\5\5\5`\n\5\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\5\6j\n\6\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5"+
		"\7v\n\7\3\b\3\b\3\b\3\t\3\t\3\t\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3\r\3\r"+
		"\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u0091\n\16\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\5\17\u009c\n\17\3\20\3\20\3\20"+
		"\3\20\5\20\u00a2\n\20\3\21\3\21\3\22\3\22\3\23\3\23\3\24\3\24\3\25\3\25"+
		"\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\31\3\32\3\32\3\32\3\32\3\32"+
		"\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33"+
		"\5\33\u00ca\n\33\3\34\5\34\u00cd\n\34\3\34\6\34\u00d0\n\34\r\34\16\34"+
		"\u00d1\3\35\5\35\u00d5\n\35\3\35\6\35\u00d8\n\35\r\35\16\35\u00d9\3\35"+
		"\3\35\6\35\u00de\n\35\r\35\16\35\u00df\3\36\3\36\7\36\u00e4\n\36\f\36"+
		"\16\36\u00e7\13\36\3\37\3\37\3 \3 \7 \u00ed\n \f \16 \u00f0\13 \3 \3 "+
		"\3 \7 \u00f5\n \f \16 \u00f8\13 \3 \5 \u00fb\n \3!\3!\3!\3!\6!\u0101\n"+
		"!\r!\16!\u0102\3!\5!\u0106\n!\3!\3!\3\"\6\"\u010b\n\"\r\"\16\"\u010c\3"+
		"\"\3\"\3\u0102\2#\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31"+
		"\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65"+
		"\34\67\359\36;\37=\2? A!C\"\3\2\7\5\2C\\aac|\b\2\60\60\62;C\\^^aac|\7"+
		"\2\f\f\17\17$$))^^\3\3\f\f\5\2\13\f\16\17\"\"\u0126\2\3\3\2\2\2\2\5\3"+
		"\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2"+
		"\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3"+
		"\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'"+
		"\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63"+
		"\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2?\3\2\2\2\2"+
		"A\3\2\2\2\2C\3\2\2\2\3E\3\2\2\2\5O\3\2\2\2\7W\3\2\2\2\t_\3\2\2\2\13i\3"+
		"\2\2\2\ru\3\2\2\2\17w\3\2\2\2\21z\3\2\2\2\23}\3\2\2\2\25\177\3\2\2\2\27"+
		"\u0082\3\2\2\2\31\u0084\3\2\2\2\33\u0090\3\2\2\2\35\u009b\3\2\2\2\37\u00a1"+
		"\3\2\2\2!\u00a3\3\2\2\2#\u00a5\3\2\2\2%\u00a7\3\2\2\2\'\u00a9\3\2\2\2"+
		")\u00ab\3\2\2\2+\u00ad\3\2\2\2-\u00af\3\2\2\2/\u00b1\3\2\2\2\61\u00b3"+
		"\3\2\2\2\63\u00b6\3\2\2\2\65\u00c9\3\2\2\2\67\u00cc\3\2\2\29\u00d4\3\2"+
		"\2\2;\u00e1\3\2\2\2=\u00e8\3\2\2\2?\u00fa\3\2\2\2A\u00fc\3\2\2\2C\u010a"+
		"\3\2\2\2EF\7.\2\2F\4\3\2\2\2GH\7c\2\2HI\7p\2\2IP\7f\2\2JK\7(\2\2KP\7("+
		"\2\2LM\7C\2\2MN\7P\2\2NP\7F\2\2OG\3\2\2\2OJ\3\2\2\2OL\3\2\2\2P\6\3\2\2"+
		"\2QR\7q\2\2RX\7t\2\2ST\7~\2\2TX\7~\2\2UV\7Q\2\2VX\7T\2\2WQ\3\2\2\2WS\3"+
		"\2\2\2WU\3\2\2\2X\b\3\2\2\2YZ\7p\2\2Z[\7q\2\2[`\7v\2\2\\]\7P\2\2]^\7Q"+
		"\2\2^`\7V\2\2_Y\3\2\2\2_\\\3\2\2\2`\n\3\2\2\2ab\7v\2\2bc\7t\2\2cd\7w\2"+
		"\2dj\7g\2\2ef\7V\2\2fg\7T\2\2gh\7W\2\2hj\7G\2\2ia\3\2\2\2ie\3\2\2\2j\f"+
		"\3\2\2\2kl\7h\2\2lm\7c\2\2mn\7n\2\2no\7u\2\2ov\7g\2\2pq\7H\2\2qr\7C\2"+
		"\2rs\7N\2\2st\7U\2\2tv\7G\2\2uk\3\2\2\2up\3\2\2\2v\16\3\2\2\2wx\7?\2\2"+
		"xy\7?\2\2y\20\3\2\2\2z{\7#\2\2{|\7?\2\2|\22\3\2\2\2}~\7>\2\2~\24\3\2\2"+
		"\2\177\u0080\7>\2\2\u0080\u0081\7?\2\2\u0081\26\3\2\2\2\u0082\u0083\7"+
		"@\2\2\u0083\30\3\2\2\2\u0084\u0085\7@\2\2\u0085\u0086\7?\2\2\u0086\32"+
		"\3\2\2\2\u0087\u0091\7A\2\2\u0088\u0089\7V\2\2\u0089\u008a\7J\2\2\u008a"+
		"\u008b\7G\2\2\u008b\u0091\7P\2\2\u008c\u008d\7v\2\2\u008d\u008e\7j\2\2"+
		"\u008e\u008f\7g\2\2\u008f\u0091\7p\2\2\u0090\u0087\3\2\2\2\u0090\u0088"+
		"\3\2\2\2\u0090\u008c\3\2\2\2\u0091\34\3\2\2\2\u0092\u009c\7<\2\2\u0093"+
		"\u0094\7G\2\2\u0094\u0095\7N\2\2\u0095\u0096\7U\2\2\u0096\u009c\7G\2\2"+
		"\u0097\u0098\7g\2\2\u0098\u0099\7n\2\2\u0099\u009a\7u\2\2\u009a\u009c"+
		"\7g\2\2\u009b\u0092\3\2\2\2\u009b\u0093\3\2\2\2\u009b\u0097\3\2\2\2\u009c"+
		"\36\3\2\2\2\u009d\u009e\7K\2\2\u009e\u00a2\7H\2\2\u009f\u00a0\7k\2\2\u00a0"+
		"\u00a2\7h\2\2\u00a1\u009d\3\2\2\2\u00a1\u009f\3\2\2\2\u00a2 \3\2\2\2\u00a3"+
		"\u00a4\7/\2\2\u00a4\"\3\2\2\2\u00a5\u00a6\7-\2\2\u00a6$\3\2\2\2\u00a7"+
		"\u00a8\7\61\2\2\u00a8&\3\2\2\2\u00a9\u00aa\7,\2\2\u00aa(\3\2\2\2\u00ab"+
		"\u00ac\7]\2\2\u00ac*\3\2\2\2\u00ad\u00ae\7_\2\2\u00ae,\3\2\2\2\u00af\u00b0"+
		"\7*\2\2\u00b0.\3\2\2\2\u00b1\u00b2\7+\2\2\u00b2\60\3\2\2\2\u00b3\u00b4"+
		"\7k\2\2\u00b4\u00b5\7p\2\2\u00b5\62\3\2\2\2\u00b6\u00b7\7p\2\2\u00b7\u00b8"+
		"\7q\2\2\u00b8\u00b9\7v\2\2\u00b9\u00ba\7\"\2\2\u00ba\u00bb\7k\2\2\u00bb"+
		"\u00bc\7p\2\2\u00bc\64\3\2\2\2\u00bd\u00be\7g\2\2\u00be\u00bf\7z\2\2\u00bf"+
		"\u00c0\7k\2\2\u00c0\u00c1\7u\2\2\u00c1\u00c2\7v\2\2\u00c2\u00ca\7u\2\2"+
		"\u00c3\u00c4\7G\2\2\u00c4\u00c5\7Z\2\2\u00c5\u00c6\7K\2\2\u00c6\u00c7"+
		"\7U\2\2\u00c7\u00c8\7V\2\2\u00c8\u00ca\7U\2\2\u00c9\u00bd\3\2\2\2\u00c9"+
		"\u00c3\3\2\2\2\u00ca\66\3\2\2\2\u00cb\u00cd\5!\21\2\u00cc\u00cb\3\2\2"+
		"\2\u00cc\u00cd\3\2\2\2\u00cd\u00cf\3\2\2\2\u00ce\u00d0\4\62;\2\u00cf\u00ce"+
		"\3\2\2\2\u00d0\u00d1\3\2\2\2\u00d1\u00cf\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d2"+
		"8\3\2\2\2\u00d3\u00d5\5!\21\2\u00d4\u00d3\3\2\2\2\u00d4\u00d5\3\2\2\2"+
		"\u00d5\u00d7\3\2\2\2\u00d6\u00d8\4\62;\2\u00d7\u00d6\3\2\2\2\u00d8\u00d9"+
		"\3\2\2\2\u00d9\u00d7\3\2\2\2\u00d9\u00da\3\2\2\2\u00da\u00db\3\2\2\2\u00db"+
		"\u00dd\7\60\2\2\u00dc\u00de\4\62;\2\u00dd\u00dc\3\2\2\2\u00de\u00df\3"+
		"\2\2\2\u00df\u00dd\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0:\3\2\2\2\u00e1\u00e5"+
		"\t\2\2\2\u00e2\u00e4\t\3\2\2\u00e3\u00e2\3\2\2\2\u00e4\u00e7\3\2\2\2\u00e5"+
		"\u00e3\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6<\3\2\2\2\u00e7\u00e5\3\2\2\2"+
		"\u00e8\u00e9\n\4\2\2\u00e9>\3\2\2\2\u00ea\u00ee\7$\2\2\u00eb\u00ed\5="+
		"\37\2\u00ec\u00eb\3\2\2\2\u00ed\u00f0\3\2\2\2\u00ee\u00ec\3\2\2\2\u00ee"+
		"\u00ef\3\2\2\2\u00ef\u00f1\3\2\2\2\u00f0\u00ee\3\2\2\2\u00f1\u00fb\7$"+
		"\2\2\u00f2\u00f6\7)\2\2\u00f3\u00f5\5=\37\2\u00f4\u00f3\3\2\2\2\u00f5"+
		"\u00f8\3\2\2\2\u00f6\u00f4\3\2\2\2\u00f6\u00f7\3\2\2\2\u00f7\u00f9\3\2"+
		"\2\2\u00f8\u00f6\3\2\2\2\u00f9\u00fb\7)\2\2\u00fa\u00ea\3\2\2\2\u00fa"+
		"\u00f2\3\2\2\2\u00fb@\3\2\2\2\u00fc\u00fd\7\61\2\2\u00fd\u00fe\7\61\2"+
		"\2\u00fe\u0100\3\2\2\2\u00ff\u0101\13\2\2\2\u0100\u00ff\3\2\2\2\u0101"+
		"\u0102\3\2\2\2\u0102\u0103\3\2\2\2\u0102\u0100\3\2\2\2\u0103\u0105\3\2"+
		"\2\2\u0104\u0106\t\5\2\2\u0105\u0104\3\2\2\2\u0106\u0107\3\2\2\2\u0107"+
		"\u0108\b!\2\2\u0108B\3\2\2\2\u0109\u010b\t\6\2\2\u010a\u0109\3\2\2\2\u010b"+
		"\u010c\3\2\2\2\u010c\u010a\3\2\2\2\u010c\u010d\3\2\2\2\u010d\u010e\3\2"+
		"\2\2\u010e\u010f\b\"\2\2\u010fD\3\2\2\2\30\2OW_iu\u0090\u009b\u00a1\u00c9"+
		"\u00cc\u00d1\u00d4\u00d9\u00df\u00e5\u00ee\u00f6\u00fa\u0102\u0105\u010c"+
		"\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}