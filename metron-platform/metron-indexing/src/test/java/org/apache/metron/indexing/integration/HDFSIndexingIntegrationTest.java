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

package org.apache.metron.indexing.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.apache.metron.common.interfaces.FieldNameConverter;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.integration.*;
import org.apache.metron.integration.components.KafkaComponent;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.storm.hdfs.bolt.rotation.TimedRotationPolicy;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class HDFSIndexingIntegrationTest extends IndexingIntegrationTest {
  protected String hdfsDir = "target/indexingIntegrationTest/hdfs";

  public static void cleanHdfsDir(String hdfsDirStr) {
    File hdfsDir = new File(hdfsDirStr);
    Stack<File> fs = new Stack<>();
    if (hdfsDir.exists()) {
      fs.push(hdfsDir);
      while (!fs.empty()) {
        File f = fs.pop();
        if (f.isDirectory()) {
          for (File child : f.listFiles()) {
            fs.push(child);
          }
        } else {
          if (f.getName().startsWith("enrichment") || f.getName().endsWith(".json")) {
            f.delete();
          }
        }
      }
    }
  }

  public static List<Map<String, Object>> readDocsFromDisk(String hdfsDirStr) throws IOException {
    List<Map<String, Object>> ret = new ArrayList<>();
    File hdfsDir = new File(hdfsDirStr);
    Stack<File> fs = new Stack<>();
    if (hdfsDir.exists()) {
      fs.push(hdfsDir);
      while (!fs.empty()) {
        File f = fs.pop();
        if (f.isDirectory()) {
          for (File child : f.listFiles()) {
            fs.push(child);
          }
        } else {
          System.out.println("Processed " + f);
          if (f.getName().startsWith("enrichment") || f.getName().endsWith(".json")) {
            List<byte[]> data = TestUtils.readSampleData(f.getPath());
            Iterables.addAll(ret, Iterables.transform(data, bytes -> {
                String s = new String(bytes);
                try {
                  return JSONUtils.INSTANCE.load(s, new TypeReference<Map<String, Object>>() {
                  });
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }));
          }
        }
      }
    }
    return ret;
  }

  @Override
  protected void preTest() {
    cleanHdfsDir(hdfsDir);
  }

  @Override
  public Processor<List<Map<String, Object>>> getProcessor(List<byte[]> inputMessages) {
    return new Processor<List<Map<String, Object>>>() {
      List<Map<String, Object>> docs = null;
      List<byte[]> errors = null;
      @Override
      public ReadinessState process(ComponentRunner runner) {
        KafkaComponent kafkaComponent = runner.getComponent("kafka", KafkaComponent.class);
        try {
          docs = readDocsFromDisk(hdfsDir);
        } catch (IOException e) {
          throw new IllegalStateException("Unable to retrieve indexed documents.", e);
        }
        if (docs.size() < inputMessages.size()) {
          errors = kafkaComponent.readMessages(ERROR_TOPIC);
          if(errors.size() > 0 && errors.size() + docs.size() == inputMessages.size()){
              return ReadinessState.READY;
          }
          return ReadinessState.NOT_READY;
        } else {
            return ReadinessState.READY;
        }
      }

      @Override
      public ProcessorResult<List<Map<String, Object>>> getResult()  {
        ProcessorResult.Builder<List<Map<String,Object>>> builder = new ProcessorResult.Builder();
        return builder.withResult(docs).withProcessErrors(errors).build();
      }
    };
  }

  @Override
  public FieldNameConverter getFieldNameConverter() {
    return originalField -> originalField;
  }

  @Override
  public InMemoryComponent getSearchComponent(Properties topologyProperties) throws Exception {
    return null;
  }

  @Override
  public void setAdditionalProperties(Properties topologyProperties) {
    topologyProperties.setProperty("batch_indexing_kafka_start", "UNCOMMITTED_EARLIEST");
    topologyProperties.setProperty("batch_indexing_workers", "1");
    topologyProperties.setProperty("batch_indexing_acker_executors", "0");
    topologyProperties.setProperty("batch_indexing_topology_max_spout_pending", "");
    topologyProperties.setProperty("batch_indexing_kafka_spout_parallelism", "1");
    topologyProperties.setProperty("bolt_hdfs_rotation_policy", TimedRotationPolicy.class.getCanonicalName());
    topologyProperties.setProperty("bolt_hdfs_rotation_policy_count", "1");
    topologyProperties.setProperty("bolt_hdfs_rotation_policy_units", "DAYS");
    topologyProperties.setProperty("metron_apps_indexed_hdfs_dir", hdfsDir);
    topologyProperties.setProperty("hdfs_writer_parallelism", "1");
  }

  @Override
  public String cleanField(String field) {
    return field;
  }

  @Override
  public String getTemplatePath() {
    return "../metron-indexing/src/main/config/hdfs.properties.j2";
  }

  @Override
  public String getFluxPath() {
    return "../metron-indexing/src/main/flux/indexing/batch/remote.yaml";
  }
}
