// Generated from org/apache/metron/common/transformation/generated/Transformation.g4 by ANTLR 4.5
package org.apache.metron.common.transformation.generated;

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
public class TransformationLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.5", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		COMMA=1, LBRACKET=2, RBRACKET=3, LPAREN=4, RPAREN=5, MINUS=6, INT_LITERAL=7, 
		DOUBLE_LITERAL=8, IDENTIFIER=9, STRING_LITERAL=10, COMMENT=11, WS=12;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"COMMA", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "MINUS", "INT_LITERAL", 
		"DOUBLE_LITERAL", "IDENTIFIER", "SCHAR", "STRING_LITERAL", "COMMENT", 
		"WS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "','", "'['", "']'", "'('", "')'", "'-'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "COMMA", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "MINUS", "INT_LITERAL", 
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


	public TransformationLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Transformation.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\16n\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6"+
		"\3\7\3\7\3\b\5\b+\n\b\3\b\6\b.\n\b\r\b\16\b/\3\t\5\t\63\n\t\3\t\6\t\66"+
		"\n\t\r\t\16\t\67\3\t\3\t\6\t<\n\t\r\t\16\t=\3\n\3\n\7\nB\n\n\f\n\16\n"+
		"E\13\n\3\13\3\13\3\f\3\f\7\fK\n\f\f\f\16\fN\13\f\3\f\3\f\3\f\7\fS\n\f"+
		"\f\f\16\fV\13\f\3\f\5\fY\n\f\3\r\3\r\3\r\3\r\6\r_\n\r\r\r\16\r`\3\r\5"+
		"\rd\n\r\3\r\3\r\3\16\6\16i\n\16\r\16\16\16j\3\16\3\16\3`\2\17\3\3\5\4"+
		"\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\2\27\f\31\r\33\16\3\2\7\5\2C\\aa"+
		"c|\b\2\60\60\62;C\\^^aac|\7\2\f\f\17\17$$))^^\3\3\f\f\5\2\13\f\16\17\""+
		"\"w\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3"+
		"\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2"+
		"\2\2\33\3\2\2\2\3\35\3\2\2\2\5\37\3\2\2\2\7!\3\2\2\2\t#\3\2\2\2\13%\3"+
		"\2\2\2\r\'\3\2\2\2\17*\3\2\2\2\21\62\3\2\2\2\23?\3\2\2\2\25F\3\2\2\2\27"+
		"X\3\2\2\2\31Z\3\2\2\2\33h\3\2\2\2\35\36\7.\2\2\36\4\3\2\2\2\37 \7]\2\2"+
		" \6\3\2\2\2!\"\7_\2\2\"\b\3\2\2\2#$\7*\2\2$\n\3\2\2\2%&\7+\2\2&\f\3\2"+
		"\2\2\'(\7/\2\2(\16\3\2\2\2)+\5\r\7\2*)\3\2\2\2*+\3\2\2\2+-\3\2\2\2,.\4"+
		"\62;\2-,\3\2\2\2./\3\2\2\2/-\3\2\2\2/\60\3\2\2\2\60\20\3\2\2\2\61\63\5"+
		"\r\7\2\62\61\3\2\2\2\62\63\3\2\2\2\63\65\3\2\2\2\64\66\4\62;\2\65\64\3"+
		"\2\2\2\66\67\3\2\2\2\67\65\3\2\2\2\678\3\2\2\289\3\2\2\29;\7\60\2\2:<"+
		"\4\62;\2;:\3\2\2\2<=\3\2\2\2=;\3\2\2\2=>\3\2\2\2>\22\3\2\2\2?C\t\2\2\2"+
		"@B\t\3\2\2A@\3\2\2\2BE\3\2\2\2CA\3\2\2\2CD\3\2\2\2D\24\3\2\2\2EC\3\2\2"+
		"\2FG\n\4\2\2G\26\3\2\2\2HL\7$\2\2IK\5\25\13\2JI\3\2\2\2KN\3\2\2\2LJ\3"+
		"\2\2\2LM\3\2\2\2MO\3\2\2\2NL\3\2\2\2OY\7$\2\2PT\7)\2\2QS\5\25\13\2RQ\3"+
		"\2\2\2SV\3\2\2\2TR\3\2\2\2TU\3\2\2\2UW\3\2\2\2VT\3\2\2\2WY\7)\2\2XH\3"+
		"\2\2\2XP\3\2\2\2Y\30\3\2\2\2Z[\7\61\2\2[\\\7\61\2\2\\^\3\2\2\2]_\13\2"+
		"\2\2^]\3\2\2\2_`\3\2\2\2`a\3\2\2\2`^\3\2\2\2ac\3\2\2\2bd\t\5\2\2cb\3\2"+
		"\2\2de\3\2\2\2ef\b\r\2\2f\32\3\2\2\2gi\t\6\2\2hg\3\2\2\2ij\3\2\2\2jh\3"+
		"\2\2\2jk\3\2\2\2kl\3\2\2\2lm\b\16\2\2m\34\3\2\2\2\17\2*/\62\67=CLTX`c"+
		"j\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}