/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opensoc.topology.runner;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import oi.thekraken.grok.api.Grok;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.hdfs.common.rotation.MoveFileAction;
import org.json.simple.JSONObject;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.ZkHosts;
import storm.kafka.bolt.KafkaBolt;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.Grouping;
import backtype.storm.spout.RawScheme;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.kryo.serializers.MapSerializer;



import com.opensoc.alerts.TelemetryAlertsBolt;
import com.opensoc.alerts.adapters.HbaseWhiteAndBlacklistAdapter;
import com.opensoc.alerts.interfaces.AlertsAdapter;
import com.opensoc.enrichment.adapters.cif.CIFHbaseAdapter;
import com.opensoc.enrichment.adapters.geo.GeoMysqlAdapter;
import com.opensoc.enrichment.adapters.host.HostFromPropertiesFileAdapter;
import com.opensoc.enrichment.adapters.whois.WhoisHBaseAdapter;
import com.opensoc.enrichment.adapters.threat.ThreatHbaseAdapter;
import com.opensoc.enrichment.common.GenericEnrichmentBolt;
import com.opensoc.enrichment.interfaces.EnrichmentAdapter;
import com.opensoc.hbase.HBaseBolt;
import com.opensoc.hbase.HBaseStreamPartitioner;
import com.opensoc.hbase.TupleTableConfig;
import com.opensoc.helpers.topology.Cli;
import com.opensoc.helpers.topology.SettingsLoader;
import com.opensoc.index.interfaces.IndexAdapter;
import com.opensoc.indexing.TelemetryIndexingBolt;
import com.opensoc.json.serialization.JSONKryoSerializer;

public abstract class TopologyRunner {

	protected Configuration config;
	protected TopologyBuilder builder;
	protected Config conf;
	protected boolean local_mode = true;
	protected boolean debug = true;
	protected String config_path = null;
	protected String default_config_path = "OpenSOC_Configs";
	protected boolean success = false;
	protected Stack<String> messageComponents = new Stack<String>();
	protected Stack<String> errorComponents = new Stack<String>();
	protected Stack<String> alertComponents = new Stack<String>();
	protected Stack<String> dataComponents = new Stack<String>();
	protected Stack<String> terminalComponents = new Stack<String>();

	public void initTopology(String args[], String subdir)
			throws Exception {
		Cli command_line = new Cli(args);
		command_line.parse();

		System.out.println("[OpenSOC] Starting topology deployment...");

		debug = command_line.isDebug();
		System.out.println("[OpenSOC] Debug mode set to: " + debug);

		local_mode = command_line.isLocal_mode();
		System.out.println("[OpenSOC] Local mode set to: " + local_mode);

		if (command_line.getPath() != null) {
			config_path = command_line.getPath();
			System.out
					.println("[OpenSOC] Setting config path to external config path: "
							+ config_path);
		} else {
			config_path = default_config_path;
			System.out
					.println("[OpenSOC] Initializing from default internal config path: "
							+ config_path);
		}

		String topology_conf_path = config_path + "/topologies/" + subdir
				+ "/topology.conf";

		String environment_identifier_path = config_path
				+ "/topologies/environment_identifier.conf";
		String topology_identifier_path = config_path + "/topologies/" + subdir
				+ "/topology_identifier.conf";

		System.out.println("[OpenSOC] Looking for environment identifier: "
				+ environment_identifier_path);
		System.out.println("[OpenSOC] Looking for topology identifier: "
				+ topology_identifier_path);
		System.out.println("[OpenSOC] Looking for topology config: "
				+ topology_conf_path);

		config = new PropertiesConfiguration(topology_conf_path);

		JSONObject environment_identifier = SettingsLoader
				.loadEnvironmentIdnetifier(environment_identifier_path);
		JSONObject topology_identifier = SettingsLoader
				.loadTopologyIdnetifier(topology_identifier_path);

		String topology_name = SettingsLoader.generateTopologyName(
				environment_identifier, topology_identifier);

		System.out.println("[OpenSOC] Initializing Topology: " + topology_name);

		builder = new TopologyBuilder();

		conf = new Config();
		conf.registerSerialization(JSONObject.class, MapSerializer.class);
		conf.setDebug(debug);

		System.out.println("[OpenSOC] Initializing Spout: " + topology_name);

		if (command_line.isGenerator_spout()) {
			String component_name = config.getString("spout.test.name",
					"DefaultTopologySpout");
			success = initializeTestingSpout(component_name);
			messageComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"spout.test");
		}

