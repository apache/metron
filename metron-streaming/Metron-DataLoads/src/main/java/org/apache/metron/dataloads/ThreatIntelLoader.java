package org.apache.metron.dataloads;


import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.apache.metron.dataloads.interfaces.ThreatIntelSource;

public class ThreatIntelLoader {

	
	private static final Logger LOG = Logger.getLogger(ThreatIntelLoader.class);
	
	private static int BULK_SIZE = 50; 
	
	public static void main(String[] args) {
		
		PropertiesConfiguration sourceConfig = null;
		ThreatIntelSource threatIntelSource = null;
		ArrayList<Put> putList = null;
		HTable table = null;
		Configuration hConf = null;
		
		CommandLine commandLine = parseCommandLine(args);
		File configFile = new File(commandLine.getOptionValue("configFile"));
		
		try {
			sourceConfig = new PropertiesConfiguration(configFile);
		} catch (org.apache.commons.configuration.ConfigurationException e) {
			LOG.error("Error in configuration file " + configFile);
			LOG.error(e);
			System.exit(-1);
		}
		
		try {
			threatIntelSource = (ThreatIntelSource) Class.forName(commandLine.getOptionValue("source")).newInstance();
			threatIntelSource.initializeSource(sourceConfig);
		} catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
			LOG.error("Error while trying to load class " + commandLine.getOptionValue("source"));
			LOG.error(e);
			System.exit(-1);
		}
		
		hConf = HBaseConfiguration.create();
		try {
			table = new HTable(hConf, commandLine.getOptionValue("table"));
		} catch (IOException e) {
			LOG.error("Exception when processing HBase config");
			LOG.error(e);
			System.exit(-1);
		}
		
		
		putList = new ArrayList<Put>();
		
		while (threatIntelSource.hasNext()) {
			
			JSONObject intel = threatIntelSource.next();
			
			/*
			 * If any of the required fields from threatIntelSource are
			 * missing, or contain invalid data, don't put it in HBase.
			 */
			try {				

				putList.add(putRequestFromIntel(intel));		
				
				if (putList.size() == BULK_SIZE) {
					table.put(putList);
					putList.clear();
				}
				
			} catch (NullPointerException|ClassCastException e) {
				LOG.error("Exception while processing intel object");
				LOG.error(intel.toString());
				LOG.error(e);
			} catch (InterruptedIOException|org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException e) {
				LOG.error("Problem communicationg with HBase");
				LOG.error(e);
				System.exit(-1);
			} catch (IOException e) {
				LOG.error("Problem communicationg with HBase");
				LOG.error(e);
				System.exit(-1);
			}
		}
		
	}
	/*
	 * Takes a JSONObject from a ThreatIntelSource implementation, ensures
	 * that the format of the returned JSONObect is correct, and returns
	 * a Put request for HBase.
	 * 
	 * @param	intel	The JSONObject from a ThreatIntelSource
	 * @return			A put request for the intel data 
	 * @throws	NullPointerException If a required field is missing
	 * @throws	ClassCastException If a field has an invalid type
	 * 
	 */
	private static Put putRequestFromIntel(JSONObject intel) {
		
		Put tempPut = new Put(Bytes.toBytes((String) intel.get("indicator")));
		
		JSONArray intelArray = (JSONArray) intel.get("data");
		
		tempPut.add(Bytes.toBytes("source"),
					Bytes.toBytes((String) intel.get("source")),
					Bytes.toBytes(intelArray.toString()));
		
		return tempPut;
	}
	/*
	 * Handles parsing of command line options and validates the options are used
	 * correctly. This will not validate the value of the options, it will just
	 * ensure that the required options are used. If the options are used 
	 * incorrectly, the help is printed, and the program exits.
	 * 
	 * @param  args The arguments from the CLI
	 * @return 		A CommandLine with the CLI arguments
	 * 
	 */
	private static CommandLine parseCommandLine(String[] args) {
		
		CommandLineParser parser = new BasicParser();
		CommandLine cli = null;
		
		Options options = new Options();
		
		options.addOption(OptionBuilder.withArgName("s").
				withLongOpt("source").
				isRequired(true).
				hasArg(true).
				withDescription("Source class to use").
				create()
				);
		options.addOption(OptionBuilder.withArgName("t").
				withLongOpt("table").
				isRequired(true).
				hasArg(true).
				withDescription("HBase table to load into").
				create()
				);
		options.addOption(OptionBuilder.withArgName("c").
				withLongOpt("configFile").
				hasArg(true).
				withDescription("Configuration file for source class").
				create()
				);
		
		try {
			cli = parser.parse(options, args);
		} catch(org.apache.commons.cli.ParseException e) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ThreatIntelLoader", options, true);
			System.exit(-1);
		}
		
		return cli;
	}
}
