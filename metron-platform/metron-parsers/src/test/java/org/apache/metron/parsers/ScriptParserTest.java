package org.apache.metron.parsers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adrianwalker.multilinestring.Multiline;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import junit.framework.Assert;

public abstract class ScriptParserTest {

	  public abstract Map getTestData();
	  public abstract String getScriptPath();
	  public abstract String getParseFunction();
	  public abstract String getLanguage();
	  public abstract List<String> getTimeFields();
	  public abstract String getDateFormat();
	  public abstract String getTimestampField();
	  
	  @Test
	  public void test() throws IOException, ParseException {

	    Map<String, Object> parserConfig = new HashMap<>();
	    parserConfig.put("path", getScriptPath());
	    parserConfig.put("function", getParseFunction());
	    parserConfig.put("language", getLanguage());
	    parserConfig.put("timeStampField",getTimestampField());
	    parserConfig.put("dateFormat", getDateFormat());
	    parserConfig.put("timeFields", getTimeFields());

	    ScriptParser scriptParser = new ScriptParser();
	    scriptParser.configure(parserConfig);
	    scriptParser.init();

	    JSONParser jsonParser = new JSONParser();
	    Map<String,String> testData = getTestData();
	    for( Map.Entry<String,String> e : testData.entrySet() ){

	      JSONObject expected = (JSONObject) jsonParser.parse(e.getValue());
	      byte[] rawMessage = e.getKey().getBytes();

	      List<JSONObject> parsedList = scriptParser.parse(rawMessage);
	      System.out.println(parsedList.get(0));
	      Assert.assertEquals(1, parsedList.size());
	      compare(expected, parsedList.get(0));
	    }

	  }

	  public boolean compare(JSONObject expected, JSONObject actual) {
	    MapDifference mapDifferences = Maps.difference(expected, actual);
	    if (mapDifferences.entriesOnlyOnLeft().size() > 0) Assert.fail("Expected JSON has extra parameters: " + mapDifferences.entriesOnlyOnLeft());
	    if (mapDifferences.entriesOnlyOnRight().size() > 0) Assert.fail("Actual JSON has extra parameters: " + mapDifferences.entriesOnlyOnRight());
	    Map actualDifferences = new HashMap();
	    if (mapDifferences.entriesDiffering().size() > 0) {
	      Map differences = Collections.unmodifiableMap(mapDifferences.entriesDiffering());
	      for (Object key : differences.keySet()) {
	        Object expectedValueObject = expected.get(key);
	        Object actualValueObject = actual.get(key);
	        if (expectedValueObject instanceof Long || expectedValueObject instanceof Integer) {
	          Long expectedValue = Long.parseLong(expectedValueObject.toString());
	          Long actualValue = Long.parseLong(actualValueObject.toString());
	          if (!expectedValue.equals(actualValue)) {
	            actualDifferences.put(key, differences.get(key));
	          }
	        } else {
	          actualDifferences.put(key, differences.get(key));
	        }
	      }
	    }
	    if (actualDifferences.size() > 0) Assert.fail("Expected and Actual JSON values don't match: " + actualDifferences);
	    return true;
	  }

}
