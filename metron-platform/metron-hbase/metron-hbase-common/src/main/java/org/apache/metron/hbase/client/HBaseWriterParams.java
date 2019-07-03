/*
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
package org.apache.metron.hbase.client;

import org.apache.hadoop.hbase.client.Durability;

/**
 * Parameters that define how the {@link HBaseWriter} writes to HBase.
 */
public class HBaseWriterParams {
  private Durability durability;
  private Long timeToLiveMillis;

  public HBaseWriterParams() {
    durability = Durability.USE_DEFAULT;
    timeToLiveMillis = 0L;
  }

  public HBaseWriterParams withDurability(Durability durability) {
    this.durability = durability;
    return this;
  }

  public HBaseWriterParams withTimeToLive(Long timeToLiveMillis) {
    this.timeToLiveMillis = timeToLiveMillis;
    return this;
  }

  public Durability getDurability() {
    return durability;
  }

  public Long getTimeToLiveMillis() {
    return timeToLiveMillis;
  }
}
