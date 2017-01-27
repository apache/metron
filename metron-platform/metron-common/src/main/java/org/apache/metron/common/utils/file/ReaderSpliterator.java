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
  /**
   * If a remaining element exists, performs the given action on it,
   * returning {@code true}; else returns {@code false}.  If this
   * Spliterator is {@link #ORDERED} the action is performed on the
   * next element in encounter order.  Exceptions thrown by the
   * action are relayed to the caller.
   *
   * @param action The action
   * @return {@code false} if no remaining elements existed
   * upon entry to this method, else {@code true}.
   * @throws NullPointerException if the specified action is null
   */
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

  /**
   * If this spliterator can be partitioned, returns a Spliterator
   * covering elements, that will, upon return from this method, not
   * be covered by this Spliterator.
   * <p>
   * <p>If this Spliterator is {@link #ORDERED}, the returned Spliterator
   * must cover a strict prefix of the elements.
   * <p>
   * <p>Unless this Spliterator covers an infinite number of elements,
   * repeated calls to {@code trySplit()} must eventually return {@code null}.
   * Upon non-null return:
   * <ul>
   * <li>the value reported for {@code estimateSize()} before splitting,
   * must, after splitting, be greater than or equal to {@code estimateSize()}
   * for this and the returned Spliterator; and</li>
   * <li>if this Spliterator is {@code SUBSIZED}, then {@code estimateSize()}
   * for this spliterator before splitting must be equal to the sum of
   * {@code estimateSize()} for this and the returned Spliterator after
   * splitting.</li>
   * </ul>
   * <p>
   * <p>This method may return {@code null} for any reason,
   * including emptiness, inability to split after traversal has
   * commenced, data structure constraints, and efficiency
   * considerations.
   *
   * @return a {@code Spliterator} covering some portion of the
   * elements, or {@code null} if this spliterator cannot be split
   * @apiNote An ideal {@code trySplit} method efficiently (without
   * traversal) divides its elements exactly in half, allowing
   * balanced parallel computation.  Many departures from this ideal
   * remain highly effective; for example, only approximately
   * splitting an approximately balanced tree, or for a tree in
   * which leaf nodes may contain either one or two elements,
   * failing to further split these nodes.  However, large
   * deviations in balance and/or overly inefficient {@code
   * trySplit} mechanics typically result in poor parallel
   * performance.
   */
  @Override
  public Spliterator<String> trySplit() {
    final HoldingConsumer<String> holder = new HoldingConsumer<>();
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

  /**
   * Returns an estimate of the number of elements that would be
   * encountered by a {@link #forEachRemaining} traversal, or returns {@link
   * Long#MAX_VALUE} if infinite, unknown, or too expensive to compute.
   * <p>
   * <p>If this Spliterator is {@link #SIZED} and has not yet been partially
   * traversed or split, or this Spliterator is {@link #SUBSIZED} and has
   * not yet been partially traversed, this estimate must be an accurate
   * count of elements that would be encountered by a complete traversal.
   * Otherwise, this estimate may be arbitrarily inaccurate, but must decrease
   * as specified across invocations of {@link #trySplit}.
   *
   * @return the estimated size, or {@code Long.MAX_VALUE} if infinite,
   * unknown, or too expensive to compute.
   * @apiNote Even an inexact estimate is often useful and inexpensive to compute.
   * For example, a sub-spliterator of an approximately balanced binary tree
   * may return a value that estimates the number of elements to be half of
   * that of its parent; if the root Spliterator does not maintain an
   * accurate count, it could estimate size to be the power of two
   * corresponding to its maximum depth.
   */
  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  /**
   * Returns a set of characteristics of this Spliterator and its
   * elements. The result is represented as ORed values from {@link
   * #ORDERED}, {@link #DISTINCT}, {@link #SORTED}, {@link #SIZED},
   * {@link #NONNULL}, {@link #IMMUTABLE}, {@link #CONCURRENT},
   * {@link #SUBSIZED}.  Repeated calls to {@code characteristics()} on
   * a given spliterator, prior to or in-between calls to {@code trySplit},
   * should always return the same result.
   * <p>
   * <p>If a Spliterator reports an inconsistent set of
   * characteristics (either those returned from a single invocation
   * or across multiple invocations), no guarantees can be made
   * about any computation using this Spliterator.
   *
   * @return a representation of characteristics
   * @apiNote The characteristics of a given spliterator before splitting
   * may differ from the characteristics after splitting.  For specific
   * examples see the characteristic values {@link #SIZED}, {@link #SUBSIZED}
   * and {@link #CONCURRENT}.
   */
  @Override
  public int characteristics() {
    return characteristics;
  }

  static class HoldingConsumer<String> implements Consumer<String> {
    String value;
    /**
     * Performs this operation on the given argument.
     *
     * @param string the input argument
     */
    @Override
    public void accept(String string) {
      this.value = string;
    }
  }

  public static Stream<String> lineStream(BufferedReader in, int batchSize) {
    return StreamSupport.stream(new ReaderSpliterator(in, batchSize), false)
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
