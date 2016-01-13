package com.apache.metron.helpers.services;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class PcapServiceCli {

	private String[] args = null;
	private Options options = new Options();

	int port = 8081;
	String uri = "/pcapGetter";

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

	public PcapServiceCli(String[] args) {

		this.args = args;

		Option help = new Option("h", "Display help menue");
		options.addOption(help);
		options.addOption(
				"port",
				true,
				"OPTIONAL ARGUMENT [portnumber] If this argument sets the port for starting the service.  If this argument is not set the port will start on defaut port 8081");
		options.addOption(
				"endpoint_uri",
				true,
				"OPTIONAL ARGUMENT [/uri/to/service] This sets the URI for the service to be hosted.  The default URI is /pcapGetter");
	}

	public void parse() {
		CommandLineParser parser = new BasicParser();

		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {

			e1.printStackTrace();
		}

		if (cmd.hasOption("h"))
			help();

		if (cmd.hasOption("port")) {

			try {
				port = Integer.parseInt(cmd.getOptionValue("port").trim());
			} catch (Exception e) {

				System.out.println("[Metron] Invalid value for port entered");
				help();
			}
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
		// .println("[Metron] Example usage: \n storm jar Metron-Topologies-0.3BETA-SNAPSHOT.jar com.apache.metron.topology.Bro -local_mode true -config_path Metron_Configs/ -generator_spout true");

		System.exit(0);
	}
}
