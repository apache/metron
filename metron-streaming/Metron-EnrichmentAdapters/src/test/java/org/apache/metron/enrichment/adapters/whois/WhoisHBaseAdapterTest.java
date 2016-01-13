
 
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
package org.apache.metron.enrichment.adapters.whois;

import java.net.InetAddress;
import java.util.Properties;

import org.json.simple.JSONObject;

import org.apache.metron.test.AbstractTestContext;

 /**
 * <ul>
 * <li>Title: </li>
 * <li>Description: </li>
 * <li>Created: Aug 25, 2014 </li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class WhoisHBaseAdapterTest extends AbstractTestContext {

    private static WhoisHBaseAdapter whoisHbaseAdapter=null;   
    private static boolean connected=false;
    /**
     * Constructs a new <code>WhoisHBaseAdapterTest</code> instance.
     * @param name
     */

    public WhoisHBaseAdapterTest(String name) {
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
        assertNotNull(prop);   
        
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
            	break;
            	//throw new Exception("Unable to reach zookeeper, skipping WHois adapter test");
            }
            
            System.out.println("kafka.zk.list ="+(String) prop.get("kafka.zk.list"));
            System.out.println("kafka.zk.list ="+(String) prop.get("kafka.zk.port"));   
            System.out.println("kafka.zk.list ="+(String) prop.get("bolt.enrichment.cif.tablename")); 
            
        }
        
        if(skipTests(this.getMode())){
            System.out.println("Local Mode Skipping tests !! ");
        }else{
            whoisHbaseAdapter=new WhoisHBaseAdapter((String)prop.get("bolt.enrichment.whois.hbase.table.name"),(String)prop.get("kafka.zk.list"),(String)prop.get("kafka.zk.port"));
            connected =whoisHbaseAdapter.initializeAdapter();
            assertTrue(connected);
        }
       
    }

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.whois.WhoisHBaseAdapter#initializeAdapter()}.
     */
    public void testInitializeAdapter() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{
           assertTrue(connected);
       }
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.whois.WhoisHBaseAdapter#enrich(java.lang.String)}.
     */
    public void testEnrich() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{
            JSONObject json = whoisHbaseAdapter.enrich("72.163.4.161");
            
            //assert Geo Response is not null
            assertNotNull(json);
            
            //assert LocId is not null
            assertNotNull(json.get("cisco.com"));
       }
    }


    /**
     * Returns the whoisHbaseAdapter.
     * @return the whoisHbaseAdapter.
     */
    
    public static WhoisHBaseAdapter getWhoisHbaseAdapter() {
        return whoisHbaseAdapter;
    }

    /**
     * Sets the whoisHbaseAdapter.
     * @param whoisHbaseAdapter the whoisHbaseAdapter.
     */
    
    public static void setWhoisHbaseAdapter(WhoisHBaseAdapter whoisHbaseAdapter) {
    
        WhoisHBaseAdapterTest.whoisHbaseAdapter = whoisHbaseAdapter;
    }
}

