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
		COMMA=1, LBRACKET=2, RBRACKET=3, LPAREN=4, RPAREN=5, INT_LITERAL=6, DOUBLE_LITERAL=7, 
		IDENTIFIER=8, STRING_LITERAL=9, COMMENT=10, WS=11;
	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"COMMA", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "INT_LITERAL", "DOUBLE_LITERAL", 
		"IDENTIFIER", "SCHAR", "STRING_LITERAL", "COMMENT", "WS"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "','", "'['", "']'", "'('", "')'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, "COMMA", "LBRACKET", "RBRACKET", "LPAREN", "RPAREN", "INT_LITERAL", 
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
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\rd\b\1\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7\6\7\'"+
		"\n\7\r\7\16\7(\3\b\6\b,\n\b\r\b\16\b-\3\b\3\b\6\b\62\n\b\r\b\16\b\63\3"+
		"\t\3\t\7\t8\n\t\f\t\16\t;\13\t\3\n\3\n\3\13\3\13\7\13A\n\13\f\13\16\13"+
		"D\13\13\3\13\3\13\3\13\7\13I\n\13\f\13\16\13L\13\13\3\13\5\13O\n\13\3"+
		"\f\3\f\3\f\3\f\6\fU\n\f\r\f\16\fV\3\f\5\fZ\n\f\3\f\3\f\3\r\6\r_\n\r\r"+
		"\r\16\r`\3\r\3\r\3V\2\16\3\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\2\25\13"+
		"\27\f\31\r\3\2\7\5\2C\\aac|\b\2\60\60\62;C\\^^aac|\7\2\f\f\17\17$$))^"+
		"^\3\3\f\f\5\2\13\f\16\17\"\"k\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t"+
		"\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\25\3\2\2"+
		"\2\2\27\3\2\2\2\2\31\3\2\2\2\3\33\3\2\2\2\5\35\3\2\2\2\7\37\3\2\2\2\t"+
		"!\3\2\2\2\13#\3\2\2\2\r&\3\2\2\2\17+\3\2\2\2\21\65\3\2\2\2\23<\3\2\2\2"+
		"\25N\3\2\2\2\27P\3\2\2\2\31^\3\2\2\2\33\34\7.\2\2\34\4\3\2\2\2\35\36\7"+
		"]\2\2\36\6\3\2\2\2\37 \7_\2\2 \b\3\2\2\2!\"\7*\2\2\"\n\3\2\2\2#$\7+\2"+
		"\2$\f\3\2\2\2%\'\4\62;\2&%\3\2\2\2\'(\3\2\2\2(&\3\2\2\2()\3\2\2\2)\16"+
		"\3\2\2\2*,\4\62;\2+*\3\2\2\2,-\3\2\2\2-+\3\2\2\2-.\3\2\2\2./\3\2\2\2/"+
		"\61\7\60\2\2\60\62\4\62;\2\61\60\3\2\2\2\62\63\3\2\2\2\63\61\3\2\2\2\63"+
		"\64\3\2\2\2\64\20\3\2\2\2\659\t\2\2\2\668\t\3\2\2\67\66\3\2\2\28;\3\2"+
		"\2\29\67\3\2\2\29:\3\2\2\2:\22\3\2\2\2;9\3\2\2\2<=\n\4\2\2=\24\3\2\2\2"+
		">B\7$\2\2?A\5\23\n\2@?\3\2\2\2AD\3\2\2\2B@\3\2\2\2BC\3\2\2\2CE\3\2\2\2"+
		"DB\3\2\2\2EO\7$\2\2FJ\7)\2\2GI\5\23\n\2HG\3\2\2\2IL\3\2\2\2JH\3\2\2\2"+
		"JK\3\2\2\2KM\3\2\2\2LJ\3\2\2\2MO\7)\2\2N>\3\2\2\2NF\3\2\2\2O\26\3\2\2"+
		"\2PQ\7\61\2\2QR\7\61\2\2RT\3\2\2\2SU\13\2\2\2TS\3\2\2\2UV\3\2\2\2VW\3"+
		"\2\2\2VT\3\2\2\2WY\3\2\2\2XZ\t\5\2\2YX\3\2\2\2Z[\3\2\2\2[\\\b\f\2\2\\"+
		"\30\3\2\2\2]_\t\6\2\2^]\3\2\2\2_`\3\2\2\2`^\3\2\2\2`a\3\2\2\2ab\3\2\2"+
		"\2bc\b\r\2\2c\32\3\2\2\2\r\2(-\639BJNVY`\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}