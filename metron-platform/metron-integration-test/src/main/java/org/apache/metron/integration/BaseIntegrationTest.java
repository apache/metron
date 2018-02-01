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
package org.apache.metron.integration;

import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.components.ZKServerComponent;

import java.util.List;
import java.util.Properties;

public abstract class BaseIntegrationTest {

  protected static KafkaComponent getKafkaComponent(final Properties topologyProperties, List<KafkaComponent.Topic> topics) {
    return new KafkaComponent().withTopics(topics).withTopologyProperties(topologyProperties);
  }

    protected static ZKServerComponent getZKServerComponent(final Properties topologyProperties) {
        return new ZKServerComponent()
                .withPostStartCallback((zkComponent) -> {
                  topologyProperties.setProperty(ZKServerComponent.ZOOKEEPER_PROPERTY, zkComponent.getConnectionString());
                  topologyProperties.setProperty("kafka.zk", zkComponent.getConnectionString());
                        }
                );
    }
}
