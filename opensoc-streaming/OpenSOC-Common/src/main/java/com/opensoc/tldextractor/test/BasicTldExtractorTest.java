
 
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
package com.opensoc.tldextractor.test;

import com.opensoc.test.AbstractConfigTest;
import com.opensoc.tldextractor.BasicTldExtractor;


 /**
 * <ul>
 * <li>Title: Basic TLD Extractor Test</li>
 * <li>Description: Basic TLD Extractor class test</li>
 * <li>Created: Feb 26, 2015</li>
 * </ul>
 * @author $Author:  $
 * @version $Revision: 1.1 $
 */
public class BasicTldExtractorTest extends AbstractConfigTest {
     /**
     * The tldExtractor.
     */
     
    private BasicTldExtractor tldExtractor=null;

    /**
     * Constructs a new <code>BasicTldExtractorTest</code> instance.
     * @param name
     */

    public BasicTldExtractorTest(String name) {
        super(name);
    }

    /**
     
     * @throws java.lang.Exception
     */
    protected static void setUpBeforeClass() throws Exception {
    }

    /**
     
     * @throws java.lang.Exception
     */
    protected static void tearDownAfterClass() throws Exception {
    }

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */

    protected void setUp() throws Exception {
        super.setUp("com.opensoc.tldextractor.test.BasicTldExtractorTest");
        this.tldExtractor=new BasicTldExtractor(this.getConfig().getString("logFile"));
    } 

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link com.opensoc.tldextractor.BasicTldExtractor#BasicTldExtractor()}.
     */
    public void testBasicTldExtractor() {
        assertNotNull(this.tldExtractor);
    }

    /**
     * Test method for {@link com.opensoc.tldextractor.BasicTldExtractor#extract2LD(java.lang.String)}.
     */
    public void testExtract2LD() {
        //fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.opensoc.tldextractor.BasicTldExtractor#extractTLD(java.lang.String)}.
     */
    public void testExtractTLD() 
    {
        String result = this.tldExtractor.extractTLD("cisco.com");
        System.out.println("result ="+result);
    }
    /**
     * Returns the tldExtractor.
     * @return the tldExtractor.
     */
    
    public BasicTldExtractor getTldExtractor() {
        return tldExtractor;
    }

    /**
     * Sets the tldExtractor.
     * @param tldExtractor the tldExtractor.
     */
    
    public void setTldExtractor(BasicTldExtractor tldExtractor) {
    
        this.tldExtractor = tldExtractor;
    }
    
}

