package com.opensoc.dataloads.cif;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;

import java.io.BufferedInputStream;

public class HBaseTableLoad {

	private static final Logger LOG = Logger.getLogger(HBaseTableLoad.class);
	private static Configuration conf = null;
	private String hbaseTable = "cif_table";
	private String dirName = "./";
	private boolean usefileList = false;
	private Set<String> files;

	/**
	 * Initialization
	 */
	static {
		conf = HBaseConfiguration.create();
	}

	public static void main(String[] args) {

		HBaseTableLoad ht = new HBaseTableLoad();

		ht.parse(args);
		//ht.LoadDirHBase();

	}

	private void LoadDirHBase() {
		LOG.info("Working on:" + dirName);
		File folder = new File(dirName);
		File[] listOfFiles = folder.listFiles();
		InputStream input;

		for (int i = 0; i < listOfFiles.length; i++) {
			File file = listOfFiles[i];

			if (file.isFile()) {

				// Check if filename is present in FileList
				if (usefileList)
					if (!files.contains(file.getAbsolutePath()))
						continue;

				// e.g. folder name is infrastructure_botnet. Col Qualifier is
				// botnet and col_family is infrastructure

				String col_family = folder.getName().split("_")[0];
				String col_qualifier = folder.getName().split("_")[1];

				// Open file
				try {
					if (file.getName().endsWith(".gz"))
						input = new BufferedInputStream(new GZIPInputStream(
								new FileInputStream(file)));
					else if (file.getName().endsWith(".zip"))
						input = new BufferedInputStream(new ZipInputStream(
								new FileInputStream(file)));
					else if (file.getName().endsWith(".json"))
						input = new BufferedInputStream((new FileInputStream(
								file)));
					else
						continue;

					LOG.info("Begin Loading File:" + file.getAbsolutePath());

					HBaseBulkPut(input, col_family, col_qualifier);
					LOG.info("Completed Loading File:" + file.getAbsolutePath());

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (file.isDirectory()) // if sub-directory then call the
											// function recursively
				this.LoadDirHBase(file.getAbsolutePath());
		}
	}

	private void LoadDirHBase(String dirname) {

		this.dirName = dirname;
		this.LoadDirHBase();

	}

	/**
	 * @param input
	 * @param hbaseTable
	 * @param col_family
	 * @throws IOException
	 * @throws ParseException
	 * 
	 * 
	 *     Inserts all json records picked up from the inputStream
	 */
	private void HBaseBulkPut(InputStream input, String col_family,
			String col_qualifier) throws IOException, ParseException {

		HTable table = new HTable(conf, hbaseTable);
		JSONParser parser = new JSONParser();

		BufferedReader br = new BufferedReader(new InputStreamReader(input));
		String jsonString;
		List<Put> allputs = new ArrayList<Put>();
		Map json;

		while ((jsonString = br.readLine()) != null) {

			try {

				json = (Map) parser.parse(jsonString);
			} catch (ParseException e) {
				// System.out.println("Unable to Parse: " +jsonString);
				continue;
			}
			// Iterator iter = json.entrySet().iterator();

			// Get Address - either IP/domain or email and make that the Key
			Put put = new Put(Bytes.toBytes((String) json.get("address")));

			// We are just adding a "Y" flag to mark this address
			put.add(Bytes.toBytes(col_family), Bytes.toBytes(col_qualifier),
					Bytes.toBytes("Y"));

			allputs.add(put);
		}
		table.put(allputs);
		table.close();
	}

	private void printUsage() {
		System.out
				.println("Usage: java -cp JarFile com.opensoc.dataloads.cif.HBaseTableLoad -d <directory> -t <tablename> -f <optional file-list>");
	}

	private void parse(String[] args) {
		CommandLineParser parser = new BasicParser();
		Options options = new Options();

		options.addOption("d", true, "description");
		options.addOption("t", true, "description");
		options.addOption("f", false, "description");

		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);

			if (cmd.hasOption("d"))
			{
				this.dirName = cmd.getOptionValue("d");
				LOG.info("Directory Name:" + cmd.getOptionValue("d"));
			}
			else {
				LOG.info("Missing Directory Name");
				printUsage();
				System.exit(-1);
			}

			if (cmd.hasOption("t"))
			{
				this.hbaseTable = cmd.getOptionValue("t");
				LOG.info("HBase Table Name:" + cmd.getOptionValue("t"));
			}
			else {
				LOG.info("Missing Table Name");
				printUsage();
				System.exit(-1);
			}

			if (cmd.hasOption("f")) {
				this.usefileList = true;
				files = LoadFileList(cmd.getOptionValue("f"));
				LOG.info("FileList:" + cmd.getOptionValue("f"));
			}

		} catch (org.apache.commons.cli.ParseException e) {
			LOG.error("Failed to parse comand line properties", e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private Set<String> LoadFileList(String filename) {

		Set<String> output = null;
		BufferedReader reader;

		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename)));
			output = new HashSet<String>();
			String in = "";

			while ((in = reader.readLine()) != null)
				output.add(in);

			reader.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return output;
	}

}