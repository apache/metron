/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.integration;

public class SnortIntegrationTest extends ParserIntegrationTest {

  @Override
  public String getFluxPath() {
    return "../Metron-Topologies/src/main/resources/Metron_Configs/topologies/snort/test.yaml";
  }

  @Override
  public String getSampleInputPath() {
    return "../Metron-Testing/src/main/resources/sample/data/SampleInput/SnortOutput";
  }

  @Override
  public String getSampleParsedPath() {
    return "../Metron-Testing/src/main/resources/sample/data/SampleParsed/SnortParsed";
  }

  @Override
  public String getSensorType() {
    return "snort";
  }

  @Override
  public String getFluxTopicProperty() {
    return "spout.kafka.topic.snort";
  }
}
