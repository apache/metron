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
package org.apache.metron.integration.util.integration.util;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Writer;
import org.apache.metron.parsing.parsers.PcapParser;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

public class PcapTestUtil {

  public static final String OUTPUT_PATH = "./metron-streaming/Metron-Topologies/src/main/resources/SampleInput/PCAPExampleOutputTest";

  public static void main(String[] args) throws IOException {
    String topic = "pcap";
    SimpleConsumer consumer = new SimpleConsumer("node1", 6667, 100000, 64 * 1024, "consumer");
    FetchRequest req = new FetchRequestBuilder()
            .clientId("consumer")
            .addFetch(topic, 0, 0, 100000)
            .build();
    FetchResponse fetchResponse = consumer.fetch(req);
    Iterator<MessageAndOffset> results = fetchResponse.messageSet(topic, 0).iterator();
    Writer writer = SequenceFile.createWriter(new Configuration(),
            Writer.file(new Path(OUTPUT_PATH)),
            Writer.compression(SequenceFile.CompressionType.NONE),
            Writer.keyClass(IntWritable.class),
            Writer.valueClass(BytesWritable.class));
    int index = 0;
    int size = 20;
    PcapParser pcapParser = new PcapParser();
    pcapParser.init();
    while(results.hasNext()) {
      if (index == size) break;
      ByteBuffer payload = results.next().message().payload();
      byte[] bytes = new byte[payload.limit()];
      payload.get(bytes);
      List<JSONObject> parsed = pcapParser.parse(bytes);
      if (parsed != null && parsed.size() > 0) {
        JSONObject message = parsed.get(0);
        if (pcapParser.validate(message)) {
          writer.append(new IntWritable(index++), new BytesWritable(bytes));
        }
      }
    }
    writer.close();
  }
}
