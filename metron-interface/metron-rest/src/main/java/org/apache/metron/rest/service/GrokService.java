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
import org.apache.metron.parsers.GrokParser;
import org.apache.metron.rest.model.GrokValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

@Service
public class GrokService {

    public static final String GROK_DEFAULT_PATH_SPRING_PROPERTY = "grok.path.default";
    public static final String GROK_TEMP_PATH_SPRING_PROPERTY = "grok.path.temp";
    public static final String GROK_CLASS_NAME = GrokParser.class.getName();
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
                String fullGrokStatement = getGrokStatement(grokPath);
                String patternLabel = (String) sensorParserConfig.getParserConfig().get(GROK_PATTERN_LABEL_KEY);
                grokStatement = fullGrokStatement.replaceFirst(patternLabel + " ", "");
            } catch(FileNotFoundException e) {}
        }
        sensorParserConfig.getParserConfig().put(GROK_STATEMENT_KEY, grokStatement);
    }

    public void addGrokPathToConfig(SensorParserConfig sensorParserConfig) {
        if (sensorParserConfig.getParserConfig().get(GROK_PATH_KEY) == null) {
            String grokStatement = (String) sensorParserConfig.getParserConfig().get(GROK_STATEMENT_KEY);
            if (grokStatement != null) {
              sensorParserConfig.getParserConfig().put(GROK_PATH_KEY,
                      new Path(environment.getProperty(GROK_DEFAULT_PATH_SPRING_PROPERTY), sensorParserConfig.getSensorTopic()).toString());
            }
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
        String patternLabel = (String) sensorParserConfig.getParserConfig().get(GROK_PATTERN_LABEL_KEY);
        String grokPath = (String) sensorParserConfig.getParserConfig().get(GROK_PATH_KEY);
        String grokStatement = (String) sensorParserConfig.getParserConfig().get(GROK_STATEMENT_KEY);
        if (grokStatement != null) {
          String fullGrokStatement = patternLabel + " " + grokStatement;
            if (!isTemporary) {
                hdfsService.write(new Path(grokPath), fullGrokStatement.getBytes());
            } else {
                File grokDirectory = new File(getTemporaryGrokRootPath());
                if (!grokDirectory.exists()) {
                    grokDirectory.mkdirs();
                }
                FileWriter fileWriter = new FileWriter(new File(grokDirectory, sensorParserConfig.getSensorTopic()));
                fileWriter.write(fullGrokStatement);
                fileWriter.close();
            }
        } else {
          throw new IllegalArgumentException("A grokStatement must be provided");
        }
    }

    public void deleteTemporaryGrokStatement(SensorParserConfig sensorParserConfig) throws IOException {
        File file = new File(getTemporaryGrokRootPath(), sensorParserConfig.getSensorTopic());
        file.delete();
    }

    public String getTemporaryGrokRootPath() {
        String grokTempPath = environment.getProperty(GROK_TEMP_PATH_SPRING_PROPERTY);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new Path(grokTempPath, authentication.getName()).toString();
    }


}
