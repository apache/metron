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
		AND=7, OR=8, NOT=9, TRUE=10, FALSE=11, ASSIGN=12, PLUSASSIGN=13, MINUSASSIGN=14, 
		DIVIDEASSIGN=15, MULTASSIGN=16, EQ=17, NEQ=18, LT=19, LTE=20, GT=21, GTE=22, 
		QUESTION=23, COLON=24, IF=25, THEN=26, ELSE=27, NULL=28, NAN=29, MATCH=30, 
		DEFAULT=31, MATCH_ACTION=32, MINUS=33, MINUSMINUS=34, PLUS=35, PLUSPLUS=36, 
		DIV=37, MUL=38, LBRACE=39, RBRACE=40, LBRACKET=41, RBRACKET=42, LPAREN=43, 
		RPAREN=44, NIN=45, EXISTS=46, EXPONENT=47, INT_LITERAL=48, DOUBLE_LITERAL=49, 
		FLOAT_LITERAL=50, LONG_LITERAL=51, IDENTIFIER=52, STRING_LITERAL=53, COMMENT=54, 
		WS=55;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "ASSIGN", "PLUSASSIGN", "MINUSASSIGN", 
		"DIVIDEASSIGN", "MULTASSIGN", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", "QUESTION", 
		"COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "MATCH", "DEFAULT", "MATCH_ACTION", 
		"MINUS", "MINUSMINUS", "PLUS", "PLUSPLUS", "DIV", "MUL", "LBRACE", "RBRACE", 
		"LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "NIN", "EXISTS", "EXPONENT", 
		"INT_LITERAL", "DOUBLE_LITERAL", "FLOAT_LITERAL", "LONG_LITERAL", "IDENTIFIER", 
		"STRING_LITERAL", "COMMENT", "WS", "ZERO", "FIRST_DIGIT", "DIGIT", "D", 
		"E", "F", "L", "EOL", "IDENTIFIER_START", "IDENTIFIER_MIDDLE", "IDENTIFIER_END"
	};

	private static final String[] _LITERAL_NAMES = {
		null, null, "'->'", "'\"'", "'''", "','", "'.'", null, null, null, null, 
		null, "'='", "'+='", "'-='", "'/='", "'*='", "'=='", "'!='", "'<'", "'<='", 
		"'>'", "'>='", "'?'", "':'", null, null, null, null, "'NaN'", null, null, 
		"'=>'", "'-'", "'--'", "'+'", "'++'", "'/'", "'*'", "'{'", "'}'", "'['", 
		"']'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "IN", "LAMBDA_OP", "DOUBLE_QUOTE", "SINGLE_QUOTE", "COMMA", "PERIOD", 
		"AND", "OR", "NOT", "TRUE", "FALSE", "ASSIGN", "PLUSASSIGN", "MINUSASSIGN", 
		"DIVIDEASSIGN", "MULTASSIGN", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", "QUESTION", 
		"COLON", "IF", "THEN", "ELSE", "NULL", "NAN", "MATCH", "DEFAULT", "MATCH_ACTION", 
		"MINUS", "MINUSMINUS", "PLUS", "PLUSPLUS", "DIV", "MUL", "LBRACE", "RBRACE", 
		"LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "NIN", "EXISTS", "EXPONENT", 
		"INT_LITERAL", "DOUBLE_LITERAL", "FLOAT_LITERAL", "LONG_LITERAL", "IDENTIFIER", 
		"STRING_LITERAL", "COMMENT", "WS"
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\29\u021d\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\4=\t="+
		"\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\3\2\3\2\3\2\3\2\5\2\u008c\n\2\3\3"+
		"\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\5\b\u00a1\n\b\3\t\3\t\3\t\3\t\3\t\3\t\5\t\u00a9\n\t\3\n\3\n\3\n\3\n"+
		"\3\n\3\n\5\n\u00b1\n\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13\u00bb"+
		"\n\13\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\5\f\u00c7\n\f\3\r\3\r\3"+
		"\16\3\16\3\16\3\17\3\17\3\17\3\20\3\20\3\20\3\21\3\21\3\21\3\22\3\22\3"+
		"\22\3\23\3\23\3\23\3\24\3\24\3\25\3\25\3\25\3\26\3\26\3\27\3\27\3\27\3"+
		"\30\3\30\3\31\3\31\3\32\3\32\3\32\3\32\5\32\u00ef\n\32\3\33\3\33\3\33"+
		"\3\33\3\33\3\33\3\33\3\33\5\33\u00f9\n\33\3\34\3\34\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\5\34\u0103\n\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\5\35"+
		"\u010d\n\35\3\36\3\36\3\36\3\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37"+
		"\3\37\3\37\5\37\u011d\n\37\3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \3 \5"+
		" \u012d\n \3!\3!\3!\3\"\3\"\3#\3#\3#\3$\3$\3%\3%\3%\3&\3&\3\'\3\'\3(\3"+
		"(\3)\3)\3*\3*\3+\3+\3,\3,\3-\3-\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\3.\5"+
		".\u0158\n.\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\3/\5/\u0166\n/\3\60\3\60\3"+
		"\60\5\60\u016b\n\60\3\60\6\60\u016e\n\60\r\60\16\60\u016f\3\61\5\61\u0173"+
		"\n\61\3\61\3\61\5\61\u0177\n\61\3\61\3\61\7\61\u017b\n\61\f\61\16\61\u017e"+
		"\13\61\5\61\u0180\n\61\3\62\3\62\3\62\7\62\u0185\n\62\f\62\16\62\u0188"+
		"\13\62\3\62\5\62\u018b\n\62\3\62\5\62\u018e\n\62\3\62\3\62\6\62\u0192"+
		"\n\62\r\62\16\62\u0193\3\62\5\62\u0197\n\62\3\62\5\62\u019a\n\62\3\62"+
		"\3\62\3\62\5\62\u019f\n\62\3\62\3\62\5\62\u01a3\n\62\3\62\3\62\5\62\u01a7"+
		"\n\62\3\63\3\63\3\63\7\63\u01ac\n\63\f\63\16\63\u01af\13\63\3\63\5\63"+
		"\u01b2\n\63\3\63\3\63\3\63\5\63\u01b7\n\63\3\63\3\63\6\63\u01bb\n\63\r"+
		"\63\16\63\u01bc\3\63\5\63\u01c0\n\63\3\63\3\63\3\63\3\63\5\63\u01c6\n"+
		"\63\3\63\3\63\5\63\u01ca\n\63\3\64\3\64\3\64\3\65\3\65\3\65\7\65\u01d2"+
		"\n\65\f\65\16\65\u01d5\13\65\3\65\3\65\5\65\u01d9\n\65\3\66\3\66\3\66"+
		"\3\66\7\66\u01df\n\66\f\66\16\66\u01e2\13\66\3\66\3\66\3\66\3\66\3\66"+
		"\3\66\7\66\u01ea\n\66\f\66\16\66\u01ed\13\66\3\66\3\66\5\66\u01f1\n\66"+
		"\3\67\3\67\3\67\3\67\6\67\u01f7\n\67\r\67\16\67\u01f8\3\67\3\67\5\67\u01fd"+
		"\n\67\3\67\3\67\38\68\u0202\n8\r8\168\u0203\38\38\39\39\3:\3:\3;\3;\3"+
		"<\3<\3=\3=\3>\3>\3?\3?\3@\3@\3A\3A\3B\3B\3C\3C\3\u01f8\2D\3\3\5\4\7\5"+
		"\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23"+
		"%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37= ?!A\"C#E$G"+
		"%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9q\2s\2u\2w\2"+
		"y\2{\2}\2\177\2\u0081\2\u0083\2\u0085\2\3\2\16\4\2))^^\7\2))^^ppttvv\4"+
		"\2$$^^\7\2$$^^ppttvv\5\2\13\f\16\17\"\"\4\2FFff\4\2GGgg\4\2HHhh\4\2NN"+
		"nn\6\2&&C\\aac|\b\2\60\60\62<C\\^^aac|\b\2\60\60\62;C\\^^aac|\u0245\2"+
		"\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2"+
		"\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2"+
		"\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2"+
		"\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2"+
		"\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2"+
		"\2\2\2=\3\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2"+
		"\2I\3\2\2\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U"+
		"\3\2\2\2\2W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2"+
		"\2\2\2c\3\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2"+
		"\2o\3\2\2\2\3\u008b\3\2\2\2\5\u008d\3\2\2\2\7\u0090\3\2\2\2\t\u0092\3"+
		"\2\2\2\13\u0094\3\2\2\2\r\u0096\3\2\2\2\17\u00a0\3\2\2\2\21\u00a8\3\2"+
		"\2\2\23\u00b0\3\2\2\2\25\u00ba\3\2\2\2\27\u00c6\3\2\2\2\31\u00c8\3\2\2"+
		"\2\33\u00ca\3\2\2\2\35\u00cd\3\2\2\2\37\u00d0\3\2\2\2!\u00d3\3\2\2\2#"+
		"\u00d6\3\2\2\2%\u00d9\3\2\2\2\'\u00dc\3\2\2\2)\u00de\3\2\2\2+\u00e1\3"+
		"\2\2\2-\u00e3\3\2\2\2/\u00e6\3\2\2\2\61\u00e8\3\2\2\2\63\u00ee\3\2\2\2"+
		"\65\u00f8\3\2\2\2\67\u0102\3\2\2\29\u010c\3\2\2\2;\u010e\3\2\2\2=\u011c"+
		"\3\2\2\2?\u012c\3\2\2\2A\u012e\3\2\2\2C\u0131\3\2\2\2E\u0133\3\2\2\2G"+
		"\u0136\3\2\2\2I\u0138\3\2\2\2K\u013b\3\2\2\2M\u013d\3\2\2\2O\u013f\3\2"+
		"\2\2Q\u0141\3\2\2\2S\u0143\3\2\2\2U\u0145\3\2\2\2W\u0147\3\2\2\2Y\u0149"+
		"\3\2\2\2[\u0157\3\2\2\2]\u0165\3\2\2\2_\u0167\3\2\2\2a\u017f\3\2\2\2c"+
		"\u01a6\3\2\2\2e\u01c9\3\2\2\2g\u01cb\3\2\2\2i\u01d8\3\2\2\2k\u01f0\3\2"+
		"\2\2m\u01f2\3\2\2\2o\u0201\3\2\2\2q\u0207\3\2\2\2s\u0209\3\2\2\2u\u020b"+
		"\3\2\2\2w\u020d\3\2\2\2y\u020f\3\2\2\2{\u0211\3\2\2\2}\u0213\3\2\2\2\177"+
		"\u0215\3\2\2\2\u0081\u0217\3\2\2\2\u0083\u0219\3\2\2\2\u0085\u021b\3\2"+
		"\2\2\u0087\u0088\7k\2\2\u0088\u008c\7p\2\2\u0089\u008a\7K\2\2\u008a\u008c"+
		"\7P\2\2\u008b\u0087\3\2\2\2\u008b\u0089\3\2\2\2\u008c\4\3\2\2\2\u008d"+
		"\u008e\7/\2\2\u008e\u008f\7@\2\2\u008f\6\3\2\2\2\u0090\u0091\7$\2\2\u0091"+
		"\b\3\2\2\2\u0092\u0093\7)\2\2\u0093\n\3\2\2\2\u0094\u0095\7.\2\2\u0095"+
		"\f\3\2\2\2\u0096\u0097\7\60\2\2\u0097\16\3\2\2\2\u0098\u0099\7c\2\2\u0099"+
		"\u009a\7p\2\2\u009a\u00a1\7f\2\2\u009b\u009c\7(\2\2\u009c\u00a1\7(\2\2"+
		"\u009d\u009e\7C\2\2\u009e\u009f\7P\2\2\u009f\u00a1\7F\2\2\u00a0\u0098"+
		"\3\2\2\2\u00a0\u009b\3\2\2\2\u00a0\u009d\3\2\2\2\u00a1\20\3\2\2\2\u00a2"+
		"\u00a3\7q\2\2\u00a3\u00a9\7t\2\2\u00a4\u00a5\7~\2\2\u00a5\u00a9\7~\2\2"+
		"\u00a6\u00a7\7Q\2\2\u00a7\u00a9\7T\2\2\u00a8\u00a2\3\2\2\2\u00a8\u00a4"+
		"\3\2\2\2\u00a8\u00a6\3\2\2\2\u00a9\22\3\2\2\2\u00aa\u00ab\7p\2\2\u00ab"+
		"\u00ac\7q\2\2\u00ac\u00b1\7v\2\2\u00ad\u00ae\7P\2\2\u00ae\u00af\7Q\2\2"+
		"\u00af\u00b1\7V\2\2\u00b0\u00aa\3\2\2\2\u00b0\u00ad\3\2\2\2\u00b1\24\3"+
		"\2\2\2\u00b2\u00b3\7v\2\2\u00b3\u00b4\7t\2\2\u00b4\u00b5\7w\2\2\u00b5"+
		"\u00bb\7g\2\2\u00b6\u00b7\7V\2\2\u00b7\u00b8\7T\2\2\u00b8\u00b9\7W\2\2"+
		"\u00b9\u00bb\7G\2\2\u00ba\u00b2\3\2\2\2\u00ba\u00b6\3\2\2\2\u00bb\26\3"+
		"\2\2\2\u00bc\u00bd\7h\2\2\u00bd\u00be\7c\2\2\u00be\u00bf\7n\2\2\u00bf"+
		"\u00c0\7u\2\2\u00c0\u00c7\7g\2\2\u00c1\u00c2\7H\2\2\u00c2\u00c3\7C\2\2"+
		"\u00c3\u00c4\7N\2\2\u00c4\u00c5\7U\2\2\u00c5\u00c7\7G\2\2\u00c6\u00bc"+
		"\3\2\2\2\u00c6\u00c1\3\2\2\2\u00c7\30\3\2\2\2\u00c8\u00c9\7?\2\2\u00c9"+
		"\32\3\2\2\2\u00ca\u00cb\7-\2\2\u00cb\u00cc\7?\2\2\u00cc\34\3\2\2\2\u00cd"+
		"\u00ce\7/\2\2\u00ce\u00cf\7?\2\2\u00cf\36\3\2\2\2\u00d0\u00d1\7\61\2\2"+
		"\u00d1\u00d2\7?\2\2\u00d2 \3\2\2\2\u00d3\u00d4\7,\2\2\u00d4\u00d5\7?\2"+
		"\2\u00d5\"\3\2\2\2\u00d6\u00d7\7?\2\2\u00d7\u00d8\7?\2\2\u00d8$\3\2\2"+
		"\2\u00d9\u00da\7#\2\2\u00da\u00db\7?\2\2\u00db&\3\2\2\2\u00dc\u00dd\7"+
		">\2\2\u00dd(\3\2\2\2\u00de\u00df\7>\2\2\u00df\u00e0\7?\2\2\u00e0*\3\2"+
		"\2\2\u00e1\u00e2\7@\2\2\u00e2,\3\2\2\2\u00e3\u00e4\7@\2\2\u00e4\u00e5"+
		"\7?\2\2\u00e5.\3\2\2\2\u00e6\u00e7\7A\2\2\u00e7\60\3\2\2\2\u00e8\u00e9"+
		"\7<\2\2\u00e9\62\3\2\2\2\u00ea\u00eb\7K\2\2\u00eb\u00ef\7H\2\2\u00ec\u00ed"+
		"\7k\2\2\u00ed\u00ef\7h\2\2\u00ee\u00ea\3\2\2\2\u00ee\u00ec\3\2\2\2\u00ef"+
		"\64\3\2\2\2\u00f0\u00f1\7V\2\2\u00f1\u00f2\7J\2\2\u00f2\u00f3\7G\2\2\u00f3"+
		"\u00f9\7P\2\2\u00f4\u00f5\7v\2\2\u00f5\u00f6\7j\2\2\u00f6\u00f7\7g\2\2"+
		"\u00f7\u00f9\7p\2\2\u00f8\u00f0\3\2\2\2\u00f8\u00f4\3\2\2\2\u00f9\66\3"+
		"\2\2\2\u00fa\u00fb\7G\2\2\u00fb\u00fc\7N\2\2\u00fc\u00fd\7U\2\2\u00fd"+
		"\u0103\7G\2\2\u00fe\u00ff\7g\2\2\u00ff\u0100\7n\2\2\u0100\u0101\7u\2\2"+
		"\u0101\u0103\7g\2\2\u0102\u00fa\3\2\2\2\u0102\u00fe\3\2\2\2\u01038\3\2"+
		"\2\2\u0104\u0105\7p\2\2\u0105\u0106\7w\2\2\u0106\u0107\7n\2\2\u0107\u010d"+
		"\7n\2\2\u0108\u0109\7P\2\2\u0109\u010a\7W\2\2\u010a\u010b\7N\2\2\u010b"+
		"\u010d\7N\2\2\u010c\u0104\3\2\2\2\u010c\u0108\3\2\2\2\u010d:\3\2\2\2\u010e"+
		"\u010f\7P\2\2\u010f\u0110\7c\2\2\u0110\u0111\7P\2\2\u0111<\3\2\2\2\u0112"+
		"\u0113\7o\2\2\u0113\u0114\7c\2\2\u0114\u0115\7v\2\2\u0115\u0116\7e\2\2"+
		"\u0116\u011d\7j\2\2\u0117\u0118\7O\2\2\u0118\u0119\7C\2\2\u0119\u011a"+
		"\7V\2\2\u011a\u011b\7E\2\2\u011b\u011d\7J\2\2\u011c\u0112\3\2\2\2\u011c"+
		"\u0117\3\2\2\2\u011d>\3\2\2\2\u011e\u011f\7f\2\2\u011f\u0120\7g\2\2\u0120"+
		"\u0121\7h\2\2\u0121\u0122\7c\2\2\u0122\u0123\7w\2\2\u0123\u0124\7n\2\2"+
		"\u0124\u012d\7v\2\2\u0125\u0126\7F\2\2\u0126\u0127\7G\2\2\u0127\u0128"+
		"\7H\2\2\u0128\u0129\7C\2\2\u0129\u012a\7W\2\2\u012a\u012b\7N\2\2\u012b"+
		"\u012d\7V\2\2\u012c\u011e\3\2\2\2\u012c\u0125\3\2\2\2\u012d@\3\2\2\2\u012e"+
		"\u012f\7?\2\2\u012f\u0130\7@\2\2\u0130B\3\2\2\2\u0131\u0132\7/\2\2\u0132"+
		"D\3\2\2\2\u0133\u0134\7/\2\2\u0134\u0135\7/\2\2\u0135F\3\2\2\2\u0136\u0137"+
		"\7-\2\2\u0137H\3\2\2\2\u0138\u0139\7-\2\2\u0139\u013a\7-\2\2\u013aJ\3"+
		"\2\2\2\u013b\u013c\7\61\2\2\u013cL\3\2\2\2\u013d\u013e\7,\2\2\u013eN\3"+
		"\2\2\2\u013f\u0140\7}\2\2\u0140P\3\2\2\2\u0141\u0142\7\177\2\2\u0142R"+
		"\3\2\2\2\u0143\u0144\7]\2\2\u0144T\3\2\2\2\u0145\u0146\7_\2\2\u0146V\3"+
		"\2\2\2\u0147\u0148\7*\2\2\u0148X\3\2\2\2\u0149\u014a\7+\2\2\u014aZ\3\2"+
		"\2\2\u014b\u014c\7p\2\2\u014c\u014d\7q\2\2\u014d\u014e\7v\2\2\u014e\u014f"+
		"\7\"\2\2\u014f\u0150\7k\2\2\u0150\u0158\7p\2\2\u0151\u0152\7P\2\2\u0152"+
		"\u0153\7Q\2\2\u0153\u0154\7V\2\2\u0154\u0155\7\"\2\2\u0155\u0156\7K\2"+
		"\2\u0156\u0158\7P\2\2\u0157\u014b\3\2\2\2\u0157\u0151\3\2\2\2\u0158\\"+
		"\3\2\2\2\u0159\u015a\7g\2\2\u015a\u015b\7z\2\2\u015b\u015c\7k\2\2\u015c"+
		"\u015d\7u\2\2\u015d\u015e\7v\2\2\u015e\u0166\7u\2\2\u015f\u0160\7G\2\2"+
		"\u0160\u0161\7Z\2\2\u0161\u0162\7K\2\2\u0162\u0163\7U\2\2\u0163\u0164"+
		"\7V\2\2\u0164\u0166\7U\2\2\u0165\u0159\3\2\2\2\u0165\u015f\3\2\2\2\u0166"+
		"^\3\2\2\2\u0167\u016a\5y=\2\u0168\u016b\5G$\2\u0169\u016b\5C\"\2\u016a"+
		"\u0168\3\2\2\2\u016a\u0169\3\2\2\2\u016a\u016b\3\2\2\2\u016b\u016d\3\2"+
		"\2\2\u016c\u016e\5u;\2\u016d\u016c\3\2\2\2\u016e\u016f\3\2\2\2\u016f\u016d"+
		"\3\2\2\2\u016f\u0170\3\2\2\2\u0170`\3\2\2\2\u0171\u0173\5C\"\2\u0172\u0171"+
		"\3\2\2\2\u0172\u0173\3\2\2\2\u0173\u0174\3\2\2\2\u0174\u0180\5q9\2\u0175"+
		"\u0177\5C\"\2\u0176\u0175\3\2\2\2\u0176\u0177\3\2\2\2\u0177\u0178\3\2"+
		"\2\2\u0178\u017c\5s:\2\u0179\u017b\5u;\2\u017a\u0179\3\2\2\2\u017b\u017e"+
		"\3\2\2\2\u017c\u017a\3\2\2\2\u017c\u017d\3\2\2\2\u017d\u0180\3\2\2\2\u017e"+
		"\u017c\3\2\2\2\u017f\u0172\3\2\2\2\u017f\u0176\3\2\2\2\u0180b\3\2\2\2"+
		"\u0181\u0182\5a\61\2\u0182\u0186\5\r\7\2\u0183\u0185\5u;\2\u0184\u0183"+
		"\3\2\2\2\u0185\u0188\3\2\2\2\u0186\u0184\3\2\2\2\u0186\u0187\3\2\2\2\u0187"+
		"\u018a\3\2\2\2\u0188\u0186\3\2\2\2\u0189\u018b\5_\60\2\u018a\u0189\3\2"+
		"\2\2\u018a\u018b\3\2\2\2\u018b\u018d\3\2\2\2\u018c\u018e\5w<\2\u018d\u018c"+
		"\3\2\2\2\u018d\u018e\3\2\2\2\u018e\u01a7\3\2\2\2\u018f\u0191\5\r\7\2\u0190"+
		"\u0192\5u;\2\u0191\u0190\3\2\2\2\u0192\u0193\3\2\2\2\u0193\u0191\3\2\2"+
		"\2\u0193\u0194\3\2\2\2\u0194\u0196\3\2\2\2\u0195\u0197\5_\60\2\u0196\u0195"+
		"\3\2\2\2\u0196\u0197\3\2\2\2\u0197\u0199\3\2\2\2\u0198\u019a\5w<\2\u0199"+
		"\u0198\3\2\2\2\u0199\u019a\3\2\2\2\u019a\u01a7\3\2\2\2\u019b\u019c\5a"+
		"\61\2\u019c\u019e\5_\60\2\u019d\u019f\5w<\2\u019e\u019d\3\2\2\2\u019e"+
		"\u019f\3\2\2\2\u019f\u01a7\3\2\2\2\u01a0\u01a2\5a\61\2\u01a1\u01a3\5_"+
		"\60\2\u01a2\u01a1\3\2\2\2\u01a2\u01a3\3\2\2\2\u01a3\u01a4\3\2\2\2\u01a4"+
		"\u01a5\5w<\2\u01a5\u01a7\3\2\2\2\u01a6\u0181\3\2\2\2\u01a6\u018f\3\2\2"+
		"\2\u01a6\u019b\3\2\2\2\u01a6\u01a0\3\2\2\2\u01a7d\3\2\2\2\u01a8\u01a9"+
		"\5a\61\2\u01a9\u01ad\5\r\7\2\u01aa\u01ac\5u;\2\u01ab\u01aa\3\2\2\2\u01ac"+
		"\u01af\3\2\2\2\u01ad\u01ab\3\2\2\2\u01ad\u01ae\3\2\2\2\u01ae\u01b1\3\2"+
		"\2\2\u01af\u01ad\3\2\2\2\u01b0\u01b2\5_\60\2\u01b1\u01b0\3\2\2\2\u01b1"+
		"\u01b2\3\2\2\2\u01b2\u01b3\3\2\2\2\u01b3\u01b4\5{>\2\u01b4\u01ca\3\2\2"+
		"\2\u01b5\u01b7\5C\"\2\u01b6\u01b5\3\2\2\2\u01b6\u01b7\3\2\2\2\u01b7\u01b8"+
		"\3\2\2\2\u01b8\u01ba\5\r\7\2\u01b9\u01bb\5u;\2\u01ba\u01b9\3\2\2\2\u01bb"+
		"\u01bc\3\2\2\2\u01bc\u01ba\3\2\2\2\u01bc\u01bd\3\2\2\2\u01bd\u01bf\3\2"+
		"\2\2\u01be\u01c0\5_\60\2\u01bf\u01be\3\2\2\2\u01bf\u01c0\3\2\2\2\u01c0"+
		"\u01c1\3\2\2\2\u01c1\u01c2\5{>\2\u01c2\u01ca\3\2\2\2\u01c3\u01c5\5a\61"+
		"\2\u01c4\u01c6\5_\60\2\u01c5\u01c4\3\2\2\2\u01c5\u01c6\3\2\2\2\u01c6\u01c7"+
		"\3\2\2\2\u01c7\u01c8\5{>\2\u01c8\u01ca\3\2\2\2\u01c9\u01a8\3\2\2\2\u01c9"+
		"\u01b6\3\2\2\2\u01c9\u01c3\3\2\2\2\u01caf\3\2\2\2\u01cb\u01cc\5a\61\2"+
		"\u01cc\u01cd\5}?\2\u01cdh\3\2\2\2\u01ce\u01d9\5\u0081A\2\u01cf\u01d3\5"+
		"\u0081A\2\u01d0\u01d2\5\u0083B\2\u01d1\u01d0\3\2\2\2\u01d2\u01d5\3\2\2"+
		"\2\u01d3\u01d1\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4\u01d6\3\2\2\2\u01d5\u01d3"+
		"\3\2\2\2\u01d6\u01d7\5\u0085C\2\u01d7\u01d9\3\2\2\2\u01d8\u01ce\3\2\2"+
		"\2\u01d8\u01cf\3\2\2\2\u01d9j\3\2\2\2\u01da\u01e0\5\t\5\2\u01db\u01df"+
		"\n\2\2\2\u01dc\u01dd\7^\2\2\u01dd\u01df\t\3\2\2\u01de\u01db\3\2\2\2\u01de"+
		"\u01dc\3\2\2\2\u01df\u01e2\3\2\2\2\u01e0\u01de\3\2\2\2\u01e0\u01e1\3\2"+
		"\2\2\u01e1\u01e3\3\2\2\2\u01e2\u01e0\3\2\2\2\u01e3\u01e4\5\t\5\2\u01e4"+
		"\u01f1\3\2\2\2\u01e5\u01eb\5\7\4\2\u01e6\u01ea\n\4\2\2\u01e7\u01e8\7^"+
		"\2\2\u01e8\u01ea\t\5\2\2\u01e9\u01e6\3\2\2\2\u01e9\u01e7\3\2\2\2\u01ea"+
		"\u01ed\3\2\2\2\u01eb\u01e9\3\2\2\2\u01eb\u01ec\3\2\2\2\u01ec\u01ee\3\2"+
		"\2\2\u01ed\u01eb\3\2\2\2\u01ee\u01ef\5\7\4\2\u01ef\u01f1\3\2\2\2\u01f0"+
		"\u01da\3\2\2\2\u01f0\u01e5\3\2\2\2\u01f1l\3\2\2\2\u01f2\u01f3\7\61\2\2"+
		"\u01f3\u01f4\7\61\2\2\u01f4\u01f6\3\2\2\2\u01f5\u01f7\13\2\2\2\u01f6\u01f5"+
		"\3\2\2\2\u01f7\u01f8\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f8\u01f6\3\2\2\2\u01f9"+
		"\u01fc\3\2\2\2\u01fa\u01fd\5\177@\2\u01fb\u01fd\7\2\2\3\u01fc\u01fa\3"+
		"\2\2\2\u01fc\u01fb\3\2\2\2\u01fd\u01fe\3\2\2\2\u01fe\u01ff\b\67\2\2\u01ff"+
		"n\3\2\2\2\u0200\u0202\t\6\2\2\u0201\u0200\3\2\2\2\u0202\u0203\3\2\2\2"+
		"\u0203\u0201\3\2\2\2\u0203\u0204\3\2\2\2\u0204\u0205\3\2\2\2\u0205\u0206"+
		"\b8\2\2\u0206p\3\2\2\2\u0207\u0208\7\62\2\2\u0208r\3\2\2\2\u0209\u020a"+
		"\4\63;\2\u020at\3\2\2\2\u020b\u020c\4\62;\2\u020cv\3\2\2\2\u020d\u020e"+
		"\t\7\2\2\u020ex\3\2\2\2\u020f\u0210\t\b\2\2\u0210z\3\2\2\2\u0211\u0212"+
		"\t\t\2\2\u0212|\3\2\2\2\u0213\u0214\t\n\2\2\u0214~\3\2\2\2\u0215\u0216"+
		"\7\f\2\2\u0216\u0080\3\2\2\2\u0217\u0218\t\13\2\2\u0218\u0082\3\2\2\2"+
		"\u0219\u021a\t\f\2\2\u021a\u0084\3\2\2\2\u021b\u021c\t\r\2\2\u021c\u0086"+
		"\3\2\2\2\61\2\u008b\u00a0\u00a8\u00b0\u00ba\u00c6\u00ee\u00f8\u0102\u010c"+
		"\u011c\u012c\u0157\u0165\u016a\u016f\u0172\u0176\u017c\u017f\u0186\u018a"+
		"\u018d\u0193\u0196\u0199\u019e\u01a2\u01a6\u01ad\u01b1\u01b6\u01bc\u01bf"+
		"\u01c5\u01c9\u01d3\u01d8\u01de\u01e0\u01e9\u01eb\u01f0\u01f8\u01fc\u0203"+
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