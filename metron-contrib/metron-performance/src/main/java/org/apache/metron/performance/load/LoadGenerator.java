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
package org.apache.metron.performance.load;


import com.google.common.base.Joiner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.performance.load.monitor.AbstractMonitor;
import org.apache.metron.performance.load.monitor.EPSGeneratedMonitor;
import org.apache.metron.performance.load.monitor.EPSWrittenMonitor;
import org.apache.metron.performance.load.monitor.MonitorTask;
import org.apache.metron.performance.load.monitor.writers.ConsoleWriter;
import org.apache.metron.performance.load.monitor.writers.Writer;
import org.apache.metron.performance.sampler.BiasedSampler;
import org.apache.metron.performance.sampler.Sampler;
import org.apache.metron.performance.sampler.UnbiasedSampler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LoadGenerator
{
  public static String CONSUMER_GROUP = "load.group";
  public static long SEND_PERIOD_MS = 100;
  public static long MONITOR_PERIOD_MS = 1000*10;
  private static ExecutorService pool;
  private static ThreadLocal<KafkaProducer> kafkaProducer;
  public static AtomicLong numSent = new AtomicLong(0);

  public static void main( String[] args ) throws Exception {
    CommandLine cli = LoadOptions.parse(new PosixParser(), args);
    EnumMap<LoadOptions, Optional<Object>> evaluatedArgs = LoadOptions.createConfig(cli);
    Map<String, Object> kafkaConfig = new HashMap<>();
    kafkaConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kafkaConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    kafkaConfig.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    if(LoadOptions.ZK.has(cli)) {
      String zkQuorum = (String) evaluatedArgs.get(LoadOptions.ZK).get();
      kafkaConfig.put("bootstrap.servers", Joiner.on(",").join(KafkaUtils.INSTANCE.getBrokersFromZookeeper(zkQuorum)));
    }
    String groupId = evaluatedArgs.get(LoadOptions.CONSUMER_GROUP).get().toString();
    System.out.println("Consumer Group: " + groupId);
    kafkaConfig.put("group.id", groupId);
    if(LoadOptions.KAFKA_CONFIG.has(cli)) {
      kafkaConfig.putAll((Map<String, Object>) evaluatedArgs.get(LoadOptions.KAFKA_CONFIG).get());
    }
    kafkaProducer = ThreadLocal.withInitial(() -> new KafkaProducer(kafkaConfig));
    int numThreads = (int)evaluatedArgs.get(LoadOptions.NUM_THREADS).get();
    System.out.println("Thread pool size: " + numThreads);
    pool = Executors.newFixedThreadPool(numThreads);
    Optional<Object> eps = evaluatedArgs.get(LoadOptions.EPS);

    Optional<Object> outputTopic = evaluatedArgs.get(LoadOptions.OUTPUT_TOPIC);
    Optional<Object> monitorTopic = evaluatedArgs.get(LoadOptions.MONITOR_TOPIC);
    long sendDelta = (long) evaluatedArgs.get(LoadOptions.SEND_DELTA).get();
    long monitorDelta = (long) evaluatedArgs.get(LoadOptions.MONITOR_DELTA).get();
    if((eps.isPresent() && outputTopic.isPresent()) || monitorTopic.isPresent()) {
      Timer timer = new Timer(false);
      long startTimeMs = System.currentTimeMillis();
      if(outputTopic.isPresent() && eps.isPresent()) {
        List<String> templates = (List<String>)evaluatedArgs.get(LoadOptions.TEMPLATE).get();
        Optional<Object> biases = evaluatedArgs.get(LoadOptions.BIASED_SAMPLE);
        Sampler sampler = new UnbiasedSampler();
        if(biases.isPresent()){
          sampler = new BiasedSampler((List<Map.Entry<Integer, Integer>>) biases.get(), templates.size());
        }
        MessageGenerator generator = new MessageGenerator(templates, sampler);
        Long targetLoad = (Long)eps.get();
        int periodsPerSecond = (int)(1000/sendDelta);
        long messagesPerPeriod = targetLoad/periodsPerSecond;
        String outputTopicStr = (String)outputTopic.get();
        System.out.println("Generating data to " + outputTopicStr + " at " + targetLoad + " events per second");
        System.out.println("Sending " + messagesPerPeriod + " messages to " + outputTopicStr + " every " + sendDelta + "ms");
        timer.scheduleAtFixedRate(new SendToKafka( outputTopicStr
                                                 , messagesPerPeriod
                                                 , numThreads
                                                 , generator
                                                 , pool
                                                 , numSent
                                                 , kafkaProducer
                                                 )
                                 , 0, sendDelta);
      }
      List<AbstractMonitor> monitors = new ArrayList<>();
      if(outputTopic.isPresent() && monitorTopic.isPresent()) {
        System.out.println("Monitoring " + monitorTopic.get() + " every " + monitorDelta + " ms");
        monitors.add(new EPSGeneratedMonitor(outputTopic, numSent));
        monitors.add(new EPSWrittenMonitor(monitorTopic, kafkaConfig));
      }
      else if(outputTopic.isPresent() && !monitorTopic.isPresent()) {
        System.out.println("Monitoring " + outputTopic.get() + " every " + monitorDelta + " ms");
        monitors.add(new EPSGeneratedMonitor(outputTopic, numSent));
        monitors.add(new EPSWrittenMonitor(outputTopic, kafkaConfig));
      }
      else if(!outputTopic.isPresent() && monitorTopic.isPresent()) {
        System.out.println("Monitoring " + monitorTopic.get() + " every " + monitorDelta + " ms");
        monitors.add(new EPSWrittenMonitor(monitorTopic, kafkaConfig));
      }
      else if(!outputTopic.isPresent() && !monitorTopic.isPresent()) {
        System.out.println("You have not specified an output topic or a monitoring topic, so I have nothing to do here.");
      }
      int lookback = (int) evaluatedArgs.get(LoadOptions.SUMMARY_LOOKBACK).get();
      if(lookback > 0) {
        System.out.println("Summarizing over the last " + lookback + " monitoring periods (" + lookback*monitorDelta + "ms)");
      }
      else {
        System.out.println("Turning off summarization.");
      }
      Writer writer = new Writer(monitors, lookback, new ArrayList<Consumer<List<Writer.Results>>>() {{
        add(new ConsoleWriter());
      }});
      timer.scheduleAtFixedRate(new MonitorTask(writer), 0, monitorDelta);
      Optional<Object> timeLimit = evaluatedArgs.get(LoadOptions.TIME_LIMIT);
      if(timeLimit.isPresent()) {
        System.out.println("Ending in " + timeLimit.get() + " ms.");
        timer.schedule(new TimerTask() {
                         @Override
                         public void run() {
                           timer.cancel();
                           long durationS = (System.currentTimeMillis() - startTimeMs)/1000;
                           System.out.println("\nGenerated " + numSent.get() + " in " + durationS + " seconds." );
                           System.exit(0);
                         }
                       }

                , (Long) timeLimit.get());
      }
    }
  }
}
