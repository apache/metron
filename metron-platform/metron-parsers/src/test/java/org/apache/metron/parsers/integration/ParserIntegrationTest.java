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

import junit.framework.Assert;
import org.apache.commons.io.FilenameUtils;
import org.apache.metron.TestConstants;
import org.apache.metron.common.Constants;
import org.apache.metron.integration.BaseIntegrationTest;
import org.apache.metron.integration.ComponentRunner;
import org.apache.metron.integration.Processor;
import org.apache.metron.integration.ReadinessState;
import org.apache.metron.integration.components.ConfigUploadComponent;
import org.apache.metron.integration.components.KafkaWithZKComponent;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.parsers.integration.components.ParserTopologyComponent;
import org.apache.metron.parsers.integration.validation.SampleDataValidation;
import org.apache.metron.test.TestDataType;
import org.apache.metron.test.utils.SampleDataUtils;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.*;

public abstract class ParserIntegrationTest extends BaseIntegrationTest {

    @Test
    public void test() throws Exception {

        final String sensorType = getSensorType();
        final List<byte[]> inputMessages = readSampleData(SampleDataUtils.getSampleDataPath(sensorType, TestDataType.RAW));

        final Properties topologyProperties = new Properties();
        final KafkaWithZKComponent kafkaComponent = getKafkaComponent(topologyProperties, new ArrayList<KafkaWithZKComponent.Topic>() {{
            add(new KafkaWithZKComponent.Topic(sensorType, 1));
        }});
        topologyProperties.setProperty("kafka.broker", kafkaComponent.getBrokerList());

        ConfigUploadComponent configUploadComponent = new ConfigUploadComponent()
                .withTopologyProperties(topologyProperties)
                .withGlobalConfigsPath(TestConstants.SAMPLE_CONFIG_PATH)
                .withParserConfigsPath(TestConstants.PARSER_CONFIGS_PATH);

        ParserTopologyComponent parserTopologyComponent = new ParserTopologyComponent.Builder()
                .withSensorType(sensorType)
                .withTopologyProperties(topologyProperties)
                .withBrokerUrl(kafkaComponent.getBrokerList()).build();

        UnitTestHelper.verboseLogging();
        ComponentRunner runner = new ComponentRunner.Builder()
                .withComponent("kafka", kafkaComponent)
                .withComponent("config", configUploadComponent)
                .withComponent("storm", parserTopologyComponent)
                .withMillisecondsBetweenAttempts(5000)
                .withNumRetries(10)
                .build();
        runner.start();

        kafkaComponent.writeMessages(sensorType, inputMessages);
        List<byte[]> outputMessages =
                runner.process(new Processor<List<byte[]>>() {
                    List<byte[]> messages = null;

                    public ReadinessState process(ComponentRunner runner) {
                        KafkaWithZKComponent kafkaWithZKComponent = runner.getComponent("kafka", KafkaWithZKComponent.class);
                        List<byte[]> outputMessages = kafkaWithZKComponent.readMessages(Constants.ENRICHMENT_TOPIC);
                        if (outputMessages.size() == inputMessages.size()) {

                            messages = outputMessages;
                            return ReadinessState.READY;
                        } else {
                            return ReadinessState.NOT_READY;
                        }
                    }

                    public List<byte[]> getResult() {
                        return messages;
                    }
                });

        List<ParserValidation> validations = getValidations();
        if (validations == null || validations.isEmpty()) {
            System.out.println("No validations configured for sensorType " + sensorType + ".  Dumping parsed messages");
            System.out.println();
            dumpParsedMessages(outputMessages);
            System.out.println();
            Assert.fail();
        } else {
            for (ParserValidation validation : validations) {
                System.out.println("Running " + validation.getName() + " on sensorType " + sensorType);
                validation.validate(sensorType, outputMessages);

            }
        }
        runner.stop();
    }

    public void dumpParsedMessages(List<byte[]> outputMessages) {
        for (byte[] outputMessage : outputMessages) {
            System.out.println(new String(outputMessage));
        }
    }


    abstract String getSensorType();
    abstract List<ParserValidation> getValidations();

    public List<byte[]> readSampleData(String samplePath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(samplePath));
        List<byte[]> ret = new ArrayList<>();
        for (String line = null; (line = br.readLine()) != null; ) {
            ret.add(line.getBytes());
        }
        br.close();
        return ret;
    }
}