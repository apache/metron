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
package org.apache.metron.pcapservice;

/**
 * HBase configuration properties.
 * 
 * @author Sayi
 */
public class HBaseConfigConstants {

  /** The Constant HBASE_ZOOKEEPER_QUORUM. */
  public static final String HBASE_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";

  /** The Constant HBASE_ZOOKEEPER_CLIENT_PORT. */
  public static final String HBASE_ZOOKEEPER_CLIENT_PORT = "hbase.zookeeper.clientPort";

  /** The Constant HBASE_ZOOKEEPER_SESSION_TIMEOUT. */
  public static final String HBASE_ZOOKEEPER_SESSION_TIMEOUT = "zookeeper.session.timeout";

  /** The Constant HBASE_ZOOKEEPER_RECOVERY_RETRY. */
  public static final String HBASE_ZOOKEEPER_RECOVERY_RETRY = "zookeeper.recovery.retry";

  /** The Constant HBASE_CLIENT_RETRIES_NUMBER. */
  public static final String HBASE_CLIENT_RETRIES_NUMBER = "hbase.client.retries.number";

  /** The delimeter. */
  String delimeter = "-";

  /** The regex. */
  String regex = "\\-";

  /** The Constant PCAP_KEY_DELIMETER. */
  public static final String PCAP_KEY_DELIMETER = "-";

  /** The Constant START_KEY. */
  public static final String START_KEY = "startKey";

  /** The Constant END_KEY. */
  public static final String END_KEY = "endKey";

}
