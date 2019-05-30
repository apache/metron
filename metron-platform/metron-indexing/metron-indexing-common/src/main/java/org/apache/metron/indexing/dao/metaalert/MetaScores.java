/*
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

package org.apache.metron.indexing.dao.metaalert;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.metron.indexing.dao.update.Document;
import org.apache.metron.stellar.common.utils.ConversionUtils;

public class MetaScores {

  protected Map<String, Object> metaScores = new HashMap<>();

  public MetaScores(List<Double> scores) {
    // A meta alert could be entirely alerts with no values.
    DoubleSummaryStatistics stats = scores
        .stream()
        .mapToDouble(a -> a)
        .summaryStatistics();
    metaScores.put("max", stats.getMax());
    metaScores.put("min", stats.getMin());
    metaScores.put("average", stats.getAverage());
    metaScores.put("count", stats.getCount());
    metaScores.put("sum", stats.getSum());

    // median isn't in the stats summary
    double[] arr = scores
        .stream()
        .mapToDouble(d -> d)
        .toArray();
    metaScores.put("median", new Median().evaluate(arr));
  }

  public Map<String, Object> getMetaScores() {
    return metaScores;
  }

  /**
   * Calculate the meta alert scores for a Document. The scores are placed directly in the provided
   * document.
   * @param metaAlert The Document containing scores
   */
  @SuppressWarnings("unchecked")
  public static void calculateMetaScores(Document metaAlert, String threatTriageField,
      String threatSort) {
    MetaScores metaScores = new MetaScores(new ArrayList<>());
    List<Object> alertsRaw = ((List<Object>) metaAlert.getDocument()
        .get(MetaAlertConstants.ALERT_FIELD));
    if (alertsRaw != null && !alertsRaw.isEmpty()) {
      ArrayList<Double> scores = new ArrayList<>();
      for (Object alertRaw : alertsRaw) {
        Map<String, Object> alert = (Map<String, Object>) alertRaw;
        Double scoreNum = parseThreatField(alert.get(threatTriageField));
        if (scoreNum != null) {
          scores.add(scoreNum);
        }
      }
      metaScores = new MetaScores(scores);
    }

    // add a summary (max, min, avg, ...) of all the threat scores from the child alerts
    metaAlert.getDocument().putAll(metaScores.getMetaScores());

    // add the overall threat score for the metaalert; one of the summary aggregations as defined
    // by `threatSort`
    Object threatScore = metaScores.getMetaScores().get(threatSort);

    // add the threat score as a float; type needs to match the threat score field from each of
    // the sensor indices
    metaAlert.getDocument()
        .put(threatTriageField, ConversionUtils.convert(threatScore, Float.class));
  }

  protected static Double parseThreatField(Object threatRaw) {
    Double threat = null;
    if (threatRaw instanceof Number) {
      threat = ((Number) threatRaw).doubleValue();
    } else if (threatRaw instanceof String) {
      threat = Double.parseDouble((String) threatRaw);
    }
    return threat;
  }
}
