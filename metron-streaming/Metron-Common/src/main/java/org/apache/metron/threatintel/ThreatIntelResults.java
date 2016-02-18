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

import org.apache.metron.hbase.converters.threatintel.ThreatIntelKey;

import java.util.HashMap;
import java.util.Map;

public class ThreatIntelResults {
    private ThreatIntelKey key;
    private Map<String, String> value;
    public ThreatIntelResults() {
        key = new ThreatIntelKey();
        value = new HashMap<>();
    }
    public ThreatIntelResults(ThreatIntelKey key, Map<String, String> value) {
        this.key = key;
        this.value = value;
    }

    public ThreatIntelKey getKey() {
        return key;
    }

    public Map<String, String> getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreatIntelResults that = (ThreatIntelResults) o;

        if (getKey() != null ? !getKey().equals(that.getKey()) : that.getKey() != null) return false;
        return getValue() != null ? getValue().equals(that.getValue()) : that.getValue() == null;

    }

    @Override
    public int hashCode() {
        int result = getKey() != null ? getKey().hashCode() : 0;
        result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ThreatIntelResults{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
