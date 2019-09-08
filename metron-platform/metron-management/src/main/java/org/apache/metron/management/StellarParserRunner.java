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
import java.nio.charset.StandardCharsets;
import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.parsers.ParserRunnerImpl;
import org.apache.metron.parsers.ParserRunnerResults;
import org.apache.metron.stellar.dsl.Context;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private int successCount;
    private int errorCount;

    /**
     * @param sensorType The sensor type of the messages to parse.
     */
    public StellarParserRunner(String sensorType) {
        this.sensorType = sensorType;
        this.successCount = 0;
        this.errorCount = 0;
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
        HashSet<String> sensorTypes = new HashSet<>();
        sensorTypes.add(sensorType);
        ParserRunnerImpl runner = new ParserRunnerImpl(sensorTypes);
        runner.init(() -> parserConfigurations, context);

        // parse each message
        List<ParserRunnerResults<JSONObject>> results = messages
                .stream()
                .map(str -> str.getBytes(StandardCharsets.UTF_8))
                .map(bytes -> DEFAULT.get(emptyMap(), bytes, false, emptyMap()))
                .map(msg -> runner.execute(sensorType, msg, parserConfigurations))
                .collect(Collectors.toList());

        // aggregate both successes and errors into a list that can be returned
        List<JSONObject> successes = results
                .stream()
                .flatMap(result -> result.getMessages().stream())
                .collect(Collectors.toList());
        successCount += successes.size();

        List<JSONObject> errors = results
                .stream()
                .flatMap(result -> result.getErrors().stream())
                .map(err -> err.getJSONObject())
                .collect(Collectors.toList());
        errorCount += errors.size();

        // return a list of both successes and errors
        successes.addAll(errors);
        return successes;
    }

    public StellarParserRunner withParserConfiguration(String sensorConfig) {
        parserConfigurations = create(sensorConfig.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public StellarParserRunner withParserConfiguration(Map<String, Object> config) {
        parserConfigurations = create(new JSONObject(config).toJSONString().getBytes(
            StandardCharsets.UTF_8));
        return this;
    }

    public StellarParserRunner withParserConfiguration(String sensorType, SensorParserConfig config) {
        parserConfigurations = new ParserConfigurations();
        parserConfigurations.updateSensorParserConfig(sensorType, config);
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

    /**
     * @return The JSON configuration of the parser.
     */
    public String toJSON() {
        try {
            return parserConfigurations.getSensorParserConfig(sensorType).toJSON();
        } catch(JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
        // this is is displayed in the REPL; nothing useful to show
        return String.format("Parser{%d successful, %d error(s)}", successCount, errorCount);
    }
}
