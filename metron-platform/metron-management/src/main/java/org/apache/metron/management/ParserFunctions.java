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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.metron.common.configuration.ConfigurationsUtils.readSensorParserConfigFromZookeeper;
import static org.apache.metron.management.Functions.getArg;
import static org.apache.metron.management.Functions.getZookeeperClient;
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
                  "sensorType - The type of sensor to parse.",
                  "config - The parser configuration."
          },
          returns = "A parser that can be used to parse messages."
  )
  public static class InitializeFunction implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String sensorType = getArg("sensorType", 0, String.class, args);
      StellarParserRunner parser = new StellarParserRunner(sensorType);

      // handle the parser configuration argument
      String configArgName = "config";
      if(args.size() == 1) {
        // no config passed by user, attempt to retrieve from zookeeper
        SensorParserConfig config = readFromZookeeper(context, sensorType);
        parser.withParserConfiguration(sensorType, config);

      } else if(hasArg(configArgName, 1, String.class, args)) {
        // parser config passed in as a string
        String arg = getArg(configArgName, 1, String.class, args);
        parser.withParserConfiguration(arg);

      } else if(hasArg(configArgName, 1, Map.class, args)){
        // parser config passed in as a map
        Map<String, Object> arg = getArg(configArgName, 1, Map.class, args);
        parser.withParserConfiguration(arg);

      } else {
        throw new ParseException(format("unexpected '%s' argument; expected string or map", configArgName));
      }

      // handle the 'globals' argument which is optional
      if(hasArg("globals", 1, Map.class, args)) {
        Map<String, Object> globals = getArg("globals", 1, Map.class, args);
        parser.withGlobals(globals);
      }

      return parser;
    }

    private SensorParserConfig readFromZookeeper(Context context, String sensorType) throws ParseException {
      SensorParserConfig config;
      try {
        CuratorFramework zkClient = getZookeeperClient(context);
        config = readSensorParserConfigFromZookeeper(sensorType, zkClient);

      } catch(Exception e) {
        throw new ParseException(ExceptionUtils.getRootCauseMessage(e), e);
      }

      if(config == null) {
        throw new ParseException("Unable to read configuration from Zookeeper; sensorType = " + sensorType);
      }

      return config;
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

  @Stellar(
          namespace = "PARSER",
          name = "PARSE",
          description = "Parse a message.",
          params = {
                  "parser - The parser created with PARSER_INIT.",
                  "input - A message or list of messages to parse."
          },
          returns = "A list of messages that result from parsing the input."
  )
  public static class ParseFunction implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      StellarParserRunner parser = getArg("parser", 0, StellarParserRunner.class, args);
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
      String inputArgName = "input";

      List<String> messages = new ArrayList<>();
      if(hasArg(inputArgName, 1, String.class, args)) {
        // the input is a single message as a string
        String msg = getArg(inputArgName, 1, String.class, args);
        messages.add(msg);

      } else if(hasArg(inputArgName, 1, List.class, args)) {
        // the input is a list of messages
        List<Object> arg1 = getArg(inputArgName, 1, List.class, args);
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

  @Stellar(
          namespace = "PARSER",
          name = "CONFIG",
          description = "Returns the configuration of the parser",
          params = {
                  "parser - The parser created with PARSER_INIT."
          },
          returns = "The parser configuration."
  )
  public static class ConfigFunction extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> args) {
      StellarParserRunner parser = getArg("parser", 0, StellarParserRunner.class, args);
      return parser.toJSON();
    }
  }
}
