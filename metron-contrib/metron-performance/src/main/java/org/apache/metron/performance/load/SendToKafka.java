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

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class SendToKafka extends TimerTask {
  private long numMessagesSent;
  private long numSentLast = 0;
  private long batchSize;
  private int numBatches;
  private Supplier<String> messageSupplier;
  private String kafkaTopic;
  private ExecutorService pool;
  protected AtomicLong numSent;
  private ThreadLocal<KafkaProducer<String, String>> kafkaProducer;
  public SendToKafka( String kafkaTopic
                    , long numMessagesSent
                    , int numBatches
                    , Supplier<String> messageSupplier
                    , ExecutorService pool
                    , AtomicLong numSent
                    , ThreadLocal<KafkaProducer<String, String>> kafkaProducer
                    )
  {
    this.numSent = numSent;
    this.kafkaProducer = kafkaProducer;
    this.pool = pool;
    this.numMessagesSent = numMessagesSent;
    this.messageSupplier = messageSupplier;
    this.numBatches = numBatches;
    this.batchSize = numMessagesSent/numBatches;
    this.kafkaTopic = kafkaTopic;
  }

  @Override
  public void run() {
    long numSentCurrent = numSent.get();
    long numSentSince = numSentCurrent - numSentLast;
    boolean sendMessages = numSentLast == 0 || numSentSince >= numMessagesSent;
    if(sendMessages) {
      Collection<Future<Long>> futures = Collections.synchronizedList(new ArrayList<>());
      for(int batch = 0;batch < numBatches;++batch) {
        try {
          futures.add(pool.submit(() -> {
            KafkaProducer<String, String> producer = kafkaProducer.get();
            Collection<Future<?>> b = Collections.synchronizedCollection(new ArrayList<>());
            for (int i = 0; i < batchSize; ++i) {
              b.add(sendToKafka(producer, kafkaTopic, messageSupplier.get()));
            }
            for(Future<?> f : b) {
              f.get();
            }
            return batchSize;
          }));

        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
      }
      for(Future<Long> f : futures) {
        try {
          f.get();
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
      }
      numSentLast = numSentCurrent;
    }
  }

  protected Future<?> sendToKafka(KafkaProducer<String, String> producer, String kafkaTopic, String message) {
    return producer.send(new ProducerRecord<>(kafkaTopic, message),
                      (recordMetadata, e) -> {
                        if(e != null) {
                          e.printStackTrace(System.err);
                        }
                        numSent.incrementAndGet();
                      }
              );
  }
}
