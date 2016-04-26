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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class ElasticsearchDataPrunerRunnerTest {

    private Options options;
    private Options help;

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;

    @Before
    public void setUp(){

        options = ElasticsearchDataPrunerRunner.buildOptions();
        help = new Options();

        Option o = new Option("h", "help", false, "This screen");
        o.setRequired(false);
        help.addOption(o);

        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

    }

    @Test(expected = RuntimeException.class)
    public void testThrowsWithoutZookeeperOrConfigLocation() throws Exception {

        String[] args = new String[]{"-n","30","-p","sensor_index","-s","03/30/2016"};
        ElasticsearchDataPrunerRunner.checkOptions(help,options,args);

    }

    @Test(expected = RuntimeException.class)
    public void testThrowsWithZookeeperAndConfiguration() throws Exception {

        String[] args = new String[]{"-n","30","-p","sensor_index","-s","03/30/2016"};
        ElasticsearchDataPrunerRunner.checkOptions(help,options,args);

    }

}
