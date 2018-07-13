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
package org.apache.metron.pcap.query;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.metron.common.Constants;
import org.apache.metron.pcap.PcapHelper;
import org.apache.metron.pcap.config.FixedPcapConfig;
import org.apache.metron.pcap.config.PcapConfig;

public class FixedCliParser extends CliParser {
  private Options fixedOptions;

  public FixedCliParser(PcapConfig.PrefixStrategy prefixStrategy) {
    super(prefixStrategy);
    fixedOptions = buildFixedOptions();
  }

  private Options buildFixedOptions() {
    Options options = buildOptions();
    options.addOption(newOption("sa", "ip_src_addr", true, "Source IP address"));
    options.addOption(newOption("da", "ip_dst_addr", true, "Destination IP address"));
    options.addOption(newOption("sp", "ip_src_port", true, "Source port"));
    options.addOption(newOption("dp", "ip_dst_port", true, "Destination port"));
    options.addOption(newOption("p", "protocol", true, "IP Protocol"));
    options.addOption(newOption("pf", "packet_filter", true, "Packet Filter regex"));
    options.addOption(newOption("pre", "prefix", true, "Result file prefix to use"));
    options.addOption(newOption("ir", "include_reverse", false, "Indicates if filter should check swapped src/dest addresses and IPs"));
    return options;
  }

  /**
   * Parses fixed pcap filter options and required parameters common to all filter types.
   *
   * @param args command line arguments to parse
   * @return Configuration tailored to fixed pcap queries
   * @throws ParseException
   */
  public FixedPcapConfig parse(String[] args) throws ParseException, java.text.ParseException {
    CommandLine commandLine = getParser().parse(fixedOptions, args);
    FixedPcapConfig config = new FixedPcapConfig(prefixStrategy);
    super.parse(commandLine, config);
    config.putFixedField(Constants.Fields.SRC_ADDR.getName(), commandLine.getOptionValue("ip_src_addr"));
    config.putFixedField(Constants.Fields.DST_ADDR.getName(), commandLine.getOptionValue("ip_dst_addr"));
    config.putFixedField(Constants.Fields.SRC_PORT.getName(), commandLine.getOptionValue("ip_src_port"));
    config.putFixedField(Constants.Fields.DST_PORT.getName(), commandLine.getOptionValue("ip_dst_port"));
    config.putFixedField(Constants.Fields.PROTOCOL.getName(), commandLine.getOptionValue("protocol"));
    config.putFixedField(Constants.Fields.INCLUDES_REVERSE_TRAFFIC.getName(), Boolean.toString(commandLine.hasOption("include_reverse")));
    config.putFixedField(PcapHelper.PacketFields.PACKET_FILTER.getName(), commandLine.getOptionValue("packet_filter"));
    if(commandLine.hasOption("prefix")) {
      config.setFinalFilenamePrefix(commandLine.getOptionValue("prefix"));
    }
    return config;
  }

  public void printHelp() {
    super.printHelp("Fixed filter options", fixedOptions);
  }

}
