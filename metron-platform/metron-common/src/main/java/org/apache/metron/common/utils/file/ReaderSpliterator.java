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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterators.spliterator;

/**
 * A Spliterator which works well on sequential streams by constructing a
 * fixed batch size split rather than inheriting the spliterator from BufferedReader.lines()
 * which gives up and reports no size and has no strategy for batching.  This is a bug
 * in Java 8 and will be fixed in Java 9.
 *
 * <p>The ideas have been informed by https://www.airpair.com/java/posts/parallel-processing-of-io-based-data-with-java-streams
 * except more specific to strings and motivated by a JDK 8 bug as
 * described at http://bytefish.de/blog/jdk8_files_lines_parallel_stream/
 */
public class ReaderSpliterator implements Spliterator<String> {
  private static int characteristics = NONNULL | ORDERED | IMMUTABLE;
  private int batchSize ;
  private BufferedReader reader;
  public ReaderSpliterator(BufferedReader reader) {
    this(reader, 128);
  }

  public ReaderSpliterator(BufferedReader reader, int batchSize) {
    this.batchSize = batchSize;
    this.reader = reader;
  }

  @Override
  public void forEachRemaining(Consumer<? super String> action) {
    if (action == null) {
      throw new NullPointerException();
    }
    try {
      for (String line = null; (line = reader.readLine()) != null;) {
        action.accept(line);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean tryAdvance(Consumer<? super String> action) {
    if (action == null) {
      throw new NullPointerException();
    }
    try {
      final String line = reader.readLine();
      if (line == null) {
        return false;
      }
      action.accept(line);
      return true;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Spliterator<String> trySplit() {
    final ConsumerWithLookback holder = new ConsumerWithLookback();
    if (!tryAdvance(holder)) {
      return null;
    }
    final String[] batch = new String[batchSize];
    int j = 0;
    do {
      batch[j] = holder.value;
    }
    while (++j < batchSize && tryAdvance(holder));
    return spliterator(batch, 0, j, characteristics() | SIZED);
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return characteristics;
  }

  static class ConsumerWithLookback implements Consumer<String> {
    String value;
    @Override
    public void accept(String string) {
      this.value = string;
    }
  }

  public static Stream<String> lineStream(BufferedReader in, int batchSize) {
    return lineStream(in, batchSize, false);
  }

  /**
   * Creates a {@link Stream} with a ReaderSpliterator underlying it.
   *
   * @param in the input for creating the stream
   * @param batchSize The batch size to be used by the spliterator
   * @param isParallel True if stream should be parallel, false otherwise
   * @return The created {@link Stream}
   */
  public static Stream<String> lineStream(BufferedReader in, int batchSize, boolean isParallel) {
    return StreamSupport.stream(new ReaderSpliterator(in, batchSize), isParallel)
                        .onClose(() -> {
                          try {
                            in.close();
                          } catch (IOException e) {
                            throw new UncheckedIOException(e);
                          }
                                       }
                                );
  }
}
