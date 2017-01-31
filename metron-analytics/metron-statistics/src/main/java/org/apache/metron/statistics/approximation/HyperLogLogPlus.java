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
package org.apache.metron.statistics.approximation;

import com.clearspring.analytics.stream.cardinality.CardinalityMergeException;
import com.clearspring.analytics.stream.cardinality.ICardinality;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

public class HyperLogLogPlus implements Serializable {
  private final com.clearspring.analytics.stream.cardinality.HyperLogLogPlus hllp;
  private final int p;
  private final int sp;

  /**
   * Construct HLLP with default precisions for normal and sparse sets. Defaults are
   * p=14, sp=25
   */
  public HyperLogLogPlus() {
    this(14, 25);
  }

  /**
   * Construct HLLP with precision for normal sets. This constructor disables the sparse set.
   *
   * @param p Normal set precision
   */
  public HyperLogLogPlus(int p) {
    this(p, 0);
  }

  /**
   * Construct HLLP with precision for both normal and sparse sets.
   * p must be a value between 4 and sp (inclusive) and sp must be less than 32 and greater than or equal to 4.
   *
   * @param p Normal set precision
   * @param sp Sparse set precision
   */
  public HyperLogLogPlus(int p, int sp) {
    this(p, sp, new com.clearspring.analytics.stream.cardinality.HyperLogLogPlus(p, sp));
  }

  private HyperLogLogPlus(int p, int sp, com.clearspring.analytics.stream.cardinality.HyperLogLogPlus hllp) {
    this.p = p;
    this.sp = sp;
    this.hllp = hllp;
  }

  public int getSp() {
    return sp;
  }

  public int getP() {
    return p;
  }

  /**
   * Adds items to the estimator set
   *
   * @param objects Items to add
   * @return True if the internal set is updated when the items are added
   */
  public boolean addAll(List<Object> objects) {
    boolean updated = false;
    for (Object o : objects) {
      updated |= add(o);
    }
    return updated;
  }

  /**
   * Adds item to the estimator set
   *
   * @param o Item to add
   * @return True if the internal set is updated when this item is added
   */
  public boolean add(Object o) {
    return hllp.offer(o);
  }

  public long cardinality() {
    return hllp.cardinality();
  }

  /**
   * Merges hllp sets and returns new merged set. Does not modify original sets.
   *
   * @param estimators hllp sets to merge
   * @return New merged hllp set
   */
  public HyperLogLogPlus merge(List<HyperLogLogPlus> estimators) {
    List<com.clearspring.analytics.stream.cardinality.HyperLogLogPlus> converted = Lists.transform(estimators, s -> s.hllp);
    ICardinality merged = null;
    try {
      merged = hllp.merge(converted.toArray(new com.clearspring.analytics.stream.cardinality.HyperLogLogPlus[]{}));
    } catch (CardinalityMergeException e) {
      throw new IllegalArgumentException("Unable to merge estimators", e);
    }
    return new HyperLogLogPlus(p, sp, (com.clearspring.analytics.stream.cardinality.HyperLogLogPlus) merged);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HyperLogLogPlus that = (HyperLogLogPlus) o;
    return hllp.equals(that.hllp);
  }

  @Override
  public int hashCode() {
    return hllp.hashCode();
  }

}
