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
 * See http://dinaburg.org/bitsquatting.html for more
 */
public class BitsquattingStrategy implements TyposquattingStrategy {
  public static int[] MASK = new int[] { 1, 2, 4, 8, 16, 32, 64, 128};
  @Override
  public Set<String> generateCandidates(String originalString) {
    Set<String> ret = new HashSet<>();
    char[] str = originalString.toCharArray();
    for(int i = 0;i < str.length;++i) {
      char c = str[i];
      for(int j : MASK) {
        int maskedNum = (int)c ^ j;
        char maskedChar = (char)maskedNum;
        if((maskedNum >= 48 && maskedNum <= 57) ||  (maskedNum >= 97 && maskedNum <= 122) || maskedNum == 45) {
          ret.add(pasteTogether(str, i, maskedChar));
        }
      }
    }
    return ret;
  }

  @Override
  public String name() {
    return "Bitsquatting";
  }

  private static String pasteTogether(char[] str, int replacementPoint, char maskedChar) {
    String ret = "";
    for(int i = 0;i < replacementPoint;++i) {
      ret += str[i];
    }
    ret += maskedChar;
    for(int i = replacementPoint+1;i < str.length;++i) {
      ret += str[i];
    }
    return ret;
  }
}
