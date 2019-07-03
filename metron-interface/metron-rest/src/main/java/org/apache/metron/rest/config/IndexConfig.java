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

import org.apache.metron.common.zookeeper.ConfigurationsCache;
import org.apache.metron.hbase.client.HBaseConnectionFactory;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.IndexDao;
import org.apache.metron.indexing.dao.IndexDaoFactory;
import org.apache.metron.indexing.dao.metaalert.MetaAlertDao;
import org.apache.metron.indexing.util.IndexingCacheUtil;
import org.apache.metron.rest.MetronRestConstants;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.GlobalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Optional;

import static org.apache.metron.rest.MetronRestConstants.INDEX_DAO_IMPL;
import static org.apache.metron.rest.MetronRestConstants.INDEX_WRITER_NAME;

@Configuration
public class IndexConfig {

  @Autowired
  private GlobalConfigService globalConfigService;

  @Autowired
  private ConfigurationsCache cache;

  @Autowired
  private Environment environment;

  @Autowired
  private HBaseConnectionFactory hBaseConnectionFactory;

  @Autowired
  private org.apache.hadoop.conf.Configuration hBaseConfiguration;

  @Autowired
  private AccessConfig accessConfig;

  @Autowired
  public IndexConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public AccessConfig accessConfig() {
    int searchMaxResults = environment.getProperty(MetronRestConstants.SEARCH_MAX_RESULTS, Integer.class, 1000);
    int searchMaxGroups = environment.getProperty(MetronRestConstants.SEARCH_MAX_GROUPS, Integer.class, 1000);

    AccessConfig config = new AccessConfig();
    config.setHbaseConnectionFactory(hBaseConnectionFactory);
    config.setHbaseConfiguration(hBaseConfiguration);
    config.setMaxSearchResults(searchMaxResults);
    config.setMaxSearchGroups(searchMaxGroups);
    config.setGlobalConfigSupplier(() -> {
      try {
        return globalConfigService.get();
      } catch (RestException e) {
        throw new IllegalStateException("Unable to retrieve the global config.", e);
      }
    });
    config.setIndexSupplier(IndexingCacheUtil.getIndexLookupFunction(cache, environment.getProperty(INDEX_WRITER_NAME)));
    config.setKerberosEnabled(environment.getProperty(MetronRestConstants.KERBEROS_ENABLED_SPRING_PROPERTY, Boolean.class, false));
    return config;
  }

  @Bean(destroyMethod = "close")
  public IndexDao indexDao() {
    try {
      String indexDaoImpl = environment.getProperty(MetronRestConstants.INDEX_DAO_IMPL, String.class, null);
      if (indexDaoImpl == null) {
        throw new IllegalStateException("You must provide an index DAO implementation via the " + INDEX_DAO_IMPL + " config");
      }

      IndexDao indexDao = IndexDaoFactory.combine(IndexDaoFactory.create(indexDaoImpl, accessConfig));
      if (indexDao == null) {
        throw new IllegalStateException("IndexDao is unable to be created.");
      }

      String metaDaoImpl = environment.getProperty(MetronRestConstants.META_DAO_IMPL, String.class, null);
      if (metaDaoImpl == null) {
        // We're not using meta alerts.
        return indexDao;

      } else {
        // Create the meta alert dao and wrap it around the index dao.
        String metaDaoSort = environment.getProperty(MetronRestConstants.META_DAO_SORT, String.class, null);
        MetaAlertDao metaAlertDao = (MetaAlertDao) IndexDaoFactory.create(metaDaoImpl, accessConfig).get(0);
        metaAlertDao.init(indexDao, Optional.ofNullable(metaDaoSort));
        return metaAlertDao;
      }

    } catch(Exception e) {
      throw new IllegalStateException("Unable to create index DAO: " + e.getMessage(), e);
    }
  }
}