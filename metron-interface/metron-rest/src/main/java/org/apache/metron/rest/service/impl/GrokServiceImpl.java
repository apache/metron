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
import org.apache.directory.api.util.Strings;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.model.GrokValidation;
import org.apache.metron.rest.service.GrokService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Map;

@Service
public class GrokServiceImpl implements GrokService {
    private Grok commonGrok;

    @Autowired
    public GrokServiceImpl(Grok commonGrok) {
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
            String statement = Strings.isEmpty(grokValidation.getStatement()) ? "" : grokValidation.getStatement();

            Grok grok = new Grok();
            grok.addPatternFromReader(new InputStreamReader(getClass().getResourceAsStream("/patterns/common")));
            grok.addPatternFromReader(new StringReader(statement));
            String patternLabel = statement.substring(0, statement.indexOf(" "));
            String grokPattern = "%{" + patternLabel + "}";
            grok.compile(grokPattern);
            Match gm = grok.match(grokValidation.getSampleData());
            gm.captures();
            results = gm.toMap();
            results.remove(patternLabel);
        } catch (StringIndexOutOfBoundsException e) {
            throw new RestException("A pattern label must be included (eg. PATTERN_LABEL %{PATTERN:field} ...)", e.getCause());
        } catch (Exception e) {
            throw new RestException(e);
        }
        grokValidation.setResults(results);
        return grokValidation;
    }

}
