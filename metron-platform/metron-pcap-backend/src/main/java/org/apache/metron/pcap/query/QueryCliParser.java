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

public class QueryCliParser extends CliParser {
  private Options queryOptions;

  public QueryCliParser() {
    queryOptions = setupOptions();
  }

  private Options setupOptions() {
    Options options = buildOptions();
    options.addOption(newOption("q", "query", true, "Query string to use as a filter"));
    return options;
  }

  /**
   * Parses query pcap filter options and required parameters common to all filter types.
   *
   * @param args command line arguments to parse
   * @return Configuration tailored to query pcap queries
   * @throws ParseException
   */
  public QueryCliConfig parse(String[] args) throws ParseException, java.text.ParseException {
    CommandLine commandLine = getParser().parse(queryOptions, args);
    QueryCliConfig config = new QueryCliConfig();
    super.parse(commandLine, config);
    if (commandLine.hasOption("query")) {
      config.setQuery(commandLine.getOptionValue("query"));
    }
    return config;
  }

  public void printHelp() {
    super.printHelp("Query filter options", queryOptions);
  }

}
