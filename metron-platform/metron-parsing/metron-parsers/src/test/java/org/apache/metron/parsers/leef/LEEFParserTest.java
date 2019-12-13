/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.parsers.leef;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.google.common.io.Resources;
import org.apache.metron.common.Constants.Fields;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class LEEFParserTest {
  private LEEFParser parser;

  @BeforeEach
  public void setUp() {
    parser = new LEEFParser();
    parser.init();
  }

  @Test
  public void testInvalid() {
    List<JSONObject> obj = parse("test test test nonsense\n");
    assertEquals(0, obj.size());
  }

  @Test
  public void testTimestampPriority() throws java.text.ParseException {
    long correctTime =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz")
            .parse("2016-05-01T09:29:11.356-0400")
            .getTime();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz");

    for (JSONObject obj :
        parse(
            "LEEF:2.0|Lancope|StealthWatch|1.0|41|src=10.0.0.1\tdevTime=May 1 2016 09:29:11.356 -0400\tdst=2.1.2.2\tspt=1232")) {
      assertEquals(new Date(correctTime), new Date((long) obj.get(Fields.TIMESTAMP.getName())));
      assertEquals(correctTime, obj.get(Fields.TIMESTAMP.getName()));
    }
    for (JSONObject obj :
        parse(
            "2016-06-01T09:29:11.356-04:00 host LEEF:2.0|Lancope|StealthWatch|1.0|41|src=10.0.0.1\tdevTime=May 1 2016 09:29:11.356 -0400\tdst=2.1.2.2\tspt=1232")) {
      assertEquals(new Date(correctTime), new Date((long) obj.get(Fields.TIMESTAMP.getName())));
      assertEquals(correctTime, obj.get(Fields.TIMESTAMP.getName()));
    }
    for (JSONObject obj :
        parse(
            "2016-05-01T09:29:11.356-04:00 host LEEF:2.0|Lancope|StealthWatch|1.0|41|src=10.0.0.1\tdevTime=May 1 2016 09:29:11.356 -0400\tdst=2.1.2.2\tspt=1232")) {
      assertEquals(new Date(correctTime), new Date((long) obj.get(Fields.TIMESTAMP.getName())));
      assertEquals(correctTime, obj.get(Fields.TIMESTAMP.getName()));
    }
    for (JSONObject obj :
        parse(
            "LEEF:2.0|Lancope|StealthWatch|1.0|41|src=10.0.0.1\tdevTime=May 1 2016 09:29:11.356 -0400\tdst=2.1.2.2\tspt=1232")) {
      assertNotNull(obj.get(Fields.TIMESTAMP.getName()));
    }
  }

  private void runMissingYear(Calendar expected, Calendar input) {
    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss.SSS");
    for (JSONObject obj :
        parse(
            "LEEF:2.0|Lancope|StealthWatch|1.0|41|\t|src=10.0.0.1\tdevTime="
                + sdf.format(input.getTime())
                + "\tdevTimeFormat=MMM dd HH:mm:ss.SSS"
                + "\tdst=2.1.2.2\tspt=1232")) {
      assertEquals(expected.getTime(), new Date((long) obj.get(Fields.TIMESTAMP.getName())));
      assertEquals(expected.getTimeInMillis(), obj.get(Fields.TIMESTAMP.getName()));
    }
  }

  @Test
  public void testMissingYearFromDate() {
    Calendar current = Calendar.getInstance();
    Calendar correct = Calendar.getInstance();

    correct.setTimeInMillis(current.getTimeInMillis());

    runMissingYear(correct, current);
  }

  @Test
  public void testFourDayFutureBecomesPast() {
    Calendar current = Calendar.getInstance();
    Calendar correct = Calendar.getInstance();

    current.add(Calendar.DAY_OF_MONTH, 5);
    // correct.setTime(current.getTime());
    correct.setTimeInMillis(current.getTimeInMillis());
    correct.add(Calendar.YEAR, -1);

    runMissingYear(correct, current);
  }

  /**
   * Sample from
   * https://docs.imperva.com/bundle/cloud-application-security/page/more/example-logs.htm#logEx2
   */
  @Test
  public void testLEEF_CEFlikeSample() {
    List<JSONObject> parse =
        parse(
            "LEEF:0|Incapsula|SIEMintegration|0|SQL Injection| fileId=3412364560000000008 sourceServiceName=test56111115.incaptest.co siteid=1333546 suid=300656 requestClientApplication=Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0 popName=mia cs2=true cs2Label=Javascript Support cs3=true cs3Label=CO Support cs1=NA cs1Label=Cap Support cs4=936e64c2-bdd1-4719-9bd0-2d882a72f30d cs4Label=VID cs5=bab1712be85b00ab21d20bf0d7b5db82701f27f53fbac19a4252efc722ac9131fdc60c0da620282b02dfb8051e7a60f9 cs5Label=clappsig dproc=Browser cs6=Firefox cs6Label=clapp calCountryOrRegion=IL cicode=Rehovot cs7=31.8969 cs7Label=latitude cs8=34.8186 cs8Label=longitude Customer=siemtest start=1460303291788 url=test56111115.incaptest.co/ requestMethod=GET qstr=keywords\\=3%29%29%29%20AND%203434%3d%28%27%3amvc%3a%27%7c%7c%28SELECT%20CASE%203434%20WHEN%203434%20THEN%201%20ELSE%200%20END%20FROM%20RDB%24DATABASE%29%7c%7c%27%3aqvi%3a%27%29%20AND%20%28%28%283793%3d3793 cn1=200 proto=HTTP cat=REQ_PASSED deviceExternalId=2323800832649 dst=54.195.35.43 dstPort=80 in=406 xff=127.0.0.1 srcPort=443 src=127.0.0.1 protoVer=TLSv1.2 ECDHE-RSA-AES128-GCM-SHA256 fileType=12999,50999,50037,50044, filePermission=37,20,1,1, cs9=,High Risk SQL Expressions,,SQL SELECT Expression, cs9Label=Rule name");
    JSONObject obj = parse.get(0);
    assertNotNull(obj);
    assertEquals("3412364560000000008", obj.get("fileId"));
    assertEquals(
        "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:38.0) Gecko/20100101 Firefox/38.0",
        obj.get("requestClientApplication"));
    assertTrue(obj.containsKey("longitude"));
    assertFalse(obj.containsKey("cs8"));
    assertFalse(obj.containsKey("cs8Label"));
  }

  @Test
  public void testLEEFParserSample() throws Exception {
    runTest(
        "sample",
        Resources.readLines(
            Resources.getResource(getClass(), "sample.leef"), StandardCharsets.UTF_8),
        Resources.toString(
            Resources.getResource(getClass(), "sample.schema"), StandardCharsets.UTF_8));
  }

  private void runTest(String name, List<String> lines, String schema) throws Exception {
    runTest(name, lines, schema, "");
  }

  private void runTest(String name, List<String> lines, String schema, String targetJson)
      throws Exception {
    for (String inputString : lines) {
      JSONObject parsed = parse(inputString).get(0);
      assertNotNull(parsed);
      assertNotNull(parsed.get(Fields.TIMESTAMP.getName()));
      assertTrue((long) parsed.get(Fields.TIMESTAMP.getName()) > 0);

      JSONParser parser = new JSONParser();

      Map<?, ?> json = null;
      json = (Map<?, ?>) parser.parse(parsed.toJSONString());
      assertEquals(true, validateJsonData(schema, json.toString()));
    }
  }

  private void assertSimpleSample(List<JSONObject> parse) {
    JSONObject obj = parse.get(0);
    assertNotNull(obj);
    assertTrue(obj.containsKey(Fields.SRC_ADDR.getName()));
    assertEquals("192.0.2.0", obj.get(Fields.SRC_ADDR.getName()));
  }

  @Test
  public void testLEEF_1_0_versionIncluded() {
    List<JSONObject> parse =
        parse(
            "LEEF:1.0|Microsoft|MSExchange|4.0 SP1|15345| src=192.0.2.0\tdst=172.50.123.1\tsev=5\tcat=anomaly\tsrcPort=81\tdstPort=21\tusrName=joe.black");
    assertSimpleSample(parse);
  }

  @Test
  public void testLEEF_2_0() {
    List<JSONObject> parse =
        parse(
            "LEEF:2.0|Vendor|Product|Version|EventID| src=192.0.2.0\tdst=172.50.123.1\tsev=5\tcat=anomaly\tsrcPort=81\tdstPort=21\tusrName=joe.black");
    assertSimpleSample(parse);
  }

  @Test
  public void testLEEF_2_0_delimiterSpecified() {
    List<JSONObject> parse =
        parse(
            "LEEF:2.0|Lancope|StealthWatch|1.0|41|^| src=192.0.2.0^dst=172.50.123.1^sev=5^cat=anomaly^srcPort=81^dstPort=21^usrName=joe.black");
    assertSimpleSample(parse);
  }

  @Test
  public void testLEEF_2_0_delimiterUsedIncorrectly() {
    List<JSONObject> parse =
        parse(
            "LEEF:2.0|Lancope|StealthWatch|1.0|41|^| src=192.0.2.0\tdst=172.50.123.1\tsev=5\tcat=anomaly\tsrcPort=81\tdstPort=21\tusrName=joe.black");
    assertFalse(parse.get(0).containsKey(Fields.DST_ADDR));
  }

  @Test
  public void testLEEFMultiLine() {
    List<JSONObject> parse =
        parse(
            "LEEF:2.0|Vendor|Product|Version|EventID| src=192.0.2.0\tdst=172.50.123.1\tsev=5\tcat=anomaly\tsrcPort=81\tdstPort=21\tusrName=line1"
                + "\nLEEF:2.0|Vendor|Product|Version|EventID| src=192.0.2.1\tdst=172.50.123.2\tsev=6\tcat=anomaly\tsrcPort=82\tdstPort=22\tusrName=line2");
    assertSimpleSample(parse);
    assertEquals(2, parse.size());
  }

  @Test
  public void testLEEFcustomdevTimeFormat() {
    String customFormat = "yyyy-MM-dd HH:mm:ss.SSS zzz";
    Date customDate = new Date();
    DateFormat customFormatter = new SimpleDateFormat(customFormat);

    List<JSONObject> parse =
        parse(
            "LEEF:2.0|Lancope|StealthWatch|1.0|41|^| src=192.0.2.0^dst=172.50.123.1^sev=5^cat=anomaly^srcPort=81^dstPort=21^usrName=joe.black^devTime="
                + customFormatter.format(customDate)
                + "^devTimeFormat="
                + customFormat);
    JSONObject obj = parse.get(0);
    assertEquals(obj.get(Fields.TIMESTAMP.getName()), customDate.getTime());
  }

  @Test
  public void testLEEFdevTimeWithNoCustomFormat() {
    String standardFormat = "MMM dd yyyy HH:mm:ss.SSS zzz";
    Date customDate = new Date();
    long expected = customDate.getTime();
    DateFormat customFormatter = new SimpleDateFormat(standardFormat);

    List<JSONObject> parse =
        parse(
            "LEEF:2.0|Lancope|StealthWatch|1.0|41|^| src=192.0.2.0^dst=172.50.123.1^sev=5^cat=anomaly^srcPort=81^dstPort=21^usrName=joe.black^devTime="
                + customFormatter.format(customDate));
    JSONObject obj = parse.get(0);
    assertEquals(obj.get(Fields.TIMESTAMP.getName()), expected);
  }

  protected boolean validateJsonData(final String jsonSchema, final String jsonData)
      throws Exception {
    final JsonNode d = JsonLoader.fromString(jsonData);
    final JsonNode s = JsonLoader.fromString(jsonSchema);

    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    JsonValidator v = factory.getValidator();

    ProcessingReport report = v.validate(s, d);

    return report.toString().contains("success");
  }

  private List<JSONObject> parse(String string) {
    Optional<MessageParserResult<JSONObject>> parse =
        parser.parseOptionalResult(string.getBytes(StandardCharsets.UTF_8));
    assertTrue(parse.isPresent());
    return parse.get().getMessages();
  }

  @Test
  public void getsReadCharsetFromConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
  }

  @Test
  public void getsReadCharsetFromDefault() {
    Map<String, Object> config = new HashMap<>();
    parser.configure(config);
    assertThat(parser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
  }
}
