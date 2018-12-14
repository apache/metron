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

public class MetaAlertConstants {
  public static String METAALERT_TYPE = "metaalert";

  /**
   * The name of the field in an alert that contains a list
   * of GUIDs of all meta-alerts the alert is associated with.
   *
   * <p>Only standard, non-metaalerts will have this field.
   */
  public static String METAALERT_FIELD = "metaalerts";
  public static String METAALERT_DOC = METAALERT_TYPE + "_doc";
  public static String THREAT_FIELD_DEFAULT = "threat:triage:score";
  public static String THREAT_SORT_DEFAULT = "sum";

  /**
   * The name of the field in a meta-alert that contains a list of
   * all alerts associated with the meta-alert.
   *
   * <p>Only meta-alerts will have this field.
   */
  public static String ALERT_FIELD = "metron_alert";
  public static String STATUS_FIELD = "status";
  public static String GROUPS_FIELD = "groups";
}
