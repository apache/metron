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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.integration.utils.TestUtils;
import org.junit.Before;
import org.junit.Test;

public class ElasticsearchImportExportTest {


  /**
   *{"_index":".kibana","_type":"visualization","_id":"AV-Sj0e2hKs1cXXnFMqF","_score":1,"_source":{"title":"Welcome to Apache Metron","visState":"{\"title\":\"Welcome to Apache Metron\",\"type\":\"markdown\",\"params\":{\"type\":\"markdown\",\"markdown\":\"This dashboard enables the validation of Apache Metron and the end-to-end functioning of its default sensor suite.  The default sensor suite includes [\\n                            Snort](https://www.snort.org/), [\\n                            Bro](https://www.bro.org/), and [\\n                            YAF](https://tools.netsa.cert.org/yaf/).  One of Apache Metron's primary goals is to simplify the on-boarding of additional sources of telemetry.  In a production deployment these default sensors should be replaced with ones applicable to the target environment.\\n\\nApache Metron enables disparate sources of telemetry to all be viewed under a 'single pane of glass.'  Telemetry from each of the default sensors can be searched, aggregated, summarized, and viewed within this dashboard. This dashboard should be used as a springboard upon which to create your own customized dashboards.\\n\\nThe panels below highlight the volume and variety of events that are currently being consumed by Apache Metron.\"},\"aggs\":[],\"listeners\":{}}","uiStateJSON":"{}","description":"","version":1,"kibanaSavedObjectMeta":{"searchSourceJSON":"{\"query\":{\"match_all\":{}},\"filter\":[]}"}}}
   *{"_index":".kibana","_type":"blah","_id":"MIKE-AV-Sj0e2hKs1cXXnFMqF","_score":1,"_source":{"title":"another Welcome to Apache Metron","visState":"{\"title\":\"Welcome to Apache Metron\",\"type\":\"markdown\",\"params\":{\"type\":\"markdown\",\"markdown\":\"This dashboard enables the validation of Apache Metron and the end-to-end functioning of its default sensor suite.  The default sensor suite includes [\\n                            Snort](https://www.snort.org/), [\\n                            Bro](https://www.bro.org/), and [\\n                            YAF](https://tools.netsa.cert.org/yaf/).  One of Apache Metron's primary goals is to simplify the on-boarding of additional sources of telemetry.  In a production deployment these default sensors should be replaced with ones applicable to the target environment.\\n\\nApache Metron enables disparate sources of telemetry to all be viewed under a 'single pane of glass.'  Telemetry from each of the default sensors can be searched, aggregated, summarized, and viewed within this dashboard. This dashboard should be used as a springboard upon which to create your own customized dashboards.\\n\\nThe panels below highlight the volume and variety of events that are currently being consumed by Apache Metron.\"},\"aggs\":[],\"listeners\":{}}","uiStateJSON":"{}","description":"","version":1,"kibanaSavedObjectMeta":{"searchSourceJSON":"{\"query\":{\"match_all\":{}},\"filter\":[]}"}}}
   */
  @Multiline
  private static String records;

  /**
   *{ "create" : { "_id": "AV-Sj0e2hKs1cXXnFMqF", "_type": "visualization" } }
   *{"title":"Welcome to Apache Metron","visState":"{\"title\":\"Welcome to Apache Metron\",\"type\":\"markdown\",\"params\":{\"type\":\"markdown\",\"markdown\":\"This dashboard enables the validation of Apache Metron and the end-to-end functioning of its default sensor suite.  The default sensor suite includes [\\n                            Snort](https://www.snort.org/), [\\n                            Bro](https://www.bro.org/), and [\\n                            YAF](https://tools.netsa.cert.org/yaf/).  One of Apache Metron's primary goals is to simplify the on-boarding of additional sources of telemetry.  In a production deployment these default sensors should be replaced with ones applicable to the target environment.\\n\\nApache Metron enables disparate sources of telemetry to all be viewed under a 'single pane of glass.'  Telemetry from each of the default sensors can be searched, aggregated, summarized, and viewed within this dashboard. This dashboard should be used as a springboard upon which to create your own customized dashboards.\\n\\nThe panels below highlight the volume and variety of events that are currently being consumed by Apache Metron.\"},\"aggs\":[],\"listeners\":{}}","uiStateJSON":"{}","description":"","version":1,"kibanaSavedObjectMeta":{"searchSourceJSON":"{\"query\":{\"match_all\":{}},\"filter\":[]}"}}
   *{ "create" : { "_id": "MIKE-AV-Sj0e2hKs1cXXnFMqF", "_type": "blah" } }
   *{"title":"another Welcome to Apache Metron","visState":"{\"title\":\"Welcome to Apache Metron\",\"type\":\"markdown\",\"params\":{\"type\":\"markdown\",\"markdown\":\"This dashboard enables the validation of Apache Metron and the end-to-end functioning of its default sensor suite.  The default sensor suite includes [\\n                            Snort](https://www.snort.org/), [\\n                            Bro](https://www.bro.org/), and [\\n                            YAF](https://tools.netsa.cert.org/yaf/).  One of Apache Metron's primary goals is to simplify the on-boarding of additional sources of telemetry.  In a production deployment these default sensors should be replaced with ones applicable to the target environment.\\n\\nApache Metron enables disparate sources of telemetry to all be viewed under a 'single pane of glass.'  Telemetry from each of the default sensors can be searched, aggregated, summarized, and viewed within this dashboard. This dashboard should be used as a springboard upon which to create your own customized dashboards.\\n\\nThe panels below highlight the volume and variety of events that are currently being consumed by Apache Metron.\"},\"aggs\":[],\"listeners\":{}}","uiStateJSON":"{}","description":"","version":1,"kibanaSavedObjectMeta":{"searchSourceJSON":"{\"query\":{\"match_all\":{}},\"filter\":[]}"}}
   */
  @Multiline
  private static String expected;
  private File tempDir;

  @Before
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
