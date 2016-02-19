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
package org.apache.metron.threatintel;

import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

public class ThreatIntelConfig implements Serializable {
    public static final long MS_IN_HOUR = 10000*60*60;
    private String hBaseTable;
    private String hBaseCF;
    private double falsePositiveRate = 0.03;
    private int expectedInsertions = 100000;
    private String trackerHBaseTable;
    private String trackerHBaseCF;
    private long millisecondsBetweenPersists = 2*MS_IN_HOUR;
    private TableProvider provider = new HTableProvider();

    public String getHBaseTable() {
        return hBaseTable;
    }

    public int getExpectedInsertions() {
        return expectedInsertions;
    }

    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public String getTrackerHBaseTable() {
        return trackerHBaseTable;
    }

    public String getTrackerHBaseCF() {
        return trackerHBaseCF;
    }

    public long getMillisecondsBetweenPersists() {
        return millisecondsBetweenPersists;
    }

    public String getHBaseCF() {
        return hBaseCF;
    }

    public TableProvider getProvider() {
        return provider;
    }

    public ThreatIntelConfig withProviderImpl(String connectorImpl) {
        if(connectorImpl == null || connectorImpl.length() == 0 || connectorImpl.charAt(0) == '$') {
            provider = new HTableProvider();
        }
        else {
            try {
                Class<? extends TableProvider> clazz = (Class<? extends TableProvider>) Class.forName(connectorImpl);
                provider = clazz.getConstructor().newInstance();
            } catch (InstantiationException e) {
                throw new IllegalStateException("Unable to instantiate connector.", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Unable to instantiate connector: illegal access", e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Unable to instantiate connector", e);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Unable to instantiate connector: no such method", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unable to instantiate connector: class not found", e);
            }
        }
        return this;
    }

    public ThreatIntelConfig withTrackerHBaseTable(String hBaseTable) {
        this.trackerHBaseTable = hBaseTable;
        return this;
    }

    public ThreatIntelConfig withTrackerHBaseCF(String cf) {
        this.trackerHBaseCF = cf;
        return this;
    }
    public ThreatIntelConfig withHBaseTable(String hBaseTable) {
        this.hBaseTable = hBaseTable;
        return this;
    }

    public ThreatIntelConfig withHBaseCF(String cf) {
        this.hBaseCF= cf;
        return this;
    }

    public ThreatIntelConfig withFalsePositiveRate(double falsePositiveRate) {
        this.falsePositiveRate = falsePositiveRate;
        return this;
    }

    public ThreatIntelConfig withExpectedInsertions(int expectedInsertions) {
        this.expectedInsertions = expectedInsertions;
        return this;
    }

    public ThreatIntelConfig withMillisecondsBetweenPersists(long millisecondsBetweenPersists) {
        this.millisecondsBetweenPersists = millisecondsBetweenPersists;
        return this;
    }
}
