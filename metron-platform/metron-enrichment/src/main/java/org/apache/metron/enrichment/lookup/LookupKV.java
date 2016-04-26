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

import java.io.Serializable;

public class LookupKV<KEY_T extends LookupKey, VALUE_T extends LookupValue> implements Serializable {
    private KEY_T key;
    private VALUE_T value;
    public LookupKV(KEY_T key, VALUE_T value) {
        this.key = key;
        this.value = value;
    }

    public KEY_T getKey() {
        return key;
    }

    public VALUE_T getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LookupKV<?, ?> lookupKV = (LookupKV<?, ?>) o;

        if (key != null ? !key.equals(lookupKV.key) : lookupKV.key != null) return false;
        return value != null ? value.equals(lookupKV.value) : lookupKV.value == null;

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "LookupKV{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }
}
