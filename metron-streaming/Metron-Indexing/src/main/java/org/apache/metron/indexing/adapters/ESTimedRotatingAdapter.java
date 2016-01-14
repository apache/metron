package org.apache.metron.indexing.adapters;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.HashBag;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class ESTimedRotatingAdapter extends AbstractIndexAdapter implements
		Serializable {

	private int _bulk_size;
	private String _index_name;
	private String _document_name;
	private String _cluster_name;
	private int _port;
	private String _ip;
	public transient TransportClient client;
	private DateFormat dateFormat;
	
	private Map<String, String> tuning_settings;

	private Bag bulk_set;

	private Settings settings;
	
	public void setOptionalSettings(Map<String, String> settings)
	{
		tuning_settings = settings;
	}

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
			

			dateFormat = new SimpleDateFormat(date_format);

			System.out.println("Bulk indexing is set to: " + _bulk_size);

			ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder() ;	
			
			if(tuning_settings != null && tuning_settings.size() > 0)
			{
					builder.put(tuning_settings);
			}
			
			builder.put("cluster.name", _cluster_name);
			builder.put("client.transport.ping_timeout","500s");
			
			
			settings = builder.build();
					
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
			
			_LOG.trace("[Metron] Incremented bulk size to: " + bulk_set.size());
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
				
				String index_postfix = dateFormat.format(new Date());

				while (iterator.hasNext()) {
					JSONObject setElement = iterator.next();
					
					_LOG.trace("[Metron] Flushing to index: " + _index_name+ "_" + index_postfix);

					IndexRequestBuilder a = client.prepareIndex(_index_name+ "_" + index_postfix,
							_document_name);
					a.setSource(setElement.toString());
					bulkRequest.add(a);

				}

				_LOG.trace("[Metron] Performing bulk load of size: "
						+ bulkRequest.numberOfActions());

				BulkResponse resp = bulkRequest.execute().actionGet();
				
				for(BulkItemResponse r: resp.getItems())
				{
					r.getResponse();
					_LOG.trace("[Metron] ES SUCCESS MESSAGE: " + r.getFailureMessage());
				}
				
				bulk_set.clear();
				
				if (resp.hasFailures()) {
					_LOG.error("[Metron] Received bulk response error: "
							+ resp.buildFailureMessage());
					
					for(BulkItemResponse r: resp.getItems())
					{
						r.getResponse();
						_LOG.error("[Metron] ES FAILURE MESSAGE: " + r.getFailureMessage());
					}
				}
				
			}

			return true;
		}

		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
