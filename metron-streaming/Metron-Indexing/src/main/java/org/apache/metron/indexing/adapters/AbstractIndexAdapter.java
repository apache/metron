package org.apache.metron.indexing.adapters;

import java.io.Serializable;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.metron.index.interfaces.IndexAdapter;
import org.apache.metron.indexing.AbstractIndexingBolt;

@SuppressWarnings("serial")
public abstract class AbstractIndexAdapter implements IndexAdapter, Serializable{
	
	protected static final Logger _LOG = LoggerFactory
			.getLogger(AbstractIndexingBolt.class);


	

	abstract public boolean initializeConnection(String ip, int port,
			String cluster_name, String index_name, String document_name,
			int bulk, String date_format) throws Exception;

}
