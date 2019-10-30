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
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SendToKafkaTest {

  @Test
  public void testWritesCorrectNumber() {
    ExecutorService executor = ForkJoinPool.commonPool();
    AtomicLong numSent = new AtomicLong(0);
    long expectedSent = 100;
    SendToKafka sender = new SendToKafka(null, expectedSent, 10, () -> "msg", executor, numSent, ThreadLocal.withInitial(() -> null) ) {
      @Override
      protected Future<?> sendToKafka(KafkaProducer producer, String kafkaTopic, String message) {
        assertEquals(message, "msg");
        return ForkJoinPool.commonPool().submit(() -> {
          numSent.incrementAndGet();
        });
      }
    };
    sender.run();
    assertEquals(numSent.get(), expectedSent);
  }

}
