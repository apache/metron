package org.apache.metron.helpers.topology;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.simple.JSONObject;

public class ErrorGenerator {

	@SuppressWarnings("unchecked")
	public static JSONObject generateErrorMessage(String message, Exception e)
	{
		JSONObject error_message = new JSONObject();
		
		/*
		 * Save full stack trace in object.
		 */
		String stackTrace = ExceptionUtils.getStackTrace(e);
		
		String exception = e.toString();
		
		error_message.put("time", System.currentTimeMillis());
		try {
			error_message.put("hostname", InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		
		error_message.put("message", message);
		error_message.put("exception", exception);
		error_message.put("stack", stackTrace);
		
		return error_message;
	}
}
