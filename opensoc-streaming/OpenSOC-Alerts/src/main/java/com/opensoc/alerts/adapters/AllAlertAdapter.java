package com.opensoc.alerts.adapters;

import java.io.IOException;
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
import org.apache.hadoop.hbase.client.Get;
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
import com.opensoc.alerts.interfaces.AlertsAdapter;

public class AllAlertAdapter implements AlertsAdapter, Serializable {

	HTableInterface whitelist_table;
	InetAddressValidator ipvalidator = new InetAddressValidator();
	String _whitelist_table_name;
	// String _blacklist_table_name;
	String _quorum;
	String _port;
	String _topologyname;
	Configuration conf = null;

	protected  Cache<String, String> cache;

	Map<String, String> id_list = new HashMap<String, String>();

	Set<String> loaded_whitelist = new HashSet<String>();
	Set<String> loaded_blacklist = new HashSet<String>();

	String _topology_name;

	protected static final Logger LOG = LoggerFactory
			.getLogger(AllAlertAdapter.class);

	public AllAlertAdapter(String whitelist_table_name,
			String blacklist_table_name, String quorum, String port,
			int _MAX_TIME_RETAIN, int _MAX_CACHE_SIZE) {

		_whitelist_table_name = whitelist_table_name;

		_quorum = quorum;
		_port = port;

		cache = CacheBuilder.newBuilder().maximumSize(_MAX_CACHE_SIZE)
				.expireAfterWrite(_MAX_TIME_RETAIN, TimeUnit.MINUTES).build();


	}


	public boolean initialize() {

		conf = HBaseConfiguration.create();
		conf.set("hbase.zookeeper.quorum", _quorum);
		conf.set("hbase.zookeeper.property.clientPort", _port);

		LOG.trace("[OpenSOC] Connecting to hbase with conf:" + conf);		
		LOG.trace("[OpenSOC] Whitelist table name: " + _whitelist_table_name);
		LOG.trace("[OpenSOC] ZK Client/port: " + conf.get("hbase.zookeeper.quorum") + " -> " + conf.get("hbase.zookeeper.property.clientPort"));

		try {

			HConnection connection = HConnectionManager.createConnection(conf);

			LOG.trace("[OpenSOC] CONNECTED TO HBASE");

			HBaseAdmin hba = new HBaseAdmin(conf);

			if (!hba.tableExists(_whitelist_table_name))
				throw new Exception("Whitelist table doesn't exist");

			whitelist_table = new HTable(conf, _whitelist_table_name);

			LOG.trace("[OpenSOC] CONNECTED TO TABLE: "+ _whitelist_table_name);

			Scan scan = new Scan();


			ResultScanner rs = whitelist_table.getScanner(scan);
			try {
				for (Result r = rs.next(); r != null; r = rs.next()) {
					loaded_whitelist.add(Bytes.toString(r.getRow()));
				}
			} catch (Exception e) {
				LOG.trace("[OpenSOC] COULD NOT READ FROM HBASE");
				e.printStackTrace();
			} finally {
				rs.close(); 
			}
			whitelist_table.close();

			LOG.trace("[OpenSOC] Number of entires in white list: " + loaded_whitelist.size());
			
			if(loaded_whitelist.size() == 0)
				throw new Exception("Hbase connection is OK, but the table is empty: " + whitelist_table);

			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
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

	public boolean getByKey(String metadata, HTableInterface table) {

		LOG.trace("[OpenSOC] Pinging HBase For:" + metadata);


		Get get = new Get(metadata.getBytes());
		Result rs;

		try {
			rs = table.get(get);

			if (rs.size() > 0)
				return true;
			else
				return false;

		} catch (IOException e) {

			e.printStackTrace();
		}

		return false;

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
		JSONObject alert = new JSONObject();

		JSONObject content = (JSONObject) raw_message.get("message");
		String source_ip = content.get("ip_src_addr").toString();
		String dst_ip = content.get("ip_dst_addr").toString();

		String source = null;

		if (loaded_whitelist.contains(source_ip))
			source = source_ip;
		else if (loaded_whitelist.contains(dst_ip))
			source = dst_ip;
		else
			source = "unknown";

		alert.put("title", "Appliance alert for: " + source_ip + "->" + dst_ip);
		alert.put("priority", "1");
		alert.put("type", "error");
		alert.put("designated_host", source);
		alert.put("source", source_ip);
		alert.put("dest", dst_ip);
		alert.put("body", "Appliance alert for: " + source_ip + "->" + dst_ip);

		String alert_id = generateAlertId(source_ip, dst_ip, 0);

		alert.put("reference_id", alert_id);
		alerts.put(alert_id, alert);
		
		LOG.trace("[OpenSOC] Returning alert: " + alerts);

		 return alerts;
	}


	public boolean containsAlertId(String alert) {
		// TODO Auto-generated method stub
		return false;
	}

}
