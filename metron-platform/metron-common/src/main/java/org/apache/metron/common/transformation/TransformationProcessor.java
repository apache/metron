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

package org.apache.metron.common.transformation;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.apache.metron.common.dsl.ErrorListener;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.VariableResolver;
import org.apache.metron.common.transformation.generated.TransformationLexer;
import org.apache.metron.common.transformation.generated.TransformationParser;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class TransformationProcessor {

  public Object parse(String rule, VariableResolver resolver) {
    if (rule == null || isEmpty(rule.trim())) {
      return null;
    }
    ANTLRInputStream input = new ANTLRInputStream(rule);
    TransformationLexer lexer = new TransformationLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ErrorListener());
    TokenStream tokens = new CommonTokenStream(lexer);
    TransformationParser parser = new TransformationParser(tokens);

    TransformationCompiler treeBuilder = new TransformationCompiler(resolver);
    parser.addParseListener(treeBuilder);
    parser.removeErrorListeners();
    parser.addErrorListener(new ErrorListener());
    parser.transformation();
    return treeBuilder.getResult();
  }

  public boolean validate(String rule) throws ParseException {
    return validate(rule, true);
  }
  public boolean validate(String rule, boolean throwException) throws ParseException {
    try {
      parse(rule, x -> null);
      return true;
    }
    catch(Throwable t) {
      if(throwException) {
        throw new ParseException("Unable to parse " + rule + ": " + t.getMessage(), t);
      }
      else {
        return false;
      }
    }
  }
}
