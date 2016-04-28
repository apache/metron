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
package org.apache.metron.api.helper.service;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.log4j.PropertyConfigurator;

public class PcapServiceCli {

  private String[] args = null;
  private Options options = new Options();

  int port = 8081;
  String uri = "/pcapGetter";
  String pcapHdfsPath= "/apps/metron/pcap";
  String queryHdfsPath = "/apps/metron/pcap_query";
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getPcapHdfsPath() {
    return pcapHdfsPath;
  }

  public String getQueryHdfsPath() {
    return queryHdfsPath;
  }
  public PcapServiceCli(String[] args) {

    this.args = args;

    Option help = new Option("h", "Display help menu");
    options.addOption(help);
    options.addOption(
            "port",
            true,
            "OPTIONAL ARGUMENT [portnumber] If this argument sets the port for starting the service.  If this argument is not set the port will start on defaut port 8081");
    options.addOption(
            "endpoint_uri",
            true,
            "OPTIONAL ARGUMENT [/uri/to/service] This sets the URI for the service to be hosted.  The default URI is /pcapGetter");
    options.addOption(
            "query_hdfs_path",
            true,
            "[query_hdfs_loc] The location in HDFS to temporarily store query results.  They will be cleaned up after the query is returned."
    );
    options.addOption(
            "pcap_hdfs_path",
            true,
            "[pcap_hdfs_path] The location in HDFS where PCAP raw data is stored in sequence files."
    );
    options.addOption(
            "log4j",
            true,
            "OPTIONAL ARGUMENT [log4j] The log4j properties."
    );
  }

  public void parse() {
    CommandLineParser parser = new BasicParser();

    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e1) {

      e1.printStackTrace();
    }

    if (cmd.hasOption("h")) {
      help();
    }

    if(cmd.hasOption("log4j")) {
      PropertyConfigurator.configure(cmd.getOptionValue("log4j"));
    }

    if (cmd.hasOption("port")) {

      try {
        port = Integer.parseInt(cmd.getOptionValue("port").trim());
      } catch (Exception e) {

        System.out.println("[Metron] Invalid value for port entered");
        help();
      }
    }
    if(cmd.hasOption("pcap_hdfs_path")) {
      pcapHdfsPath = cmd.getOptionValue("pcap_hdfs_path");
    }
    else {
      throw new IllegalStateException("You must specify the pcap hdfs path");
    }
    if(cmd.hasOption("query_hdfs_path")) {
      queryHdfsPath = cmd.getOptionValue("query_hdfs_path");
    }
    else {
      throw new IllegalStateException("You must specify the query temp hdfs path");
    }
    if (cmd.hasOption("endpoint_uri")) {

      try {

        if (uri == null || uri.equals(""))
          throw new Exception("invalid uri");

        uri = cmd.getOptionValue("uri").trim();

        if (uri.charAt(0) != '/')
          uri = "/" + uri;

        if (uri.charAt(uri.length()) == '/')
          uri = uri.substring(0, uri.length() - 1);

      } catch (Exception e) {
        System.out.println("[Metron] Invalid URI entered");
        help();
      }
    }

  }

  private void help() {
    // This prints out some help
    HelpFormatter formater = new HelpFormatter();

    formater.printHelp("Topology Options:", options);

    // System.out
    // .println("[Metron] Example usage: \n storm jar Metron-Topologies-0.3BETA-SNAPSHOT.jar org.apache.metron.topology.Bro -local_mode true -config_path Metron_Configs/ -generator_spout true");

    System.exit(0);
  }
}
