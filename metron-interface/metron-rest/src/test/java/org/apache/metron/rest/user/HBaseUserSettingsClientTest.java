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
package org.apache.metron.rest.user;


import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.metron.hbase.client.FakeHBaseClient;
import org.apache.metron.hbase.client.FakeHBaseClientFactory;
import org.apache.metron.hbase.client.FakeHBaseConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.metron.rest.user.HBaseUserSettingsClient.USER_SETTINGS_HBASE_CF;
import static org.apache.metron.rest.user.HBaseUserSettingsClient.USER_SETTINGS_HBASE_TABLE;
import static org.apache.metron.rest.user.HBaseUserSettingsClient.USER_SETTINGS_MAX_SCAN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HBaseUserSettingsClientTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();
  private static final String tableName = "some_table";
  private static final String columnFamily = "cf";
  private static byte[] cf = Bytes.toBytes(columnFamily);

  private Map<String, Object> globals;
  private HBaseUserSettingsClient userSettingsClient;
  private FakeHBaseClient hBaseClient;

  @Before
  public void setUp() throws Exception {
    globals = new HashMap<String, Object>() {{
      put(USER_SETTINGS_HBASE_TABLE, tableName);
      put(USER_SETTINGS_HBASE_CF, columnFamily);
      put(USER_SETTINGS_MAX_SCAN, "100000");
    }};

    hBaseClient = new FakeHBaseClient();
    hBaseClient.deleteAll();

    userSettingsClient = new HBaseUserSettingsClient(
            () -> globals,
            new FakeHBaseClientFactory(hBaseClient),
            new FakeHBaseConnectionFactory(),
            HBaseConfiguration.create());
    userSettingsClient.init();

    // timezone settings
    userSettingsClient.save("user1", "timezone", "EST");
    userSettingsClient.save("user2", "timezone", "MST");
    userSettingsClient.save("user3", "timezone", "PST");

    // language settings
    userSettingsClient.save("user1", "language", "EN");
    userSettingsClient.save("user2", "language", "ES");
    userSettingsClient.save("user3", "language", "FR");
  }

  @After
  public void tearDown() throws IOException {
    userSettingsClient.close();
  }

  @Test
  public void shouldFindSetting() throws Exception {
    assertEquals("EST", userSettingsClient.findOne("user1", "timezone").get());
    assertEquals("MST", userSettingsClient.findOne("user2", "timezone").get());
    assertEquals("PST", userSettingsClient.findOne("user3", "timezone").get());

    assertEquals("EN", userSettingsClient.findOne("user1", "language").get());
    assertEquals("ES", userSettingsClient.findOne("user2", "language").get());
    assertEquals("FR", userSettingsClient.findOne("user3", "language").get());
  }

  @Test
  public void shouldNotFindSetting() throws Exception {
    assertFalse(userSettingsClient.findOne("user999", "timezone").isPresent());
    assertFalse(userSettingsClient.findOne("user999", "language").isPresent());
  }

  @Test
  public void shouldFindSettingsForUser() throws Exception {
    Map<String, String> settings = userSettingsClient.findOne("user1");
    assertEquals(2, settings.size());
    assertEquals("EN", settings.get("language"));
    assertEquals("EST", settings.get("timezone"));
  }

  @Test
  public void shouldNotFindSettingsForUser() throws Exception {
    Map<String, String> settings = userSettingsClient.findOne("user999");
    assertEquals(0, settings.size());
  }

  @Test
  public void shouldFindAllSettingsByType() throws Exception {
    Map<String, Optional<String>> settings = userSettingsClient.findAll("timezone");

    // there should be a 'timezone' setting defined for each user
    int numberOfUsers = 3;
    assertEquals(numberOfUsers, settings.size());
    assertEquals("EST", settings.get("user1").get());
    assertEquals("MST", settings.get("user2").get());
    assertEquals("PST", settings.get("user3").get());
  }

  @Test
  public void shouldFindAllSettings() throws Exception {
    Map<String, Map<String, String>> settings = userSettingsClient.findAll();

    // there should be a set of settings defined for each user
    int numberOfUsers = 3;
    assertEquals(numberOfUsers, settings.size());

    assertEquals("EST", settings.get("user1").get("timezone"));
    assertEquals("MST", settings.get("user2").get("timezone"));
    assertEquals("PST", settings.get("user3").get("timezone"));
    assertEquals("EN", settings.get("user1").get("language"));
    assertEquals("ES", settings.get("user2").get("language"));
    assertEquals("FR", settings.get("user3").get("language"));
  }

  @Test
  public void shouldDeleteSetting() throws Exception {
    // before deleting the "timezone" setting, they should all exist
    assertEquals("EST", userSettingsClient.findOne("user1", "timezone").get());
    assertEquals("EN", userSettingsClient.findOne("user1", "language").get());

    // delete the "timezone" setting for "user1"
    userSettingsClient.delete("user1", "timezone");

    assertFalse(userSettingsClient.findOne("user1", "timezone").isPresent());
    assertEquals("EN", userSettingsClient.findOne("user1", "language").get());
  }

  @Test
  public void shouldNotDeleteSetting() throws Exception {
    // delete the "timezone" setting for a user that does not exist
    userSettingsClient.delete("user999", "timezone");

    // the settings should still not exist and nothing should blow-up
    assertFalse(userSettingsClient.findOne("user999", "timezone").isPresent());
    assertFalse(userSettingsClient.findOne("user999", "language").isPresent());
  }

  @Test
  public void shouldDeleteSettingsByUser() throws Exception {
    // before deleting user1, all the settings should exist
    assertEquals("EST", userSettingsClient.findOne("user1", "timezone").get());
    assertEquals("EN", userSettingsClient.findOne("user1", "language").get());

    // delete the user
    userSettingsClient.delete("user1");

    // none of user1's settings should exist any longer
    assertFalse(userSettingsClient.findOne("user1", "timezone").isPresent());
    assertFalse(userSettingsClient.findOne("user1", "language").isPresent());
  }

  @Test
  public void shouldNotDeleteSettingsByUser() throws Exception {
    // delete the settings for a user that does not exist
    userSettingsClient.delete("user999");

    // the settings should still not exist and nothing should blow-up
    Map<String, String> settings = userSettingsClient.findOne("user999");
    assertEquals(0, settings.size());
  }
}
