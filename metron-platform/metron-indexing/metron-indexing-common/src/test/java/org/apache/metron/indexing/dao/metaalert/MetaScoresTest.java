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

import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.ALERT_FIELD;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.METAALERT_TYPE;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.THREAT_FIELD_DEFAULT;
import static org.apache.metron.indexing.dao.metaalert.MetaAlertConstants.THREAT_SORT_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.metron.common.Constants;
import org.apache.metron.indexing.dao.AccessConfig;
import org.apache.metron.indexing.dao.update.Document;
import org.junit.jupiter.api.Test;

public class MetaScoresTest {
  @Test
  public void testCalculateMetaScoresList() {
    final double delta = 0.001;
    List<Map<String, Object>> alertList = new ArrayList<>();

    // add an alert with a threat score
    alertList.add(Collections.singletonMap(THREAT_FIELD_DEFAULT, 10.0f));

    // add a second alert with a threat score
    alertList.add(Collections.singletonMap(THREAT_FIELD_DEFAULT, 20.0f));

    // add a third alert with NO threat score
    alertList.add(Collections.singletonMap("alert3", "has no threat score"));

    // create the metaalert
    Map<String, Object> docMap = new HashMap<>();
    docMap.put(ALERT_FIELD, alertList);
    Document metaalert = new Document(docMap, "guid", METAALERT_TYPE, 0L);

    // calculate the threat score for the metaalert
    MetaScores.calculateMetaScores(metaalert, THREAT_FIELD_DEFAULT, THREAT_SORT_DEFAULT);

    // the metaalert must contain a summary of all child threat scores
    assertEquals(20D, (Double) metaalert.getDocument().get("max"), delta);
    assertEquals(10D, (Double) metaalert.getDocument().get("min"), delta);
    assertEquals(15D, (Double) metaalert.getDocument().get("average"), delta);
    assertEquals(2L, metaalert.getDocument().get("count"));
    assertEquals(30D, (Double) metaalert.getDocument().get("sum"), delta);
    assertEquals(15D, (Double) metaalert.getDocument().get("median"), delta);

    // it must contain an overall threat score; a float to match the type of the threat score of
    // the other sensor indices
    Object threatScore = metaalert.getDocument().get(THREAT_FIELD_DEFAULT);
    assertTrue(threatScore instanceof Float);

    // by default, the overall threat score is the sum of all child threat scores
    assertEquals(30.0F, threatScore);
  }

  @Test
  public void testCalculateMetaScoresWithDifferentFieldName() {
    List<Map<String, Object>> alertList = new ArrayList<>();

    // add an alert with a threat score
    alertList.add( Collections.singletonMap(MetaAlertConstants.THREAT_FIELD_DEFAULT, 10.0f));

    // create the metaalert
    Map<String, Object> docMap = new HashMap<>();
    docMap.put(MetaAlertConstants.ALERT_FIELD, alertList);
    Document metaalert = new Document(docMap, "guid", MetaAlertConstants.METAALERT_TYPE, 0L);

    // Configure a different threat triage score field name
    AccessConfig accessConfig = new AccessConfig();
    accessConfig.setGlobalConfigSupplier(() -> new HashMap<String, Object>() {{
      put(Constants.THREAT_SCORE_FIELD_PROPERTY, MetaAlertConstants.THREAT_FIELD_DEFAULT);
    }});

    MetaScores.calculateMetaScores(metaalert, MetaAlertConstants.THREAT_FIELD_DEFAULT, MetaAlertConstants.THREAT_SORT_DEFAULT);
    assertNotNull(metaalert.getDocument().get(MetaAlertConstants.THREAT_FIELD_DEFAULT));
  }
}
