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
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.metron.common.utils.KafkaUtils;
import org.apache.metron.performance.sampler.BiasedSampler;
import org.apache.metron.performance.sampler.Sampler;
import org.apache.metron.performance.sampler.UnbiasedSampler;
import org.apache.metron.performance.util.KafkaUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class LoadGenerator
{
  public static String CONSUMER_GROUP = "load.group";
  public static long SEND_PERIOD_MS = 100;
  public static long MONITOR_PERIOD_MS = 1000*10;
  private static ExecutorService pool;
  private static ThreadLocal<KafkaProducer> kafkaProducer;
  public static AtomicLong numSent = new AtomicLong(0);
  public static class SendTask extends TimerTask {
    private long numMessagesSent;
    private long numSentLast = 0;
    private long batchSize;
    private int numBatches;
    private Supplier<String> messageSupplier;
    private String kafkaTopic;
    public SendTask(String kafkaTopic, long numMessagesSent, int numBatches, Supplier<String> messageSupplier, long sendPeriod) {
      System.out.println("Sending " + numMessagesSent + " messages to " + kafkaTopic + " every " + sendPeriod + "ms");
      this.numMessagesSent = numMessagesSent;
      this.messageSupplier = messageSupplier;
      this.numBatches = numBatches;
      this.batchSize = numMessagesSent/this.numBatches;
      this.kafkaTopic = kafkaTopic;
    }

    @Override
    public void run() {
      long numSentCurrent = numSent.get();
      long numSentSince = numSentCurrent - numSentLast;
      boolean sendMessages = numSentLast == 0 || numSentSince >= numMessagesSent;
      if(sendMessages) {
        AtomicInteger legitCount = new AtomicInteger(0);
        Collection<Future<Long>> futures = Collections.synchronizedList(new ArrayList<>());
        Collection<KafkaProducer<String, String>> kps = Collections.synchronizedCollection(new HashSet<>());
        for(int batch = 0;batch < numBatches;++batch) {
          try {
            futures.add(pool.submit(() -> {
              KafkaProducer<String, String> producer = kafkaProducer.get();
              kps.add(producer);
              Collection<Future<?>> b = Collections.synchronizedCollection(new ArrayList<>());
              for (int i = 0; i < batchSize; ++i) {
                b.add(producer.send(new ProducerRecord<>(kafkaTopic, messageSupplier.get()),
                        (recordMetadata, e) -> {
                          if(e != null) {
                            e.printStackTrace();
                          }
                          numSent.incrementAndGet();
                        }
                ));
                legitCount.incrementAndGet();
              }
              for(Future<?> f : b) {
                f.get();
              }
              return batchSize;
            }));

          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        for(Future<Long> f : futures) {
          try {
            f.get();
          } catch (InterruptedException e) {
            e.printStackTrace();
          } catch (ExecutionException e) {
            e.printStackTrace();
          }
        }
        numSentLast = numSentCurrent;
      }
      else {
      }
    }
  }

  public static class MonitorTask extends TimerTask {
    List<AbstractMonitor> monitors;
    public MonitorTask(List<AbstractMonitor> monitors) {
      this.monitors = monitors;
    }

    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
      List<String> parts = new ArrayList<>();
      for(AbstractMonitor m : monitors) {
        parts.add(m.get());
      }
      DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      Date date = new Date();
      String output = dateFormat.format(date) + " - ";

      System.out.println(output + Joiner.on(", ").skipNulls().join(parts));
    }
  }

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
        timer.scheduleAtFixedRate(new SendTask(outputTopicStr, messagesPerPeriod, numThreads, generator, sendDelta), 0, sendDelta);
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
      timer.scheduleAtFixedRate(new MonitorTask(monitors), 0, monitorDelta);
    }
  }
}
