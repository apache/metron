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
package org.apache.metron.parsers.contrib.chainlink;

import org.apache.metron.parsers.contrib.links.fields.IdentityLink;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestChainLink {

    private ChainLink link;

    @Before
    public void setUp() {
        this.link = new IdentityLink();
    }

    @After
    public void tearDown() {
        this.link = null;
    }

    @Test(expected = AssertionError.class)
    public void testSelfLink() {
        // A self-link must not occur since it will result into infinite loops
        this.link.setNextLink(this.link);
    }

    @Test
    public void testNextLink() {
        ChainLink otherLink = new IdentityLink();
        this.link.setNextLink(otherLink);
        assertEquals(otherLink, this.link.getNextLink());
        assertTrue(this.link.hasNextLink());
    }

}
