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
package org.apache.metron;

public class Constants {

  public static final String GLOBAL_CONFIG_NAME = "global";
  public static final String SENSORS_CONFIG_NAME = "sensors";
  public static final String ZOOKEEPER_ROOT = "/metron";
  public static final String ZOOKEEPER_TOPOLOGY_ROOT = ZOOKEEPER_ROOT + "/topology";
  public static final String ZOOKEEPER_GLOBAL_ROOT = ZOOKEEPER_TOPOLOGY_ROOT + "/" + GLOBAL_CONFIG_NAME;
  public static final String ZOOKEEPER_SENSOR_ROOT = ZOOKEEPER_TOPOLOGY_ROOT + "/" + SENSORS_CONFIG_NAME;
  public static final long DEFAULT_CONFIGURED_BOLT_TIMEOUT = 5000;
  public static final String SENSOR_TYPE = "source.type";
  public static final String ENRICHMENT_TOPIC = "enrichments";
  public static final String ERROR_STREAM = "error";

}

