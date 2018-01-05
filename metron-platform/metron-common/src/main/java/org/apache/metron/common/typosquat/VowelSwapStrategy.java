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
 * A typo strategy around swapping vowels (e.g. omazon.com vs amazon.com)
 */
public class VowelSwapStrategy implements TyposquattingStrategy {
  private static Set<Character> VOWELS = new HashSet<Character>() {{
    add('a');
    add('e');
    add('i');
    add('o');
    add('u');
  }};

  @Override
  public Set<String> generateCandidates(String domain) {

    HashSet<String> ret = new HashSet<>();
    for(int i = 0;i < domain.length();++i) {
      char c = domain.charAt(i);
      for(char vowel : VOWELS) {
        if(VOWELS.contains(c)) {
          ret.add( domain.substring(0, i)
                 + vowel
                 + domain.substring(i+1)
                 );
        }
      }
    }
    return ret;
  }

  @Override
  public String name() {
    return "Vowel-swap";
  }
}
