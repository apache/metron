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

package org.apache.metron.stellar.common.evaluators;

import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;
import org.apache.metron.stellar.common.FrameContext;
import org.apache.metron.stellar.common.generated.StellarParser;

public class LongLiteralEvaluator implements NumberEvaluator<StellarParser.LongLiteralContext> {
  @Override
  public Token<Long> evaluate(StellarParser.LongLiteralContext context, FrameContext.Context contextVariety) {
    if (context == null) {
      throw new IllegalArgumentException("Cannot evaluate a context that is null.");
    }

    String value = context.getText();
    if (value.endsWith("l") || value.endsWith("L")) {
      value = value.substring(0, value.length() - 1); // Drop the 'L' or 'l'. Long.parseLong does not accept a string with either of these.
      return new Token<>(Long.parseLong(value), Long.class, contextVariety);
    } else {
      // Technically this should never happen, but just being safe.
      throw new ParseException("Invalid format for long. Failed trying to parse a long with the following value: " + value);
    }
  }
}
