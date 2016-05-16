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

import org.json.simple.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GrokLenelBadgeParserTest {

	private final String grokPath = "../metron-parsers/src/main/resources/patterns/lenelbadge";
	private final String grokLabel = "LENELBADGE";
	private final String dateFormat = "yyyy-MM-dd HH:mm:ss";
	private final String timestampField = "timestamp_string";
	
	@Test
	public void testParseLine() throws Exception {
		//Set up parser, parse message
		GrokLenelBadgeParser parser = new GrokLenelBadgeParser(grokPath, grokLabel);
		parser.withDateFormat(dateFormat).withTimestampField(timestampField);
		String testString = "<13> server01.data.com \"2016-04-07 17:44:56\" PANEL_ID=\"87223\", PANEL_EVT_ID=\"3458831671\", BLDG_NUM=\"182\", BLDG_NM=\"Kathryn Janeway\", BLDG_USGE_DESC=\"COMMAND\", BLDG_DEPT_ID=\"12421\", BLDG_SPACE_PLANR_NM=\"Tom Paris\", BLDG_STR_ADR_TXT=\"24593 Federation Drive\", BLDG_CITY_NM=\"San Francisco\", BLDG_ST_NM=\"CA\", BLDG_PSTL_CD=\"12345\", CNTRY_NM=\"USA\", BADGE_ID=\"123456\", BADGE_TYPE_DESC=\"Associate\", BADGE_STAT_DESC=\"Active\", BADGE_HOLDR_FRST_NM=\"James\", BADGE_HOLDR_MID_NM=\"Tiberius\", BADGE_HOLDR_LAST_NM=\"Kirk\", BADGE_HOLDR_TYPE_DESC=\"Associate\", EMP_ENT_USER_ID=\"JTK578\", SUPVR_ENT_USER_ID=\"JLP626\", SUPVR_FRST_NM=\"Jean\", SUPVR_MID_NM=\"Luc\", SUPVR_LAST_NM=\"Picard\", EMP_DEPT_ID=\"27159\", EMP_DEPT_NM=\"MN - Processing\", PRSNL_TYPE_CD=\"E\", REG_TEMP_TYPE_CD=\"R\", FULL_TM_PART_TM_TYPE_CD=\"F\", EVT_TYPE_DESC=\"Access Granted\", EVT_TS=\"2016-04-07 17:44:56.0\", EVT_LOCL_DT=\"2016-04-07\", EVT_LOCL_HOUR_NUM=\"12\", EVT_LOCL_WEEK_NUM=\"14\", EVT_LOCL_DAY_NM=\"THURSDAY\", EVT_LOCL_DAY_HOL_IND=\"N\", EMP_FRST_DLY_CMPS_SWIPE_IND=\"N\", EMP_FRST_UNQ_CMPS_SWIPE_IND=\"N\", WPSDW_PUBLN_ID=\"20160410220230\"";
		List<JSONObject> result = parser.parse(testString.getBytes());
		JSONObject parsedJSON = result.get(0);

		assertEquals(parsedJSON.get("priority").toString(), "13");
		assertEquals(parsedJSON.get("panel_id").toString(), "87223");
		assertEquals(parsedJSON.get("panel_event_id").toString(), "3458831671");
		assertEquals(parsedJSON.get("building_number").toString(), "182");
		assertEquals(parsedJSON.get("building_name").toString(), "Kathryn Janeway");
		assertEquals(parsedJSON.get("building_usage_description").toString(), "COMMAND");
		assertEquals(parsedJSON.get("building_department_id").toString(), "12421");
		assertEquals(parsedJSON.get("building_space_planner_name").toString(), "Tom Paris");
		assertEquals(parsedJSON.get("building_street_address").toString(),  "24593 Federation Drive");
		assertEquals(parsedJSON.get("building_city_name").toString(), "San Francisco");
		assertEquals(parsedJSON.get("building_state_name").toString(), "CA");
		assertEquals(parsedJSON.get("building_postal_code").toString(), "12345");
		assertEquals(parsedJSON.get("building_country_name").toString(), "USA");
		assertEquals(parsedJSON.get("badge_id").toString(), "123456");
		assertEquals(parsedJSON.get("badge_type_description").toString(), "Associate");
		assertEquals(parsedJSON.get("badge_status_description").toString(), "Active");
		assertEquals(parsedJSON.get("badge_holder_first_name").toString(), "James");
		assertEquals(parsedJSON.get("badge_holder_middle_name").toString(), "Tiberius");
		assertEquals(parsedJSON.get("badge_holder_last_name").toString(), "Kirk");
		assertEquals(parsedJSON.get("badge_holder_type_description").toString(), "Associate");
		assertEquals(parsedJSON.get("user_id").toString(), "JTK578");
		assertEquals(parsedJSON.get("supervisor_user_id").toString(), "JLP626");
		assertEquals(parsedJSON.get("supervisor_first_name").toString(), "Jean");
		assertEquals(parsedJSON.get("supervisor_middle_name").toString(), "Luc");
		assertEquals(parsedJSON.get("supervisor_last_name").toString(), "Picard");
		assertEquals(parsedJSON.get("employee_department_id").toString(), "27159");
		assertEquals(parsedJSON.get("employee_department_name").toString(), "MN - Processing");
		assertEquals(parsedJSON.get("personnel_type_code").toString(), "E");
		assertEquals(parsedJSON.get("regular_temp_type_code").toString(), "R");
		assertEquals(parsedJSON.get("full_time_part_time_type_code").toString(), "F");
		assertEquals(parsedJSON.get("event_type_description").toString(), "Access Granted");
		assertEquals(parsedJSON.get("timestamp").toString(), "1460051096000");
		assertEquals(parsedJSON.get("event_local_date").toString(), "2016-04-07");
		assertEquals(parsedJSON.get("event_local_hour_num").toString(), "12");
		assertEquals(parsedJSON.get("event_local_week_num").toString(), "14");
		assertEquals(parsedJSON.get("event_local_day_name").toString(), "THURSDAY");
		assertEquals(parsedJSON.get("event_local_day_holiday_indicator").toString(), "N");
		assertEquals(parsedJSON.get("emp_first_dly_cmps_swipe_ind").toString(), "N");
		assertEquals(parsedJSON.get("emp_first_unq_cmps_swipe_ind").toString(), "N");
		assertEquals(parsedJSON.get("wpsdw_publn_id").toString(), "20160410220230");
	}

	@Test
	public void testParseMalformedLine() throws Exception {
		//Set up parser, attempt to parse malformed message
		GrokLenelBadgeParser parser = new GrokLenelBadgeParser(grokPath, grokLabel);
		String testString = "\"<13> server01.data.com \"2016-04-07 17:44:56\" PANEL_ID=\"87223\", PANEL_EVT_ID=\"3458831671\", BLD";
		List<JSONObject> result = parser.parse(testString.getBytes());
		assertEquals(null, result);
	}

	@Test
	public void testParseEmptyLine() throws Exception {
		//Set up parser, attempt to parse malformed message
		GrokLenelBadgeParser parser = new GrokLenelBadgeParser(grokPath, grokLabel);
		String testString = "";
		List<JSONObject> result = parser.parse(testString.getBytes());		
		assertEquals(null, result);
	}
		
}
