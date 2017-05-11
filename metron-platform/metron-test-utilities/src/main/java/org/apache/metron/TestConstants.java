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

public class TestConstants {

  public final static String SAMPLE_CONFIG_PATH = "../metron-integration-test/src/main/config/zookeeper/";
  public final static String SAMPLE_EXTENSIONS_CONFIG_PATH = "../metron-integration-test/src/main/config/zookeeper/extensions";
  public final static String SAMPLE_EXTENSIONS_PARSER_CONFIG_PATH = "../metron-integration-test/src/main/config/zookeeper/extensions/parsers";
  public final static String PARSER_CONFIGS_PATH = "../metron-parsers/src/main/config/zookeeper/";
  public final static String A_PARSER_CONFIGS_PATH_FMT = "../metron-extensions/metron-parser-extensions/metron-parser-%s-extension/metron-parser-%s/src/main/config/zookeeper/";
  public final static String THIS_PARSER_CONFIGS_PATH = "src/main/config/zookeeper/";
  public final static String ENRICHMENTS_CONFIGS_PATH = "../metron-enrichment/src/main/config/zookeeper/";
  public final static String SAMPLE_DATA_PATH = "../metron-integration-test/src/main/sample/data/";
  public final static String SAMPLE_DATA_INPUT_PATH = "../metron-integration-test/src/main/sample/data/yaf/raw/";
  public final static String SAMPLE_DATA_PARSED_PATH = "../metron-integration-test/src/main/sample/data/test/parsed/";
  public final static String SAMPLE_DATA_INDEXED_PATH = "../metron-integration-test/src/main/sample/data/test/indexed/";
}
