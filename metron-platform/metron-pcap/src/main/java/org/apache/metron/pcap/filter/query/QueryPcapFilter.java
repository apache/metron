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
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.MapVariableResolver;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.apache.metron.stellar.common.StellarPredicateProcessor;
import org.apache.metron.stellar.dsl.VariableResolver;
import org.apache.metron.pcap.PacketInfo;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.filter.PcapFilter;
import org.apache.metron.pcap.filter.PcapFilterConfigurator;
import org.apache.metron.pcap.filter.PcapFilters;

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
  private StellarPredicateProcessor predicateProcessor = new StellarPredicateProcessor();

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
    Map<String, Object> fields = packetToFields(input);
    VariableResolver resolver = new MapVariableResolver(fields);
    return predicateProcessor.parse(queryString, resolver, StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
  }

  protected Map<String, Object> packetToFields(PacketInfo pi) {
    return PcapHelper.packetToFields(pi);
  }
}
