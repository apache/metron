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
package org.apache.metron.dataloads.bulk;

import org.apache.commons.io.FileUtils;
import org.apache.metron.domain.Configuration;
import org.easymock.EasyMock;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE, numDataNodes = 1, numClientNodes = 0)
public class ElasticsearchDataPrunerIntegrationTest extends ElasticsearchIntegrationTest {

    private static File dataPath = new File("./target/elasticsearch-test");
    private Date testingDate;
    private Date yesterday = new Date();
    private DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH");
    private Configuration configuration;

    @BeforeClass
    public static void setupClass() throws Exception {

        if (dataPath.isDirectory()) {
            FileUtils.deleteDirectory(dataPath);
        }

        if (!dataPath.mkdirs()) {
            throw new RuntimeException("Couldn't create dataPath at: " + dataPath.getAbsolutePath());
        }

    }

    @AfterClass
    public static void teardownClass() throws Exception {

        if (dataPath.isDirectory()) {
            FileUtils.deleteDirectory(dataPath);
        }

    }

    @Before
    public void setUp() throws Exception {

        super.setUp();
        ensureGreen();

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        testingDate = calendar.getTime();
        yesterday.setTime(testingDate.getTime() - TimeUnit.DAYS.toMillis(1));
        dateFormat.setTimeZone(timeZone);

        File resourceFile = new File("../Metron-Testing/src/main/resources/sample/config/");
        Path resourcePath = Paths.get(resourceFile.getCanonicalPath());

        configuration = new Configuration(resourcePath);
    }

    @Test(expected = IndexMissingException.class)
    public void testWillThrowOnMissingIndex() throws Exception {

        ElasticsearchDataPruner pruner = new ElasticsearchDataPruner(yesterday, 30, configuration,client(), "*");
        pruner.deleteIndex(admin(), "baz");

    }

    @Test
    public void testDeletesCorrectIndexes() throws Exception {

        Integer numDays = 5;

        Date createStartDate = new Date();

        createStartDate.setTime(yesterday.getTime() - TimeUnit.DAYS.toMillis(numDays - 1));

        ElasticsearchDataPruner pruner = new ElasticsearchDataPruner(yesterday, 30, configuration,client(), "*");
        String indexesToDelete = "sensor_index_" + new SimpleDateFormat("yyyy.MM.dd").format(createStartDate) + ".*";
        Boolean deleted = pruner.deleteIndex(admin(), indexesToDelete);

        assertTrue("Index deletion should be acknowledged", deleted);

    }

    @Test
    public void testHandlesNoIndicesToDelete() throws Exception {

        ElasticsearchDataPruner pruner = new ElasticsearchDataPruner(yesterday, 1, configuration, client(), "sensor_index_");
        Long deleteCount = pruner.prune();
        assertEquals("Should have pruned 0 indices", 0L, deleteCount.longValue());


    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {

        return ImmutableSettings.settingsBuilder()
                .put("node.data", true)
                .put("gateway.type", "none")
                .put("path.data", dataPath.getPath() + "/data")
                .put("path.work", dataPath.getPath() + "/work")
                .put("path.logs", dataPath.getPath() + "/logs")
                .put("cluster.routing.schedule", "50ms")
                .put("node.local", true).build();

    }

    public Settings indexSettings() {

        return ImmutableSettings.settingsBuilder()
                .put("index.store.type", "memory")
                .put("index.store.fs.memory.enabled", "true")
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0).build();

    }

}