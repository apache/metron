/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsing.parsers;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.json.simple.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class GrokSourcefireParser extends BasicParser {
	
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
	public void init() {

	}

	@Override
	public List<JSONObject> parse(byte[] raw_message) {
		JSONObject payload = new JSONObject();
		String toParse = "";
		JSONObject toReturn;

		List<JSONObject> messages = new ArrayList<>();
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
			messages.add(toReturn);
			return messages;
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
	}

	

}
