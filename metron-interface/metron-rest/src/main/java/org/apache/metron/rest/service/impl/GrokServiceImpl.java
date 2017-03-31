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
package org.apache.metron.rest.service.impl;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import org.apache.commons.io.IOUtils;
import org.apache.directory.api.util.Strings;
import org.apache.hadoop.fs.Path;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.GrokValidation;
import org.apache.metron.rest.service.GrokService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;

import static org.apache.metron.rest.MetronRestConstants.GROK_TEMP_PATH_SPRING_PROPERTY;

@Service
public class GrokServiceImpl implements GrokService {

    private Environment environment;

    private Grok commonGrok;

    @Autowired
    public GrokServiceImpl(Environment environment, Grok commonGrok) {
        this.environment = environment;
        this.commonGrok = commonGrok;
    }

    @Override
    public Map<String, String> getCommonGrokPatterns() {
        return commonGrok.getPatterns();
    }

    @Override
    public GrokValidation validateGrokStatement(GrokValidation grokValidation) throws RestException {
        Map<String, Object> results;
        try {
            if (grokValidation.getPatternLabel() == null) {
              throw new RestException("Pattern label is required");
            }
            if (Strings.isEmpty(grokValidation.getStatement())) {
              throw new RestException("Grok statement is required");
            }
            Grok grok = new Grok();
            grok.addPatternFromReader(new InputStreamReader(getClass().getResourceAsStream("/patterns/common")));
            grok.addPatternFromReader(new StringReader(grokValidation.getStatement()));
            String grokPattern = "%{" + grokValidation.getPatternLabel() + "}";
            grok.compile(grokPattern);
            Match gm = grok.match(grokValidation.getSampleData());
            gm.captures();
            results = gm.toMap();
            results.remove(grokValidation.getPatternLabel());
        } catch (Exception e) {
            throw new RestException(e);
        }
        grokValidation.setResults(results);
        return grokValidation;
    }

    @Override
    public File saveTemporary(String statement, String name) throws RestException {
        if (statement != null) {
            try {
                File grokDirectory = new File(getTemporaryGrokRootPath());
                if (!grokDirectory.exists()) {
                  grokDirectory.mkdirs();
                }
                File path = new File(grokDirectory, name);
                FileWriter fileWriter = new FileWriter(new File(grokDirectory, name));
                fileWriter.write(statement);
                fileWriter.close();
                return path;
            } catch (IOException e) {
                throw new RestException(e);
            }
        } else {
            throw new RestException("A grokStatement must be provided");
        }
    }

    private String getTemporaryGrokRootPath() {
      String grokTempPath = environment.getProperty(GROK_TEMP_PATH_SPRING_PROPERTY);
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      return new Path(grokTempPath, authentication.getName()).toString();
    }

    public String getStatementFromClasspath(String path) throws RestException {
      try {
        return IOUtils.toString(getClass().getResourceAsStream(path));
      } catch (Exception e) {
        throw new RestException("Could not find a statement at path " + path);
      }
    }

}
