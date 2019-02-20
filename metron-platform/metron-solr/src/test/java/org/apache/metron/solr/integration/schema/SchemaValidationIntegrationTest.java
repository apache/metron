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
package org.apache.metron.solr.integration.schema;

import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import org.apache.metron.common.configuration.writer.WriterConfiguration;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.common.writer.BulkMessage;
import org.apache.metron.common.writer.BulkWriterResponse;
import org.apache.metron.solr.integration.components.SolrComponent;
import org.apache.metron.solr.writer.SolrWriter;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.apache.metron.solr.SolrConstants.SOLR_ZOOKEEPER;

public class SchemaValidationIntegrationTest {
  public static Iterable<String> getData(String sensor) throws IOException {
    return Iterables.filter(
            Files.readLines(new File("src/test/resources/example_data/" + sensor), Charset.defaultCharset()),
            s -> !s.startsWith("#") && s.length() > 0
    );
  }

  public static Map<String, Object> getGlobalConfig(String sensorType, SolrComponent component) {
    Map<String, Object> globalConfig = new HashMap<>();
    globalConfig.put(SOLR_ZOOKEEPER, component.getZookeeperUrl());
    return globalConfig;
  }

  public static SolrComponent createSolrComponent(String sensor) throws Exception {
    return new SolrComponent.Builder().build();
  }

  @Test
  public void testError() throws Exception {
    test("error");
  }

  @Test
  public void testBro() throws Exception {
    test("bro");
  }

  @Test
  public void testSnort() throws Exception {
    test("snort");
  }

  @Test
  public void testYaf() throws Exception {
    test("yaf");
  }

  public String getGuid(Map<String, Object> m) {
    if(m.containsKey("guid")) {
      return (String)m.get("guid");
    }
    else {
      return (String) m.get("original_string");
    }
  }

  public void test(String sensorType) throws Exception {
    SolrComponent component = null;
    try {
      component = createSolrComponent(sensorType);
      component.start();
      component.addCollection(String.format("%s", sensorType), String.format("src/main/config/schema/%s", sensorType));
      Map<String, Object> globalConfig = getGlobalConfig(sensorType, component);

      List<BulkMessage<JSONObject>> messages = new ArrayList<>();
      Map<String, Map<String, Object>> index = new HashMap<>();
      int i = 0;
      for (String message : getData(sensorType)) {
        if (message.trim().length() > 0) {
          Map<String, Object> m = JSONUtils.INSTANCE.load(message.trim(), JSONUtils.MAP_SUPPLIER);
          String guid = getGuid(m);
          index.put(guid, m);
          messages.add(new BulkMessage<>(String.format("message%d", ++i), new JSONObject(m)));
        }
      }
      Assert.assertTrue(messages.size() > 0);

      SolrWriter solrWriter = new SolrWriter();

      WriterConfiguration writerConfig = new WriterConfiguration() {
        @Override
        public int getBatchSize(String sensorName) {
          return messages.size();
        }

        @Override
        public int getBatchTimeout(String sensorName) {
          return 0;
        }

        @Override
        public List<Integer> getAllConfiguredTimeouts() {
          return new ArrayList<>();
        }

        @Override
        public String getIndex(String sensorName) {
          return sensorType;
        }

        @Override
        public boolean isEnabled(String sensorName) {
          return true;
        }

        @Override
        public Map<String, Object> getSensorConfig(String sensorName) {
          return new HashMap<String, Object>() {{
            put("index", sensorType);
            put("batchSize", messages.size());
            put("enabled", true);
          }};
        }

        @Override
        public Map<String, Object> getGlobalConfig() {
          return globalConfig;
        }

        @Override
        public boolean isDefault(String sensorName) {
          return false;
        }

        @Override
        public String getFieldNameConverter(String sensorName) {
          return null;
        }
      };

      solrWriter.init(null, null, writerConfig);

      BulkWriterResponse response = solrWriter.write(sensorType, writerConfig, messages);
      Assert.assertTrue(response.getErrors().isEmpty());
      for (Map<String, Object> m : component.getAllIndexedDocs(sensorType)) {
        Map<String, Object> expected = index.get(getGuid(m));
        for (Map.Entry<String, Object> field : expected.entrySet()) {
          if (field.getValue() instanceof Collection && ((Collection) field.getValue()).size() == 0) {
            continue;
          }
          if(m.get(field.getKey()) instanceof Number) {
            Number n1 = ConversionUtils.convert(field.getValue(), Double.class);
            Number n2 = (Number)m.get(field.getKey());
            boolean isSame = Math.abs(n1.doubleValue() - n2.doubleValue()) < 1e-3;
            if(!isSame) {
              String s1 = "" + n1.doubleValue();
              String s2 = "" + n2.doubleValue();
              isSame = s1.startsWith(s2) || s2.startsWith(s1);
            }
            Assert.assertTrue("Unable to validate " + field.getKey() + ": " + n1 + " != " + n2, isSame);
          }
          else {
            Assert.assertEquals("Unable to find " + field.getKey(), "" + field.getValue(), "" + m.get(field.getKey()));
          }
        }
      }
    }
    finally {
      if(component != null) {
        component.stop();
      }
    }
  }

}
