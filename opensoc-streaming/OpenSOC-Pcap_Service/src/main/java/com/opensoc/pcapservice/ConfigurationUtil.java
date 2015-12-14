package com.opensoc.pcapservice;

import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.util.Assert;

import com.opensoc.configuration.ConfigurationManager;



/**
 * utility class for this module which loads commons configuration to fetch
 * properties from underlying resources to communicate with hbase.
 * 
 * @author Sayi
 */
public class ConfigurationUtil {

	/** Configuration definition file name for fetching pcaps from hbase */
	private static final String configDefFileName = "config-definition-hbase.xml";
	
	/** property configuration. */
	private static Configuration propConfiguration = null;


	/**
	 * The Enum SizeUnit.
	 */
	public enum SizeUnit {

		/** The kb. */
		KB,
		/** The mb. */
		MB
	};

	/** The Constant DEFAULT_HCONNECTION_RETRY_LIMIT. */
	private static final int DEFAULT_HCONNECTION_RETRY_LIMIT = 0;

	/**
	 * Loads configuration resources 
	 * @return Configuration
	 */
	public static Configuration getConfiguration() {
		if(propConfiguration == null){
			propConfiguration =  ConfigurationManager.getConfiguration(configDefFileName);
		}
		return propConfiguration;
	}

	/**
	 * Returns the configured default result size in bytes, if the user input is
	 * null; otherwise, returns the user input after validating with the
	 * configured max value. Throws IllegalArgumentException if : 1. input is
	 * less than or equals to 0 OR 2. input is greater than configured
	 * {hbase.scan.max.result.size} value
	 * 
	 * @param input
	 *            the input
	 * @return long
	 */
	public static long validateMaxResultSize(String input) {
		if (input == null) {
			return getDefaultResultSize();
		}
		// validate the user input
		long value = convertToBytes(Long.parseLong(input), getResultSizeUnit());
		Assert.isTrue(
				isAllowableResultSize(value),
				"'maxResponseSize' param value must be positive and less than {hbase.scan.max.result.size} value");
		return convertToBytes(value, getResultSizeUnit());
	}

	/**
	 * Checks if is allowable result size.
	 * 
	 * @param input
	 *            the input
	 * @return true, if is allowable result size
	 */
	public static boolean isAllowableResultSize(long input) {
		if (input <= 0 || input > getMaxResultSize()) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the configured default result size in bytes.
	 * 
	 * @return long
	 */
	public static long getDefaultResultSize() {
		float value = ConfigurationUtil.getConfiguration().getFloat(
				"hbase.scan.default.result.size");
		return convertToBytes(value, getResultSizeUnit());
	}

	/**
	 * Returns the configured max result size in bytes.
	 * 
	 * @return long
	 */
	public static long getMaxResultSize() {
		float value = ConfigurationUtil.getConfiguration().getFloat(
				"hbase.scan.max.result.size");
		return convertToBytes(value, getResultSizeUnit());
	}

	/**
	 * Returns the configured max row size in bytes.
	 * 
	 * @return long
	 */
	public static long getMaxRowSize() {
		float maxRowSize = ConfigurationUtil.getConfiguration().getFloat(
				"hbase.table.max.row.size");
		return convertToBytes(maxRowSize, getRowSizeUnit());
	}

	/**
	 * Gets the result size unit.
	 * 
	 * @return the result size unit
	 */
	public static SizeUnit getResultSizeUnit() {
		return SizeUnit.valueOf(ConfigurationUtil.getConfiguration()
				.getString("hbase.scan.result.size.unit"));
	}

	/**
	 * Gets the row size unit.
	 * 
	 * @return the row size unit
	 */
	public static SizeUnit getRowSizeUnit() {
		return SizeUnit.valueOf(ConfigurationUtil.getConfiguration()
				.getString("hbase.table.row.size.unit"));
	}

	/**
	 * Gets the connection retry limit.
	 * 
	 * @return the connection retry limit
	 */
	public static int getConnectionRetryLimit() {
		return ConfigurationUtil.getConfiguration().getInt(
				"hbase.hconnection.retries.number",
				DEFAULT_HCONNECTION_RETRY_LIMIT);
	}

	/**
	 * Checks if is default include reverse traffic.
	 * 
	 * @return true, if is default include reverse traffic
	 */
	public static boolean isDefaultIncludeReverseTraffic() {
		return ConfigurationUtil.getConfiguration().getBoolean(
				"pcaps.include.reverse.traffic");
	}

	/**
	 * Gets the table name.
	 * 
	 * @return the table name
	 */
	public static byte[] getTableName() {
		return Bytes.toBytes(ConfigurationUtil.getConfiguration().getString(
				"hbase.table.name"));
	}

	/**
	 * Gets the column family.
	 * 
	 * @return the column family
	 */
	public static byte[] getColumnFamily() {
		return Bytes.toBytes(ConfigurationUtil.getConfiguration().getString(
				"hbase.table.column.family"));
	}

	/**
	 * Gets the column qualifier.
	 * 
	 * @return the column qualifier
	 */
	public static byte[] getColumnQualifier() {
		return Bytes.toBytes(ConfigurationUtil.getConfiguration().getString(
				"hbase.table.column.qualifier"));
	}

	/**
	 * Gets the max versions.
	 * 
	 * @return the max versions
	 */
	public static int getMaxVersions() {
		return ConfigurationUtil.getConfiguration().getInt(
				"hbase.table.column.maxVersions");
	}

	/**
	 * Gets the configured tokens in rowkey.
	 * 
	 * @return the configured tokens in rowkey
	 */
	public static int getConfiguredTokensInRowkey() {
		return ConfigurationUtil.getConfiguration().getInt(
				"hbase.table.row.key.tokens");
	}

	/**
	 * Gets the minimum tokens in inputkey.
	 * 
	 * @return the minimum tokens in inputkey
	 */
	public static int getMinimumTokensInInputkey() {
		return ConfigurationUtil.getConfiguration().getInt(
				"rest.api.input.key.min.tokens");
	}

	/**
	 * Gets the appending token digits.
	 * 
	 * @return the appending token digits
	 */
	public static int getAppendingTokenDigits() {
		return ConfigurationUtil.getConfiguration().getInt(
				"hbase.table.row.key.token.appending.digits");
	}

	/**
	 * Convert to bytes.
	 * 
	 * @param value
	 *            the value
	 * @param unit
	 *            the unit
	 * @return the long
	 */
	public static long convertToBytes(float value, SizeUnit unit) {
		if (SizeUnit.KB == unit) {
			return (long) (value * 1024);
		}
		if (SizeUnit.MB == unit) {
			return (long) (value * 1024 * 1024);
		}
		return (long) value;
	}

	/**
	 * The main method.
	 * 
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		long r1 = getMaxRowSize();
		System.out.println("getMaxRowSizeInBytes = " + r1);
		long r2 = getMaxResultSize();
		System.out.println("getMaxAllowableResultSizeInBytes = " + r2);

		SizeUnit u1 = getRowSizeUnit();
		System.out.println("getMaxRowSizeUnit = " + u1.toString());
		SizeUnit u2 = getResultSizeUnit();
		System.out.println("getMaxAllowableResultsSizeUnit = " + u2.toString());
	}

}
