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
package org.apache.metron.writer.hdfs;

import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;

public class ClonedSyncPolicyCreator implements SyncPolicyCreator {
  SyncPolicy syncPolicy;
  public ClonedSyncPolicyCreator(SyncPolicy policy) {
    syncPolicy = policy;
  }

  @Override
  public SyncPolicy create(String sensor, WriterConfiguration config) {
    try {
      //we do a deep clone of the SyncPolicy via kryo serialization.  This gives us a fresh policy
      //to work with.  The reason we need a fresh policy is that we want to ensure each handler
      //(one handler per task & sensor type and one handler per file) has its own sync policy.
      // Reusing a sync policy is a bad idea, so we need to clone it here.  Unfortunately the
      // SyncPolicy object does not implement Cloneable, so we'll need to clone it via serialization
      //to get a fresh policy object.  Note: this would be expensive if it was in the critical path,
      // but should be called infrequently (once per sync).

      // Reset the SyncPolicy to ensure that the new count properly resets.
      syncPolicy.reset();
      byte[] serializedForm = SerDeUtils.toBytes(syncPolicy);
      return SerDeUtils.fromBytes(serializedForm, SyncPolicy.class);
    }
    catch (Exception e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
  }
}
