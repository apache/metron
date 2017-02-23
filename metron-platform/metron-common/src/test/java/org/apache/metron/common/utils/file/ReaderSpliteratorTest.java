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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
  public static final File dataFile = new File("target/readerspliteratortest.data");

  @BeforeClass
  public static void setup() throws IOException {
    if(dataFile.exists()) {
      dataFile.delete();
    }
    Files.write(dataFile.toPath(), data.getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
    dataFile.deleteOnExit();
  }

  public static BufferedReader getReader() {
    try {
      return new BufferedReader(new FileReader(dataFile));
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  @Test
  public void testParallelStreamSmallBatch() throws FileNotFoundException {
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 2)) {

      Map<String, Integer> count =
              stream.parallel().map( s -> s.trim())
                      .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
      Assert.assertEquals(5, count.size());
      Assert.assertEquals(3, (int)count.get("foo"));
      Assert.assertEquals(2, (int)count.get("bar"));
      Assert.assertEquals(1, (int)count.get("and"));
      Assert.assertEquals(1, (int)count.get("the"));
    }
  }

  @Test
  public void testParallelStreamLargeBatch() throws FileNotFoundException {
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 100)) {
      Map<String, Integer> count =
              stream.parallel().map(s -> s.trim())
                      .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
      Assert.assertEquals(5, count.size());
      Assert.assertEquals(3, (int) count.get("foo"));
      Assert.assertEquals(2, (int) count.get("bar"));
      Assert.assertEquals(1, (int) count.get("and"));
      Assert.assertEquals(1, (int) count.get("the"));
    }
  }

  @Test
  public void testSequentialStreamLargeBatch() throws FileNotFoundException {
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 100)) {
      Map<String, Integer> count =
              stream.map(s -> s.trim())
                      .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
      Assert.assertEquals(5, count.size());
      Assert.assertEquals(3, (int) count.get("foo"));
      Assert.assertEquals(2, (int) count.get("bar"));
      Assert.assertEquals(1, (int) count.get("and"));
      Assert.assertEquals(1, (int) count.get("the"));
    }
  }



  private void actuallyParallel(Supplier<Stream<String>> streamCreator, Predicate<Integer> condition) throws ExecutionException, InterruptedException {
    final AtomicBoolean passed = new AtomicBoolean(false);
    long start = System.currentTimeMillis();
    //Sporadically this will not parallelize due to choices of the JVM.
    //We want to ensure that it CAN parallelize, not that it always parallelizes,
    //so we'll run it for a minute at most and ensure at least one of those times we get multiple threads,
    //but not more than expected.
    while(!passed.get() && (System.currentTimeMillis() - start) < TimeUnit.MINUTES.toMillis(1)) {
      try (Stream<String> stream = streamCreator.get()) {
        ForkJoinPool forkJoinPool = new ForkJoinPool(10);
        forkJoinPool.submit(() -> {
                  Map<String, Integer> threads =
                          stream.parallel().map(s -> Thread.currentThread().getName())
                                  .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
                  passed.set(condition.test(threads.size()));
                }
        ).get();
        forkJoinPool.shutdownNow();
      }
    }
    Assert.assertTrue("Medium batch did not actually parallelize.", passed.get());
  }

  @Test
  public void testActuallyParallel() throws ExecutionException, InterruptedException, FileNotFoundException {
    //With 9 elements and a batch of 2, we should only ceil(9/2) = 5 batches, so at most min(5, 2) = 2 threads will be used
    actuallyParallel(() -> ReaderSpliterator.lineStream(getReader(), 2), n -> n > 0 && n <= 2);
  }

  @Test
  public void testActuallyParallel_mediumBatch() throws ExecutionException, InterruptedException, FileNotFoundException {
    //With 9 elements and a batch of 2, we should only ceil(9/2) = 5 batches, so at most 5 threads of the pool of 10 will be used
    actuallyParallel(() -> ReaderSpliterator.lineStream(getReader(), 2), n -> (n <= (int) Math.ceil(9.0 / 2) && n > 1));
  }

  @Test
  public void testActuallyParallel_mediumBatchImplicitlyParallel() throws ExecutionException, InterruptedException, FileNotFoundException {
    //With 9 elements and a batch of 2, we should only ceil(9/2) = 5 batches, so at most 5 threads of the pool of 10 will be used
    actuallyParallel(() -> ReaderSpliterator.lineStream(getReader(), 2, true), n -> (n <= (int) Math.ceil(9.0 / 2) && n > 1));
  }

  @Test
  public void testActuallyParallel_mediumBatchNotImplicitlyParallel() throws ExecutionException, InterruptedException, FileNotFoundException {
    //Since this is not parallel and we're not making the stream itself parallel, we should only use one thread from the thread pool.
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 2, false)) {
      ForkJoinPool forkJoinPool = new ForkJoinPool(10);
      forkJoinPool.submit(() -> {
                Map<String, Integer> threads =
                        stream.map(s -> Thread.currentThread().getName())
                                .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
                Assert.assertEquals(1, threads.size());
              }
      ).get();
      forkJoinPool.shutdownNow();
    }
  }

  @Test
  public void testActuallyParallel_bigBatch() throws ExecutionException, InterruptedException, FileNotFoundException {
    //With 9 elements and a batch of 10, we should only have one batch, so only one thread will be used
    //despite the thread pool size of 2.
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 10)) {
      ForkJoinPool forkJoinPool = new ForkJoinPool(2);
      forkJoinPool.submit(() -> {
                Map<String, Integer> threads =
                        stream.parallel().map(s -> Thread.currentThread().getName())
                                .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
                Assert.assertEquals(1, threads.size());
              }
      ).get();
      forkJoinPool.shutdownNow();
    }
  }

}
