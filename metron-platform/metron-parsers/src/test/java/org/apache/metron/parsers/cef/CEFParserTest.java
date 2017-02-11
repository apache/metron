package org.apache.metron.parsers.cef;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.metron.guava.io.Resources;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;

import junit.framework.TestCase;

public class CEFParserTest extends TestCase {

	private static final Charset UTF_8 = Charset.forName("utf-8");
	private CEFParser parser;

	@Override
	public void setUp() {
		parser = new CEFParser();
		parser.init();
	}

	@Test
	public void testEscaping() {
		for (JSONObject obj : parse(
				"Sep 19 08:26:10 host CEF:0|security|threatmanager|1.0|100|detected a \\ in packet|10|src=10.0.0.1 act=blocked a \\ dst=1.1.1.1")) {
			assertEquals("10.0.0.1", obj.get("ip_src_addr"));
			assertEquals("blocked a \\", obj.get("deviceAction"));
			assertEquals("1.1.1.1", obj.get("ip_dst_addr"));
		}
	}

	public void testBasicHeader() {
		for (JSONObject obj : parse(
				"CEF:0|Security|threatmanager|1.0|100|worm successfully stopped|10|src=10.0.0.1 dst=2.1.2.2 spt=1232")) {
			assertEquals("Security", obj.get("DeviceVendor"));
			assertEquals("threatmanager", obj.get("DeviceProduct"));
			assertEquals("1.0", obj.get("DeviceVersion"));
			assertEquals("100", obj.get("DeviceEvent"));
			assertEquals("worm successfully stopped", obj.get("Name"));
			assertEquals(10, obj.get("Severity"));
		}
	}

	public void testBasicExtensions() {
		for (JSONObject obj : parse(
				"CEF:0|Security|threatmanager|1.0|100|worm successfully stopped|10|src=10.0.0.1 dst=2.1.2.2 spt=1232")) {
			assertEquals("10.0.0.1", obj.get("ip_src_addr"));
			assertEquals("2.1.2.2", obj.get("ip_dst_addr"));
			assertEquals(1232, obj.get("ip_src_port"));
		}
	}

	public void testCustomLabelWithSpace() {
		for (JSONObject obj : parse(
				"CEF:0|Security|threatmanager|1.0|100|worm successfully stopped|10|src=10.0.0.1 dst=2.1.2.2 spt=1232 custom=Text with space customLabel=Label with space")) {
			assertEquals(true, obj.containsKey("Label with space"));
			assertEquals("Text with space", obj.get("Label with space"));
		}
	}

	public void testCEFParserAdallom() throws Exception {
		runTest("adallom", Resources.readLines(Resources.getResource(getClass(), "adallom.cef"), UTF_8),
				Resources.toString(Resources.getResource(getClass(), "adallom.schema"), UTF_8));
	}

	public void testCEFParserCyberArk() throws Exception {
		runTest("cyberark", Resources.readLines(Resources.getResource(getClass(), "cyberark.cef"), UTF_8),
				Resources.toString(Resources.getResource(getClass(), "cyberark.schema"), UTF_8),
				Resources.toString(Resources.getResource(getClass(), "cyberark.json"), UTF_8));
	}

	public void testCEFParserWAF() throws Exception {
		URL waf_url = Resources.getResource(getClass(), "waf.cef");
		runTest("waf", Resources.readLines(waf_url, UTF_8),
				Resources.toString(Resources.getResource(getClass(), "waf.schema"), UTF_8));
	}

	private void runTest(String name, List<String> lines, String schema) throws Exception {
		runTest(name, lines, schema, "");
	}

	private void runTest(String name, List<String> lines, String schema, String targetJson) throws Exception {
		for (String inputString : lines) {
			JSONObject parsed = parse(inputString).get(0);
			assertNotNull(parsed);
			assertNotNull(parsed.get("timestamp"));
			assertTrue((long) parsed.get("timestamp") > 0);
			
			System.out.println(parsed);
			JSONParser parser = new JSONParser();

			Map<?, ?> json = null;
			try {
				json = (Map<?, ?>) parser.parse(parsed.toJSONString());
				Assert.assertEquals(true, validateJsonData(schema, json.toString()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			// test against an explicit json example
			if(!targetJson.isEmpty()) {
				
			}
		}
	}

	protected boolean validateJsonData(final String jsonSchema, final String jsonData) throws Exception {
		final JsonNode d = JsonLoader.fromString(jsonData);
		final JsonNode s = JsonLoader.fromString(jsonSchema);

		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
		JsonValidator v = factory.getValidator();

		ProcessingReport report = v.validate(s, d);
		System.out.println(report);

		return report.toString().contains("success");
	}

	private List<JSONObject> parse(String string) {
		List<JSONObject> parse = parser.parse(string.getBytes(Charset.forName("utf-8")));
		assertNotNull(parse);
		return parse;
	}

}
