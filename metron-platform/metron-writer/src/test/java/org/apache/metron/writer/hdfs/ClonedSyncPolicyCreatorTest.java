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

package org.apache.metron.writer.hdfs;

import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.junit.Assert;
import org.junit.Test;

public class ClonedSyncPolicyCreatorTest {

  @Test
  public void testClonedPolicy() {
    CountSyncPolicy basePolicy = new CountSyncPolicy(5);
    ClonedSyncPolicyCreator creator = new ClonedSyncPolicyCreator(basePolicy);
    //ensure cloned policy continues to work and adheres to the contract: mark on 5th call.
    SyncPolicy clonedPolicy = creator.create("blah", null);
    for(int i = 0;i < 4;++i) {
      Assert.assertFalse(clonedPolicy.mark(null, i));
    }
    Assert.assertTrue(clonedPolicy.mark(null, 5));
    //reclone policy and ensure it adheres to the original contract.
    clonedPolicy = creator.create("blah", null);
    Assert.assertFalse(clonedPolicy.mark(null, 0));
  }
}
