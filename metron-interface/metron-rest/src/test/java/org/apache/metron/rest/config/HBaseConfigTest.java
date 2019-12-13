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
package org.apache.metron.rest.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.configuration.EnrichmentConfigurations;
import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.mock.MockHBaseTableProvider;
import org.apache.metron.rest.service.GlobalConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.apache.metron.rest.user.UserSettingsClient.USER_SETTINGS_HBASE_CF;
import static org.apache.metron.rest.user.UserSettingsClient.USER_SETTINGS_HBASE_TABLE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class HBaseConfigTest {

  private GlobalConfigService globalConfigService;
  private HBaseConfig hBaseConfig;

  @BeforeEach
  public void setUp() {
    globalConfigService = mock(GlobalConfigService.class);
    hBaseConfig = mock(HBaseConfig.class,
            withSettings().useConstructor(globalConfigService).defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  public void userSettingsTableShouldBeReturnedFromGlobalConfigByDefault() throws Exception {
    when(globalConfigService.get()).thenReturn(new HashMap<String, Object>() {{
      put(USER_SETTINGS_HBASE_TABLE, "global_config_user_settings_table");
      put(USER_SETTINGS_HBASE_CF, "global_config_user_settings_cf");
    }});
    HTableProvider htableProvider = mock(HTableProvider.class);

    doReturn(htableProvider).when(hBaseConfig).getTableProvider();
    hBaseConfig.userSettingsClient();
    verify(htableProvider).getTable(any(Configuration.class), eq("global_config_user_settings_table"));
    verifyNoMoreInteractions(htableProvider);
  }

  @Test
  public void hBaseClientShouldBeCreatedWithSpecifiedProvider() throws Exception {
    when(globalConfigService.get()).thenReturn(new HashMap<String, Object>() {{
      put(EnrichmentConfigurations.TABLE_PROVIDER, MockHBaseTableProvider.class.getName());
      put(EnrichmentConfigurations.TABLE_NAME, "enrichment_list_hbase_table_name");
    }});
    assertNotNull(hBaseConfig.hBaseClient());
  }

}
