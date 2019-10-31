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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.integration.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ElasticsearchImportExportTest {


  /**
   *{"_index":".kibana","_type":"visualization","_id":"AV-Sj0e2hKs1cXXnFMqF","_score":1,"_source":{"title":"Welcome to Apache Metron"}}
   *{"_index":".kibana","_type":"blah","_id":"MIKE-AV-Sj0e2hKs1cXXnFMqF","_score":1,"_source":{"title":"another Welcome to Apache Metron"}}
   */
  @Multiline
  private static String records;

  /**
   *{ "create" : { "_id": "AV-Sj0e2hKs1cXXnFMqF", "_type": "visualization" } }
   *{"title":"Welcome to Apache Metron"}
   *{ "create" : { "_id": "MIKE-AV-Sj0e2hKs1cXXnFMqF", "_type": "blah" } }
   *{"title":"another Welcome to Apache Metron"}
   */
  @Multiline
  private static String expected;
  private File tempDir;

  @BeforeEach
  public void setup() throws Exception {
    tempDir = TestUtils.createTempDir(this.getClass().getName());
  }

  @Test
  public void bulk_exporter_writes_elasticsearch_records_in_bulk_import_format() throws Exception {
    Path recordsFile = Paths.get(tempDir.getPath(), "inputfile.json");
    Path outputFile = Paths.get(tempDir.getPath(), "outputfile.json");
    TestUtils.write(recordsFile.toFile(), records);

    ElasticsearchImportExport tool = new ElasticsearchImportExport();
    tool.bulkify(recordsFile, outputFile);
    String actual = TestUtils.read(outputFile.toFile());
    assertThat(actual, equalTo(expected));
  }

}
