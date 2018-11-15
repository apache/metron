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
package org.apache.metron.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.ParserRunnerImpl;
import org.apache.metron.parsers.ParserRunnerResults;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.apache.metron.common.message.metadata.RawMessageStrategies.DEFAULT;

/**
 * Enables parsing of messages in the Stellar REPL.
 *
 * <p>Maintains all of the state between subsequent executions of the parser functions.
 */
public class StellarParserRunner {

    private String sensorType;
    private ParserConfigurations parserConfigurations;
    private Context context;

    /**
     * @param sensorType The sensor type of the messages to parse.
     */
    public StellarParserRunner(String sensorType) {
        this.sensorType = sensorType;
    }

    public List<JSONObject> parse(List<String> messages) {
        if(parserConfigurations == null) {
            throw new IllegalArgumentException("Missing required parser configuration");
        }
        if(context == null) {
            throw new IllegalArgumentException("Missing required context");
        }
        return doParse(messages);
    }

    private List<JSONObject> doParse(List<String> messages) {
        // initialize
        ParserRunnerImpl runner = new ParserRunnerImpl(Collections.singleton(sensorType));
        runner.init(() -> parserConfigurations, context);

        // parse each message
        List<ParserRunnerResults<JSONObject>> results = messages
                .stream()
                .map(str -> str.getBytes())
                .map(msg -> DEFAULT.get(emptyMap(), msg, false, emptyMap()))
                .map(msg -> runner.execute(sensorType, msg, parserConfigurations))
                .collect(Collectors.toList());

        // aggregate both successes and errors into a list that can be returned
        Stream<JSONObject> successes = results
                .stream()
                .flatMap(result -> result.getMessages().stream());
        Stream<JSONObject> errors = results
                .stream()
                .flatMap(result -> result.getErrors().stream())
                .map(err -> err.getJSONObject());
        return Stream.concat(successes, errors)
                .collect(Collectors.toList());
    }

    public StellarParserRunner withParserConfiguration(String sensorConfig) {
        parserConfigurations = create(sensorConfig.getBytes());
        return this;
    }

    public StellarParserRunner withParserConfiguration(Map<String, Object> config) {
        parserConfigurations = create(new JSONObject(config).toJSONString().getBytes());
        return this;
    }

    public StellarParserRunner withContext(Context context) {
        this.context = context;
        return this;
    }

    public StellarParserRunner withGlobals(Map<String, Object> globals) {
        parserConfigurations.updateGlobalConfig(globals);
        return this;
    }

    public ParserConfigurations getParserConfigurations() {
        return parserConfigurations;
    }

    private ParserConfigurations create(byte[] sensorConfig) {
        try {
            ParserConfigurations result = new ParserConfigurations();
            result.updateSensorParserConfig(sensorType, SensorParserConfig.fromBytes(sensorConfig));
            return result;

        } catch(IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return parserConfigurations.getSensorParserConfig(sensorType).toJSON();
        } catch(JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
