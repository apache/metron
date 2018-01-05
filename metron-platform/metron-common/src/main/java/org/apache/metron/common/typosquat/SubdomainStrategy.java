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
package org.apache.metron.common.typosquat;

import java.util.HashSet;
import java.util.Set;

/**
 * A typo strategy around random subdomains (e.g. am.azon.com vs amazon.com)
 */
public class SubdomainStrategy implements TyposquattingStrategy{

  @Override
  public Set<String> generateCandidates(String domain) {
    Set<String> ret = new HashSet<>();
    for(int i = 1;i < domain.length();++i) {
      Character c = domain.charAt(i);
      Character prevC = domain.charAt(i-1);
      if(c != '-' && c != '.'
      && prevC != '-' && prevC != '.'
        )
      {
        ret.add(domain.substring(0, i) + "." + domain.substring(i, domain.length()));
      }
    }
    return ret;
  }

  @Override
  public String name() {
    return "Subdomain";
  }
}
