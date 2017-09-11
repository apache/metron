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

import static org.apache.metron.rest.MetronRestConstants.INDEX_DAO_IMPL;

import org.apache.metron.hbase.HTableProvider;
import org.apache.metron.hbase.TableProvider;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.IndexDaoFactory;
import org.apache.metron.indexing.dao.MetaAlertDao;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class IndexConfig {

  @Autowired
  private GlobalConfigService globalConfigService;

  @Autowired
  private Environment environment;

  @Autowired
  public IndexConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public IndexDao indexDao() {
    try {
      String hbaseProviderImpl = environment.getProperty(MetronRestConstants.INDEX_HBASE_TABLE_PROVIDER_IMPL, String.class, null);
      String indexDaoImpl = environment.getProperty(MetronRestConstants.INDEX_DAO_IMPL, String.class, null);
      int searchMaxResults = environment.getProperty(MetronRestConstants.SEARCH_MAX_RESULTS, Integer.class, 1000);
      int searchMaxGroups = environment.getProperty(MetronRestConstants.SEARCH_MAX_GROUPS, Integer.class, 1000);
      String metaDaoImpl = environment.getProperty(MetronRestConstants.META_DAO_IMPL, String.class, null);
      String metaDaoSort = environment.getProperty(MetronRestConstants.META_DAO_SORT, String.class, null);
      AccessConfig config = new AccessConfig();
      config.setMaxSearchResults(searchMaxResults);
      config.setMaxSearchGroups(searchMaxGroups);
      config.setGlobalConfigSupplier(() -> {
        try {
          return globalConfigService.get();
        } catch (RestException e) {
          throw new IllegalStateException("Unable to retrieve the global config.", e);
        }
      });
      config.setTableProvider(TableProvider.create(hbaseProviderImpl, () -> new HTableProvider()));
      if (indexDaoImpl == null) {
        throw new IllegalStateException("You must provide an index DAO implementation via the " + INDEX_DAO_IMPL + " config");
      }
      IndexDao indexDao = IndexDaoFactory.combine(IndexDaoFactory.create(indexDaoImpl, config));
      if (indexDao == null) {
        throw new IllegalStateException("IndexDao is unable to be created.");
      }
      if (metaDaoImpl == null) {
        // We're not using meta alerts.
        return indexDao;
      }

      // Create the meta alert dao and wrap it around the index dao.
      MetaAlertDao ret = (MetaAlertDao) IndexDaoFactory.create(metaDaoImpl, config).get(0);
      ret.init(indexDao, metaDaoSort);
      return ret;
    }
    catch(RuntimeException re) {
      throw re;
    }
    catch(Exception e) {
      throw new IllegalStateException("Unable to create index DAO: " + e.getMessage(), e);
    }
  }
}