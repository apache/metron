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

import org.adrianwalker.multilinestring.Multiline;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.dataloads.hbase.mr.HBaseUtil;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.test.utils.UnitTestHelper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SimpleEnrichmentFlatFileLoaderIntegrationTest {

  private static HBaseTestingUtility testUtil;

  /** The test table. */
  private static HTable testTable;
  private static Configuration config = null;
  private TestingServer testZkServer;
  private String zookeeperUrl;
  private static final String tableName = "enrichment";
  private static final String cf = "cf";
  private static final String csvFile="input.csv";
  private static final String extractorJson = "extractor.json";
  private static final String enrichmentJson = "enrichment_config.json";
  private static final String log4jProperty = "log4j";
  private static final File file1 = new File("target/sefflt_data_1.csv");
  private static final File file2 = new File("target/sefflt_data_2.csv");
  private static final File multilineFile= new File("target/sefflt_data_2.csv");
  private static final File multilineZipFile= new File("target/sefflt_data_2.csv.zip");
  private static final File multilineGzFile= new File("target/sefflt_data_2.csv.gz");
  private static final File lineByLineExtractorConfigFile = new File("target/sefflt_extractorConfig_lbl.json");
  private static final File wholeFileExtractorConfigFile = new File("target/sefflt_extractorConfig_wf.json");
  private static final File stellarExtractorConfigFile = new File("target/sefflt_extractorConfig_stellar.json");
  private static final int NUM_LINES = 1000;

  /**
   {
      "config" : {
        "columns" : {
          "host" : 0,
          "meta" : 2
                    },
        "indicator_column" : "host",
        "separator" : ",",
        "type" : "enrichment"
                 },
      "extractor" : "CSV"
   }
   */
  @Multiline
  private static String lineByLineExtractorConfig;

  /**
   {
      "config" : {
        "columns" : {
          "host" : 0,
          "meta" : 2
                    },
        "indicator_column" : "host",
        "separator" : ",",
        "type" : "enrichment"
                 },
      "extractor" : "CSV",
      "inputFormat" : "WHOLE_FILE"
   }
   */
  @Multiline
  private static String wholeFileExtractorConfig;

  /**
   *{
   *  "config" : {
   *    "columns" : {
   *      "host" : 0,
   *      "meta" : 1
   *    },
   *    "value_transform" : {
   *      "host" : "TO_UPPER(host)"
   *    },
   *    "value_filter" : "LENGTH(host) > 0",
   *    "indicator_column" : "host",
   *    "indicator_transform" : {
   *      "indicator" : "TO_UPPER(indicator)"
   *    },
   *    "indicator_filter" : "LENGTH(indicator) > 0",
   *    "type" : "enrichment",
   *    "separator" : ","
   *  }
   *}
   */
  @Multiline
  public static String stellarExtractorConfig;

  @BeforeClass
  public static void setup() throws Exception {
    UnitTestHelper.setJavaLoggingLevel(Level.SEVERE);
    Map.Entry<HBaseTestingUtility, Configuration> kv = HBaseUtil.INSTANCE.create(true);
    config = kv.getValue();
    testUtil = kv.getKey();
    testTable = testUtil.createTable(Bytes.toBytes(tableName), Bytes.toBytes(cf));

    for(Result r : testTable.getScanner(Bytes.toBytes(cf))) {
      Delete d = new Delete(r.getRow());
      testTable.delete(d);
    }

    if(lineByLineExtractorConfigFile.exists()) {
      lineByLineExtractorConfigFile.delete();
    }
    Files.write( lineByLineExtractorConfigFile.toPath()
               , lineByLineExtractorConfig.getBytes()
               , StandardOpenOption.CREATE_NEW , StandardOpenOption.TRUNCATE_EXISTING
    );
    if(wholeFileExtractorConfigFile.exists()) {
      wholeFileExtractorConfigFile.delete();
    }
    Files.write( wholeFileExtractorConfigFile.toPath()
               , wholeFileExtractorConfig.getBytes()
               , StandardOpenOption.CREATE_NEW , StandardOpenOption.TRUNCATE_EXISTING
    );
    if(stellarExtractorConfigFile.exists()) {
      stellarExtractorConfigFile.delete();
    }
    Files.write(stellarExtractorConfigFile.toPath()
            , stellarExtractorConfig.getBytes()
            , StandardOpenOption.CREATE_NEW , StandardOpenOption.TRUNCATE_EXISTING
    );
    if(file1.exists()) {
      file1.delete();
    }
    Files.write( file1.toPath()
               , "google1.com,1,foo2\n".getBytes()
               , StandardOpenOption.CREATE_NEW , StandardOpenOption.TRUNCATE_EXISTING
    );
    if(file2.exists()) {
      file2.delete();
    }
    Files.write( file2.toPath()
               , "google2.com,2,foo2\n".getBytes()
               , StandardOpenOption.CREATE_NEW , StandardOpenOption.TRUNCATE_EXISTING
    );

    if(multilineFile.exists()) {
      multilineFile.delete();
    }
    if(multilineGzFile.exists()) {
      multilineGzFile.delete();
    }
    if(multilineGzFile.exists()) {
      multilineZipFile.delete();
    }
    PrintWriter[] pws =new PrintWriter[] {};
    try {
      ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(multilineZipFile));
      ZipEntry entry = new ZipEntry("file");
      zos.putNextEntry(entry);
       pws = new PrintWriter[]{
         new PrintWriter(multilineFile),
         new PrintWriter(zos),
         new PrintWriter(new GZIPOutputStream(new FileOutputStream(multilineGzFile)))
                              };
      for(int i = 0;i < NUM_LINES;++i) {
        for(PrintWriter pw : pws) {
          pw.println("google" + i + ".com," + i + ",foo" + i);
        }
      }
    }
    finally {
      for(PrintWriter pw : pws) {
        pw.close();
      }
    }

  }

  @AfterClass
  public static void teardown() throws Exception {
    HBaseUtil.INSTANCE.teardown(testUtil);
    file1.delete();
    file2.delete();
    multilineFile.delete();
    multilineGzFile.delete();
    multilineZipFile.delete();
    lineByLineExtractorConfigFile.delete();
    wholeFileExtractorConfigFile.delete();
  }


  @Test
  public void testArgs() throws Exception {
    String[] argv = {"-c cf", "-t enrichment"
            , "-e extractor.json", "-n enrichment_config.json"
            , "-l log4j", "-i input.csv"
            , "-p 2", "-b 128", "-q"
    };

    String[] otherArgs = new GenericOptionsParser(config, argv).getRemainingArgs();

    CommandLine cli = LoadOptions.parse(new PosixParser(), otherArgs);
    Assert.assertEquals(extractorJson, LoadOptions.EXTRACTOR_CONFIG.get(cli).trim());
    Assert.assertEquals(cf, LoadOptions.HBASE_CF.get(cli).trim());
    Assert.assertEquals(tableName, LoadOptions.HBASE_TABLE.get(cli).trim());
    Assert.assertEquals(enrichmentJson, LoadOptions.ENRICHMENT_CONFIG.get(cli).trim());
    Assert.assertEquals(csvFile, LoadOptions.INPUT.get(cli).trim());
    Assert.assertEquals(log4jProperty, LoadOptions.LOG4J_PROPERTIES.get(cli).trim());
    Assert.assertEquals("2", LoadOptions.NUM_THREADS.get(cli).trim());
    Assert.assertEquals("128", LoadOptions.BATCH_SIZE.get(cli).trim());
  }

  @Test
  public void testLocalLineByLine() throws Exception {
    String[] argv = {"-c cf", "-t enrichment"
            , "-e " + lineByLineExtractorConfigFile.getPath()
            , "-i " + multilineFile.getPath()
            , "-p 2", "-b 128", "-q"
    };
    SimpleEnrichmentFlatFileLoader.main(config, argv);
    EnrichmentConverter converter = new EnrichmentConverter();
    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for (Result r : scanner) {
      results.add(converter.fromResult(r, cf));
      testTable.delete(new Delete(r.getRow()));
    }
    Assert.assertEquals(NUM_LINES, results.size());
    Assert.assertTrue(results.get(0).getKey().indicator.startsWith("google"));
    Assert.assertEquals(results.get(0).getKey().type, "enrichment");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 2);
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("meta").toString().startsWith("foo"));
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("host").toString().startsWith("google"));
  }

  @Test
  public void testLocalLineByLine_gz() throws Exception {
    String[] argv = {"-c cf", "-t enrichment"
            , "-e " + lineByLineExtractorConfigFile.getPath()
            , "-i " + multilineGzFile.getPath()
            , "-p 2", "-b 128", "-q"
    };
    SimpleEnrichmentFlatFileLoader.main(config, argv);
    EnrichmentConverter converter = new EnrichmentConverter();
    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for (Result r : scanner) {
      results.add(converter.fromResult(r, cf));
      testTable.delete(new Delete(r.getRow()));
    }
    Assert.assertEquals(NUM_LINES, results.size());
    Assert.assertTrue(results.get(0).getKey().indicator.startsWith("google"));
    Assert.assertEquals(results.get(0).getKey().type, "enrichment");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 2);
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("meta").toString().startsWith("foo"));
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("host").toString().startsWith("google"));

  }

  @Test
  public void testLocalLineByLine_zip() throws Exception {
    String[] argv = {"-c cf", "-t enrichment"
            , "-e " + lineByLineExtractorConfigFile.getPath()
            , "-i " + multilineZipFile.getPath()
            , "-p 2", "-b 128", "-q"
    };
    SimpleEnrichmentFlatFileLoader.main(config, argv);
    EnrichmentConverter converter = new EnrichmentConverter();
    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for (Result r : scanner) {
      results.add(converter.fromResult(r, cf));
      testTable.delete(new Delete(r.getRow()));
    }
    Assert.assertEquals(NUM_LINES, results.size());
    Assert.assertTrue(results.get(0).getKey().indicator.startsWith("google"));
    Assert.assertEquals(results.get(0).getKey().type, "enrichment");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 2);
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("meta").toString().startsWith("foo"));
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("host").toString().startsWith("google"));

  }

  @Test
  public void testLocalWholeFile() throws Exception {
    String[] argv = { "-c cf", "-t enrichment"
            , "-e " + wholeFileExtractorConfigFile.getPath()
            , "-i " + file1.getPath() + "," + file2.getPath()
            , "-p 2", "-b 128", "-q"
    };
    SimpleEnrichmentFlatFileLoader.main(config, argv);
    EnrichmentConverter converter = new EnrichmentConverter();
    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for(Result r : scanner) {
      results.add(converter.fromResult(r, cf));
      testTable.delete(new Delete(r.getRow()));
    }
    Assert.assertEquals(2, results.size());
    Assert.assertTrue(results.get(0).getKey().indicator.startsWith("google"));
    Assert.assertEquals(results.get(0).getKey().type, "enrichment");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 2);
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("meta").toString().startsWith("foo"));
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("host").toString().startsWith( "google"));

  }

  @Test
  public void testMRLineByLine() throws Exception {
    String[] argv = {"-c cf", "-t enrichment"
            , "-e " + lineByLineExtractorConfigFile.getPath()
            , "-i " + multilineFile.getName()
            , "-m MR"
            , "-p 2", "-b 128", "-q"
    };
    FileSystem fs = FileSystem.get(config);
    HBaseUtil.INSTANCE.writeFile(new String(Files.readAllBytes(multilineFile.toPath())), new Path(multilineFile.getName()), fs);
    SimpleEnrichmentFlatFileLoader.main(config, argv);
    EnrichmentConverter converter = new EnrichmentConverter();
    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for (Result r : scanner) {
      results.add(converter.fromResult(r, cf));
      testTable.delete(new Delete(r.getRow()));
    }
    Assert.assertEquals(NUM_LINES, results.size());
    Assert.assertTrue(results.get(0).getKey().indicator.startsWith("google"));
    Assert.assertEquals(results.get(0).getKey().type, "enrichment");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 2);
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("meta").toString().startsWith("foo"));
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("host").toString().startsWith("google"));
  }

  @Test
  public void transforms_fields() throws Exception {
    String[] argv = {"-c cf", "-t enrichment"
            , "-e " + stellarExtractorConfigFile.getPath()
            , "-i " + multilineFile.getPath()
            , "-p 2", "-b 128", "-q"
    };
    SimpleEnrichmentFlatFileLoader.main(config, argv);
    EnrichmentConverter converter = new EnrichmentConverter();
    ResultScanner scanner = testTable.getScanner(Bytes.toBytes(cf));
    List<LookupKV<EnrichmentKey, EnrichmentValue>> results = new ArrayList<>();
    for (Result r : scanner) {
      results.add(converter.fromResult(r, cf));
      testTable.delete(new Delete(r.getRow()));
    }
    Assert.assertEquals(NUM_LINES, results.size());
    Assert.assertTrue(results.get(0).getKey().indicator.startsWith("google"));
    Assert.assertEquals(results.get(0).getKey().type, "enrichment");
    Assert.assertEquals(results.get(0).getValue().getMetadata().size(), 2);
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("meta").toString().startsWith("foo"));
    Assert.assertTrue(results.get(0).getValue().getMetadata().get("host").toString().startsWith("GOOGLE"));
  }

}
