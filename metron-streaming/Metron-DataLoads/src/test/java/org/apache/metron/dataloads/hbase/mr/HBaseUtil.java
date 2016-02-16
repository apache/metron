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

/**
 * Created by cstella on 2/5/16.
 */
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
