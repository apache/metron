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
package org.apache.metron.rest.generator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SampleDataGenerator {

    private Logger LOG = LoggerFactory.getLogger(SampleDataGenerator.class);

    private Map<String, List<String>> sampleData = new HashMap<>();
    private Map<String, Integer> indexes = new HashMap<>();
    private KafkaProducer kafkaProducer;
    private String brokerUrl;
    private Integer num = -1;
    private String selectedSensorType = null;
    private Integer delay = 1000;

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public void setSelectedSensorType(String selectedSensorType) {
        this.selectedSensorType = selectedSensorType;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public static void main(String[] args) throws org.apache.commons.cli.ParseException, IOException, ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cli = parser.parse(getOptions(), args);
        Integer num = Integer.parseInt(cli.getOptionValue("n", "-1"));
        String selectedSensorType = cli.getOptionValue("s");
        Integer delay = Integer.parseInt(cli.getOptionValue("d", "1000"));
        String path = cli.getOptionValue("p");
        if (selectedSensorType == null || path == null) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "sample_data_generator", getOptions());
        } else {
            SampleDataGenerator sampleDataGenerator = new SampleDataGenerator();
            sampleDataGenerator.setNum(num);
            sampleDataGenerator.setSelectedSensorType(selectedSensorType);
            sampleDataGenerator.setDelay(delay);
            sampleDataGenerator.generateSampleData(path);
        }

    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption("b", "brokerUrl", true, "Kafka Broker Url");
        options.addOption("n", "num", false, "Number of messages to emit");
        options.addOption("s", "sensorType", true, "Emit messages to this topic");
        options.addOption("d", "delay", false, "Number of milliseconds to wait between each message.  Defaults to 1 second");
        options.addOption("p", "path", true, "Local path to data file");
        return options;
    }

    public void generateSampleData(String path) throws IOException, ParseException {
        loadData(path);
        startClients();
        try {
            emitData(num, selectedSensorType, delay);
        } finally {
            stopClients();
        }
    }

    private void loadData(String sampleDataPath) throws IOException, ParseException {
        sampleData.put(selectedSensorType, FileUtils.readLines(new File(sampleDataPath)));
        indexes.put(selectedSensorType, 0);
    }

    private void emitData(int num, String selectedSensorType, int delay) {
        int count = 0;
        boolean continueEmitting = false;
        do {
            for (String sensorType : sampleData.keySet()) {
                if (selectedSensorType == null || selectedSensorType.equals(sensorType)) {
                    List<String> sensorData = sampleData.get(sensorType);
                    int index = indexes.get(sensorType);
                    String message = sensorData.get(index++);
                    emitSensorData(sensorType, message, delay);
                    if (num != -1 && ++count >= num) {
                        continueEmitting = false;
                        break;
                    }
                    continueEmitting = true;
                    if (index == sensorData.size()) {
                        index = 0;
                    }
                    indexes.put(sensorType, index);
                }
            }
        } while (continueEmitting);
    }

    private void emitSensorData(String sensorType, String message, int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOG.info("Emitting {} message {}", sensorType, message);
        emitToKafka(sensorType, message);
    }

    private void startClients() {
        startKafka();
    }

    private void startKafka() {
        Map<String, Object> producerConfig = new HashMap<>();
        producerConfig.put("bootstrap.servers", brokerUrl);
        producerConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer = new KafkaProducer<>(producerConfig);
    }

    private void stopClients() {
        stopKafka();
    }

    private void stopKafka() {
        LOG.info("Stopping Kafka producer");
        kafkaProducer.close();
    }

    private void emitToKafka(String topic, String message) {
        kafkaProducer.send(new ProducerRecord<String, String>(topic, message));
    }

}
