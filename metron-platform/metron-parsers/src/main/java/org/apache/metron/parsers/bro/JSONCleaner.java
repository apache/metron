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
package org.apache.metron.parsers.bro;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSONCleaner implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * @param jsonString
	 * @return
	 * @throws ParseException
	 * Takes a json String as input and removes any Special Chars (^ a-z A-Z 0-9) in the keys
	 */
	@SuppressWarnings({"unchecked","rawtypes"})
	public JSONObject clean(String jsonString) throws ParseException
	{
		JSONParser parser = new JSONParser();
		
		
		Map json = (Map) parser.parse(jsonString);
		JSONObject output = new JSONObject();
	    Iterator iter = json.entrySet().iterator();

		 while(iter.hasNext()){
		      Map.Entry entry = (Map.Entry)iter.next();
		      
		      String key = ((String)entry.getKey()).replaceAll("[^\\._a-zA-Z0-9]+","");
		      output.put(key, entry.getValue());
		    }

		return output;
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	public static void main(String args[])
	{
		String jsonText = "{\"first_1\": 123, \"second\": [4, 5, 6], \"third\": 789}";
		JSONCleaner cleaner = new JSONCleaner();
		try {
			//cleaner.clean(jsonText);
			Map obj=new HashMap();
			  obj.put("name","foo");
			  obj.put("num",new Integer(100));
			  obj.put("balance",new Double(1000.21));
			  obj.put("is_vip",new Boolean(true));
			  obj.put("nickname",null);
			Map obj1 = new HashMap();
			obj1.put("sourcefile", obj);
			
			JSONObject json = new JSONObject(obj1);
			System.out.println(json);
			  
			  
			  
			  System.out.print(jsonText);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
