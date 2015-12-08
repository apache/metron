package com.opensoc.dataloads.cif;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.conf.Configuration;

import java.io.BufferedInputStream;

public class HBaseTableLoad {

	private static Configuration conf = null;
	private final static String hbaseTable = "cif_table";
	/**
	 * Initialization
	 */
	static {
		conf = HBaseConfiguration.create();
	}

	public static void main(String[] args) {

		LoadDirHBase(args[0]);

	}

	public static void LoadDirHBase(String dirName) {
		System.out.println("Working on:" + dirName);
		File folder = new File(dirName);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			File file = listOfFiles[i];

			if (file.isFile() && file.getName().endsWith(".gz")) {

				// e.g. folder name is infrastructure_botnet. Col Qualifier is
				// botnet and col_family is infrastructure

				String col_family = folder.getName().split("_")[0];
				String col_qualifier = folder.getName().split("_")[1];

				// Open gz file
				try {
					InputStream input = new BufferedInputStream(
							new GZIPInputStream(new FileInputStream(file)));

					HBaseBulkPut(input, col_family, col_qualifier);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (file.isDirectory()) // if sub-directory then call the
											// function recursively
				LoadDirHBase(file.getAbsolutePath());
		}
	}

	/**
	 * @param input
	 * @param hbaseTable
	 * @param col_family
	 * @throws IOException
	 * @throws ParseException
	 * 
	 * 
	 *             Inserts all json records picked up from the inputStream
	 */
	public static void HBaseBulkPut(InputStream input, String col_family,
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
				//System.out.println("Unable to Parse: " +jsonString);
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
		System.out.println("---------------Values------------------"
				+ hbaseTable);
		table.close();
	}
}