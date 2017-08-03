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

import java.util.List;
import java.util.Locale;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.similarity.FuzzyScore;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;

public class TextFunctions {

  @Stellar(name = "FUZZY_SCORE",
      description =
          "Returns the Fuzzy Score which indicates the similarity score between two Strings "
              +
              "One point is given for every matched character. Subsequent matches yield two bonus "
              +
              "points. A higher score indicates a higher similarity",
      params = {
          "string - The full term that should be matched against",
          "string - The query that will be matched against a term",
          "string - The IETF BCP 47 language code to use"
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
      
      if(!(oterm instanceof String) || !(oquery instanceof String) || !(olang instanceof String)){
        return 0;
      }
      String term = (String) oterm;
      String query = (String) oquery;
      String lang = (String) olang;
      if (StringUtils.isEmpty(term) || StringUtils.isEmpty(query) || StringUtils.isEmpty(lang)) {
        return 0;
      }

      Locale locale = Locale.forLanguageTag(lang);
      FuzzyScore score = new FuzzyScore(locale);
      return score.fuzzyScore(term, query);
    }
  }
}
