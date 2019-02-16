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

import java.nio.charset.StandardCharsets;
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
import java.util.Spliterator;
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
import java.util.stream.StreamSupport;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
    Files.write(dataFile.toPath(), data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
    dataFile.deleteOnExit();
  }

  public static BufferedReader getReader() {
    try {
      return new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8));
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private static void validateMapCount(Map<String, Integer> count) {
    Assert.assertEquals(5, count.size());
    Assert.assertEquals(3, (int)count.get("foo"));
    Assert.assertEquals(2, (int)count.get("bar"));
    Assert.assertEquals(1, (int)count.get("and"));
    Assert.assertEquals(1, (int)count.get("the"));
  }

  @Test
  public void testParallelStreamSmallBatch() throws FileNotFoundException {
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 2)) {

      Map<String, Integer> count =
              stream.parallel().map( s -> s.trim())
                      .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
      validateMapCount(count);
    }
  }

  @Test
  public void testParallelStreamLargeBatch() throws FileNotFoundException {
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 100)) {
      Map<String, Integer> count =
              stream.parallel().map(s -> s.trim())
                      .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
      validateMapCount(count);
    }
  }

  @Test
  public void testSequentialStreamLargeBatch() throws FileNotFoundException {
    try( Stream<String> stream = ReaderSpliterator.lineStream(getReader(), 100)) {
      Map<String, Integer> count =
              stream.map(s -> s.trim())
                      .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
      validateMapCount(count);
    }
  }

  private int getNumberOfBatches(final ReaderSpliterator spliterator) throws ExecutionException, InterruptedException {
    final AtomicInteger numSplits = new AtomicInteger(0);
    //we want to wrap the spliterator and count the (valid) splits
    Spliterator<String> delegatingSpliterator = spy(spliterator);
    doAnswer(invocationOnMock -> {
      Spliterator<String> ret = spliterator.trySplit();
      if(ret != null) {
        numSplits.incrementAndGet();
      }
      return ret;
    }).when(delegatingSpliterator).trySplit();

    Stream<String> stream = StreamSupport.stream(delegatingSpliterator, true);

    //now run it in a parallel pool and do some calculation that doesn't really matter.
    ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
    forkJoinPool.submit(() -> {
                    Map<String, Integer> threads =
                      stream.parallel().map(s -> Thread.currentThread().getName())
                              .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
                    Assert.assertTrue(threads.size() > 0);
            }
    ).get();
    return numSplits.get();
  }

  @Test
  public void testSmallBatch() throws ExecutionException, InterruptedException, IOException {
    //With 9 elements and a batch of 1, we should have ceil(9/1) = 9 batches
    try(BufferedReader reader = getReader()) {
      Assert.assertEquals(9, getNumberOfBatches(new ReaderSpliterator(reader, 1)));
    }
  }

  @Test
  public void testMediumBatch() throws ExecutionException, InterruptedException, IOException {
    //With 9 elements and a batch of 2, we should have ceil(9/2) = 5 batches
    try(BufferedReader reader = getReader()) {
      Assert.assertEquals(5, getNumberOfBatches(new ReaderSpliterator(reader, 2)));
    }
  }

  @Test
  public void testOneBigBatch() throws ExecutionException, InterruptedException, IOException {
    //With 9 elements and a batch of 10, we should only have one batch
    try(BufferedReader reader = getReader()) {
      Assert.assertEquals(1, getNumberOfBatches(new ReaderSpliterator(reader, 10)));
    }
  }

}
