/*
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

package org.apache.metron.indexing.util;

import java.util.Map;
import java.util.function.Function;
import org.apache.metron.common.configuration.IndexingConfigurations;
import org.apache.metron.common.zookeeper.ConfigurationsCache;

public class IndexingCacheUtil {

  @SuppressWarnings("unchecked")
  public static Function<String, String> getIndexLookupFunction(ConfigurationsCache cache, String writerName) {
    return sensorType -> {
      String indexingTopic = sensorType;
      IndexingConfigurations indexingConfigs = cache.get( IndexingConfigurations.class);
      Map<String, Object> indexingSensorConfigs = indexingConfigs.getSensorIndexingConfig(sensorType);
      if (indexingSensorConfigs != null) {
        Map<String, Object> writerConfigs = (Map<String, Object>) indexingSensorConfigs.get(writerName);
        if (writerConfigs != null) {
          indexingTopic = (String) writerConfigs.getOrDefault(IndexingConfigurations.INDEX_CONF, indexingTopic);
        }
      }
      return indexingTopic;
    };
  }
}
