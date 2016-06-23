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

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class PredicateLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		AND=1, OR=2, NOT=3, TRUE=4, FALSE=5, EQ=6, NEQ=7, LT=8, LTE=9, GT=10, 
		GTE=11, COMMA=12, LBRACKET=13, RBRACKET=14, LPAREN=15, RPAREN=16, IN=17, 
		NIN=18, EXISTS=19, MINUS=20, INT_LITERAL=21, DOUBLE_LITERAL=22, IDENTIFIER=23, 
		STRING_LITERAL=24, SEMI=25, COMMENT=26, WS=27;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", 
		"COMMA", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "IN", "NIN", "EXISTS", 
		"MINUS", "INT_LITERAL", "DOUBLE_LITERAL", "IDENTIFIER", "SCHAR", "STRING_LITERAL", 
		"SEMI", "COMMENT", "WS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, null, null, null, null, "'=='", "'!='", "'<'", "'<='", "'>'", 
		"'>='", "','", "'['", "']'", "'('", "')'", "'in'", "'not in'", "'exists'", 
		"'-'", null, null, null, null, "';'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", "GT", 
		"GTE", "COMMA", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "IN", "NIN", 
		"EXISTS", "MINUS", "INT_LITERAL", "DOUBLE_LITERAL", "IDENTIFIER", "STRING_LITERAL", 
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


	public PredicateLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Predicate.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\35\u00df\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\3\2\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\3\2\5\2D\n\2\3\3\3\3\3\3\3\3\3\3\3\3\5\3L\n\3\3\4\3\4\3\4\3\4\3\4"+
		"\3\4\5\4T\n\4\3\5\3\5\3\5\3\5\3\5\3\5\3\5\3\5\5\5^\n\5\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\5\6j\n\6\3\7\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\n"+
		"\3\n\3\n\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\16\3\16\3\17\3\17\3\20\3\20\3"+
		"\21\3\21\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3"+
		"\24\3\24\3\24\3\24\3\24\3\25\3\25\3\26\5\26\u009a\n\26\3\26\6\26\u009d"+
		"\n\26\r\26\16\26\u009e\3\27\5\27\u00a2\n\27\3\27\6\27\u00a5\n\27\r\27"+
		"\16\27\u00a6\3\27\3\27\6\27\u00ab\n\27\r\27\16\27\u00ac\3\30\3\30\7\30"+
		"\u00b1\n\30\f\30\16\30\u00b4\13\30\3\31\3\31\3\32\3\32\7\32\u00ba\n\32"+
		"\f\32\16\32\u00bd\13\32\3\32\3\32\3\32\7\32\u00c2\n\32\f\32\16\32\u00c5"+
		"\13\32\3\32\5\32\u00c8\n\32\3\33\3\33\3\34\3\34\3\34\3\34\6\34\u00d0\n"+
		"\34\r\34\16\34\u00d1\3\34\5\34\u00d5\n\34\3\34\3\34\3\35\6\35\u00da\n"+
		"\35\r\35\16\35\u00db\3\35\3\35\3\u00d1\2\36\3\3\5\4\7\5\t\6\13\7\r\b\17"+
		"\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24\'\25)\26+"+
		"\27-\30/\31\61\2\63\32\65\33\67\349\35\3\2\7\5\2C\\aac|\b\2\60\60\62;"+
		"C\\^^aac|\7\2\f\f\17\17$$))^^\3\3\f\f\5\2\13\f\16\17\"\"\u00ef\2\3\3\2"+
		"\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17"+
		"\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2"+
		"\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3"+
		"\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\63\3"+
		"\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\3C\3\2\2\2\5K\3\2\2\2\7S\3"+
		"\2\2\2\t]\3\2\2\2\13i\3\2\2\2\rk\3\2\2\2\17n\3\2\2\2\21q\3\2\2\2\23s\3"+
		"\2\2\2\25v\3\2\2\2\27x\3\2\2\2\31{\3\2\2\2\33}\3\2\2\2\35\177\3\2\2\2"+
		"\37\u0081\3\2\2\2!\u0083\3\2\2\2#\u0085\3\2\2\2%\u0088\3\2\2\2\'\u008f"+
		"\3\2\2\2)\u0096\3\2\2\2+\u0099\3\2\2\2-\u00a1\3\2\2\2/\u00ae\3\2\2\2\61"+
		"\u00b5\3\2\2\2\63\u00c7\3\2\2\2\65\u00c9\3\2\2\2\67\u00cb\3\2\2\29\u00d9"+
		"\3\2\2\2;<\7c\2\2<=\7p\2\2=D\7f\2\2>?\7(\2\2?D\7(\2\2@A\7C\2\2AB\7P\2"+
		"\2BD\7F\2\2C;\3\2\2\2C>\3\2\2\2C@\3\2\2\2D\4\3\2\2\2EF\7q\2\2FL\7t\2\2"+
		"GH\7~\2\2HL\7~\2\2IJ\7Q\2\2JL\7T\2\2KE\3\2\2\2KG\3\2\2\2KI\3\2\2\2L\6"+
		"\3\2\2\2MN\7p\2\2NO\7q\2\2OT\7v\2\2PQ\7P\2\2QR\7Q\2\2RT\7V\2\2SM\3\2\2"+
		"\2SP\3\2\2\2T\b\3\2\2\2UV\7v\2\2VW\7t\2\2WX\7w\2\2X^\7g\2\2YZ\7V\2\2Z"+
		"[\7T\2\2[\\\7W\2\2\\^\7G\2\2]U\3\2\2\2]Y\3\2\2\2^\n\3\2\2\2_`\7h\2\2`"+
		"a\7c\2\2ab\7n\2\2bc\7u\2\2cj\7g\2\2de\7H\2\2ef\7C\2\2fg\7N\2\2gh\7U\2"+
		"\2hj\7G\2\2i_\3\2\2\2id\3\2\2\2j\f\3\2\2\2kl\7?\2\2lm\7?\2\2m\16\3\2\2"+
		"\2no\7#\2\2op\7?\2\2p\20\3\2\2\2qr\7>\2\2r\22\3\2\2\2st\7>\2\2tu\7?\2"+
		"\2u\24\3\2\2\2vw\7@\2\2w\26\3\2\2\2xy\7@\2\2yz\7?\2\2z\30\3\2\2\2{|\7"+
		".\2\2|\32\3\2\2\2}~\7]\2\2~\34\3\2\2\2\177\u0080\7_\2\2\u0080\36\3\2\2"+
		"\2\u0081\u0082\7*\2\2\u0082 \3\2\2\2\u0083\u0084\7+\2\2\u0084\"\3\2\2"+
		"\2\u0085\u0086\7k\2\2\u0086\u0087\7p\2\2\u0087$\3\2\2\2\u0088\u0089\7"+
		"p\2\2\u0089\u008a\7q\2\2\u008a\u008b\7v\2\2\u008b\u008c\7\"\2\2\u008c"+
		"\u008d\7k\2\2\u008d\u008e\7p\2\2\u008e&\3\2\2\2\u008f\u0090\7g\2\2\u0090"+
		"\u0091\7z\2\2\u0091\u0092\7k\2\2\u0092\u0093\7u\2\2\u0093\u0094\7v\2\2"+
		"\u0094\u0095\7u\2\2\u0095(\3\2\2\2\u0096\u0097\7/\2\2\u0097*\3\2\2\2\u0098"+
		"\u009a\5)\25\2\u0099\u0098\3\2\2\2\u0099\u009a\3\2\2\2\u009a\u009c\3\2"+
		"\2\2\u009b\u009d\4\62;\2\u009c\u009b\3\2\2\2\u009d\u009e\3\2\2\2\u009e"+
		"\u009c\3\2\2\2\u009e\u009f\3\2\2\2\u009f,\3\2\2\2\u00a0\u00a2\5)\25\2"+
		"\u00a1\u00a0\3\2\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00a4\3\2\2\2\u00a3\u00a5"+
		"\4\62;\2\u00a4\u00a3\3\2\2\2\u00a5\u00a6\3\2\2\2\u00a6\u00a4\3\2\2\2\u00a6"+
		"\u00a7\3\2\2\2\u00a7\u00a8\3\2\2\2\u00a8\u00aa\7\60\2\2\u00a9\u00ab\4"+
		"\62;\2\u00aa\u00a9\3\2\2\2\u00ab\u00ac\3\2\2\2\u00ac\u00aa\3\2\2\2\u00ac"+
		"\u00ad\3\2\2\2\u00ad.\3\2\2\2\u00ae\u00b2\t\2\2\2\u00af\u00b1\t\3\2\2"+
		"\u00b0\u00af\3\2\2\2\u00b1\u00b4\3\2\2\2\u00b2\u00b0\3\2\2\2\u00b2\u00b3"+
		"\3\2\2\2\u00b3\60\3\2\2\2\u00b4\u00b2\3\2\2\2\u00b5\u00b6\n\4\2\2\u00b6"+
		"\62\3\2\2\2\u00b7\u00bb\7$\2\2\u00b8\u00ba\5\61\31\2\u00b9\u00b8\3\2\2"+
		"\2\u00ba\u00bd\3\2\2\2\u00bb\u00b9\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc\u00be"+
		"\3\2\2\2\u00bd\u00bb\3\2\2\2\u00be\u00c8\7$\2\2\u00bf\u00c3\7)\2\2\u00c0"+
		"\u00c2\5\61\31\2\u00c1\u00c0\3\2\2\2\u00c2\u00c5\3\2\2\2\u00c3\u00c1\3"+
		"\2\2\2\u00c3\u00c4\3\2\2\2\u00c4\u00c6\3\2\2\2\u00c5\u00c3\3\2\2\2\u00c6"+
		"\u00c8\7)\2\2\u00c7\u00b7\3\2\2\2\u00c7\u00bf\3\2\2\2\u00c8\64\3\2\2\2"+
		"\u00c9\u00ca\7=\2\2\u00ca\66\3\2\2\2\u00cb\u00cc\7\61\2\2\u00cc\u00cd"+
		"\7\61\2\2\u00cd\u00cf\3\2\2\2\u00ce\u00d0\13\2\2\2\u00cf\u00ce\3\2\2\2"+
		"\u00d0\u00d1\3\2\2\2\u00d1\u00d2\3\2\2\2\u00d1\u00cf\3\2\2\2\u00d2\u00d4"+
		"\3\2\2\2\u00d3\u00d5\t\5\2\2\u00d4\u00d3\3\2\2\2\u00d5\u00d6\3\2\2\2\u00d6"+
		"\u00d7\b\34\2\2\u00d78\3\2\2\2\u00d8\u00da\t\6\2\2\u00d9\u00d8\3\2\2\2"+
		"\u00da\u00db\3\2\2\2\u00db\u00d9\3\2\2\2\u00db\u00dc\3\2\2\2\u00dc\u00dd"+
		"\3\2\2\2\u00dd\u00de\b\35\2\2\u00de:\3\2\2\2\24\2CKS]i\u0099\u009e\u00a1"+
		"\u00a6\u00ac\u00b2\u00bb\u00c3\u00c7\u00d1\u00d4\u00db\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}