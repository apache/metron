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

package org.apache.metron.enrichment.lookup;

import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;

import java.io.Serializable;
import java.util.Objects;

public class EnrichmentResult implements Serializable {
    private EnrichmentKey key;
    private EnrichmentValue value;
    
    public EnrichmentResult(EnrichmentKey key, EnrichmentValue value) {
        this.key = key;
        this.value = value;
    }

    public EnrichmentKey getKey() {
        return key;
    }

    public EnrichmentValue getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrichmentResult)) return false;
        EnrichmentResult lookupKV = (EnrichmentResult) o;
        return Objects.equals(key, lookupKV.key) &&
                Objects.equals(value, lookupKV.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "LookupKV{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
