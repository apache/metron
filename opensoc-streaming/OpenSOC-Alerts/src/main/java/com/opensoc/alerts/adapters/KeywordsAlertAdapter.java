package com.opensoc.alerts.adapters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.log4j.Logger;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.opensoc.alerts.interfaces.AlertsAdapter;

public class KeywordsAlertAdapter extends AbstractAlertAdapter {

	HTableInterface blacklist_table;
	HTableInterface whitelist_table;
	InetAddressValidator ipvalidator = new InetAddressValidator();
	String _whitelist_table_name;
	String _blacklist_table_name;
	String _quorum;
	String _port;
	String _topologyname;
	Configuration conf = null;

	String _topology_name;

	Set<String> loaded_whitelist = new HashSet<String>();
	Set<String> loaded_blacklist = new HashSet<String>();

	List<String> keywordList;
	List<String> keywordExceptionList;
	
	protected static final Logger LOG = Logger.getLogger(AllAlertAdapter.class);
	
	public KeywordsAlertAdapter(Map<String, String> config) {
		try {
			
			if(!config.containsKey("keywords"))
				throw new Exception("Keywords are missing");
			
			keywordList = Arrays.asList(config.get("keywords").split("\\|"));
			
			if(	config.containsKey("exceptions")) {
				keywordExceptionList = Arrays.asList(config.get("exceptions").split("\\|"));
			} else {
				keywordExceptionList = new ArrayList<String>();
			}
				
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

			generateCache(_MAX_CACHE_SIZE_OBJECTS_NUM, _MAX_TIME_RETAIN_MINUTES);
			
		} catch (Exception e) {
			System.out.println("Could not initialize Alerts Adapter");
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	@Override
	public boolean initialize() {
		conf = HBaseConfiguration.create();
		//conf.set("hbase.zookeeper.quorum", _quorum);
		//conf.set("hbase.zookeeper.property.clientPort", _port);

		LOG.trace("[OpenSOC] Connecting to hbase with conf:" + conf);
		LOG.trace("[OpenSOC] Whitelist table name: " + _whitelist_table_name);
		LOG.trace("[OpenSOC] Whitelist table name: " + _blacklist_table_name);
		LOG.trace("[OpenSOC] ZK Client/port: "
				+ conf.get("hbase.zookeeper.quorum") + " -> "
				+ conf.get("hbase.zookeeper.property.clientPort"));

		try {

			LOG.trace("[OpenSOC] Attempting to connect to hbase");

			HConnection connection = HConnectionManager.createConnection(conf);

			LOG.trace("[OpenSOC] CONNECTED TO HBASE");

			HBaseAdmin hba = new HBaseAdmin(conf);

			if (!hba.tableExists(_whitelist_table_name))
				throw new Exception("Whitelist table doesn't exist");

			if (!hba.tableExists(_blacklist_table_name))
				throw new Exception("Blacklist table doesn't exist");

			whitelist_table = new HTable(conf, _whitelist_table_name);

			LOG.trace("[OpenSOC] CONNECTED TO TABLE: " + _whitelist_table_name);
			blacklist_table = new HTable(conf, _blacklist_table_name);
			LOG.trace("[OpenSOC] CONNECTED TO TABLE: " + _blacklist_table_name);

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
				LOG.trace("[OpenSOC] COULD NOT READ FROM HBASE");
				e.printStackTrace();
			} finally {
				rs.close(); // always close the ResultScanner!
				hba.close();
			}
			whitelist_table.close();

			LOG.trace("[OpenSOC] READ IN WHITELIST: " + loaded_whitelist.size());
			
			System.out.println("LOADED WHITELIST IS: ");
			
			for(String str: loaded_whitelist)
				System.out.println("WHITELIST: " + str);

			scan = new Scan();

			rs = blacklist_table.getScanner(scan);
			try {
				for (Result r = rs.next(); r != null; r = rs.next()) {
					loaded_blacklist.add(Bytes.toString(r.getRow()));
				}
			} catch (Exception e) {
				LOG.trace("[OpenSOC] COULD NOT READ FROM HBASE");
				e.printStackTrace();
			} finally {
				rs.close(); // always close the ResultScanner!
				hba.close();
			}
			blacklist_table.close();

			LOG.trace("[OpenSOC] READ IN WHITELIST: " + loaded_whitelist.size());

			rs.close(); // always close the ResultScanner!
			hba.close();

			return true;
		} catch (Exception e) {

			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean refresh() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAlertId(String alert) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, JSONObject> alert(JSONObject raw_message) {
		
		Map<String, JSONObject> alerts = new HashMap<String, JSONObject>();
		JSONObject content = (JSONObject) raw_message.get("message");

		JSONObject enrichment = null;
		if (raw_message.containsKey("enrichment"))
			enrichment = (JSONObject) raw_message.get("enrichment");

		for (String keyword : keywordList) {
			if (content.toString().contains(keyword)) {
				
				//check it doesn't have an "exception" keyword in it
				for (String exception : keywordExceptionList) {
					if (content.toString().contains(exception)) {
						LOG.info("[OpenSOC] KeywordAlertsAdapter: Omitting alert due to exclusion: " + exception);
						return null;
					}
				}
				
				LOG.info("[OpenSOC] KeywordAlertsAdapter: Found match for " + keyword);
				JSONObject alert = new JSONObject();

				String source = "unknown";
				String dest = "unknown";
				String host = "unknown";

				if (content.containsKey("ip_src_addr"))
				{
					source = content.get("ip_src_addr").toString();
					
					if(RangeChecker.checkRange(loaded_whitelist, source))
						host = source;				
				}

				if (content.containsKey("ip_dst_addr"))
				{
					dest = content.get("ip_dst_addr").toString();
					
					if(RangeChecker.checkRange(loaded_whitelist, dest))
						host = dest;	
				}

				alert.put("designated_host", host);
				alert.put("description", content.get("original_string").toString());
				alert.put("priority", "MED");	

				String alert_id = generateAlertId(source, dest, 0);

				alert.put("alert_id", alert_id);
				alerts.put(alert_id, alert);

				alert.put("enrichment", enrichment);

				return alerts;
			}
		}
		
		return null;
	}

}
