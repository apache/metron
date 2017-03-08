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
package org.apache.metron.rest.model;

import java.util.HashMap;
import java.util.Map;

public class GrokValidation {

    private String patternLabel;
    private String statement;
    private String sampleData;
    private Map<String, Object> results;

    public String getPatternLabel() {
        return patternLabel;
    }

    public void setPatternLabel(String patternLabel) {
        this.patternLabel = patternLabel;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public String getSampleData() {
        return sampleData;
    }

    public void setSampleData(String sampleData) {
        this.sampleData = sampleData;
    }

    public Map<String, Object> getResults() {
        if (results == null) {
            return new HashMap<>();
        }
        return results;
    }

    public void setResults(Map<String, Object> results) {
        this.results = results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GrokValidation that = (GrokValidation) o;

        if (patternLabel != null ? !patternLabel.equals(that.patternLabel) : that.patternLabel != null) return false;
        if (statement != null ? !statement.equals(that.statement) : that.statement != null) return false;
        if (sampleData != null ? !sampleData.equals(that.sampleData) : that.sampleData != null) return false;
        return results != null ? results.equals(that.results) : that.results == null;
    }

    @Override
    public int hashCode() {
        int result = patternLabel != null ? patternLabel.hashCode() : 0;
        result = 31 * result + (statement != null ? statement.hashCode() : 0);
        result = 31 * result + (sampleData != null ? sampleData.hashCode() : 0);
        result = 31 * result + (results != null ? results.hashCode() : 0);
        return result;
    }
}
