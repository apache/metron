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

package org.apache.metron.parsers.integration;

import org.apache.commons.collections.Buffer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.metron.TestConstants;
import org.apache.metron.common.Constants;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.FluxTopologyComponent;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.test.utils.UnitTestHelper;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class WindowsSyslogIntegrationTest extends ParserIntegrationTest {

    @Override
    public String getFluxPath() {
        return "./src/main/flux/windowssyslog/test.yaml";
    }

    @Override
    public String getSampleInputPath() {
        return TestConstants.SAMPLE_DATA_INPUT_PATH + "WindowsSyslogExampleOutput.txt";
    }

    @Override
    public List<byte[]> readSampleData(String samplePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(samplePath));
        List<byte[]> ret = new ArrayList<>();
        byte[] entireAD = "".getBytes();
        for (String line = null; (line = br.readLine()) != null; ) {
            entireAD = ArrayUtils.addAll(entireAD,line.getBytes());
        }
        br.close();
        ret.add(entireAD);
        return ret;
    }

    @Override
    public String getSampleParsedPath() {
        return TestConstants.SAMPLE_DATA_PARSED_PATH + "WindowsSyslogParsed";
    }

    @Override
    public String getSensorType() {
        return "windowssyslog";
    }

    @Override
    public String getFluxTopicProperty() {
        return "spout.kafka.topic.windowssyslog";
    }

}
