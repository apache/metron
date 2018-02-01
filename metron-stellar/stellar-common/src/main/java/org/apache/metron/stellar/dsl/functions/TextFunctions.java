/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership.  The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.  You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.metron.stellar.dsl.functions;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;

public class TextFunctions {

  private static final List<String> tagsList;

  static {
    List<String> tags = new ArrayList<>();
    for (Locale locale : Locale.getAvailableLocales()) {
      tags.add(locale.toLanguageTag());
    }
    tagsList = ImmutableList.copyOf(tags);
  }

  @Stellar(name = "LANGS",
      namespace = "FUZZY",
      description = "Returns a list of IETF BCP 47 available to the system, such as en, fr, de. "
          + "These values may be passed to FUZZY_SCORE",
      params = {},
      returns = "A list of IEF BCP 47 language tag strings")
  /**
   * GetAvailableLanaguageTags exposes IEF BCP 47 lanaguage tags available to the system
   */
  public static class GetAvailableLanaguageTags extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      return tagsList;
    }
  }

  @Stellar(name = "SCORE",
      namespace = "FUZZY",
      description =
          "Returns the Fuzzy Score which indicates the similarity score between two Strings "
              +
              "One point is given for every matched character. Subsequent matches yield two bonus "
              +
              "points. A higher score indicates a higher similarity",
      params = {
          "string - The full term that should be matched against",
          "string - The query that will be matched against a term",
          "string - The IETF BCP 47 language code to use such as en, fr, de "
              +
              "( SEE  FUZZY_LANGS  and https://tools.ietf.org/html/bcp47)"
      },
      returns = "integer representing the score")
  /**
   * FuzzyScoreFunction exposes the Apache Commons Text Similarity FuzzyScore through
   * Stellar.
   */
  public static class FuzzyScoreFunction extends BaseStellarFunction {

    @Override
    public Object apply(List<Object> list) {
      if (list.size() < 3) {
        throw new IllegalStateException("FUZZY_SCORE expects three args: [string, string, string]");
      }
      Object oterm = list.get(0);
      Object oquery = list.get(1);
      Object olang = list.get(2);

      // return 0 here, validate will pass 3 nulls
      // if we change validate to pass default of expected type, we can differentiate
      if (!(oterm instanceof String) || !(oquery instanceof String) || !(olang instanceof String)) {
        return 0;
      }

      String term = (String) oterm;
      String query = (String) oquery;
      String lang = (String) olang;

      if (!tagsList.contains(lang)) {
        throw new ParseException(
            "FUZZY_SCORE requires a valid IETF BCP47 language code see FUZZY_LANGS and https://tools.ietf.org/html/bcp47");
      }
      
      if (StringUtils.isEmpty(term) || StringUtils.isEmpty(query)) {
        return 0;
      }

      Locale locale = Locale.forLanguageTag(lang);
      FuzzyScore score = new FuzzyScore(locale);
      return score.fuzzyScore(term, query);
    }
  }
}
