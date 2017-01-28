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
package org.apache.metron.common.utils.file;

import org.adrianwalker.multilinestring.Multiline;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReaderSpliteratorTest {
  /**
   foo
   bar
   grok
   foo
   the
   and
   grok
   foo
   bar
   */
  @Multiline
  public static String data;

  @Test
  public void testParallelStreamSmallBatch() {
    Stream<String> stream = ReaderSpliterator.lineStream(new BufferedReader(new StringReader(data)), 2);
    Map<String, Integer> count =
            stream.parallel().map( s -> s.trim())
                             .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
    Assert.assertEquals(5, count.size());
    Assert.assertEquals(3, (int)count.get("foo"));
    Assert.assertEquals(2, (int)count.get("bar"));
    Assert.assertEquals(1, (int)count.get("and"));
    Assert.assertEquals(1, (int)count.get("the"));
  }

  @Test
  public void testParallelStreamLargeBatch() {
    Stream<String> stream = ReaderSpliterator.lineStream(new BufferedReader(new StringReader(data)), 100);
    Map<String, Integer> count =
            stream.parallel().map( s -> s.trim())
                             .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
    Assert.assertEquals(5, count.size());
    Assert.assertEquals(3, (int)count.get("foo"));
    Assert.assertEquals(2, (int)count.get("bar"));
    Assert.assertEquals(1, (int)count.get("and"));
    Assert.assertEquals(1, (int)count.get("the"));
  }

  @Test
  public void testSequentialStreamLargeBatch() {
    Stream<String> stream = ReaderSpliterator.lineStream(new BufferedReader(new StringReader(data)), 100);
    Map<String, Integer> count =
            stream.map( s -> s.trim())
                  .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
    Assert.assertEquals(5, count.size());
    Assert.assertEquals(3, (int)count.get("foo"));
    Assert.assertEquals(2, (int)count.get("bar"));
    Assert.assertEquals(1, (int)count.get("and"));
    Assert.assertEquals(1, (int)count.get("the"));
  }

  @Test
  public void testActuallyParallel() throws ExecutionException, InterruptedException {
    //With 9 elements and a batch of 2, we should only ceil(9/2) = 5 batches, so at most min(5, 2) = 2 threads will be used
    Stream<String> stream = ReaderSpliterator.lineStream(new BufferedReader(new StringReader(data)), 2);
    ForkJoinPool forkJoinPool = new ForkJoinPool(2);
    forkJoinPool.submit(() -> {
              Map<String, Integer> threads=
                      stream.parallel().map(s -> Thread.currentThread().getName())
                              .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
              Assert.assertTrue(threads.size() <= 2);
            }
    ).get();
  }

  @Test
  public void testActuallyParallel_mediumBatch() throws ExecutionException, InterruptedException {
    //With 9 elements and a batch of 2, we should only ceil(9/2) = 5 batches, so at most 5 threads of the pool of 10 will be used
    Stream<String> stream = ReaderSpliterator.lineStream(new BufferedReader(new StringReader(data)), 2);
    ForkJoinPool forkJoinPool = new ForkJoinPool(10);
    forkJoinPool.submit(() -> {
              Map<String, Integer> threads=
                      stream.parallel().map(s -> Thread.currentThread().getName())
                              .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
              Assert.assertTrue(threads.size() <= (int)Math.ceil(9.0/2) && threads.size() > 1);
            }
    ).get();
  }

  @Test
  public void testActuallyParallel_bigBatch() throws ExecutionException, InterruptedException {
    //With 9 elements and a batch of 10, we should only have one batch, so only one thread will be used
    //despite the thread pool size of 2.
    Stream<String> stream = ReaderSpliterator.lineStream(new BufferedReader(new StringReader(data)), 10);
    ForkJoinPool forkJoinPool = new ForkJoinPool(2);
    forkJoinPool.submit(() -> {
              Map<String, Integer> threads=
                      stream.parallel().map(s -> Thread.currentThread().getName())
                              .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
              Assert.assertEquals(1, threads.size());
            }
    ).get();
  }
}
