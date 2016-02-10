package org.apache.metron.filters;

import org.apache.metron.parser.interfaces.MessageFilter;
import org.json.simple.JSONObject;

import java.io.Serializable;

public class GenericMessageFilter implements MessageFilter<JSONObject>,
				Serializable {

	private static final long serialVersionUID = 3626397212398318852L;

	public boolean emitTuple(JSONObject message) {
		return true;
	}

}
