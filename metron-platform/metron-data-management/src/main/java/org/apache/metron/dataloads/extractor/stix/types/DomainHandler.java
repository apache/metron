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
package org.apache.metron.dataloads.extractor.stix.types;

import org.apache.metron.dataloads.extractor.stix.StixExtractor;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.mitre.cybox.common_2.StringObjectPropertyType;
import org.mitre.cybox.objects.DomainName;
import org.mitre.cybox.objects.DomainNameTypeEnum;

import java.io.IOException;
import java.util.*;

public class DomainHandler extends AbstractObjectTypeHandler<DomainName> {
  public static final String TYPE_CONFIG = "stix_domain_type";
  EnumSet<DomainNameTypeEnum> SUPPORTED_TYPES = EnumSet.of(DomainNameTypeEnum.FQDN);
  public DomainHandler() {
    super(DomainName.class);
  }

  @Override
  public Iterable<LookupKV> extract(final DomainName type, Map<String, Object> config) throws IOException {
    List<LookupKV> ret = new ArrayList<>();
    String typeStr = getType();
    if(config != null) {
      Object o = config.get(TYPE_CONFIG);
      if(o != null) {
        typeStr = o.toString();
      }
    }
    final DomainNameTypeEnum domainType = type.getType();
    if(domainType == null || SUPPORTED_TYPES.contains(domainType)) {
      StringObjectPropertyType value = type.getValue();
      for (String token : StixExtractor.split(value)) {
        final String indicatorType = typeStr + ":" + DomainNameTypeEnum.FQDN;
        LookupKV results = new LookupKV(new EnrichmentKey(indicatorType, token)
                , new EnrichmentValue(
                new HashMap<String, Object>() {{
                  put("source-type", "STIX");
                  put("indicator-type", indicatorType);
                  put("source", type.toXMLString());
                }}
        )
        );
        ret.add(results);
      }
    }
    return ret;
  }
  @Override
  public List<String> getPossibleTypes() {
    String typeStr = getType();
    List<String> ret = new ArrayList<>();
    for(DomainNameTypeEnum e : SUPPORTED_TYPES)
    {
       ret.add(typeStr + ":" + e);
    }
    return ret;
  }
}
