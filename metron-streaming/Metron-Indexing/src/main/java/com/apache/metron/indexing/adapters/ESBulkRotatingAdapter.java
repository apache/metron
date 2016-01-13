package com.opensoc.indexing.adapters;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.simple.JSONObject;

@SuppressWarnings({ "deprecation", "serial" })
public class ESBulkRotatingAdapter extends AbstractIndexAdapter {

	private Client client;
	private BulkRequestBuilder bulkRequest;
	private int _bulk_size;
	private String _index_name;
	private String _document_name;
	private int element_count;
	private String index_postfix;
	private String running_index_postfix;

	private HttpClient httpclient;
	private HttpPost post;

	private DateFormat dateFormat;

	public boolean initializeConnection(String ip, int port,
			String cluster_name, String index_name, String document_name,
			int bulk_size, String date_format) {

		_LOG.info("Initializing ESBulkAdapter...");

		try {
			httpclient = new DefaultHttpClient();
			String alias_link = "http://" + ip + ":" + 9200 + "/_aliases";
			post = new HttpPost(alias_link);

			_index_name = index_name;
			_document_name = document_name;

			_bulk_size = bulk_size - 1;
			

			dateFormat = new SimpleDateFormat(date_format);
			
			element_count = 0;
			running_index_postfix = "NONE";

			Settings settings = ImmutableSettings.settingsBuilder()
					.put("cluster.name", cluster_name).build();
			client = new TransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(ip,
							port));

			bulkRequest = client.prepareBulk();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public int bulkIndex(JSONObject raw_message) {

		index_postfix = dateFormat.format(new Date());

		bulkRequest.add(client.prepareIndex(_index_name + "_" + index_postfix,
				_document_name).setSource(raw_message));

		return doIndex();
	}

	public int bulkIndex(String raw_message) {

		index_postfix = dateFormat.format(new Date());

		bulkRequest.add(client.prepareIndex(_index_name + "_" + index_postfix,
				_document_name).setSource(raw_message));

		return doIndex();
	}

	public int doIndex() {

		element_count++;
		
		if (element_count != _bulk_size)
			return 0;

		if (element_count == _bulk_size) {
			_LOG.debug("Starting bulk load of size: " + _bulk_size);
			BulkResponse resp = bulkRequest.execute().actionGet();
			element_count = 0;
			_LOG.debug("Received bulk response: " + resp.toString());

			if (resp.hasFailures()) {
				_LOG.error("Bulk update failed");
				return 2;
			}

			if (!running_index_postfix.equals(index_postfix)) {

				_LOG.debug("Attempting to apply a new alias");

				try {

					String alias = "{\"actions\" : [{ \"add\" : { \"index\" : \""
							+ _index_name
							+ "-"
							+ index_postfix
							+ "\", \"alias\" : \"" + _index_name + "\" } } ]}";

					post.setEntity(new StringEntity(alias));

					HttpResponse response = httpclient.execute(post);
					String res = EntityUtils.toString(response.getEntity());

					_LOG.debug("Alias request received the following response: "
							+ res);

					running_index_postfix = index_postfix;
				}

				catch (Exception e) {
					e.printStackTrace();
					_LOG.error("Alias request failed...");
					return 2;
				}
			}

			index_postfix = dateFormat.format(new Date());
		}

		_LOG.debug("Adding to bulk load: element " + element_count
				+ " of bulk size " + _bulk_size);

		return 1;
	}

	public void setOptionalSettings(Map<String, String> settings) {
		// TODO Auto-generated method stub
		
	}

}
