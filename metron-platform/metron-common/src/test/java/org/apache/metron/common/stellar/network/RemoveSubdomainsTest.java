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
package org.apache.metron.common.stellar.network;

import com.google.common.collect.ImmutableList;
import org.apache.metron.common.dsl.functions.NetworkFunctions;
import org.junit.Assert;
import org.junit.Test;

public class RemoveSubdomainsTest {

  @Test
  public void testEdgeCasesTldSquared() {
    NetworkFunctions.RemoveSubdomains removeSubdomains = new NetworkFunctions.RemoveSubdomains();
    Assert.assertEquals("com.com", removeSubdomains.apply(ImmutableList.of("com.com")));
    Assert.assertEquals("net.net", removeSubdomains.apply(ImmutableList.of("net.net")));
    Assert.assertEquals("uk.co.uk", removeSubdomains.apply(ImmutableList.of("co.uk.co.uk")));
    Assert.assertEquals("com.com", removeSubdomains.apply(ImmutableList.of("www.subdomain.com.com")));
  }

  @Test
  public void testHappyPath() {
    NetworkFunctions.RemoveSubdomains removeSubdomains = new NetworkFunctions.RemoveSubdomains();
    Assert.assertEquals("example.com", removeSubdomains.apply(ImmutableList.of("my.example.com")));
    Assert.assertEquals("example.co.uk", removeSubdomains.apply(ImmutableList.of("my.example.co.uk")));
  }

  @Test
  public void testEmptyString() {
    NetworkFunctions.RemoveSubdomains removeSubdomains = new NetworkFunctions.RemoveSubdomains();
    Assert.assertEquals(null, removeSubdomains.apply(ImmutableList.of("")));
  }
}
