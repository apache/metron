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
package org.apache.metron.helpers.topology;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Cli {

	private String[] args = null;
	private Options options = new Options();

	private String path = null;
	private boolean debug = true;
	private boolean local_mode = true;
	private boolean generator_spout = false;

	public boolean isGenerator_spout() {
		return generator_spout;
	}

	public void setGenerator_spout(boolean generator_spout) {
		this.generator_spout = generator_spout;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isLocal_mode() {
		return local_mode;
	}

	public void setLocal_mode(boolean local_mode) {
		this.local_mode = local_mode;
	}

	public Cli(String[] args) {

		this.args = args;

		Option help = new Option("h", "Display help menue");
		options.addOption(help);
		options.addOption(
				"config_path",
				true,
				"OPTIONAL ARGUMENT [/path/to/configs] Path to configuration folder. If not provided topology will initialize with default configs");
		options.addOption(
				"local_mode",
				true,
				"REQUIRED ARGUMENT [true|false] Local mode or cluster mode.  If set to true the topology will run in local mode.  If set to false the topology will be deployed to Storm nimbus");
		options.addOption(
				"debug",
				true,
				"OPTIONAL ARGUMENT [true|false] Storm debugging enabled.  Default value is true");
		options.addOption(
				"generator_spout",
				true,
				"REQUIRED ARGUMENT [true|false] Turn on test generator spout.  Default is set to false.  If test generator spout is turned on then kafka spout is turned off.  Instead the generator spout will read telemetry from file and ingest it into a topology");
	}

	public void parse() {
		CommandLineParser parser = new BasicParser();

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);

			if (cmd.hasOption("h"))
				help();

			if (cmd.hasOption("local_mode")) {

				String local_value = cmd.getOptionValue("local_mode").trim()
						.toLowerCase();

				if (local_value.equals("true"))
					local_mode = true;

				else if (local_value.equals("false"))
					local_mode = false;
				else {
					System.out
							.println("[Metron] ERROR: Invalid value for local mode");
					System.out
							.println("[Metron] ERROR: Using cli argument -local_mode="
									+ cmd.getOptionValue("local_mode"));
					help();
				}
			} else {
				System.out
						.println("[Metron] ERROR: Invalid value for local mode");
				help();
			}
			if (cmd.hasOption("generator_spout")) {

				String local_value = cmd.getOptionValue("generator_spout").trim()
						.toLowerCase();

				if (local_value.equals("true"))
					generator_spout = true;

				else if (local_value.equals("false"))
					generator_spout = false;
				else {
					System.out
							.println("[Metron] ERROR: Invalid value for local generator_spout");
					System.out
							.println("[Metron] ERROR: Using cli argument -generator_spout="
									+ cmd.getOptionValue("generator_spout"));
					help();
				}
			} else {
				System.out
						.println("[Metron] ERROR: Invalid value for generator_spout");
				help();
			}
			if (cmd.hasOption("config_path")) {

				path = cmd.getOptionValue("config_path").trim();

				File file = new File(path);

				if (!file.isDirectory() || !file.exists()) {
					System.out
							.println("[Metron] ERROR: Invalid settings directory name given");
					System.out
							.println("[Metron] ERROR: Using cli argument -config_path="
									+ cmd.getOptionValue("config_path"));
					help();
				}
			}

			if (cmd.hasOption("debug")) {
				String debug_value = cmd.getOptionValue("debug");

				if (debug_value.equals("true"))
					debug = true;
				else if (debug_value.equals("false"))
					debug = false;
				else {
					System.out
							.println("[Metron] ERROR: Invalid value for debug_value");
					System.out
							.println("[Metron] ERROR: Using cli argument -debug_value="
									+ cmd.getOptionValue("debug_value"));
					help();
				}
			}

		} catch (ParseException e) {
			System.out
					.println("[Metron] ERROR: Failed to parse command line arguments");
			help();
		}
	}

	private void help() {
		// This prints out some help
		HelpFormatter formater = new HelpFormatter();

		formater.printHelp("Topology Options:", options);

		System.out
				.println("[Metron] Example usage: \n storm jar Metron-Topologies-0.3BETA-SNAPSHOT.jar org.apache.metron.topology.Bro -local_mode true -config_path Metron_Configs/ -generator_spout true");

		System.exit(0);
	}
}
