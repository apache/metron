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
package org.apache.metron.enrichment.adapters.cif;

import java.net.InetAddress;
import java.util.Properties;

import org.apache.metron.enrichment.bolt.CacheKey;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.apache.metron.test.AbstractTestContext;
import org.junit.Assert;


 /**
 * <ul>
 * <li>Title: CIFHbaseAdapterTest</li>
 * <li>Description: Test Class for CIGFHbaseAdapter</li>
 * <li>Created: Aug 7, 2014</li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class CIFHbaseAdapterTest extends AbstractTestContext {

    private static CIFHbaseAdapter cifHbaseAdapter=null;



    /**
     * Constructs a new <code>CIFHbaseAdapterTest</code> instance.
     * @param name
     */

    public CIFHbaseAdapterTest(String name) {
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
        super.setUp();
        
        Properties prop = super.getTestProperties();
        Assert.assertNotNull(prop);
        
        if(skipTests(this.getMode())){
            return;//skip tests
        }
        
        String[] zk = prop.get("kafka.zk.list").toString().split(",");
        
        for(String z : zk)
        {
        	InetAddress address = InetAddress.getByName(z);
            boolean reachable = address.isReachable(100);

            if(!reachable)
            {
            	this.setMode("local");
            	//throw new Exception("Unable to reach zookeeper, skipping CIF adapter test");
            	break;
            }
            
        }
        
        if(skipTests(this.getMode()))
            return;//skip tests
            
        System.out.println("kafka.zk.list ="+(String) prop.get("kafka.zk.list"));
        System.out.println("kafka.zk.list ="+(String) prop.get("kafka.zk.port"));   
        System.out.println("kafka.zk.list ="+(String) prop.get("bolt.enrichment.cif.tablename"));   
        if(skipTests(this.getMode())){
            System.out.println("Local Mode Skipping tests !! ");
        }else{
            cifHbaseAdapter=new CIFHbaseAdapter((String) prop.get("kafka.zk.list"), (String) prop.get("kafka.zk.port"),(String) prop.get("bolt.enrichment.cif.tablename"));
        }
    }

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */

    protected void tearDown() throws Exception {
        super.tearDown();
        cifHbaseAdapter=null;
    }

    /**
     * Test method for {@link EnrichmentAdapter#initializeAdapter()}.
     */
    public void testInitializeAdapter() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{
            Assert.assertTrue(cifHbaseAdapter.initializeAdapter());
        }
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.cif.CIFHbaseAdapter#enrichByIP(java.lang.String)}.
     */
    public void testEnrichByIP() {
        if(skipTests(this.getMode())){
             return;//skip tests
        }else{      
           Assert.assertNull(cifHbaseAdapter.enrichByIP("11.1.1"));
       }
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.cif.CIFHbaseAdapter#enrichByDomain(java.lang.String)}.
     */
    public void testEnrichByDomain() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{       
           Assert.assertNull(cifHbaseAdapter.enrichByIP("invaliddomain"));
       }
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.cif.CIFHbaseAdapter#enrichByEmail(java.lang.String)}.
     */
    public void testEnrichByEmail() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{
           Assert.assertNull(cifHbaseAdapter.enrichByIP("sample@invalid.com"));
       }
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.cif.CIFHbaseAdapter#CIFHbaseAdapter(java.lang.String, java.lang.String, java.lang.String)}.
     */
    public void testCIFHbaseAdapter() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{
           Assert.assertNotNull(cifHbaseAdapter);
       }
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.cif.CIFHbaseAdapter#enrich(CacheKey)}.
     */
    public void testEnrich() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{
            cifHbaseAdapter.initializeAdapter();
            Assert.assertNotNull(cifHbaseAdapter.enrich(new CacheKey("cif", "testinvalid.metadata", null)));
            
            Assert.assertNotNull(cifHbaseAdapter.enrich(new CacheKey("cif", "ivalid.ip", null)));
            Assert.assertNotNull(cifHbaseAdapter.enrich(new CacheKey("cif", "1.1.1.10", null)));
       }
    }
    

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.cif.CIFHbaseAdapter#getCIFObject(java.lang.String)}.
     */
    public void testGetCIFObject() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{        
           cifHbaseAdapter.initializeAdapter();
           Assert.assertNotNull(cifHbaseAdapter.getCIFObject("testkey"));
       }
    }
    /**
     * Returns the cifHbaseAdapter.
     * @return the cifHbaseAdapter.
     */
    
    public static CIFHbaseAdapter getCifHbaseAdapter() {
        return CIFHbaseAdapterTest.cifHbaseAdapter;
    }

    /**
     * Sets the cifHbaseAdapter.
     * @param cifHbaseAdapter the cifHbaseAdapter.
     */
    
    public static void setCifHbaseAdapter(CIFHbaseAdapter cifHbaseAdapter) {
    
        CIFHbaseAdapterTest.cifHbaseAdapter = cifHbaseAdapter;
    }

}

