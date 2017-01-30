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
package org.apache.metron.rest.model;

import java.util.Properties;

public class KafkaTopic {

    private String name;
    private int numPartitions;
    private int replicationFactor;
    private Properties properties = new Properties();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumPartitions() {
        return numPartitions;
    }

    public void setNumPartitions(int numPartitions) {
        this.numPartitions = numPartitions;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KafkaTopic that = (KafkaTopic) o;

        if (numPartitions != that.numPartitions) return false;
        if (replicationFactor != that.replicationFactor) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return properties != null ? properties.equals(that.properties) : that.properties == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + numPartitions;
        result = 31 * result + replicationFactor;
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
