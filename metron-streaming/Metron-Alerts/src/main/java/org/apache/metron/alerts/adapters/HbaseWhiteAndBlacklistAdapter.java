package org.apache.metron.alerts.adapters;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HConnectionManager;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.metron.alerts.interfaces.AlertsAdapter;

public class HbaseWhiteAndBlacklistAdapter implements AlertsAdapter,
		Serializable {

	HTableInterface blacklist_table;
	HTableInterface whitelist_table;
	InetAddressValidator ipvalidator = new InetAddressValidator();
	String _whitelist_table_name;
	String _blacklist_table_name;
	String _quorum;
	String _port;
	String _topologyname;
	Configuration conf = null;

	Cache<String, String> cache;
	String _topology_name;

	Set<String> loaded_whitelist = new HashSet<String>();
	Set<String> loaded_blacklist = new HashSet<String>();

	protected static final Logger LOG = LoggerFactory
			.getLogger(HbaseWhiteAndBlacklistAdapter.class);

	public HbaseWhiteAndBlacklistAdapter(Map<String, String> config) {

		try {
			if(!config.containsKey("whitelist_table_name"))
				throw new Exception("Whitelist table name is missing");
				
			_whitelist_table_name = config.get("whitelist_table_name");
			
			if(!config.containsKey("blacklist_table_name"))
				throw new Exception("Blacklist table name is missing");
			
			_blacklist_table_name = config.get("blacklist_table_name");
			
			if(!config.containsKey("quorum"))
				throw new Exception("Quorum name is missing");
			
			_quorum = config.get("quorum");
			
			if(!config.containsKey("port"))
				throw new Exception("port name is missing");
			
			_port = config.get("port");

			if(!config.containsKey("_MAX_CACHE_SIZE_OBJECTS_NUM"))
				throw new Exception("_MAX_CACHE_SIZE_OBJECTS_NUM name is missing");
			
			int _MAX_CACHE_SIZE_OBJECTS_NUM = Integer.parseInt(config
					.get("_MAX_CACHE_SIZE_OBJECTS_NUM"));
			
			if(!config.containsKey("_MAX_TIME_RETAIN_MINUTES"))
				throw new Exception("_MAX_TIME_RETAIN_MINUTES name is missing");
			
			int _MAX_TIME_RETAIN_MINUTES = Integer.parseInt(config
					.get("_MAX_TIME_RETAIN_MINUTES"));

			cache = CacheBuilder.newBuilder().maximumSize(_MAX_CACHE_SIZE_OBJECTS_NUM)
					.expireAfterWrite(_MAX_TIME_RETAIN_MINUTES, TimeUnit.MINUTES)
					.build();
		} catch (Exception e) {
			System.out.println("Could not initialize Alerts Adapter");
			e.printStackTrace();
			System.exit(0);
		}

	}

	public boolean initialize() {

		conf = HBaseConfiguration.create();
		//conf.set("hbase.zookeeper.quorum", _quorum);
		//conf.set("hbase.zookeeper.property.clientPort", _port);

		LOG.trace("[Metron] Connecting to hbase with conf:" + conf);
		LOG.trace("[Metron] Whitelist table name: " + _whitelist_table_name);
		LOG.trace("[Metron] Whitelist table name: " + _blacklist_table_name);
		LOG.trace("[Metron] ZK Client/port: "
				+ conf.get("hbase.zookeeper.quorum") + " -> "
				+ conf.get("hbase.zookeeper.property.clientPort"));

		try {

			LOG.trace("[Metron] Attempting to connect to hbase");

			HConnection connection = HConnectionManager.createConnection(conf);

			LOG.trace("[Metron] CONNECTED TO HBASE");

			HBaseAdmin hba = new HBaseAdmin(conf);

			if (!hba.tableExists(_whitelist_table_name))
				throw new Exception("Whitelist table doesn't exist");

			if (!hba.tableExists(_blacklist_table_name))
				throw new Exception("Blacklist table doesn't exist");

			whitelist_table = new HTable(conf, _whitelist_table_name);

			LOG.trace("[Metron] CONNECTED TO TABLE: " + _whitelist_table_name);
			blacklist_table = new HTable(conf, _blacklist_table_name);
			LOG.trace("[Metron] CONNECTED TO TABLE: " + _blacklist_table_name);

			if (connection == null || whitelist_table == null
					|| blacklist_table == null)
				throw new Exception("Unable to initialize hbase connection");

			Scan scan = new Scan();

			ResultScanner rs = whitelist_table.getScanner(scan);
			try {
				for (Result r = rs.next(); r != null; r = rs.next()) {
					loaded_whitelist.add(Bytes.toString(r.getRow()));
				}
			} catch (Exception e) {
				LOG.trace("[Metron] COULD NOT READ FROM HBASE");
				e.printStackTrace();
			} finally {
				rs.close(); // always close the ResultScanner!
				hba.close();
			}
			whitelist_table.close();

			LOG.trace("[Metron] READ IN WHITELIST: " + loaded_whitelist.size());

			scan = new Scan();

			rs = blacklist_table.getScanner(scan);
			try {
				for (Result r = rs.next(); r != null; r = rs.next()) {
					loaded_blacklist.add(Bytes.toString(r.getRow()));
				}
			} catch (Exception e) {
				LOG.trace("[Metron] COULD NOT READ FROM HBASE");
				e.printStackTrace();
			} finally {
				rs.close(); // always close the ResultScanner!
				hba.close();
			}
			blacklist_table.close();

			LOG.trace("[Metron] READ IN WHITELIST: " + loaded_whitelist.size());

			rs.close(); // always close the ResultScanner!
			hba.close();

			return true;
		} catch (Exception e) {

			e.printStackTrace();
		}

		return false;

	}

	protected String generateAlertId(String source_ip, String dst_ip,
			int alert_type) {

		String key = makeKey(source_ip, dst_ip, alert_type);

		if (cache.getIfPresent(key) != null)
			return cache.getIfPresent(key);

		String new_UUID = System.currentTimeMillis() + "-" + UUID.randomUUID();

		cache.put(key, new_UUID);
		key = makeKey(dst_ip, source_ip, alert_type);
		cache.put(key, new_UUID);

		return new_UUID;

	}

	public boolean refresh() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	private String makeKey(String ip1, String ip2, int alert_type) {
		return (ip1 + "-" + ip2 + "-" + alert_type);
	}

	@SuppressWarnings("unchecked")
	public Map<String, JSONObject> alert(JSONObject raw_message) {

		Map<String, JSONObject> alerts = new HashMap<String, JSONObject>();

		JSONObject content = (JSONObject) raw_message.get("message");
		JSONObject enrichment = null;

		if (raw_message.containsKey("enrichment"))
			enrichment = (JSONObject) raw_message.get("enrichment");

		if (!content.containsKey("ip_src_addr")
				|| !content.containsKey("ip_dst_addr")) {

			int alert_type = 0;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", "Uknown");
			alert.put("source", "NA");
			alert.put("dest", "NA");
			alert.put("body", "Source or destination IP is missing");

			String alert_id = UUID.randomUUID().toString();

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);

			if (enrichment != null)
				alert.put("enrichment", enrichment);

			LOG.trace("[Metron] Returning alert: " + alerts);

			return alerts;

		}

		String source_ip = content.get("ip_src_addr").toString();
		String dst_ip = content.get("ip_dst_addr").toString();

		if (source_ip == null && dst_ip == null) {

			int alert_type = 1;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", "Uknown");
			alert.put("source", source_ip);
			alert.put("dest", dst_ip);
			alert.put(
					"body",
					"This communication does not contain a source or destination IP string. Communication between two IPs: "
							+ source_ip + " -> " + dst_ip);

			String alert_id = generateAlertId(source_ip, dst_ip, alert_type);

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);
			if (enrichment != null)
				alert.put("enrichment", enrichment);

			LOG.trace("[Metron] Returning alert: " + alerts);

			return alerts;

		}

		if (!ipvalidator.isValidInet4Address(source_ip)
				&& !ipvalidator.isValidInet4Address(dst_ip)) {
			int alert_type = 2;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", "Uknown");
			alert.put("source", source_ip);
			alert.put("dest", dst_ip);
			alert.put(
					"content",
					"This communication contains souce and destination IP strings, but these strings are not valid. Communication between two IPs: "
							+ source_ip + " -> " + dst_ip);

			String alert_id = generateAlertId(source_ip, dst_ip, alert_type);

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);
			if (enrichment != null)
				alert.put("enrichment", enrichment);

			LOG.trace("[Metron] Returning alert: " + alerts);

			return alerts;

		}

		String designated_host = null;

		if (loaded_whitelist.contains(source_ip))
			designated_host = source_ip;
		else if (loaded_whitelist.contains(dst_ip))
			designated_host = dst_ip;

		if (designated_host == null) {
			int alert_type = 3;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", "Uknown");
			alert.put("source", source_ip);
			alert.put("dest", dst_ip);
			alert.put(
					"content",
					"This communication does not contain a source or a destination IP that is in the white list. Communication between two IPs: "
							+ source_ip + " -> " + dst_ip);

			String alert_id = generateAlertId(source_ip, dst_ip, alert_type);

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);
			if (enrichment != null)
				alert.put("enrichment", enrichment);

			LOG.trace("[Metron] Returning alert: " + alerts);

			return alerts;

		}

		if (source_ip.equals(designated_host)
				&& !ipvalidator.isValidInet4Address(dst_ip)) {
			int alert_type = 4;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", designated_host);
			alert.put("source", source_ip);
			alert.put("dest", dst_ip);
			alert.put(
					"content",
					"This communication contains an IP that is not valid. Communication between two IPs: "
							+ source_ip + " -> " + dst_ip);

			String alert_id = generateAlertId(source_ip, dst_ip, alert_type);

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);
			if (enrichment != null)
				alert.put("enrichment", enrichment);

		}

		if (dst_ip.equals(designated_host)
				&& !ipvalidator.isValidInet4Address(source_ip)) {
			int alert_type = 5;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", designated_host);
			alert.put("source", source_ip);
			alert.put("dest", dst_ip);
			alert.put(
					"content",
					"This communication contains IP that is not valid. Communication between two IPs: "
							+ source_ip + " -> " + dst_ip);

			String alert_id = generateAlertId(source_ip, dst_ip, alert_type);

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);
			if (enrichment != null)
				alert.put("enrichment", enrichment);

		}

		if (loaded_blacklist.contains(source_ip)) {
			int alert_type = 6;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", designated_host);
			alert.put("source", source_ip);
			alert.put("dest", dst_ip);
			alert.put(
					"content",
					"This communication contains IP that is black listed. Communication between two IPs: "
							+ source_ip + " -> " + dst_ip);

			String alert_id = generateAlertId(source_ip, dst_ip, alert_type);

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);
			if (enrichment != null)
				alert.put("enrichment", enrichment);

		}

		if (loaded_blacklist.contains(dst_ip)) {
			int alert_type = 7;

			JSONObject alert = new JSONObject();

			alert.put("title", "IP Check Error Type: : " + alert_type);
			alert.put("priority", "1");
			alert.put("type", "error");
			alert.put("designated_host", designated_host);
			alert.put("source", source_ip);
			alert.put("dest", dst_ip);
			alert.put(
					"content",
					"This communication contains IP that is black listed. Communication between two IPs: "
							+ source_ip + " -> " + dst_ip);

			String alert_id = generateAlertId(source_ip, dst_ip, alert_type);

			alert.put("reference_id", alert_id);
			alerts.put(alert_id, alert);
			if (enrichment != null)
				alert.put("enrichment", enrichment);

		}

		if (alerts.isEmpty())
			return null;
		else
			return alerts;
	}

	public boolean containsAlertId(String alert) {
		// TODO Auto-generated method stub
		return false;
	}

}
