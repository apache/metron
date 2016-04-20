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
package org.apache.metron.dataloads.hbase.mr;

import com.google.common.base.Joiner;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;

import java.io.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public enum HBaseUtil {
    INSTANCE;
    public Map.Entry<HBaseTestingUtility,Configuration> create(boolean startMRCluster) throws Exception {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.master.hostname", "localhost");
        config.set("hbase.regionserver.hostname", "localhost");
        HBaseTestingUtility testUtil = new HBaseTestingUtility(config);

        testUtil.startMiniCluster(1);
        if(startMRCluster) {
            testUtil.startMiniMapReduceCluster();
        }
        return new AbstractMap.SimpleEntry<>(testUtil, config);
    }
    public void writeFile(String contents, Path filename, FileSystem fs) throws IOException {
        FSDataOutputStream os = fs.create(filename, true);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(os));
        pw.print(contents);
        pw.flush();
        os.close();
    }

    public String readFile(FileSystem fs, Path filename) throws IOException {
        FSDataInputStream in = fs.open(filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        List<String> contents = new ArrayList<>();
        for(String line = null;(line = br.readLine()) != null;) {
            contents.add(line);
        }
        return Joiner.on('\n').join(contents);
    }

    public void teardown(HBaseTestingUtility testUtil) throws Exception {
        testUtil.shutdownMiniMapReduceCluster();
        testUtil.shutdownMiniCluster();
        testUtil.cleanupTestDir();
    }
}
