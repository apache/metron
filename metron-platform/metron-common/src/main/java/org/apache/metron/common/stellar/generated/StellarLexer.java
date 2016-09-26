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
		GT=11, GTE=12, QUESTION=13, COLON=14, IF=15, THEN=16, ELSE=17, NULL=18, 
		MINUS=19, PLUS=20, DIV=21, MUL=22, LBRACE=23, RBRACE=24, LBRACKET=25, 
		RBRACKET=26, LPAREN=27, RPAREN=28, IN=29, NIN=30, EXISTS=31, EXPONENT=32, 
		INT_LITERAL=33, DOUBLE_LITERAL=34, IDENTIFIER=35, STRING_LITERAL=36, COMMENT=37, 
		WS=38;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"COMMA", "AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", "LTE", 
		"GT", "GTE", "QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", "MINUS", 
		"PLUS", "DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", "LPAREN", 
		"RPAREN", "IN", "NIN", "EXISTS", "EXPONENT", "INT_LITERAL", "DOUBLE_LITERAL", 
		"IDENTIFIER", "SCHAR", "STRING_LITERAL", "COMMENT", "WS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "','", null, null, null, null, null, "'=='", "'!='", "'<'", "'<='", 
		"'>'", "'>='", "'?'", "':'", null, null, null, null, "'-'", "'+'", "'/'", 
		"'*'", "'{'", "'}'", "'['", "']'", "'('", "')'", "'in'", "'not in'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "COMMA", "AND", "OR", "NOT", "TRUE", "FALSE", "EQ", "NEQ", "LT", 
		"LTE", "GT", "GTE", "QUESTION", "COLON", "IF", "THEN", "ELSE", "NULL", 
		"MINUS", "PLUS", "DIV", "MUL", "LBRACE", "RBRACE", "LBRACKET", "RBRACKET", 
		"LPAREN", "RPAREN", "IN", "NIN", "EXISTS", "EXPONENT", "INT_LITERAL", 
		"DOUBLE_LITERAL", "IDENTIFIER", "STRING_LITERAL", "COMMENT", "WS"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2(\u0139\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\3\2\3\2\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\5\3\\\n\3\3\4\3\4\3\4\3\4\3\4\3\4\5\4d\n\4\3\5\3"+
		"\5\3\5\3\5\3\5\3\5\5\5l\n\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6v\n\6\3"+
		"\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7\u0082\n\7\3\b\3\b\3\b\3\t\3"+
		"\t\3\t\3\n\3\n\3\13\3\13\3\13\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\17\3\17"+
		"\3\20\3\20\3\20\3\20\5\20\u009c\n\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\5\21\u00a6\n\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\5\22\u00b0"+
		"\n\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\5\23\u00ba\n\23\3\24\3\24"+
		"\3\25\3\25\3\26\3\26\3\27\3\27\3\30\3\30\3\31\3\31\3\32\3\32\3\33\3\33"+
		"\3\34\3\34\3\35\3\35\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \5 \u00e6\n \3!\3!\3!\5!\u00eb\n!"+
		"\3!\6!\u00ee\n!\r!\16!\u00ef\3\"\5\"\u00f3\n\"\3\"\6\"\u00f6\n\"\r\"\16"+
		"\"\u00f7\3#\5#\u00fb\n#\3#\6#\u00fe\n#\r#\16#\u00ff\3#\3#\6#\u0104\n#"+
		"\r#\16#\u0105\3#\5#\u0109\n#\3$\3$\7$\u010d\n$\f$\16$\u0110\13$\3%\3%"+
		"\3&\3&\7&\u0116\n&\f&\16&\u0119\13&\3&\3&\3&\7&\u011e\n&\f&\16&\u0121"+
		"\13&\3&\5&\u0124\n&\3\'\3\'\3\'\3\'\6\'\u012a\n\'\r\'\16\'\u012b\3\'\5"+
		"\'\u012f\n\'\3\'\3\'\3(\6(\u0134\n(\r(\16(\u0135\3(\3(\3\u012b\2)\3\3"+
		"\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21"+
		"!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!"+
		"A\"C#E$G%I\2K&M\'O(\3\2\b\4\2GGgg\5\2C\\aac|\b\2\60\60\62<C\\^^aac|\7"+
		"\2\f\f\17\17$$))^^\3\3\f\f\5\2\13\f\16\17\"\"\u0152\2\3\3\2\2\2\2\5\3"+
		"\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2"+
		"\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3"+
		"\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'"+
		"\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2\2\2\2\63"+
		"\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3\2\2\2\2"+
		"?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2K\3\2\2\2\2M\3"+
		"\2\2\2\2O\3\2\2\2\3Q\3\2\2\2\5[\3\2\2\2\7c\3\2\2\2\tk\3\2\2\2\13u\3\2"+
		"\2\2\r\u0081\3\2\2\2\17\u0083\3\2\2\2\21\u0086\3\2\2\2\23\u0089\3\2\2"+
		"\2\25\u008b\3\2\2\2\27\u008e\3\2\2\2\31\u0090\3\2\2\2\33\u0093\3\2\2\2"+
		"\35\u0095\3\2\2\2\37\u009b\3\2\2\2!\u00a5\3\2\2\2#\u00af\3\2\2\2%\u00b9"+
		"\3\2\2\2\'\u00bb\3\2\2\2)\u00bd\3\2\2\2+\u00bf\3\2\2\2-\u00c1\3\2\2\2"+
		"/\u00c3\3\2\2\2\61\u00c5\3\2\2\2\63\u00c7\3\2\2\2\65\u00c9\3\2\2\2\67"+
		"\u00cb\3\2\2\29\u00cd\3\2\2\2;\u00cf\3\2\2\2=\u00d2\3\2\2\2?\u00e5\3\2"+
		"\2\2A\u00e7\3\2\2\2C\u00f2\3\2\2\2E\u00fa\3\2\2\2G\u010a\3\2\2\2I\u0111"+
		"\3\2\2\2K\u0123\3\2\2\2M\u0125\3\2\2\2O\u0133\3\2\2\2QR\7.\2\2R\4\3\2"+
		"\2\2ST\7c\2\2TU\7p\2\2U\\\7f\2\2VW\7(\2\2W\\\7(\2\2XY\7C\2\2YZ\7P\2\2"+
		"Z\\\7F\2\2[S\3\2\2\2[V\3\2\2\2[X\3\2\2\2\\\6\3\2\2\2]^\7q\2\2^d\7t\2\2"+
		"_`\7~\2\2`d\7~\2\2ab\7Q\2\2bd\7T\2\2c]\3\2\2\2c_\3\2\2\2ca\3\2\2\2d\b"+
		"\3\2\2\2ef\7p\2\2fg\7q\2\2gl\7v\2\2hi\7P\2\2ij\7Q\2\2jl\7V\2\2ke\3\2\2"+
		"\2kh\3\2\2\2l\n\3\2\2\2mn\7v\2\2no\7t\2\2op\7w\2\2pv\7g\2\2qr\7V\2\2r"+
		"s\7T\2\2st\7W\2\2tv\7G\2\2um\3\2\2\2uq\3\2\2\2v\f\3\2\2\2wx\7h\2\2xy\7"+
		"c\2\2yz\7n\2\2z{\7u\2\2{\u0082\7g\2\2|}\7H\2\2}~\7C\2\2~\177\7N\2\2\177"+
		"\u0080\7U\2\2\u0080\u0082\7G\2\2\u0081w\3\2\2\2\u0081|\3\2\2\2\u0082\16"+
		"\3\2\2\2\u0083\u0084\7?\2\2\u0084\u0085\7?\2\2\u0085\20\3\2\2\2\u0086"+
		"\u0087\7#\2\2\u0087\u0088\7?\2\2\u0088\22\3\2\2\2\u0089\u008a\7>\2\2\u008a"+
		"\24\3\2\2\2\u008b\u008c\7>\2\2\u008c\u008d\7?\2\2\u008d\26\3\2\2\2\u008e"+
		"\u008f\7@\2\2\u008f\30\3\2\2\2\u0090\u0091\7@\2\2\u0091\u0092\7?\2\2\u0092"+
		"\32\3\2\2\2\u0093\u0094\7A\2\2\u0094\34\3\2\2\2\u0095\u0096\7<\2\2\u0096"+
		"\36\3\2\2\2\u0097\u0098\7K\2\2\u0098\u009c\7H\2\2\u0099\u009a\7k\2\2\u009a"+
		"\u009c\7h\2\2\u009b\u0097\3\2\2\2\u009b\u0099\3\2\2\2\u009c \3\2\2\2\u009d"+
		"\u009e\7V\2\2\u009e\u009f\7J\2\2\u009f\u00a0\7G\2\2\u00a0\u00a6\7P\2\2"+
		"\u00a1\u00a2\7v\2\2\u00a2\u00a3\7j\2\2\u00a3\u00a4\7g\2\2\u00a4\u00a6"+
		"\7p\2\2\u00a5\u009d\3\2\2\2\u00a5\u00a1\3\2\2\2\u00a6\"\3\2\2\2\u00a7"+
		"\u00a8\7G\2\2\u00a8\u00a9\7N\2\2\u00a9\u00aa\7U\2\2\u00aa\u00b0\7G\2\2"+
		"\u00ab\u00ac\7g\2\2\u00ac\u00ad\7n\2\2\u00ad\u00ae\7u\2\2\u00ae\u00b0"+
		"\7g\2\2\u00af\u00a7\3\2\2\2\u00af\u00ab\3\2\2\2\u00b0$\3\2\2\2\u00b1\u00b2"+
		"\7p\2\2\u00b2\u00b3\7w\2\2\u00b3\u00b4\7n\2\2\u00b4\u00ba\7n\2\2\u00b5"+
		"\u00b6\7P\2\2\u00b6\u00b7\7W\2\2\u00b7\u00b8\7N\2\2\u00b8\u00ba\7N\2\2"+
		"\u00b9\u00b1\3\2\2\2\u00b9\u00b5\3\2\2\2\u00ba&\3\2\2\2\u00bb\u00bc\7"+
		"/\2\2\u00bc(\3\2\2\2\u00bd\u00be\7-\2\2\u00be*\3\2\2\2\u00bf\u00c0\7\61"+
		"\2\2\u00c0,\3\2\2\2\u00c1\u00c2\7,\2\2\u00c2.\3\2\2\2\u00c3\u00c4\7}\2"+
		"\2\u00c4\60\3\2\2\2\u00c5\u00c6\7\177\2\2\u00c6\62\3\2\2\2\u00c7\u00c8"+
		"\7]\2\2\u00c8\64\3\2\2\2\u00c9\u00ca\7_\2\2\u00ca\66\3\2\2\2\u00cb\u00cc"+
		"\7*\2\2\u00cc8\3\2\2\2\u00cd\u00ce\7+\2\2\u00ce:\3\2\2\2\u00cf\u00d0\7"+
		"k\2\2\u00d0\u00d1\7p\2\2\u00d1<\3\2\2\2\u00d2\u00d3\7p\2\2\u00d3\u00d4"+
		"\7q\2\2\u00d4\u00d5\7v\2\2\u00d5\u00d6\7\"\2\2\u00d6\u00d7\7k\2\2\u00d7"+
		"\u00d8\7p\2\2\u00d8>\3\2\2\2\u00d9\u00da\7g\2\2\u00da\u00db\7z\2\2\u00db"+
		"\u00dc\7k\2\2\u00dc\u00dd\7u\2\2\u00dd\u00de\7v\2\2\u00de\u00e6\7u\2\2"+
		"\u00df\u00e0\7G\2\2\u00e0\u00e1\7Z\2\2\u00e1\u00e2\7K\2\2\u00e2\u00e3"+
		"\7U\2\2\u00e3\u00e4\7V\2\2\u00e4\u00e6\7U\2\2\u00e5\u00d9\3\2\2\2\u00e5"+
		"\u00df\3\2\2\2\u00e6@\3\2\2\2\u00e7\u00ea\t\2\2\2\u00e8\u00eb\5)\25\2"+
		"\u00e9\u00eb\5\'\24\2\u00ea\u00e8\3\2\2\2\u00ea\u00e9\3\2\2\2\u00ea\u00eb"+
		"\3\2\2\2\u00eb\u00ed\3\2\2\2\u00ec\u00ee\4\62;\2\u00ed\u00ec\3\2\2\2\u00ee"+
		"\u00ef\3\2\2\2\u00ef\u00ed\3\2\2\2\u00ef\u00f0\3\2\2\2\u00f0B\3\2\2\2"+
		"\u00f1\u00f3\5\'\24\2\u00f2\u00f1\3\2\2\2\u00f2\u00f3\3\2\2\2\u00f3\u00f5"+
		"\3\2\2\2\u00f4\u00f6\4\62;\2\u00f5\u00f4\3\2\2\2\u00f6\u00f7\3\2\2\2\u00f7"+
		"\u00f5\3\2\2\2\u00f7\u00f8\3\2\2\2\u00f8D\3\2\2\2\u00f9\u00fb\5\'\24\2"+
		"\u00fa\u00f9\3\2\2\2\u00fa\u00fb\3\2\2\2\u00fb\u00fd\3\2\2\2\u00fc\u00fe"+
		"\4\62;\2\u00fd\u00fc\3\2\2\2\u00fe\u00ff\3\2\2\2\u00ff\u00fd\3\2\2\2\u00ff"+
		"\u0100\3\2\2\2\u0100\u0101\3\2\2\2\u0101\u0103\7\60\2\2\u0102\u0104\4"+
		"\62;\2\u0103\u0102\3\2\2\2\u0104\u0105\3\2\2\2\u0105\u0103\3\2\2\2\u0105"+
		"\u0106\3\2\2\2\u0106\u0108\3\2\2\2\u0107\u0109\5A!\2\u0108\u0107\3\2\2"+
		"\2\u0108\u0109\3\2\2\2\u0109F\3\2\2\2\u010a\u010e\t\3\2\2\u010b\u010d"+
		"\t\4\2\2\u010c\u010b\3\2\2\2\u010d\u0110\3\2\2\2\u010e\u010c\3\2\2\2\u010e"+
		"\u010f\3\2\2\2\u010fH\3\2\2\2\u0110\u010e\3\2\2\2\u0111\u0112\n\5\2\2"+
		"\u0112J\3\2\2\2\u0113\u0117\7$\2\2\u0114\u0116\5I%\2\u0115\u0114\3\2\2"+
		"\2\u0116\u0119\3\2\2\2\u0117\u0115\3\2\2\2\u0117\u0118\3\2\2\2\u0118\u011a"+
		"\3\2\2\2\u0119\u0117\3\2\2\2\u011a\u0124\7$\2\2\u011b\u011f\7)\2\2\u011c"+
		"\u011e\5I%\2\u011d\u011c\3\2\2\2\u011e\u0121\3\2\2\2\u011f\u011d\3\2\2"+
		"\2\u011f\u0120\3\2\2\2\u0120\u0122\3\2\2\2\u0121\u011f\3\2\2\2\u0122\u0124"+
		"\7)\2\2\u0123\u0113\3\2\2\2\u0123\u011b\3\2\2\2\u0124L\3\2\2\2\u0125\u0126"+
		"\7\61\2\2\u0126\u0127\7\61\2\2\u0127\u0129\3\2\2\2\u0128\u012a\13\2\2"+
		"\2\u0129\u0128\3\2\2\2\u012a\u012b\3\2\2\2\u012b\u012c\3\2\2\2\u012b\u0129"+
		"\3\2\2\2\u012c\u012e\3\2\2\2\u012d\u012f\t\6\2\2\u012e\u012d\3\2\2\2\u012f"+
		"\u0130\3\2\2\2\u0130\u0131\b\'\2\2\u0131N\3\2\2\2\u0132\u0134\t\7\2\2"+
		"\u0133\u0132\3\2\2\2\u0134\u0135\3\2\2\2\u0135\u0133\3\2\2\2\u0135\u0136"+
		"\3\2\2\2\u0136\u0137\3\2\2\2\u0137\u0138\b(\2\2\u0138P\3\2\2\2\34\2[c"+
		"ku\u0081\u009b\u00a5\u00af\u00b9\u00e5\u00ea\u00ef\u00f2\u00f7\u00fa\u00ff"+
		"\u0105\u0108\u010e\u0117\u011f\u0123\u012b\u012e\u0135\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}