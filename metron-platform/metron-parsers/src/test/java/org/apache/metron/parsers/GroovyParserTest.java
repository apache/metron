package org.apache.metron.parsers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adrianwalker.multilinestring.Multiline;

public class GroovyParserTest extends ScriptParserTest {
	  
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
		return "../metron-integration-test/src/main/sample/scripts/test.groovy";
	}

	@Override
	public String getParseFunction() {
		return "parse";
	}

	@Override
	public String getLanguage() {
		return "groovy";
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
