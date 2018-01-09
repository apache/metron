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

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.IDN;
import java.util.*;

/**
 *  Substituting characters for ascii or unicode analogues which are visually similar (e.g. latlmes.com for latimes.com)
 *
 */
public class HomoglyphStrategy implements TyposquattingStrategy{

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final Map<Character, List<String>> glyphs = new HashMap<Character, List<String>>()
  {{
    put('a', ImmutableList.of("à", "á", "â", "ã", "ä", "å", "ɑ", "а", "ạ", "ǎ", "ă", "ȧ", "ӓ"));
    put('b', ImmutableList.of("d", "lb", "ib", "ʙ", "Ь", "b̔", "ɓ", "Б"));
    put('c', ImmutableList.of("ϲ", "с", "ƈ", "ċ", "ć", "ç"));
    put('d', ImmutableList.of("b", "cl", "dl", "di", "ԁ", "ժ", "ɗ", "đ"));
    put('e', ImmutableList.of("é", "ê", "ë", "ē", "ĕ", "ě", "ė", "е", "ẹ", "ę", "є", "ϵ", "ҽ"));
    put('f', ImmutableList.of("Ϝ", "ƒ", "Ғ"));
    put('g', ImmutableList.of("q", "ɢ", "ɡ", "Ԍ", "Ԍ", "ġ", "ğ", "ց", "ǵ", "ģ"));
    put('h', ImmutableList.of("lh", "ih", "һ", "հ", "Ꮒ", "н"));
    put('i', ImmutableList.of("1", "l", "Ꭵ", "í", "ï", "ı", "ɩ", "ι", "ꙇ", "ǐ", "ĭ"));
    put('j', ImmutableList.of("ј", "ʝ", "ϳ", "ɉ"));
    put('k', ImmutableList.of("lk", "ik", "lc", "κ", "ⲕ", "κ"));
    put('l', ImmutableList.of("1", "i", "ɫ", "ł"));
    put('m', ImmutableList.of("n", "nn", "rn", "rr", "ṃ", "ᴍ", "м", "ɱ"));
    put('n', ImmutableList.of("m", "r", "ń"));
    put('o', ImmutableList.of("0", "Ο", "ο", "О", "о", "Օ", "ȯ", "ọ", "ỏ", "ơ", "ó", "ö", "ӧ"));
    put('p', ImmutableList.of("ρ", "р", "ƿ", "Ϸ", "Þ"));
    put('q', ImmutableList.of("g", "զ", "ԛ", "գ", "ʠ"));
    put('r', ImmutableList.of("ʀ", "Г", "ᴦ", "ɼ", "ɽ"));
    put('s', ImmutableList.of("Ⴝ", "Ꮪ", "ʂ", "ś", "ѕ"));
    put('t', ImmutableList.of("τ", "т", "ţ"));
    put('u', ImmutableList.of("μ", "υ", "Ս", "ս", "ц", "ᴜ", "ǔ", "ŭ"));
    put('v', ImmutableList.of("ѵ", "ν", "v̇"));
    put('w', ImmutableList.of("vv", "ѡ", "ա", "ԝ"));
    put('x', ImmutableList.of("х", "ҳ", "ẋ"));
    put('y', ImmutableList.of("ʏ", "γ", "у", "Ү", "ý"));
    put('z', ImmutableList.of("ʐ", "ż", "ź", "ʐ", "ᴢ"));
  }};

  @Override
  public Set<String> generateCandidates(String originalString) {
    Set<String> result = new HashSet<>();
    String domain = originalString;
    if(StringUtils.isEmpty(domain)) {
      return result;
    }
    if(isAce(domain)) {
      //this is an ace domain.
      domain = IDN.toUnicode(domain);
    }
    for(int ws = 0;ws < domain.length();ws++) {
      for(int i = 0;i < domain.length() - ws + 1;++i) {
        String win = domain.substring(i, i+ws);
        for(int j = 0;j < ws;j++) {
          char c = win.charAt(j);
          if( glyphs.containsKey(c)) {
            for( String g : glyphs.get(c)) {
              String winNew = win.replaceAll("" + c, g);
              String d = domain.substring(0, i) + winNew + domain.substring(i + ws);
              result.add(d);
              if(!isAce(d)) {
                try {
                  String dAscii = IDN.toASCII(d, IDN.ALLOW_UNASSIGNED);
                  if (!d.equals(dAscii)) {
                    result.add(dAscii);
                  }
                }
                catch(IllegalArgumentException iae) {
                  LOG.debug("Unable to parse " + d + ": " + iae.getMessage(), iae);
                }
              }
            }
          }
        }
      }
    }
    return result;
  }

  public static boolean isAce(String domainRaw) {
    String domain = domainRaw.toLowerCase();
    return domain.startsWith("xn--") || domain.contains(".xn--");
  }

  @Override
  public String name() {
    return "Homoglyph";
  }
}
