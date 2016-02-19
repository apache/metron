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
package org.apache.metron.hbase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class TableConfig implements Serializable {
    static final long serialVersionUID = -1L;
    private String tableName;
    private boolean batch = true;
    protected Map<String, Set<String>> columnFamilies = new HashMap<>();
    private long writeBufferSize = 0L;
    private String connectorImpl;

    public TableConfig() {

    }

    public TableConfig(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public TableConfig withConnectorImpl(String impl) {
        connectorImpl = impl;
        return this;
    }

    public TableConfig withTable(String table) {
        this.tableName = table;
        return this;
    }

    public TableConfig withBatch(Boolean isBatch) {
        this.batch = isBatch;
        return this;
    }

    public String getConnectorImpl() {
        return connectorImpl;
    }

    /**
     * @return Whether batch mode is enabled
     */
    public boolean isBatch() {
        return batch;
    }

    /**
     * @param batch
     *          Whether to enable HBase's client-side write buffer.
     *          <p>
     *          When enabled your bolt will store put operations locally until the
     *          write buffer is full, so they can be sent to HBase in a single RPC
     *          call. When disabled each put operation is effectively an RPC and
     *          is sent straight to HBase. As your bolt can process thousands of
     *          values per second it is recommended that the write buffer is
     *          enabled.
     *          <p>
     *          Enabled by default
     */
    public void setBatch(boolean batch) {
        this.batch = batch;
    }
    /**
     * @param writeBufferSize
     *          Overrides the client-side write buffer size.
     *          <p>
     *          By default the write buffer size is 2 MB (2097152 bytes). If you
     *          are storing larger data, you may want to consider increasing this
     *          value to allow your bolt to efficiently group together a larger
     *          number of records per RPC
     *          <p>
     *          Overrides the write buffer size you have set in your
     *          hbase-site.xml e.g. <code>hbase.client.write.buffer</code>
     */
    public void setWriteBufferSize(long writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }

    /**
     * @return the writeBufferSize
     */
    public long getWriteBufferSize() {
        return writeBufferSize;
    }
    /**
     * @return A Set of configured column families
     */
    public Set<String> getColumnFamilies() {
        return this.columnFamilies.keySet();
    }


}
