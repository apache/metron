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

package org.apache.metron.pcap.filter.query;

import org.apache.hadoop.conf.Configuration;
import org.apache.metron.common.Constants;
import org.apache.metron.common.query.PredicateProcessor;
import org.apache.metron.common.query.VariableResolver;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.PcapFieldResolver;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;

import java.util.EnumMap;
import java.util.Map;

public class QueryPcapFilter implements PcapFilter {
  public static final String QUERY_STR_CONFIG = "mql";

  public static class Configurator implements PcapFilterConfigurator<String> {
    @Override
    public void addToConfig(String query, Configuration conf) {
      conf.set(QUERY_STR_CONFIG, query);
      conf.set(PCAP_FILTER_NAME_CONF, PcapFilters.QUERY.name());
    }

    @Override
    public String queryToString(String fields) {
      return (fields == null ? "" :
              fields.trim().replaceAll("\\s", "_")
                      .replace(".", "-")
                      .replace("'", "")
      );
    }
  }

  private String queryString = null;
  private PredicateProcessor predicateProcessor = new PredicateProcessor();

  @Override
  public void configure(Iterable<Map.Entry<String, String>> config) {
    for (Map.Entry<String, String> entry : config) {
      if (entry.getKey().equals(QUERY_STR_CONFIG)) {
        queryString = entry.getValue();
      }
    }
    predicateProcessor.validate(queryString);
  }

  @Override
  public boolean test(PacketInfo input) {
    EnumMap<Constants.Fields, Object> fields = packetToFields(input);
    VariableResolver resolver = new PcapFieldResolver(fields);
    return predicateProcessor.parse(queryString, resolver);
  }

  protected EnumMap<Constants.Fields, Object> packetToFields(PacketInfo pi) {
    return PcapHelper.packetToFields(pi);
  }
}
