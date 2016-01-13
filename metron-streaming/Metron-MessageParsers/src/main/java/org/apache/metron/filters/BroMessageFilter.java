package com.apache.metron.filters;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;

import com.apache.metron.parser.interfaces.MessageFilter;

public class BroMessageFilter implements MessageFilter,Serializable {

	/**
	 * Filter protocols based on whitelists and blacklists
	 */
	
	private static final long serialVersionUID = -3824683649114625033L;
	private String _key;
	private final Set<String> _known_protocols;

	 /**
	 * @param  filter  Commons configuration for reading properties files
	 * @param  key Key in a JSON mesage where the protocol field is located
	 */
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BroMessageFilter(Configuration conf, String key) {
		_key = key;
		_known_protocols = new HashSet<String>();
		List known_protocols = conf.getList("source.known.protocols");
		_known_protocols.addAll(known_protocols);
	}

	 /**
	 * @param  message  JSON representation of a message with a protocol field
	 * @return      False if message if filtered and True if message is not filtered
	 */
	
	public boolean emitTuple(JSONObject message) {
		return _known_protocols.contains(message.get(_key));
	}
}