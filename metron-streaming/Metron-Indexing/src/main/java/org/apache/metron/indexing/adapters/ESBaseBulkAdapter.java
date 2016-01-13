package com.apache.metron.indexing.adapters;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class ESBaseBulkAdapter extends AbstractIndexAdapter implements
		Serializable {

	private int _bulk_size;
	private String _index_name;
	private String _document_name;
	private String _cluster_name;
	private int _port;
	private String _ip;
	public transient TransportClient client;

	private Bag bulk_set;

	private Settings settings;

	@Override
	public boolean initializeConnection(String ip, int port,
			String cluster_name, String index_name, String document_name,
			int bulk_size, String date_format) throws Exception {

		bulk_set = new HashBag();

		_LOG.trace("[Metron] Initializing ESBulkAdapter...");

		try {
			_ip = ip;
			_port = port;
			_cluster_name = cluster_name;
			_index_name = index_name;
			_document_name = document_name;
			_bulk_size = bulk_size;

			_LOG.trace("[Metron] Bulk indexing is set to: " + _bulk_size);

			settings = ImmutableSettings.settingsBuilder()
					.put("cluster.name", _cluster_name).build();
			client = new TransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(_ip,
							_port));

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @param raw_message
	 *            message to bulk index in Elastic Search
	 * @return integer (0) loaded into a bulk queue, (1) bulk indexing executed,
	 *         (2) error
	 */
	@SuppressWarnings("unchecked")
	public int bulkIndex(JSONObject raw_message) {

		boolean success = true;
		int set_size = 0;

		synchronized (bulk_set) {
			bulk_set.add(raw_message);
			set_size = bulk_set.size();
			
			_LOG.trace("[Metron] Bulk size is now: " + bulk_set.size());
		}

		try {

			if (set_size >= _bulk_size) {
				success = doIndex();

				if (success)
					return 1;
				else
					return 2;
			}

			return 0;

		} catch (Exception e) {
			e.printStackTrace();
			return 2;
		}
	}

	public boolean doIndex() throws Exception {

		try {

			synchronized (bulk_set) {
				if (client == null)
					throw new Exception("client is null");

				BulkRequestBuilder bulkRequest = client.prepareBulk();

				Iterator<JSONObject> iterator = bulk_set.iterator();

				while (iterator.hasNext()) {
					JSONObject setElement = iterator.next();

					IndexRequestBuilder a = client.prepareIndex(_index_name,
							_document_name);
					a.setSource(setElement.toString());
					bulkRequest.add(a);

				}

				_LOG.trace("[Metron] Performing bulk load of size: "
						+ bulkRequest.numberOfActions());

				BulkResponse resp = bulkRequest.execute().actionGet();
				_LOG.trace("[Metron] Received bulk response: "
						+ resp.toString());
				bulk_set.clear();
			}

			return true;
		}

		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void setOptionalSettings(Map<String, String> settings) {
		// TODO Auto-generated method stub
		
	}
}
