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

package org.apache.metron.parsers.soltra;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.XML;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("serial")
public class SoltraParser extends BasicParser {

	private static final Logger _LOG = LoggerFactory.getLogger(SoltraParser.class);
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	private JSONObject payload;
	
	@Override
	public void init() {

	}

	@Override
	public void configure(Map<String, Object> config) {

	}

	public List<JSONObject> parse(byte[] msg) throws Exception {
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		String message = "";
		List<JSONObject> messages = new ArrayList<>();
		payload = new JSONObject();
		
		try {
			message = new String(msg, "UTF-8");

			org.json.JSONObject fullJSONtemp = XML.toJSONObject(message);
			org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();

			JSONObject fullJSON = (JSONObject)parser.parse(fullJSONtemp.toString());

			JSONObject topLevel = (JSONObject)fullJSON.get("stix:STIX_Package");
			payload.put("Edge_ID", topLevel.get("id"));


			if (topLevel.containsKey("stix:Indicators")) {
				handleIndicator(topLevel);
			} else if (topLevel.containsKey("stix:TTPs")) {
				handleTTP(topLevel);
			} else if (topLevel.containsKey("stix:Observables")) {
				if(((JSONObject)((JSONObject)topLevel.get("stix:Observables")).get("cybox:Observable")).containsKey("cybox:Observable_Composition")){
					handleObservableComposition(topLevel);
				}else{
					handleObservable(topLevel);
				}
			}else{
				//something new
				payload.put("Type", "Unknown");
			}

			// put metron standard fields
			payload.put("original_string", "");
			payload.put("timestamp", formatDate((String)topLevel.get("timestamp")));

			messages.add(payload);
			return messages;
		} catch (org.json.simple.parser.ParseException e1) {
			e1.printStackTrace();
			throw e1;
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			throw e1;
		} catch (Exception e) {
			_LOG.error("Failed to parse: " + message, e);
			throw e;
		}
	}


	/**
	 *
	 * @param input An unformatted date string
	 * @return An epoch time date long
	 */
	private Long formatDate(String input) {
		try {
			// remove extra characters so all timestamps have same format
			String trimmedInput = input.substring(0, 19);
			Date date = dateFormat.parse(trimmedInput);
			return date.getTime();
		} catch (IndexOutOfBoundsException | java.text.ParseException e) {
			_LOG.error(e.toString());
		}
		// if dates can't be formatted, return original input
		return System.currentTimeMillis();
	}

	/**
	 * Parses the file if it is an indicator soltra file
	 *
	 * @param topLevel
	 */
	private void handleIndicator(JSONObject topLevel) {
		try{
			payload.put("Type", "Indicator");

			JSONObject indicator = (JSONObject)((JSONObject)topLevel.get("stix:Indicators")).get("stix:Indicator");
			payload.put("ID", indicator.get("id"));

			// id
			payload.put("ID", indicator.get("id"));

			// title
			payload.put("Title", indicator.get("indicator:Title"));

			// description
			payload.put("Description", indicator.get("indicator:Description"));

			// producer
			JSONObject producer = (JSONObject)((JSONObject)indicator.get("indicator:Producer")).get("stixCommon:Identity");
			payload.put("Producer", producer.get("id"));

			// referenced observable
			payload.put("Child", ((JSONObject)indicator.get("indicator:Observable")).get("idref"));

			// referenced ttp
			if(indicator.get("indicator:Indicated_TTP") != null){
				JSONArray ttpArray = new JSONArray();
				if(indicator.get("indicator:Indicated_TTP").getClass() == JSONObject.class){
					ttpArray.add((JSONObject)indicator.get("indicator:Indicated_TTP"));
				}else{
					ttpArray = (JSONArray)indicator.get("indicator:Indicated_TTP");
				}
				JSONArray ttpIDArray = new JSONArray();
				for(int i = 0; i < ttpArray.size(); i++){
					String ttp = (String)((JSONObject)((JSONObject)ttpArray.get(i)).get("stixCommon:TTP")).get("idref");
					ttpIDArray.add(ttp);
				}
				payload.put("TTP_Children", ttpIDArray);
			}

		}catch(Exception e){
			//If something fails, it's most likely a format we haven't anticipated
			payload.put("Type", "Unknown");
		}

	}

