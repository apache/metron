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
package org.apache.metron.tldextractor;

import org.apache.metron.AbstractConfigTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


 /**
 * <ul>
 * <li>Title: Basic TLD Extractor Test</li>
 * <li>Description: Basic TLD Extractor class test</li>
 * <li>Created: Feb 26, 2015</li>
 * </ul>
 * @author $Author:  $
 * @version $Revision: 1.1 $
 */
public class BasicTldExtractorTest {

    private BasicTldExtractor tldExtractor=null;

     @Before
    public void setUp() throws Exception {
        //super.setUp("org.apache.metron.tldextractor.BasicTldExtractorTest");
        this.tldExtractor=new BasicTldExtractor();
    }

     @After
    public void tearDown() throws Exception {
        //super.tearDown();
    }

    /**
     * Test method for {@link org.apache.metron.tldextractor.BasicTldExtractor#extract2LD(java.lang.String)}.
     */
    @Test
    public void testExtract2LD() {
        String result = this.tldExtractor.extract2LD("cisco.com");
        Assert.assertEquals(result, "cisco.com");
    }

    /**
     * Test method for {@link org.apache.metron.tldextractor.BasicTldExtractor#extractTLD(java.lang.String)}.
     */
    @Test
    public void testExtractTLD() 
    {
        String result = this.tldExtractor.extractTLD("cisco.com");
        Assert.assertEquals(result, ".com");
    }
}

