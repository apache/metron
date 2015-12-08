package com.opensoc.parser.interfaces;

import org.json.simple.JSONObject;


public interface MessageFilter {

	public boolean emitTuple(JSONObject message);

}
