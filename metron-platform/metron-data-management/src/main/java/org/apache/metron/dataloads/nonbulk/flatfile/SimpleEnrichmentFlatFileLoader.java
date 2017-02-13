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
import org.apache.metron.common.configuration.enrichment.SensorEnrichmentUpdateConfig;
import org.apache.metron.common.utils.JSONUtils;
import org.apache.metron.dataloads.extractor.ExtractorHandler;
import org.apache.metron.dataloads.nonbulk.flatfile.importer.ImportStrategy;

import java.io.File;
import java.util.EnumMap;
import java.util.Optional;

public class SimpleEnrichmentFlatFileLoader {


  public static void main(String... argv) throws Exception {
    Configuration hadoopConfig = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(hadoopConfig, argv).getRemainingArgs();
    main(hadoopConfig, otherArgs);
  }

  public static void main(Configuration hadoopConfig, String[] argv) throws Exception {

    CommandLine cli = LoadOptions.parse(new PosixParser(), argv);
    EnumMap<LoadOptions, Optional<Object>> config = LoadOptions.createConfig(cli);
    if(LoadOptions.LOG4J_PROPERTIES.has(cli)) {
      PropertyConfigurator.configure(LoadOptions.LOG4J_PROPERTIES.get(cli));
    }
    ExtractorHandler handler = ExtractorHandler.load(
            FileUtils.readFileToString(new File(LoadOptions.EXTRACTOR_CONFIG.get(cli).trim()))
    );
    ImportStrategy strategy = (ImportStrategy) config.get(LoadOptions.IMPORT_MODE).get();
    strategy.getImporter().importData(config, handler, hadoopConfig);

    SensorEnrichmentUpdateConfig sensorEnrichmentUpdateConfig = null;
    if(LoadOptions.ENRICHMENT_CONFIG.has(cli)) {
      sensorEnrichmentUpdateConfig = JSONUtils.INSTANCE.load( new File(LoadOptions.ENRICHMENT_CONFIG.get(cli))
              , SensorEnrichmentUpdateConfig.class
      );
    }

    if(sensorEnrichmentUpdateConfig != null) {
      sensorEnrichmentUpdateConfig.updateSensorConfigs();
    }
  }
}
