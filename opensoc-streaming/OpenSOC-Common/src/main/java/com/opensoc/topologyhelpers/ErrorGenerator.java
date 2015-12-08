package com.opensoc.topologyhelpers;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.json.simple.JSONObject;

public class ErrorGenerator {

	public static JSONObject generateErrorMessage(String message, String exception)
	{
		JSONObject error_message = new JSONObject();
		
		error_message.put("time", System.currentTimeMillis());
		try {
			error_message.put("hostname", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		error_message.put("message", message);
		error_message.put("exception", exception);
		
		return error_message;
	}
}
