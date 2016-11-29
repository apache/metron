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
package org.apache.metron.rest.service;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import org.apache.hadoop.fs.Path;
import org.apache.metron.common.configuration.SensorParserConfig;
import org.apache.metron.rest.model.GrokValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class GrokService {

    public static final String GROK_PATH_SPRING_PROPERTY = "grok.path";
    public static final String GROK_CLASS_NAME = "org.apache.metron.parsers.GrokParser";
    public static final String GROK_PATH_KEY = "grokPath";
    public static final String GROK_STATEMENT_KEY = "grokStatement";
    public static final String GROK_PATTERN_LABEL_KEY = "patternLabel";

    @Autowired
    private Environment environment;

    @Autowired
    private Grok commonGrok;

    @Autowired
    private HdfsService hdfsService;

    public Map<String, String> getCommonGrokPatterns() {
        return commonGrok.getPatterns();
    }

    public GrokValidation validateGrokStatement(GrokValidation grokValidation) {
        Map<String, Object> results = new HashMap<>();
        try {
            Grok grok = new Grok();
            grok.addPatternFromReader(new InputStreamReader(getClass().getResourceAsStream("/patterns/common")));
            grok.addPatternFromReader(new StringReader(grokValidation.getStatement()));
            String patternLabel = grokValidation.getStatement().substring(0, grokValidation.getStatement().indexOf(" "));
            String grokPattern = "%{" + patternLabel + "}";
            grok.compile(grokPattern);
            Match gm = grok.match(grokValidation.getSampleData());
            gm.captures();
            results = gm.toMap();
            results.remove(patternLabel);
        } catch (StringIndexOutOfBoundsException e) {
            results.put("error", "A pattern label must be included (ex. PATTERN_LABEL ${PATTERN:field} ...)");
        } catch (Exception e) {
            results.put("error", e.getMessage());
        }
        grokValidation.setResults(results);
        return grokValidation;
    }

    public boolean isGrokConfig(SensorParserConfig sensorParserConfig) {
        return GROK_CLASS_NAME.equals(sensorParserConfig.getParserClassName());
    }

    public void addGrokStatementToConfig(SensorParserConfig sensorParserConfig) throws IOException {
        String grokStatement = "";
        String grokPath = (String) sensorParserConfig.getParserConfig().get(GROK_PATH_KEY);
        if (grokPath != null) {
            try {
                grokStatement = getGrokStatement(grokPath);
            } catch(FileNotFoundException e) {}
        }
        sensorParserConfig.getParserConfig().put(GROK_STATEMENT_KEY, grokStatement);
    }

    public void addGrokPathToConfig(SensorParserConfig sensorParserConfig) {
        String grokStatement = (String) sensorParserConfig.getParserConfig().get(GROK_STATEMENT_KEY);
        if (grokStatement != null) {
            sensorParserConfig.getParserConfig().put(GROK_PATH_KEY, getGrokPath(sensorParserConfig.getSensorTopic()).toString());
        }
    }

    public String getGrokStatement(String path) throws IOException {
        return new String(hdfsService.read(new Path(path)));
    }

    public void saveGrokStatement(SensorParserConfig sensorParserConfig) throws IOException {
        saveGrokStatement(sensorParserConfig, false);
    }

    public void saveTemporaryGrokStatement(SensorParserConfig sensorParserConfig) throws IOException {
        saveGrokStatement(sensorParserConfig, true);
    }

    private void saveGrokStatement(SensorParserConfig sensorParserConfig, boolean isTemporary) throws IOException {
        String grokStatement = (String) sensorParserConfig.getParserConfig().get(GROK_STATEMENT_KEY);
        if (grokStatement != null) {
            if (!isTemporary) {
                hdfsService.write(getGrokPath(sensorParserConfig.getSensorTopic()), grokStatement.getBytes());
            } else {
                hdfsService.write(getTempGrokPath(sensorParserConfig.getSensorTopic()), grokStatement.getBytes());
            }
        }
    }

    public Path getGrokPath(String name) {
        return new Path(environment.getProperty(GROK_PATH_SPRING_PROPERTY), name);
    }

    public Path getTempGrokPath(String name) {
        return new Path(environment.getProperty(GROK_PATH_SPRING_PROPERTY) + "/temp", name);
    }

    public void deleteTemporaryGrokStatement(SensorParserConfig sensorParserConfig) throws IOException {
        hdfsService.delete(getTempGrokPath(sensorParserConfig.getSensorTopic()), false);
    }


}
