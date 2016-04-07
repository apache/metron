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
package org.apache.metron.reference.lookup.accesstracker;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import org.apache.metron.reference.lookup.LookupKey;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class BloomAccessTracker implements AccessTracker {
    private static final long serialVersionUID = 1L;
    public static final String EXPECTED_INSERTIONS_KEY = "expected_insertions";
    public static final String FALSE_POSITIVE_RATE_KEY = "false_positive_rate";
    public static final String NAME_KEY = "name";

    private static class LookupKeyFunnel implements Funnel<LookupKey> {
        @Override
        public void funnel(LookupKey lookupKey, PrimitiveSink primitiveSink) {
            primitiveSink.putBytes(lookupKey.toBytes());
        }


        @Override
        public boolean equals(Object obj) {
            return this.getClass().equals(obj.getClass());
        }

    }

    private static Funnel<LookupKey> LOOKUPKEY_FUNNEL = new LookupKeyFunnel();

    BloomFilter<LookupKey> filter;
    String name;
    int expectedInsertions;
    double falsePositiveRate;
    int numInsertions = 0;

    public BloomAccessTracker(String name, int expectedInsertions, double falsePositiveRate) {
        this.name = name;
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveRate = falsePositiveRate;
        filter = BloomFilter.create(LOOKUPKEY_FUNNEL, expectedInsertions, falsePositiveRate);
    }
    public BloomAccessTracker() {}
    public BloomAccessTracker(Map<String, Object> config) {
        configure(config);
    }

    protected BloomFilter<LookupKey> getFilter() {
        return filter;
    }
    @Override
    public void logAccess(LookupKey key) {
        numInsertions++;
        filter.put(key);
    }

    @Override
    public void configure(Map<String, Object> config) {
        expectedInsertions = toInt(config.get(EXPECTED_INSERTIONS_KEY));
        falsePositiveRate = toDouble(config.get(FALSE_POSITIVE_RATE_KEY));
        name = config.get(NAME_KEY).toString();
        filter = BloomFilter.create(LOOKUPKEY_FUNNEL, expectedInsertions, falsePositiveRate);
    }

    @Override
    public boolean hasSeen(LookupKey key) {
        return filter.mightContain(key);
    }

    @Override
    public void reset() {
        filter = BloomFilter.create(LOOKUPKEY_FUNNEL, expectedInsertions, falsePositiveRate);
    }

    private static double toDouble(Object o) {
        if(o instanceof String) {
            return Double.parseDouble((String)o);
        }
        else if(o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        else {
            throw new IllegalStateException("Unable to convert " + o + " to a double.");
        }
    }
    private static int toInt(Object o) {
        if(o instanceof String) {
            return Integer.parseInt((String)o);
        }
        else if(o instanceof Number) {
            return ((Number) o).intValue();
        }
        else {
            throw new IllegalStateException("Unable to convert " + o + " to a double.");
        }
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public AccessTracker union(AccessTracker tracker) {
        if(filter == null) {
            throw new IllegalStateException("Unable to union access tracker, because this tracker is not initialized.");
        }
        if(tracker instanceof BloomAccessTracker ) {
            filter.putAll(((BloomAccessTracker)tracker).getFilter());
            return this;
        }
        else {
            throw new IllegalStateException("Unable to union access tracker, because it's not of the right type (BloomAccessTracker)");
        }
    }

    @Override
    public boolean isFull() {
        return numInsertions >= expectedInsertions;
    }

    @Override
    public void cleanup() throws IOException {

    }
}
