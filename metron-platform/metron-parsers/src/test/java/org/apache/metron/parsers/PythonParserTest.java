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

package org.apache.metron.parsers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adrianwalker.multilinestring.Multiline;

public class PythonParserTest extends ScriptParserTest {
	
	  public String result=" {"+
	   "\"source\":\"userlog\","+
	    "\"customerId\":117,"+
	   "\"first_name\":\"karthik\","+
	   "\"last_name\":\"narayanan\","+
	   "\"age\":\"38\","+
	   "\"login-time\":\"2016-01-28 15:29:48\","+
	   "\"ip-address\":\"216.21.170.221\","+
	   "\"os\":\"windows 10\","+
	   "\"device\":\"Dell Inspiron\""+
	   "\"original_string\":\"2016-01-28 15:29:48|117|karthik|narayanan|38|216.21.170.221|windows 10|Dell Inspiron\""+
	   "}";
	  
	@Override
	public Map getTestData() {
		Map testData = new HashMap<String,String>();
	    String input = "2016-01-28 15:29:48|117|karthik|narayanan|38|216.21.170.221|windows 10|Dell Inspiron";
	    testData.put(input,result);
	    return testData;
	}

	@Override
	public String getScriptPath() {
		return "../metron-integration-test/src/main/sample/scripts/test.py";
	}

	@Override
	public String getParseFunction() {
		return "parse";
	}

	@Override
	public String getLanguage() {
		return "python";
	}

	@Override
	public List<String> getTimeFields() {
		return null;
	}

	@Override
	public String getDateFormat() {
		return "yyyy-MM-dd HH:mm:ss";
	}

	@Override
	public String getTimestampField() {
		return "login-time";
	}

}
