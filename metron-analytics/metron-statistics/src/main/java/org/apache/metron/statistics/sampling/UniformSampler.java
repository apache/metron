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
package org.apache.metron.statistics.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This is a reservoir sampler without replacement where each element sampled will be included
 * with equal probability in the reservoir.
 */
public class UniformSampler implements Sampler {
  private List<Object> reservoir;
  private int seen = 0;
  private int size;
  private Random rng = new Random(0);

  public UniformSampler() {
    this(DEFAULT_SIZE);
  }

  public UniformSampler(int size) {
    this.size = size;
    reservoir = new ArrayList<>(size);
  }

  @Override
  public Iterable<Object> get() {
    return reservoir;
  }

  /**
   * Add an object to the reservoir
   * @param o
   */
  public void add(Object o) {
    if(o == null) {
      return;
    }
    if (reservoir.size() < size) {
      reservoir.add(o);
    } else {
      int rIndex = rng.nextInt(seen + 1);
      if (rIndex < size) {
        reservoir.set(rIndex, o);
      }
    }
    seen++;
  }

  @Override
  public Sampler cloneEmpty() {
    return new UniformSampler(getSize());
  }

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UniformSampler that = (UniformSampler) o;

    if (getSize() != that.getSize()) return false;
    return reservoir != null ? reservoir.equals(that.reservoir) : that.reservoir == null;

  }

  @Override
  public int hashCode() {
    int result = reservoir != null ? reservoir.hashCode() : 0;
    result = 31 * result + getSize();
    return result;
  }
}