		if (!command_line.isGenerator_spout()) {
			String component_name = config.getString("spout.kafka.name",
					"DefaultTopologyKafkaSpout");

			success = initializeKafkaSpout(component_name);
			messageComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"spout.kafka");
		}

		if (config.getBoolean("bolt.parser.enabled", true)) {
			String component_name = config.getString("bolt.parser.name",
					"DefaultTopologyParserBot");

			success = initializeParsingBolt(topology_name, component_name);
			messageComponents.add(component_name);
			errorComponents.add(component_name);

			dataComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.parser");
		}

		if (config.getBoolean("bolt.enrichment.geo.enabled", false)) {
			String component_name = config.getString(
					"bolt.enrichment.geo.name", "DefaultGeoEnrichmentBolt");

			success = initializeGeoEnrichment(topology_name, component_name);
			messageComponents.add(component_name);
			errorComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.enrichment.geo");
			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"mysql");
		}

		if (config.getBoolean("bolt.enrichment.host.enabled", false)) {
			String component_name = config.getString(
					"bolt.enrichment.host.name", "DefaultHostEnrichmentBolt");

			success = initializeHostsEnrichment(topology_name, component_name,
					"OpenSOC_Configs/etc/whitelists/known_hosts.conf");
			messageComponents.add(component_name);
			errorComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.enrichment.host");
		}

		if (config.getBoolean("bolt.enrichment.whois.enabled", false)) {
			String component_name = config.getString(
					"bolt.enrichment.whois.name", "DefaultWhoisEnrichmentBolt");

			success = initializeWhoisEnrichment(topology_name, component_name);
			messageComponents.add(component_name);
			errorComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.enrichment.whois");
		}

		if (config.getBoolean("bolt.enrichment.cif.enabled", false)) {
			String component_name = config.getString(
					"bolt.enrichment.cif.name", "DefaultCIFEnrichmentBolt");

			success = initializeCIFEnrichment(topology_name, component_name);
			messageComponents.add(component_name);
			errorComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.enrichment.cif");
		}
		
		if (config.getBoolean("bolt.enrichment.threat.enabled", false)) {
			String component_name = config.getString(
					"bolt.enrichment.threat.name", "DefaultThreatEnrichmentBolt");

			success = initializeThreatEnrichment(topology_name, component_name);
			messageComponents.add(component_name);
			errorComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.enrichment.threat");
		}

		if (config.getBoolean("bolt.alerts.enabled", false)) {
			String component_name = config.getString("bolt.alerts.name",
					"DefaultAlertsBolt");

			success = initializeAlerts(topology_name, component_name,
					config_path + "/topologies/" + subdir + "/alerts.xml",
					environment_identifier, topology_identifier);

			messageComponents.add(component_name);
			errorComponents.add(component_name);
			alertComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.alerts");
		}

		if (config.getBoolean("bolt.alerts.indexing.enabled") && config.getBoolean("bolt.alerts.enabled")) {

			String component_name = config.getString(
					"bolt.alerts.indexing.name", "DefaultAlertsBolt");

			success = initializeAlertIndexing(component_name);
			terminalComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.alerts.indexing");
		}

		if (config.getBoolean("bolt.kafka.enabled", false)) {
			String component_name = config.getString("bolt.kafka.name",
					"DefaultKafkaBolt");

			success = initializeKafkaBolt(component_name);
			terminalComponents.add(component_name);

			System.out.println("[OpenSOC] Component " + component_name
					+ " initialized");

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.kafka");
		}

		if (config.getBoolean("bolt.indexing.enabled", true)) {
			String component_name = config.getString("bolt.indexing.name",
					"DefaultIndexingBolt");

			success = initializeIndexingBolt(component_name);
			errorComponents.add(component_name);
			terminalComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.indexing");
		}

		if (config.getBoolean("bolt.hdfs.enabled", false)) {
			String component_name = config.getString("bolt.hdfs.name",
					"DefaultHDFSBolt");

			success = initializeHDFSBolt(topology_name, component_name);
			terminalComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.hdfs");
		}

		if (config.getBoolean("bolt.error.indexing.enabled")) {
			String component_name = config.getString(
					"bolt.error.indexing.name", "DefaultErrorIndexingBolt");

			success = initializeErrorIndexBolt(component_name);
			terminalComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.error");
		}

		if (config.containsKey("bolt.hbase.enabled")
				&& config.getBoolean("bolt.hbase.enabled")) {
			String component_name = config.getString("bolt.hbase.name",
					"DefaultHbaseBolt");

			String shuffleType = config.getString("bolt.hbase.shuffle.type",
					"direct");
			success = initializeHbaseBolt(component_name, shuffleType);
			terminalComponents.add(component_name);

			System.out.println("[OpenSOC] ------Component " + component_name
					+ " initialized with the following settings:");

			SettingsLoader.printConfigOptions((PropertiesConfiguration) config,
					"bolt.hbase");
		}

		System.out.println("[OpenSOC] Topology Summary: ");
		System.out.println("[OpenSOC] Message Stream: "
				+ printComponentStream(messageComponents));
		System.out.println("[OpenSOC] Alerts Stream: "
				+ printComponentStream(alertComponents));
		System.out.println("[OpenSOC] Error Stream: "
				+ printComponentStream(errorComponents));
		System.out.println("[OpenSOC] Data Stream: "
				+ printComponentStream(dataComponents));
		System.out.println("[OpenSOC] Terminal Components: "
				+ printComponentStream(terminalComponents));

		if (local_mode) {
			conf.setNumWorkers(config.getInt("num.workers"));
			conf.setMaxTaskParallelism(1);
			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology(topology_name, conf,
					builder.createTopology());
		} else {

			conf.setNumWorkers(config.getInt("num.workers"));
			conf.setNumAckers(config.getInt("num.ackers"));
			StormSubmitter.submitTopology(topology_name, conf,
					builder.createTopology());
		}

	}

	private String printComponentStream(List<String> messageComponents) {
		StringBuilder print_string = new StringBuilder();

		for (String component : messageComponents) {
			print_string.append(component + " -> ");
		}

		print_string.append("[TERMINAL COMPONENT]");

		return print_string.toString();
	}

	public boolean initializeHbaseBolt(String name, String shuffleType) {

		try {

			String messageUpstreamComponent = dataComponents.get(dataComponents
					.size()-1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			String tableName = config.getString("bolt.hbase.table.name")
					.toString();
			TupleTableConfig hbaseBoltConfig = new TupleTableConfig(tableName,
					config.getString("bolt.hbase.table.key.tuple.field.name")
							.toString(), config.getString(
							"bolt.hbase.table.timestamp.tuple.field.name")
							.toString());

			String allColumnFamiliesColumnQualifiers = config.getString(
					"bolt.hbase.table.fields").toString();
			// This is expected in the form
			// "<cf1>:<cq11>,<cq12>,<cq13>|<cf2>:<cq21>,<cq22>|......."
			String[] tokenizedColumnFamiliesWithColumnQualifiers = StringUtils
					.split(allColumnFamiliesColumnQualifiers, "\\|");
			for (String tokenizedColumnFamilyWithColumnQualifiers : tokenizedColumnFamiliesWithColumnQualifiers) {
				String[] cfCqTokens = StringUtils.split(
						tokenizedColumnFamilyWithColumnQualifiers, ":");
				String columnFamily = cfCqTokens[0];
				String[] columnQualifiers = StringUtils.split(cfCqTokens[1],
						",");
				for (String columnQualifier : columnQualifiers) {
					hbaseBoltConfig.addColumn(columnFamily, columnQualifier);
				}

				// hbaseBoltConfig.setDurability(Durability.valueOf(conf.get(
				// "storm.topology.pcap.bolt.hbase.durability").toString()));

				hbaseBoltConfig.setBatch(Boolean.valueOf(config.getString(
						"bolt.hbase.enable.batching").toString()));

				HBaseBolt hbase_bolt = new HBaseBolt(hbaseBoltConfig,
						config.getString("kafka.zk.list"),
						config.getString("kafka.zk.port"));
				hbase_bolt.setAutoAck(true);

				BoltDeclarer declarer = builder.setBolt(name, hbase_bolt,
						config.getInt("bolt.hbase.parallelism.hint"))
						.setNumTasks(config.getInt("bolt.hbase.num.tasks"));

				if (Grouping._Fields.CUSTOM_OBJECT.toString().equalsIgnoreCase(
						shuffleType)) {
					declarer.customGrouping(
							messageUpstreamComponent,
							"pcap_data_stream",
							new HBaseStreamPartitioner(
									hbaseBoltConfig.getTableName(),
									0,
									Integer.parseInt(conf
											.get("bolt.hbase.partitioner.region.info.refresh.interval.mins")
											.toString())));
				} else if (Grouping._Fields.DIRECT.toString().equalsIgnoreCase(
						shuffleType)) {
					declarer.fieldsGrouping(messageUpstreamComponent,
							"pcap_data_stream", new Fields("pcap_id"));
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return true;
	}

	private boolean initializeErrorIndexBolt(String component_name) {
		try {
			
			Class loaded_class = Class.forName(config.getString("bolt.error.indexing.adapter"));
			IndexAdapter adapter = (IndexAdapter) loaded_class.newInstance();

			String dateFormat = "yyyy.MM";
			if (config.containsKey("bolt.alerts.indexing.timestamp")) {
				dateFormat = config.getString("bolt.alerts.indexing.timestamp");
			}
			
			TelemetryIndexingBolt indexing_bolt = new TelemetryIndexingBolt()
					.withIndexIP(config.getString("es.ip"))
					.withIndexPort(config.getInt("es.port"))
					.withClusterName(config.getString("es.clustername"))
					.withIndexName(
							config.getString("bolt.error.indexing.indexname"))
					.withDocumentName(
							config.getString("bolt.error.indexing.documentname"))
					.withIndexTimestamp(dateFormat)
					.withBulk(config.getInt("bolt.error.indexing.bulk"))
					.withIndexAdapter(adapter)
					.withMetricConfiguration(config);

			BoltDeclarer declarer = builder
					.setBolt(
							component_name,
							indexing_bolt,
							config.getInt("bolt.error.indexing.parallelism.hint"))
					.setNumTasks(config.getInt("bolt.error.indexing.num.tasks"));

			for (String component : errorComponents)
				declarer.shuffleGrouping(component, "error");

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

	}

	private boolean initializeKafkaSpout(String name) {
		try {

			BrokerHosts zk = new ZkHosts(config.getString("kafka.zk"));
			String input_topic = config.getString("spout.kafka.topic");
			SpoutConfig kafkaConfig = new SpoutConfig(zk, input_topic, "",
					input_topic);
			kafkaConfig.scheme = new SchemeAsMultiScheme(new RawScheme());
			kafkaConfig.forceFromStart = Boolean.valueOf("True");
			kafkaConfig.startOffsetTime = -1;

			builder.setSpout(name, new KafkaSpout(kafkaConfig),
					config.getInt("spout.kafka.parallelism.hint")).setNumTasks(
					config.getInt("spout.kafka.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	abstract boolean initializeParsingBolt(String topology_name, String name);

	abstract boolean initializeTestingSpout(String name);

	private boolean initializeGeoEnrichment(String topology_name, String name) {

		try {
			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			
			String[] keys_from_settings = config.getStringArray("bolt.enrichment.geo.fields");
			List<String> geo_keys = new ArrayList<String>(Arrays.asList(keys_from_settings));
			
			GeoMysqlAdapter geo_adapter = new GeoMysqlAdapter(
					config.getString("mysql.ip"), config.getInt("mysql.port"),
					config.getString("mysql.username"),
					config.getString("mysql.password"),
					config.getString("bolt.enrichment.geo.adapter.table"));

			GenericEnrichmentBolt geo_enrichment = new GenericEnrichmentBolt()
					.withEnrichmentTag(
							config.getString("bolt.enrichment.geo.enrichment_tag"))
					.withOutputFieldName(topology_name)
					.withAdapter(geo_adapter)
					.withMaxTimeRetain(
							config.getInt("bolt.enrichment.geo.MAX_TIME_RETAIN_MINUTES"))
					.withMaxCacheSize(
							config.getInt("bolt.enrichment.geo.MAX_CACHE_SIZE_OBJECTS_NUM"))
					.withKeys(geo_keys).withMetricConfiguration(config);

			builder.setBolt(name, geo_enrichment,
					config.getInt("bolt.enrichment.geo.parallelism.hint"))
					.fieldsGrouping(messageUpstreamComponent, "message",
							new Fields("key"))
					.setNumTasks(config.getInt("bolt.enrichment.geo.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	private boolean initializeHostsEnrichment(String topology_name,
			String name, String hosts_path) {

		try {

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			List<String> hosts_keys = new ArrayList<String>();
			hosts_keys.add(config.getString("source.ip"));
			hosts_keys.add(config.getString("dest.ip"));

			Map<String, JSONObject> known_hosts = SettingsLoader
					.loadKnownHosts(hosts_path);

			HostFromPropertiesFileAdapter host_adapter = new HostFromPropertiesFileAdapter(
					known_hosts);

			GenericEnrichmentBolt host_enrichment = new GenericEnrichmentBolt()
					.withEnrichmentTag(
							config.getString("bolt.enrichment.host.enrichment_tag"))
					.withAdapter(host_adapter)
					.withMaxTimeRetain(
							config.getInt("bolt.enrichment.host.MAX_TIME_RETAIN_MINUTES"))
					.withMaxCacheSize(
							config.getInt("bolt.enrichment.host.MAX_CACHE_SIZE_OBJECTS_NUM"))
					.withOutputFieldName(topology_name).withKeys(hosts_keys)
					.withMetricConfiguration(config);

			builder.setBolt(name, host_enrichment,
					config.getInt("bolt.enrichment.host.parallelism.hint"))
					.fieldsGrouping(messageUpstreamComponent, "message",
							new Fields("key"))
					.setNumTasks(
							config.getInt("bolt.enrichment.host.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	@SuppressWarnings("rawtypes")
	private boolean initializeAlerts(String topology_name, String name,
			String alerts_path, JSONObject environment_identifier,
			JSONObject topology_identifier) {
		try {
			
			Class loaded_class = Class.forName(config.getString("bolt.alerts.adapter"));
			Constructor constructor = loaded_class.getConstructor(new Class[] { Map.class});
			
			Map<String, String> settings = SettingsLoader.getConfigOptions((PropertiesConfiguration)config, config.getString("bolt.alerts.adapter") + ".");
			
			System.out.println("Adapter Settings: ");
			SettingsLoader.printOptionalSettings(settings);
			
			AlertsAdapter alerts_adapter = (AlertsAdapter) constructor.newInstance(settings);
			
	

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			JSONObject alerts_identifier = SettingsLoader
					.generateAlertsIdentifier(environment_identifier,
							topology_identifier);

			 

			TelemetryAlertsBolt alerts_bolt = new TelemetryAlertsBolt()
					.withIdentifier(alerts_identifier).withMaxCacheSize(1000)
					.withMaxTimeRetain(3600).withAlertsAdapter(alerts_adapter)
					.withOutputFieldName("message")
					.withMetricConfiguration(config);

			builder.setBolt(name, alerts_bolt,
					config.getInt("bolt.alerts.parallelism.hint"))
					.fieldsGrouping(messageUpstreamComponent, "message",
							new Fields("key"))
					.setNumTasks(config.getInt("bolt.alerts.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return true;
	}

	private boolean initializeAlertIndexing(String name) {
		
		try{
		String messageUpstreamComponent = alertComponents.get(alertComponents
				.size() - 1);

		System.out.println("[OpenSOC] ------" + name + " is initializing from "
				+ messageUpstreamComponent);
		
		Class loaded_class = Class.forName(config.getString("bolt.alerts.indexing.adapter"));
		IndexAdapter adapter = (IndexAdapter) loaded_class.newInstance();

		String dateFormat = "yyyy.MM.dd";
		if (config.containsKey("bolt.alerts.indexing.timestamp")) {
			dateFormat = config.getString("bolt.alerts.indexing.timestamp");
		}
		TelemetryIndexingBolt indexing_bolt = new TelemetryIndexingBolt()
				.withIndexIP(config.getString("es.ip"))
				.withIndexPort(config.getInt("es.port"))
				.withClusterName(config.getString("es.clustername"))
				.withIndexName(
						config.getString("bolt.alerts.indexing.indexname"))
				.withDocumentName(
						config.getString("bolt.alerts.indexing.documentname"))
				.withIndexTimestamp(dateFormat)
				.withBulk(config.getInt("bolt.alerts.indexing.bulk"))
				.withIndexAdapter(adapter)
				.withMetricConfiguration(config);

		String alerts_name = config.getString("bolt.alerts.indexing.name");
		builder.setBolt(alerts_name, indexing_bolt,
				config.getInt("bolt.indexing.parallelism.hint"))
				.shuffleGrouping(messageUpstreamComponent, "alert")
				.setNumTasks(config.getInt("bolt.indexing.num.tasks"));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private boolean initializeKafkaBolt(String name) {
		try {

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			Map<String, String> kafka_broker_properties = new HashMap<String, String>();
			kafka_broker_properties.put("zk.connect",
					config.getString("kafka.zk"));
			kafka_broker_properties.put("metadata.broker.list",
					config.getString("kafka.br"));

			kafka_broker_properties.put("serializer.class",
					"com.opensoc.json.serialization.JSONKafkaSerializer");

			kafka_broker_properties.put("key.serializer.class",
					"kafka.serializer.StringEncoder");

			String output_topic = config.getString("bolt.kafka.topic");

			conf.put("kafka.broker.properties", kafka_broker_properties);
			conf.put("topic", output_topic);

			builder.setBolt(name, new KafkaBolt<String, JSONObject>(),
					config.getInt("bolt.kafka.parallelism.hint"))
					.shuffleGrouping(messageUpstreamComponent, "message")
					.setNumTasks(config.getInt("bolt.kafka.num.tasks"));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return true;
	}

	private boolean initializeWhoisEnrichment(String topology_name, String name) {
		try {

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			String[] keys_from_settings = config.getString("bolt.enrichment.whois.fields").split(",");
			List<String> whois_keys = new ArrayList<String>(Arrays.asList(keys_from_settings));

			EnrichmentAdapter whois_adapter = new WhoisHBaseAdapter(
					config.getString("bolt.enrichment.whois.hbase.table.name"),
					config.getString("kafka.zk.list"),
					config.getString("kafka.zk.port"));

			GenericEnrichmentBolt whois_enrichment = new GenericEnrichmentBolt()
					.withEnrichmentTag(
							config.getString("bolt.enrichment.whois.enrichment_tag"))
					.withOutputFieldName(topology_name)
					.withAdapter(whois_adapter)
					.withMaxTimeRetain(
							config.getInt("bolt.enrichment.whois.MAX_TIME_RETAIN_MINUTES"))
					.withMaxCacheSize(
							config.getInt("bolt.enrichment.whois.MAX_CACHE_SIZE_OBJECTS_NUM"))
					.withKeys(whois_keys).withMetricConfiguration(config);

			builder.setBolt(name, whois_enrichment,
					config.getInt("bolt.enrichment.whois.parallelism.hint"))
					.fieldsGrouping(messageUpstreamComponent, "message",
							new Fields("key"))
					.setNumTasks(
							config.getInt("bolt.enrichment.whois.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	private boolean initializeIndexingBolt(String name) {
		try {

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);
			
			Class loaded_class = Class.forName(config.getString("bolt.indexing.adapter"));
			IndexAdapter adapter = (IndexAdapter) loaded_class.newInstance();
			
			Map<String, String> settings = SettingsLoader.getConfigOptions((PropertiesConfiguration)config, "optional.settings.bolt.index.search.");
			
			if(settings != null && settings.size() > 0)
			{
				adapter.setOptionalSettings(settings);
				System.out.println("[OpenSOC] Index Bolt picket up optional settings:");
				SettingsLoader.printOptionalSettings(settings);			
			}

			// dateFormat defaults to hourly if not specified
			String dateFormat = "yyyy.MM.dd.hh";
			if (config.containsKey("bolt.indexing.timestamp")) {
				dateFormat = config.getString("bolt.indexing.timestamp");
			}
			TelemetryIndexingBolt indexing_bolt = new TelemetryIndexingBolt()
					.withIndexIP(config.getString("es.ip"))
					.withIndexPort(config.getInt("es.port"))
					.withClusterName(config.getString("es.clustername"))
					.withIndexName(config.getString("bolt.indexing.indexname"))
					.withIndexTimestamp(dateFormat)
					.withDocumentName(
							config.getString("bolt.indexing.documentname"))
					.withBulk(config.getInt("bolt.indexing.bulk"))
					.withIndexAdapter(adapter)
					.withMetricConfiguration(config);

			builder.setBolt(name, indexing_bolt,
					config.getInt("bolt.indexing.parallelism.hint"))
					.fieldsGrouping(messageUpstreamComponent, "message",
							new Fields("key"))
					.setNumTasks(config.getInt("bolt.indexing.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}
	
	
	private boolean initializeThreatEnrichment(String topology_name, String name) {
		try {

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			String[] fields = config.getStringArray("bolt.enrichment.threat.fields");
			List<String> threat_keys = new ArrayList<String>(Arrays.asList(fields));

			GenericEnrichmentBolt threat_enrichment = new GenericEnrichmentBolt()
					.withEnrichmentTag(
							config.getString("bolt.enrichment.threat.enrichment_tag"))
					.withAdapter(
							new ThreatHbaseAdapter(config
									.getString("kafka.zk.list"), config
									.getString("kafka.zk.port"), config
									.getString("bolt.enrichment.threat.tablename")))
					.withOutputFieldName(topology_name)
					.withEnrichmentTag(config.getString("bolt.enrichment.threat.enrichment_tag"))
					.withKeys(threat_keys)
					.withMaxTimeRetain(
							config.getInt("bolt.enrichment.threat.MAX_TIME_RETAIN_MINUTES"))
					.withMaxCacheSize(
							config.getInt("bolt.enrichment.threat.MAX_CACHE_SIZE_OBJECTS_NUM"))
					.withMetricConfiguration(config);

			builder.setBolt(name, threat_enrichment,
					config.getInt("bolt.enrichment.threat.parallelism.hint"))
					.fieldsGrouping(messageUpstreamComponent, "message",
							new Fields("key"))
					.setNumTasks(config.getInt("bolt.enrichment.threat.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	private boolean initializeCIFEnrichment(String topology_name, String name) {
		try {

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			List<String> cif_keys = new ArrayList<String>();

			String[] ipFields = config.getStringArray("bolt.enrichment.cif.fields.ip");
			cif_keys.addAll(Arrays.asList(ipFields));
			
			String[] hostFields = config.getStringArray("bolt.enrichment.cif.fields.host");
			cif_keys.addAll(Arrays.asList(hostFields));
			
			String[] emailFields = config.getStringArray("bolt.enrichment.cif.fields.email");
			cif_keys.addAll(Arrays.asList(emailFields));
			
			GenericEnrichmentBolt cif_enrichment = new GenericEnrichmentBolt()
					.withEnrichmentTag(
							config.getString("bolt.enrichment.cif.enrichment_tag"))
					.withAdapter(
							new CIFHbaseAdapter(config
									.getString("kafka.zk.list"), config
									.getString("kafka.zk.port"), config
									.getString("bolt.enrichment.cif.tablename")))
					.withOutputFieldName(topology_name)
					.withKeys(cif_keys)
					.withMaxTimeRetain(
							config.getInt("bolt.enrichment.cif.MAX_TIME_RETAIN_MINUTES"))
					.withMaxCacheSize(
							config.getInt("bolt.enrichment.cif.MAX_CACHE_SIZE_OBJECTS_NUM"))
					.withMetricConfiguration(config);

			builder.setBolt(name, cif_enrichment,
					config.getInt("bolt.enrichment.cif.parallelism.hint"))
					.fieldsGrouping(messageUpstreamComponent, "message",
							new Fields("key"))
					.setNumTasks(config.getInt("bolt.enrichment.cif.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

	private boolean initializeHDFSBolt(String topology_name, String name) {
		try {

			String messageUpstreamComponent = messageComponents
					.get(messageComponents.size() - 1);

			System.out.println("[OpenSOC] ------" + name
					+ " is initializing from " + messageUpstreamComponent);

			RecordFormat format = new DelimitedRecordFormat()
					.withFieldDelimiter(
							config.getString("bolt.hdfs.field.delimiter")
									.toString()).withFields(
							new Fields("message"));

			// sync the file system after every x number of tuples
			SyncPolicy syncPolicy = new CountSyncPolicy(Integer.valueOf(config
					.getString("bolt.hdfs.batch.size").toString()));

			// rotate files when they reach certain size
			FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(
					Float.valueOf(config.getString(
							"bolt.hdfs.file.rotation.size.in.mb").toString()),
					Units.MB);

			FileNameFormat fileNameFormat = new DefaultFileNameFormat()
					.withPath(config.getString("bolt.hdfs.wip.file.path")
							.toString());

			// Post rotate action
			MoveFileAction moveFileAction = (new MoveFileAction())
					.toDestination(config.getString(
							"bolt.hdfs.finished.file.path").toString());

			HdfsBolt hdfsBolt = new HdfsBolt()
					.withFsUrl(
							config.getString("bolt.hdfs.file.system.url")
									.toString())
					.withFileNameFormat(fileNameFormat)
					.withRecordFormat(format)
					.withRotationPolicy(rotationPolicy)
					.withSyncPolicy(syncPolicy)
					.addRotationAction(moveFileAction);
			if (config.getString("bolt.hdfs.compression.codec.class") != null) {
				hdfsBolt.withCompressionCodec(config.getString(
						"bolt.hdfs.compression.codec.class").toString());
			}

			builder.setBolt(name, hdfsBolt,
					config.getInt("bolt.hdfs.parallelism.hint"))
					.shuffleGrouping(messageUpstreamComponent, "message")
					.setNumTasks(config.getInt("bolt.hdfs.num.tasks"));

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return true;
	}

}
