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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.parsers.ParserRunnerImpl;
import org.apache.metron.parsers.ParserRunnerResults;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.metron.management.Functions.getArg;
import static org.apache.metron.common.message.metadata.RawMessageStrategies.*;
import static org.apache.metron.management.Functions.hasArg;
import static java.util.Collections.emptyMap;

/**
 * Stellar functions related to Parsers.
 */
public class ParserFunctions {

  /**
   * TODO
   */
  private static final String FIXED_SENSOR_TYPE = "sensor";

  /**
   *
   */
  public static class Parser {

    private ParserConfigurations parserConfigurations;

    public Parser(String sensorConfig) {
      parserConfigurations = create(sensorConfig.getBytes());
    }

    public Parser(Map<String, Object> sensorConfig) {
      parserConfigurations = create(new JSONObject(sensorConfig).toJSONString().getBytes());
    }

    private static ParserConfigurations create(byte[] sensorConfig) {
      try {
        ParserConfigurations result = new ParserConfigurations();
        result.updateSensorParserConfig(FIXED_SENSOR_TYPE, SensorParserConfig.fromBytes(sensorConfig));
        return result;

      } catch(IOException e) {
        throw new IllegalArgumentException(e);
      }
    }

    public Parser withGlobals(Map<String, Object> globals) {
      parserConfigurations.updateGlobalConfig(globals);
      return this;
    }

    public ParserConfigurations getParserConfigurations() {
      return parserConfigurations;
    }

    @Override
    public String toString() {
      try {
        return parserConfigurations.getSensorParserConfig(FIXED_SENSOR_TYPE).toJSON();

      } catch(JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Stellar(
          namespace = "PARSER",
          name = "INIT",
          description = "TODO",
          params = {
                  "config - A map containing the sensor type as key and the sensor configuration as value.",
                  "globals - An optional map of global configuration values."
          },
          returns = "TODO"
  )
  public static class ParserInit implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      Parser parser;
      if(hasArg("config", 0, String.class, args)) {
        String arg = getArg("config", 0, String.class, args);
        parser = new Parser(arg);

      } else {
        Map<String, Object> arg = getArg("config", 0, Map.class, args);
        parser = new Parser(arg);
      }

      // handle the optional globals
      if(hasArg("globals", 1, Map.class, args)) {
        Map<String, Object> globals = getArg("globals", 1, Map.class, args);
        parser.withGlobals(globals);
      }

      return parser;
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
          description = "TODO",
          params = {
                  "TODO"
          },
          returns = "A parser runner."
  )
  public static class ParserRun implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      Parser parser = getArg("config", 0, Parser.class, args);

      byte[] inputArg;
      if(hasArg("input", 1, String.class, args)) {
        // convert a String to raw bytes
        inputArg = getArg("input", 1, String.class, args).getBytes();

      } else {
        // otherwise assume its the raw bytes
        inputArg = getArg("input", 1, byte[].class, args);
      }

      // run the parser against the raw input
      RawMessage rawMessage = DEFAULT.get(emptyMap(), inputArg, false, emptyMap());
      ParserRunnerImpl parserRunner = new ParserRunnerImpl(Collections.singleton(FIXED_SENSOR_TYPE));
      parserRunner.init(() -> parser.getParserConfigurations(), context);
      ParserRunnerResults<JSONObject> results = parserRunner.execute(FIXED_SENSOR_TYPE, rawMessage, parser.getParserConfigurations());

      // join both messages and errors into a list that can be returned
      Stream<JSONObject> messages = results
              .getMessages()
              .stream();
      Stream<JSONObject> errors = results
              .getErrors()
              .stream()
              .map(error -> error.getJSONObject());
      return Stream.concat(messages, errors)
              .collect(Collectors.toList());
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
