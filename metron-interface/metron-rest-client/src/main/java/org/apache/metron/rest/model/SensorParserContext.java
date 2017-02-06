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

import org.apache.metron.common.configuration.SensorParserConfig;

import java.util.HashMap;
import java.util.Map;

public class SensorParserContext {

    private Map<String, Object> sampleData;
    private SensorParserConfig sensorParserConfig;

    public Map<String, Object> getSampleData() {
        if (sampleData == null) {
            return new HashMap<>();
        }
        return sampleData;
    }

    public void setSampleData(Map<String, Object> sampleData) {
        this.sampleData = sampleData;
    }

    public SensorParserConfig getSensorParserConfig() {
        if (sensorParserConfig == null) {
            return new SensorParserConfig();
        }
        return sensorParserConfig;
    }

    public void setSensorParserConfig(SensorParserConfig sensorParserConfig) {
        this.sensorParserConfig = sensorParserConfig;
    }
}
