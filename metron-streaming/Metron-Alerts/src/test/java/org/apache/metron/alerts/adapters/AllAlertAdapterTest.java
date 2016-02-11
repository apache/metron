/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.alerts.adapters;

import java.util.Map;
import java.util.Properties;

import org.apache.metron.test.AbstractConfigTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AllAlertAdapterTest extends AbstractConfigTest {

     /**
     * The allAlertAdapter.
     */
    private AllAlertAdapter allAlertAdapter=null;

    public AllAlertAdapterTest() throws Exception {
        super.setUp("org.apache.metron.alerts.adapters.AllAlertAdapter");
        Properties prop = super.getTestProperties();
        Assert.assertNotNull(prop);

        Map<String, String> settings = super.getSettings();

        allAlertAdapter = new AllAlertAdapter(settings);
    }

    /**
     * Test method for {@link org.apache.metron.alerts.adapters.AllAlertAdapter#initialize()}.
     */
    @Ignore //FIXME - This is really an integration test, e.g. requires a running ZK cluster
    @Test
    public void testInitializeAdapter() {
        boolean initialized = this.allAlertAdapter.initialize();
        Assert.assertTrue(initialized);
    }
    
    /**
     * Test method for containsAlertId(@link  org.apache.metron.alerts.adapters.AlllterAdapter#containsAlertId()}.
     */
    @Test
    public void testContainsAlertId() {
        boolean containsAlert = this.allAlertAdapter.containsAlertId("test");
        Assert.assertFalse(containsAlert);
    }
}

