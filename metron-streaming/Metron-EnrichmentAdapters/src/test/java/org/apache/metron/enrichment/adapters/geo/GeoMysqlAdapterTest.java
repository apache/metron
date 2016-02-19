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
package org.apache.metron.enrichment.adapters.geo;

import java.net.URL;
import java.util.Properties;

import org.apache.metron.enrichment.adapters.jdbc.JdbcAdapter;
import org.apache.metron.enrichment.adapters.jdbc.MySqlConfig;
import org.apache.metron.enrichment.interfaces.EnrichmentAdapter;
import org.json.simple.JSONObject;

import org.apache.metron.test.AbstractSchemaTest;
import org.junit.Assert;


/**
 * <ul>
 * <li>Title: GeoMySqlAdapterTest</li>
 * <li>Description: Tests for GeoMySqlAdapter</li>
 * <li>Created: Aug 25, 2014</li>
 * </ul>
 * @version $Revision: 1.1 $
 */
public class GeoMysqlAdapterTest extends AbstractSchemaTest {

    private static JdbcAdapter geoMySqlAdapter=null;
    private static boolean connected=false;

    /**
     * Constructs a new <code>GeoMysqlAdapterTest</code> instance.
     * @param name
     */

    public GeoMysqlAdapterTest(String name) {
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
        System.out.println("username ="+(String)prop.get("mysql.username"));
        if(skipTests(this.getMode())){
            System.out.println(getClass().getName()+" Skipping Tests !!Local Mode");
            return;//skip tests
       }else{
          MySqlConfig mySqlConfig = new MySqlConfig();
          mySqlConfig.setHost((String)prop.get("mysql.ip"));
          mySqlConfig.setPort(new Integer((String) prop.get("mysql.port")));
          mySqlConfig.setUsername((String)prop.get("mysql.username"));
          mySqlConfig.setPassword((String)prop.get("mysql.password"));
          mySqlConfig.setTable((String)prop.get("bolt.enrichment.geo.adapter.table"));
          JdbcAdapter geoAdapter = new GeoAdapter().withJdbcConfig(mySqlConfig);
           GeoMysqlAdapterTest.setGeoMySqlAdapter(geoAdapter);
           connected =geoMySqlAdapter.initializeAdapter();
           Assert.assertTrue(connected);
           URL schema_url = getClass().getClassLoader().getResource(
               "TestSchemas/GeoMySqlSchema.json");
           super.setSchemaJsonString(super.readSchemaFromFile(schema_url));  
       }
    }

    /* 
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */

    protected void tearDown() throws Exception {
        super.tearDown();
        GeoMysqlAdapterTest.setGeoMySqlAdapter(null);
    }

    /**
     * Test method for {@link org.apache.metron.enrichment.adapters.geo.GeoAdapter#enrich(java.lang.String)}.
     */
    public void testEnrich() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{
           
         try {           
                JSONObject json = geoMySqlAdapter.enrich("72.163.4.161");
                
                //assert Geo Response is not null
                System.out.println("json ="+json);
                assertNotNull(json);
        
                Assert.assertEquals(true, super.validateJsonData(super.getSchemaJsonString(), json.toString()));
                //assert LocId is not null
                assertNotNull(json.get("locID"));
                
                //assert right LocId is being returned
                Assert.assertEquals("4522",json.get("locID"));
         } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
         }
       }
    }

    /**
     * Test method for {@link EnrichmentAdapter#initializeAdapter()}.
     */
    public void testInitializeAdapter() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{        
        boolean connected =geoMySqlAdapter.initializeAdapter();
        Assert.assertTrue(connected);
       }
    }
 
    /**
     * Test method for
     *
     *
     *
     *
     *
     * {@link org.apache.metron.enrichment.adapters.geo.GeoAdapter}.
     */
    public void testGeoMysqlAdapter() {
        if(skipTests(this.getMode())){
            return;//skip tests
       }else{       
           Assert.assertTrue(connected);
       }
    }

    /**
     * Returns the geoMySqlAdapter.
     * @return the geoMySqlAdapter.
     */
    
    public static JdbcAdapter getGeoMySqlAdapter() {
        return geoMySqlAdapter;
    }

    /**
     * Sets the geoMySqlAdapter.
     * @param geoMySqlAdapter the geoMySqlAdapter.
     */
    
    public static void setGeoMySqlAdapter(JdbcAdapter geoMySqlAdapter) {
    
        GeoMysqlAdapterTest.geoMySqlAdapter = geoMySqlAdapter;
    }
}