	/**
	 * Parses the file if it is a TTP soltra file
	 *
	 * @param topLevel
	 */
	private void handleTTP(JSONObject topLevel) {
		try{
			payload.put("Type", "TTP");

			JSONObject ttp = (JSONObject)((JSONObject)topLevel.get("stix:TTPs")).get("stix:TTP");
			payload.put("ID", ttp.get("id"));


			// title
			payload.put("Title", ttp.get("ttp:Title"));

			// Victim case
			JSONObject victim = (JSONObject)ttp.get("ttp:Victim_Targeting");
			if(victim != null){
				//id
				payload.put("TTP_Identity", ((JSONObject)victim.get("ttp:Identity")).get("id"));
				// Name
				payload.put("Name", ((JSONObject)victim.get("ttp:Identity")).get("stixCommon:Name"));

			}

			//Malware case
			JSONObject behavior = (JSONObject)ttp.get("ttp:Behavior");
			if(behavior != null){
				if(behavior.containsKey("ttp:Malware")){
					JSONObject malware = (JSONObject)((JSONObject)behavior.get("ttp:Malware")).get("ttp:Malware_Instance");
					payload.put("Malware_Identity", malware.get("id"));
				}
			}
		}catch(Exception e){
			//If something fails, it's most likely a format we haven't anticipated
			payload.put("Type", "Unknown");
		}
	}

	/**
	 * Parses the file if it is an Observable_Composition soltra file
	 *
	 * @param topLevel
	 */
	private void handleObservableComposition(JSONObject topLevel) {
		try{
			payload.put("Type", "Parent");

			JSONObject observable = (JSONObject)((JSONObject)topLevel.get("stix:Observables")).get("cybox:Observable");
			payload.put("ID", observable.get("id"));

			JSONArray composition = (JSONArray)((JSONObject)observable.get("cybox:Observable_Composition")).get("cybox:Observable");
			JSONArray children = new JSONArray();
			for(int i = 0; i < composition.size(); i++){
				children.add(((JSONObject)composition.get(i)).get("idref"));
			}

			payload.put("Children", children);
		}catch (Exception e){
			//If something fails, it's most likely a format we haven't anticipated
			payload.put("Type", "Unknown");
		}
	}

	/**
	 * Parses the file if it is an Observable soltra file
	 *
	 * @param topLevel
	 */
	private void handleObservable(JSONObject topLevel) {
		try{
			JSONObject observable = (JSONObject)((JSONObject)topLevel.get("stix:Observables")).get("cybox:Observable");
			JSONObject properties = (JSONObject)((JSONObject)((JSONObject)((JSONObject)topLevel.get("stix:Observables")).get("cybox:Observable")).get("cybox:Object")).get("cybox:Properties");

			payload.put("ID", observable.get("id"));

			if (properties.containsKey("AddressObj:Address_Value")) {
				handleIPObservable(observable);
			}else if (properties.containsKey("URIObj:Value")) {
				handleURIObservable(observable);
			}else if (properties.containsKey("DomainNameObj:Value")) {
				handleDomainObservable(observable);
			}else if (properties.containsKey("FileObj:Hashes")) {
				handleMD5Observable(observable);
			}else if (properties.containsKey("PortObj:Port_Value")) {
				handlePortObservable(observable);
			}else{
				payload.put("Type", "Unknown");
			}
		}catch (Exception e){
			//If something fails, it's most likely a format we haven't anticipated
			payload.put("Type", "Unknown");
		}
	}

	/**
	 * parses the Ip subtype of Observable
	 *
	 * @param observable
	 */
	private void handleIPObservable(JSONObject observable) {
		JSONObject object = (JSONObject)observable.get("cybox:Object");
		JSONObject properties = (JSONObject)object.get("cybox:Properties");
		String ipObservable = (String)((JSONObject)properties.get("AddressObj:Address_Value")).get("content");

		// DShield incorrectly uses the AddressObj in order to store the email
		// address
		// of the person who registered a tor exit node.
		// We need to remove these since they are not ip addresses...
		if (!ipObservable.contains("@")) {
			payload.put("Type", "IP");

			if (ipObservable.contains("##comma##")) {
				payload.put("IP_Range_To", ipObservable.substring(ipObservable.indexOf("comma##") + "comma##".length()));
				payload.put("IP_Range_From", ipObservable.substring(0, ipObservable.indexOf("#")));

				payload.put("IP_Range_Long_To", convertIPStringToLong(ipObservable.substring(ipObservable.indexOf("comma##") + "comma##".length())));
				payload.put("IP_Range_Long_From", convertIPStringToLong(ipObservable.substring(0, ipObservable.indexOf("#"))));
			} else {
				payload.put("IP_Range_To", ipObservable);
				payload.put("IP_Range_From", ipObservable);

				payload.put("IP_Range_Long_To", convertIPStringToLong(ipObservable));
				payload.put("IP_Range_Long_From", convertIPStringToLong(ipObservable));
			}
		} else {
			payload.put("Type", "Registrar_Email");
			payload.put("Email", ipObservable);
		}
	}

