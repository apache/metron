/*
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
package org.apache.metron.dataloads.nonbulk.geo;

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.metron.common.utils.CompressionStrategies;
import org.apache.metron.integration.utils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class MaxmindDbEnrichmentLoaderTest {
  private class MockMaxmindDbEnrichmentLoader extends MaxmindDbEnrichmentLoader {
    @Override
    protected void pushConfig(Path srcPath, Path dstPath, String configName, String zookeeper) {
    }
  }

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private File remoteDir;
  private File tmpDir;

  @Before
  public void setup() throws Exception {
      testFolder.create();
      remoteDir = testFolder.newFolder("remoteDir");
      tmpDir = testFolder.newFolder("tmpDir");
  }

  @Test
  public void testCommandLineShortOpts() throws Exception {
    String[] argv = {
        "-g testGeoUrl",
        "-a testAsnUrl",
        "-r /test/remoteDirGeo",
        "-ra", "/test/remoteDirAsn",
        "-t /test/tmpDir",
        "-z test:2181"
    };
    String[] otherArgs = new GenericOptionsParser(argv).getRemainingArgs();

    CommandLine cli = MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.parse(new PosixParser(), otherArgs);
    Assert.assertEquals("testGeoUrl", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.GEO_URL.get(cli).trim());
    Assert.assertEquals("testAsnUrl", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.ASN_URL.get(cli).trim());
    Assert.assertEquals("/test/remoteDirGeo", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.REMOTE_GEO_DIR.get(cli).trim());
    Assert.assertEquals("/test/remoteDirAsn", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.REMOTE_ASN_DIR.get(cli).trim());
    Assert.assertEquals("/test/tmpDir", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.TMP_DIR.get(cli).trim());
    Assert.assertEquals("test:2181", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.ZK_QUORUM.get(cli).trim());
  }

  @Test
  public void testCommandLineLongOpts() throws Exception {
    String[] argv = {
        "--geo_url", "testGeoUrl",
        "--remote_dir", "/test/remoteDir",
        "-ra", "/test/remoteDir",
        "--tmp_dir", "/test/tmpDir",
        "--zk_quorum", "test:2181"
    };
    String[] otherArgs = new GenericOptionsParser(argv).getRemainingArgs();

    CommandLine cli = MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.parse(new PosixParser(), otherArgs);
    Assert.assertEquals("testGeoUrl", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.GEO_URL.get(cli).trim());
    Assert.assertEquals("/test/remoteDir", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.REMOTE_GEO_DIR.get(cli).trim());
    Assert.assertEquals("/test/tmpDir", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.TMP_DIR.get(cli).trim());
    Assert.assertEquals("test:2181", MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.ZK_QUORUM.get(cli).trim());
  }

  @Test
  public void testLoadGeoIpDatabase() throws Exception {
    File dbPlainTextFile = new File(remoteDir.getAbsolutePath() + "/MaxmindDbEnrichmentLoaderTest.mmdb");
    TestUtils.write(dbPlainTextFile, "hello world");
    File dbFile = new File(remoteDir.getAbsolutePath() + "/MaxmindDbEnrichmentLoaderTest.mmdb.gz");
    CompressionStrategies.GZIP.compress(dbPlainTextFile, dbFile);
    String[] argv = {
        "--geo_url", "file://" + dbFile.getAbsolutePath(),
        "--remote_dir", remoteDir.getAbsolutePath(),
        "--remote_asn_dir", remoteDir.getAbsolutePath(),
        "--tmp_dir", tmpDir.getAbsolutePath(),
        "--zk_quorum", "test:2181"
    };
    String[] otherArgs = new GenericOptionsParser(argv).getRemainingArgs();
    CommandLine cli = MaxmindDbEnrichmentLoader.GeoEnrichmentOptions.parse(new PosixParser(), otherArgs);

    MaxmindDbEnrichmentLoader loader = new MockMaxmindDbEnrichmentLoader();
    loader.loadGeoLiteDatabase(cli);
    Configuration config = new Configuration();
    FileSystem fs = FileSystem.get(config);
    assertTrue(fs.exists(new Path(remoteDir + "/" + dbFile.getName())));
  }

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void loader_throws_exception_on_bad_gzip_file() throws Exception {
    File dbFile = new File(remoteDir.getAbsolutePath() + "/MaxmindDbEnrichmentLoaderTest.mmdb");
    dbFile.createNewFile();

    String geoUrl = "file://" + dbFile.getAbsolutePath();
    int numRetries = 2;
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unable to download geo enrichment database.");
    MaxmindDbEnrichmentLoader loader = new MockMaxmindDbEnrichmentLoader();
    loader.downloadGeoFile(geoUrl, tmpDir.getAbsolutePath(), numRetries);
  }

}
