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

import org.apache.commons.cli.*;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.metron.common.configuration.Configuration;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ElasticsearchDataPrunerRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchDataPruner.class);

    public static void main(String... argv) throws IOException, java.text.ParseException, ClassNotFoundException, InterruptedException {

        /**
         * Example
         * start=$(date -d '30 days ago' +%m/%d/%Y)
         * yarn jar Metron-DataLoads-{VERSION}.jar org.apache.metron.dataloads.bulk.ElasticsearchDataPrunerRunner -i host1:9300 -p '/bro_index_' -s $(date -d '30 days ago' +%m/%d/%Y) -n 1;
         * echo ${start}
         **/

        Options options = buildOptions();
        Options help = new Options();
        TransportClient client = null;

        Option o = new Option("h", "help", false, "This screen");
        o.setRequired(false);
        help.addOption(o);



        try {

            CommandLine cmd = checkOptions(help,options, argv);

            String start = cmd.getOptionValue("s");
            Date startDate = new SimpleDateFormat("MM/dd/yyyy").parse(start);

            Integer numDays = Integer.parseInt(cmd.getOptionValue("n"));
            String indexPrefix = cmd.getOptionValue("p");

            if(LOG.isDebugEnabled()) {
                LOG.debug("Running prune with args: " + startDate + " " + numDays);
            }

            Configuration configuration = null;

            if( cmd.hasOption("z")){

                RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
                CuratorFramework framework = CuratorFrameworkFactory.newClient(cmd.getOptionValue("z"),retryPolicy);
                framework.start();
                configuration = new Configuration(framework);

            } else if ( cmd.hasOption("c") ){

                String resourceFile = cmd.getOptionValue("c");
                configuration = new Configuration(Paths.get(resourceFile));

            }

            configuration.update();

            Map<String, Object> globalConfiguration = configuration.getGlobalConfig();

            Settings.Builder settingsBuilder = Settings.settingsBuilder();
            settingsBuilder.put("cluster.name", globalConfiguration.get("es.clustername"));
            settingsBuilder.put("curatorFramework.transport.ping_timeout","500s");
            Settings settings = settingsBuilder.build();
            client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(
                            new InetSocketTransportAddress(InetAddress.getByName(globalConfiguration.get("es.ip").toString()), Integer.parseInt(globalConfiguration.get("es.port").toString()) )
                    );

            DataPruner pruner = new ElasticsearchDataPruner(startDate, numDays, configuration, client, indexPrefix);

            LOG.info("Pruned " + pruner.prune() + " indices from " +  globalConfiguration.get("es.ip") + ":" + globalConfiguration.get("es.port") + "/" + indexPrefix);


        } catch (Exception e) {

            e.printStackTrace();
            System.exit(-1);

        } finally {

            if( null != client) {
                client.close();
            }

        }

    }

    public static CommandLine checkOptions(Options help, Options options, String ... argv) throws ParseException {

        CommandLine cmd = null;
        CommandLineParser parser = new PosixParser();


        try {

            cmd = parser.parse(help,argv,true);

            if( cmd.getOptions().length > 0){
                final HelpFormatter usageFormatter = new HelpFormatter();
                usageFormatter.printHelp("ElasticsearchDataPrunerRunner", null, options, null, true);
                System.exit(0);
            }

            cmd = parser.parse(options, argv);

        } catch (ParseException e) {

            final HelpFormatter usageFormatter = new HelpFormatter();
            usageFormatter.printHelp("ElasticsearchDataPrunerRunner", null, options, null, true);
            throw e;

        }


        if( (cmd.hasOption("z") && cmd.hasOption("c")) || (!cmd.hasOption("z") && !cmd.hasOption("c")) ){

            System.err.println("One (only) of zookeeper-hosts or config-location is required");
            final HelpFormatter usageFormatter = new HelpFormatter();
            usageFormatter.printHelp("ElasticsearchDataPrunerRunner", null, options, null, true);
            throw new RuntimeException("Must specify zookeeper-hosts or config-location, but not both");

        }

        return cmd;
    }

    public static Options buildOptions(){

        Options options = new Options();

        Option o = new Option("s", "start-date", true, "Starting Date (MM/DD/YYYY)");
        o.setArgName("START_DATE");
        o.setRequired(true);
        options.addOption(o);

        o = new Option("n", "numdays", true, "Number of days back to purge");
        o.setArgName("NUMDAYS");
        o.setRequired(true);
        options.addOption(o);

        o = new Option("p", "index-prefix", true, "Index prefix  - e.g. bro_index_");
        o.setArgName("PREFIX");
        o.setRequired(true);
        options.addOption(o);

        o = new Option("c", "config-location", true, "Directory Path - e.g. /path/to/config/dir");
        o.setArgName("CONFIG");
        o.setRequired(false);
        options.addOption(o);

        o = new Option("z", "zookeeper-hosts", true, "Zookeeper URL - e.g. zkhost1:2181,zkhost2:2181,zkhost3:2181");
        o.setArgName("PREFIX");
        o.setRequired(false);
        options.addOption(o);

        return options;
    }
}
