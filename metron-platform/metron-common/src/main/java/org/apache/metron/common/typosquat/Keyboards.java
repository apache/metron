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

import java.util.HashMap;
import java.util.Map;

/**
 * This provides a mapping of nearby keys for a variety of keyboard layouts.  This is useful in determining likely
 * typos.
 */
public enum Keyboards {
  QWERTY(new HashMap<Character, String>()
  {{
    put('j', "ikmnhu");
    put('w', "3esaq2");
    put('v', "cfgb");
    put('l', "kop");
    put('y', "7uhgt6");
    put('x', "zsdc");
    put('r', "5tfde4");
    put('u', "8ijhy7");
    put('a', "qwsz");
    put('q', "12wa");
    put('c', "xdfv");
    put('b', "vghn");
    put('e', "4rdsw3");
    put('d', "rfcxse");
    put('g', "yhbvft");
    put('p', "lo0");
    put('i', "9okju8");
    put('h', "ujnbgy");
    put('k', "olmji");
    put('f', "tgvcdr");
    put('m', "njk");
    put('s', "edxzaw");
    put('o', "0plki9");
    put('n', "bhjm");
    put('1', "2q");
    put('0', "po9");
    put('3', "4ew2");
    put('2', "3wq1");
    put('5', "6tr4");
    put('4', "5re3");
    put('7', "8uy6");
    put('6', "7yt5");
    put('9', "0oi8");
    put('8', "9iu7");
    put('z', "asx");
    put('t', "6ygfr5");
  }}),
  QWERTZ(new HashMap<Character, String>() {{
    put('j', "ikmnhu");
    put('w', "3esaq2");
    put('v', "cfgb");
    put('l', "kop");
    put('y', "asx");
    put('x', "ysdc");
    put('r', "5tfde4");
    put('u', "8ijhz7");
    put('a', "qwsy");
    put('q', "12wa");
    put('c', "xdfv");
    put('b', "vghn");
    put('e', "4rdsw3");
    put('d', "rfcxse");
    put('g', "zhbvft");
    put('p', "lo0");
    put('i', "9okju8");
    put('h', "ujnbgz");
    put('k', "olmji");
    put('f', "tgvcdr");
    put('m', "njk");
    put('s', "edxyaw");
    put('o', "0plki9");
    put('n', "bhjm");
    put('1', "2q");
    put('0', "po9");
    put('3', "4ew2");
    put('2', "3wq1");
    put('5', "6tr4");
    put('4', "5re3");
    put('7', "8uz6");
    put('6', "7zt5");
    put('9', "0oi8");
    put('8', "9iu7");
    put('z', "7uhgt6");
    put('t', "6zgfr5");
  }}),
  AZERTY(new HashMap<Character, String>() {{
    put('j', "iknhu");
    put('w', "sxq");
    put('v', "cfgb");
    put('l', "kopm");
    put('y', "7uhgt6");
    put('x', "zsdc");
    put('r', "5tfde4");
    put('u', "8ijhy7");
    put('a', "2zq1");
    put('q', "zswa");
    put('c', "xdfv");
    put('b', "vghn");
    put('e', "4rdsz3");
    put('d', "rfcxse");
    put('g', "yhbvft");
    put('p', "lo0m");
    put('i', "9okju8");
    put('h', "ujnbgy");
    put('k', "olji");
    put('f', "tgvcdr");
    put('m', "lp");
    put('s', "edxwqz");
    put('o', "0plki9");
    put('n', "bhj");
    put('1', "2a");
    put('0', "po9");
    put('3', "4ez2");
    put('2', "3za1");
    put('5', "6tr4");
    put('4', "5re3");
    put('7', "8uy6");
    put('6', "7yt5");
    put('9', "0oi8");
    put('8', "9iu7");
    put('z', "3esqa2");
    put('t', "6ygfr5");
  }})
  ;
  private Map<Character, String> mapping;
  Keyboards(Map<Character, String> mapping) {
   this.mapping = mapping;
  }
  public Map<Character, String> getMapping() {
    return mapping;
  }
}
