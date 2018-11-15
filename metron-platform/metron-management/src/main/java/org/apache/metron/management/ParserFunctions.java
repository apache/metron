/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.metron.management;

import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.metron.management.Functions.getArg;
import static org.apache.metron.management.Functions.hasArg;

/**
 * Stellar functions that allow the user to parse messages in the Stellar REPL.
 */
public class ParserFunctions {

  @Stellar(
          namespace = "PARSER",
          name = "INIT",
          description = "Initialize a parser to parse messages.",
          params = {
                  "config - A map containing the sensor type as key and the sensor configuration as value.",
                  "globals - An optional map of global configuration values."
          },
          returns = "A parser that can be used to parse messages."
  )
  public static class InitializeFunction extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) throws ParseException {

      String sensorType = getArg("sensorType", 0, String.class, args);
      StellarParserRunner parser = new StellarParserRunner(sensorType);

      // handle the parser configuration argument
      if(hasArg("config", 1, String.class, args)) {
        // parser config passed in as a string
        String arg = getArg("config", 1, String.class, args);
        parser.withParserConfiguration(arg);

      } else {
        // parser configuration passed in as a map
        Map<String, Object> arg = getArg("config", 1, Map.class, args);
        parser.withParserConfiguration(arg);
      }

      // handle the 'globals' argument which is optional
      if(hasArg("globals", 1, Map.class, args)) {
        Map<String, Object> globals = getArg("globals", 1, Map.class, args);
        parser.withGlobals(globals);
      }

      return parser;
    }
  }

  @Stellar(
          namespace = "PARSER",
          name = "PARSE",
          description = "Parse a message.",
          params = {
                  "input - A message or list of messages to parse."
          },
          returns = "A list of messages that result from parsing the input."
  )
  public static class ParseFunction implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      StellarParserRunner parser = getArg("config", 0, StellarParserRunner.class, args);
      parser.withContext(context);

      List<String> messages = getMessages(args);
      return parser.parse(messages);
    }

    /**
     * Retrieves the messages that need parsed from the function arguments.
     * @param args The function arguments.
     * @return The list of messages to parse.
     */
    private List<String> getMessages(List<Object> args) {
      List<String> messages = new ArrayList<>();
      if(hasArg("input", 1, String.class, args)) {
        // the input is a single message as a strign
        String msg = getArg("input", 1, String.class, args);
        messages.add(msg);

      } else if(hasArg("input", 1, List.class, args)) {
        // the input is a list of messages
        List<Object> arg1 = getArg("input", 1, List.class, args);
        for(Object object: arg1) {
          String msg = String.class.cast(object);
          messages.add(msg);
        }

      } else {
        throw new IllegalArgumentException(format("Expected a string or list of strings to parse."));
      }

      return messages;
    }

    @Override
    public void initialize(Context context) {
      // nothing to do
    }

    @Override
    public boolean isInitialized() {
      return true;
    }
  }
}
