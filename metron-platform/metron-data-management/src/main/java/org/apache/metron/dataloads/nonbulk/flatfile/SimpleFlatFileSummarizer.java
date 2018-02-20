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
package org.apache.metron.dataloads.nonbulk.flatfile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.log4j.PropertyConfigurator;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.nonbulk.flatfile.importer.Summarizers;

import java.io.File;
import java.util.EnumMap;
import java.util.Optional;

public class SimpleFlatFileSummarizer {
    public static void main(String... argv) throws Exception {
    Configuration hadoopConfig = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(hadoopConfig, argv).getRemainingArgs();
    main(hadoopConfig, otherArgs);
  }

  public static void main(Configuration hadoopConfig, String[] argv) throws Exception {
    CommandLine cli = SummarizeOptions.parse(new PosixParser(), argv);
    EnumMap<SummarizeOptions, Optional<Object>> config = SummarizeOptions.createConfig(cli);
    if(SummarizeOptions.LOG4J_PROPERTIES.has(cli)) {
      PropertyConfigurator.configure(SummarizeOptions.LOG4J_PROPERTIES.get(cli));
    }
    ExtractorHandler handler = ExtractorHandler.load(
            FileUtils.readFileToString(new File(SummarizeOptions.EXTRACTOR_CONFIG.get(cli).trim()))
    );
    Summarizers strategy = (Summarizers) config.get(SummarizeOptions.IMPORT_MODE).get();
    strategy.getSummarizer().importData(config, handler, hadoopConfig);
  }
}
