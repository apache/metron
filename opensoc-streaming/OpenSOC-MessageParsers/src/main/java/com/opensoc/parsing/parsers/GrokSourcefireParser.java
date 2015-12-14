package com.opensoc.parsing.parsers;

import java.net.URL;

import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.exception.GrokException;

import org.json.simple.JSONObject;


public class GrokSourcefireParser extends AbstractParser{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Grok grok;
	
	public GrokSourcefireParser() throws GrokException
	{
		URL pattern_url = getClass().getClassLoader().getResource(
				"pattarns/sourcefire");
		grok = Grok.create(pattern_url.getFile());
		grok.compile("%{SOURCEFIRE}");
	}

	public GrokSourcefireParser(String filepath) throws GrokException
	{

		grok = Grok.create(filepath);
		grok.compile("%{SOURCEFIRE}");
	}
	
	public GrokSourcefireParser(String filepath, String pattern) throws GrokException
	{

		grok = Grok.create(filepath);
		grok.compile("%{"+pattern+"}");
	}
	
	@Override
	public JSONObject parse(byte[] raw_message) {
		JSONObject payload = new JSONObject();
		String toParse = "";
		JSONObject toReturn;
		

		try {

			toParse = new String(raw_message, "UTF-8");
			Match gm = grok.match(toParse);
			gm.captures();
			
			toReturn = new JSONObject();
			
			toReturn.putAll(gm.toMap());
			toReturn.remove("SOURCEFIRE");
			String proto = toReturn.get("protocol").toString();
			proto = proto.replace("{", "");
			proto = proto.replace("}", "");
			toReturn.put("protocol", proto);
			return toReturn;
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
	}

	

}
