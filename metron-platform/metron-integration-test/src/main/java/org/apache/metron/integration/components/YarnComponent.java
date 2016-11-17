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

package org.apache.metron.integration.components;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.JarFinder;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.nodemanager.NodeManager;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.capacity.CapacityScheduler;
import org.apache.metron.integration.InMemoryComponent;
import org.apache.metron.integration.UnableToStartException;

import java.io.*;
import java.net.URL;

public class YarnComponent implements InMemoryComponent{
    protected MiniYARNCluster yarnCluster = null;
    protected YarnConfiguration conf = null;
    private static final int NUM_NMS = 1;
    private String testName = null;
    protected String appmasterJar = null;

    public YarnComponent withApplicationMasterClass(Class clazz){
       appmasterJar = JarFinder.getJar(clazz);
        return this;
    }

    public YarnComponent withTestName(String name){
        this.testName = name;
        return this;
    }

    public String getAppMasterJar(){
        return appmasterJar;
    }

    public YarnConfiguration getConfig(){
        return conf;
    }

    public MiniYARNCluster getYARNCluster(){
        return yarnCluster;
    }

    @Override
    public void start() throws UnableToStartException {
        conf = new YarnConfiguration();
        conf.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 128);
        conf.set("yarn.log.dir", "target");
        conf.setBoolean(YarnConfiguration.TIMELINE_SERVICE_ENABLED, true);
        conf.set(YarnConfiguration.RM_SCHEDULER, CapacityScheduler.class.getName());
        conf.setBoolean(YarnConfiguration.NODE_LABELS_ENABLED, true);

        try {
            yarnCluster =
                    new MiniYARNCluster(testName, 1,
                            NUM_NMS, 1, 1, true);
            yarnCluster.init(conf);

            yarnCluster.start();

            waitForNMsToRegister();

            URL url = Thread.currentThread().getContextClassLoader().getResource("yarn-site.xml");
            if (url == null) {
                throw new RuntimeException("Could not find 'yarn-site.xml' dummy file in classpath");
            }
            Configuration yarnClusterConfig = yarnCluster.getConfig();
            yarnClusterConfig.set("yarn.application.classpath", new File(url.getPath()).getParent());
            //write the document to a buffer (not directly to the file, as that
            //can cause the file being written to get read -which will then fail.
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            yarnClusterConfig.writeXml(bytesOut);
            bytesOut.close();
            //write the bytes to the file in the classpath
            OutputStream os = new FileOutputStream(new File(url.getPath()));
            os.write(bytesOut.toByteArray());
            os.close();
            FileContext fsContext = FileContext.getLocalFSFileContext();
            fsContext
                    .delete(
                            new Path(conf
                                    .get("yarn.timeline-service.leveldb-timeline-store.path")),
                            true);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }catch(Exception e){
            throw new UnableToStartException("Exception setting up yarn cluster",e);
        }
    }

    @Override
    public void stop() {
        if (yarnCluster != null) {
            try {
                yarnCluster.stop();
            } finally {
                yarnCluster = null;
            }
        }
        try {
            FileContext fsContext = FileContext.getLocalFSFileContext();
            fsContext
                    .delete(
                            new Path(conf
                                    .get("yarn.timeline-service.leveldb-timeline-store.path")),
                            true);
        }catch(Exception e){}
    }

    protected void waitForNMsToRegister() throws Exception {
        int sec = 60;
        while (sec >= 0) {
            if (yarnCluster.getResourceManager().getRMContext().getRMNodes().size()
                    >= NUM_NMS) {
                break;
            }
            Thread.sleep(1000);
            sec--;
        }
    }
}