	/**
	 * parses the URI subtype of Observable
	 *
	 * @param observable
	 */
	private void handleURIObservable(JSONObject observable) {
		payload.put("Type", "URI");

		JSONObject object = (JSONObject)observable.get("cybox:Object");
		JSONObject properties = (JSONObject)object.get("cybox:Properties");

		String uriObservable = (String)((JSONObject)properties.get("URIObj:Value")).get("content");
		if (uriObservable.contains("&amp;")) {
			uriObservable = uriObservable.replaceAll("&amp;", "&");
			// for url's that have multiple & next to each other
			if (uriObservable.contains("amp;")) {
				uriObservable = uriObservable.replaceAll("amp;", "&");
			}
		}

		// also grab just the hostname
		String hostname;
		String beginning = "";

		if (uriObservable.contains("http://www.")) {
			beginning = "http://www.";
		} else if (uriObservable.contains("http://")) {
			beginning = "http://";
		} else if (uriObservable.contains("https://www.")) {
			beginning = "https://www.";
		} else if (uriObservable.contains("https://")) {
			beginning = "https://";
		}
		int end = uriObservable.indexOf("/", beginning.length());
		if (end == -1) {
			end = uriObservable.length();
		}
		hostname = uriObservable.substring(beginning.length(), end);
		payload.put("Hostname", hostname);
		payload.put("URI", uriObservable);
	}

	/**
	 * parses the Domain subtype of Observable
	 *
	 * @param observable
	 */
	private void handleDomainObservable(JSONObject observable) {
		payload.put("Type", "Domain");

		JSONObject object = (JSONObject)observable.get("cybox:Object");
		JSONObject properties = (JSONObject)object.get("cybox:Properties");
		String domainObservable = (String)((JSONObject)properties.get("DomainNameObj:Value")).get("content");

		payload.put("Domain", domainObservable);
	}

	/**
	 * parses the Port subtype of Observable
	 *
	 * @param observable
	 */
	private void handlePortObservable(JSONObject observable) {
		payload.put("Type", "Port");

		JSONObject object = (JSONObject)observable.get("cybox:Object");
		JSONObject properties = (JSONObject)object.get("cybox:Properties");
		long portObservable = (long)((JSONObject)properties.get("PortObj:Port_Value")).get("content");

		payload.put("Port", Long.toString(portObservable));
	}

	/**
	 * parses the MD5 Hash subtype of Observable
	 *
	 * @param observable
	 */
	private void handleMD5Observable(JSONObject observable) {
		payload.put("Type", "Hash");

		JSONObject object = (JSONObject)observable.get("cybox:Object");
		JSONObject properties = (JSONObject)object.get("cybox:Properties");

		String hashObservable = (String)((JSONObject)((JSONObject)((JSONObject)properties.get("FileObj:Hashes")).get("cyboxCommon:Hash")).get("cyboxCommon:Simple_Hash_Value")).get("content");
		payload.put("Hash", hashObservable);

		String filename = (String)properties.get("FileObj:File_Name");

		payload.put("Filename", filename);
	}

	private Long convertIPStringToLong(String ip){
		String[] ipSectionsString = ip.split("\\.");
		long[] ipSections = new long[4];
		try{
			for(int i = 0; i < ipSections.length; i++) {
				ipSections[i] = Long.parseLong(ipSectionsString[i]);
			}
			//The long representation of 192.168.1.2 is 192*256^3 + 168*256^2 + 1*256+ 2
			//Using the expanded form so it doesn't need to recalculate each time
			return 16777216L/*256^3*/ * ipSections[0] + 65536L/*256^2*/ * ipSections[1] + 256L*ipSections[2] + ipSections[3];
		}catch(Exception e){
			_LOG.error("IPString to Long conversion failed, invalid IP address", e);
		}

		return 0L;
	}



}
