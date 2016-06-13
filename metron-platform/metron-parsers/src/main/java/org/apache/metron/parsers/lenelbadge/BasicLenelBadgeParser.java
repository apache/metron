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

package org.apache.metron.parsers.lenelbadge;

import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class BasicLenelBadgeParser extends BasicParser {

	private static final Map<String, String> keyMap;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("PANEL_ID","panel_id");
		map.put("PANEL_EVT_ID","panel_event_id");
		map.put("BLDG_NUM","building_number");
		map.put("BLDG_NM","building_name");
		map.put("BLDG_USGE_DESC","building_usage_description");
		map.put("BLDG_DEPT_ID","building_department_id");
		map.put("BLDG_SPACE_PLANR_NM","building_space_planner_name");
		map.put("BLDG_STR_ADR_TXT","building_street_address");
		map.put("BLDG_CITY_NM","building_city_name");
		map.put("BLDG_ST_NM","building_state_name");
		map.put("BLDG_PSTL_CD","building_postal_code");
		map.put("CNTRY_NM","building_country_name");
		map.put("BADGE_ID","badge_id");
		map.put("BADGE_TYPE_DESC","badge_type_description");
		map.put("BADGE_STAT_DESC","badge_status_description");
		map.put("BADGE_HOLDR_FRST_NM","badge_holder_first_name");
		map.put("BADGE_HOLDR_MID_NM","badge_holder_middle_name");
		map.put("BADGE_HOLDR_LAST_NM","badge_holder_last_name");
		map.put("BADGE_HOLDR_TYPE_DESC","badge_holder_type_description");
		map.put("EMP_ENT_USER_ID","user_id");
		map.put("SUPVR_ENT_USER_ID","supervisor_user_id");
		map.put("SUPVR_FRST_NM","supervisor_first_name");
		map.put("SUPVR_MID_NM","supervisor_middle_name");
		map.put("SUPVR_LAST_NM","supervisor_last_name");
		map.put("EMP_DEPT_ID","employee_department_id");
		map.put("EMP_DEPT_NM","employee_department_name");
		map.put("PRSNL_TYPE_CD","personnel_type_code");
		map.put("REG_TEMP_TYPE_CD","regular_temp_type_code");
		map.put("FULL_TM_PART_TM_TYPE_CD","full_time_part_time_type_code");
		map.put("EVT_TYPE_DESC","event_type_description");
		map.put("EVT_TS","event_timestamp");
		map.put("EVT_LOCL_DT","event_local_date");
		map.put("EVT_LOCL_HOUR_NUM","event_local_hour_num");
		map.put("EVT_LOCL_WEEK_NUM","event_local_week_num");
		map.put("EVT_LOCL_DAY_NM","event_local_day_name");
		map.put("EVT_LOCL_DAY_HOL_IND","event_local_day_holiday_indicator");
		map.put("EMP_FRST_DLY_CMPS_SWIPE_IND","emp_first_dly_cmps_swipe_ind");
		map.put("EMP_FRST_UNQ_CMPS_SWIPE_IND","emp_first_unq_cmps_swipe_ind");
		map.put("WPSDW_PUBLN_ID","wpsdw_publn_id");
		keyMap = Collections.unmodifiableMap(map);
	}

	private static final Logger LOG = LoggerFactory.getLogger(BasicLenelBadgeParser.class);
	private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");

	@Override
	public void init() { }

	@Override
	public void configure(Map<String, Object> parserConfig) { }

	@Override
	public List<JSONObject> parse(byte[] rawMessage) throws Exception {
		List<JSONObject> messages = new ArrayList<>();
		JSONObject payload = new JSONObject();

		try {
			String message = new String(rawMessage, "UTF-8");
			payload.put("original_string", message);

			// split message into comma delimited substrings
			String[] substrings = message.split(",");

			// the first substring contains timestamp and priority as well as a key value pair
			// parse and remove the extra data from substring[0]
			String [] logstart = substrings[0].split("<|>| | \"");
			Date date = df.parse(logstart[4].replace("\"","") + " " +logstart[5].replace("\"","") + " UTC");
			long epoch = date.getTime();
			payload.put("timestamp", epoch);
			payload.put("priority", logstart[1]);

			substrings[0] = logstart[6];

			// now all the substrings are key value pairs with format key="value"
			for (int i = 0; i < substrings.length; ++i){
				String[] kvp = substrings[i].split("=",2);
				if (keyMap.containsKey(kvp[0].trim())){
					// format keys
					String key = keyMap.get(kvp[0].trim());
					// remove quotes that surround values
					String value = kvp[1];
					payload.put(key, value.substring(1, value.length()-1));
				}
			}

			removeEmptyFields(payload);
			messages.add(payload);
			return messages;
		} catch (Exception e) {
			LOG.error("Failed to parse: " + new String(rawMessage), e);
			throw e;
		}

	}

	@SuppressWarnings("unchecked")
	private void removeEmptyFields(JSONObject json) {
		Iterator<Object> keyIter = json.keySet().iterator();
		while (keyIter.hasNext()) {
			Object key = keyIter.next();
			Object value = json.get(key);
			if (null == value || "".equals(value.toString())) {
				keyIter.remove();
			}
		}
	}

}
