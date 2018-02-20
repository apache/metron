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

package org.apache.metron.elasticsearch.bulk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.metron.common.utils.JSONUtils;

/**
 * This is a utility for taking a file of JSON objects that were exported from ES and transforming
 * it into a bulk import format. This was useful for backing up and restoring the Kibana dashboard
 * index. The notable gap is that it expects one record per line in the file, which is not how
 * ES generally returns results. Elasticsearch-dump was used as the intermediary to export data in
 * the desired format for consumption by this tool.
 * @see <a href="https://github.com/taskrabbit/elasticsearch-dump">https://github.com/taskrabbit/elasticsearch-dump</a>
 */
public class ElasticsearchImportExport {

  public static void main(String[] args) {
    if (args.length != 2) {
      throw new RuntimeException("Expects 'input' and 'output' file arguments.");
    }
    final String inPath = args[0];
    final String outPath = args[1];
    try {
      new ElasticsearchImportExport().bulkify(Paths.get(inPath), Paths.get(outPath));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }

  /**
   * Takes a file of line-delimited JSON objects and converts them to an Elasticsearch bulk import
   * format.
   *
   * @param input input JSON file (note, each line is expected to be a separate complete JSON
   * object, not the file as a whole.)
   * @param output Elasticsearch bulk import file.
   * @throws IOException
   */
  public void bulkify(Path input, Path output) throws IOException {
    List<String> outRecs = new ArrayList<String>();
    try (BufferedReader br = new BufferedReader(new FileReader(input.toFile()))) {
      String line;
      while ((line = br.readLine()) != null) {
        Map<String, Object> inDoc = JSONUtils.INSTANCE
            .load(line, JSONUtils.MAP_SUPPLIER);
        Object id = inDoc.get("_id");
        Object type = inDoc.get("_type");
        String createRaw = String
            .format("{ \"create\" : { \"_id\": \"%s\", \"_type\": \"%s\" } }", id, type);
        String outData = JSONUtils.INSTANCE.toJSON(inDoc.get("_source"), false);
        outRecs.add(createRaw);
        outRecs.add(outData);
      }
    }
    try (BufferedWriter br = new BufferedWriter(new FileWriter(output.toFile()))) {
      for (String line : outRecs) {
        br.write(line);
        br.write(System.lineSeparator());
      }
    }
  }

}
