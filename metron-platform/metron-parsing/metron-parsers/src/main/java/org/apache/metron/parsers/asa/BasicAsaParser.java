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
package org.apache.metron.parsers.asa;

import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.metron.common.Constants;
import org.apache.metron.common.utils.LazyLogger;
import org.apache.metron.common.utils.LazyLoggerFactory;
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.ParseException;
import org.apache.metron.parsers.utils.SyslogUtils;
import org.json.simple.JSONObject;

public class BasicAsaParser extends BasicParser {

  protected static final LazyLogger LOG = LazyLoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected Clock deviceClock;
  private String syslogPattern = "%{CISCO_TAGGED_SYSLOG}";

  private Grok syslogGrok;

  private static final Map<String, String> patternMap = ImmutableMap.<String, String> builder()
      .put("ASA-2-106001", "CISCOFW106001")
      .put("ASA-2-106006", "CISCOFW106006_106007_106010")
      .put("ASA-2-106007", "CISCOFW106006_106007_106010")
      .put("ASA-2-106010", "CISCOFW106006_106007_106010")
      .put("ASA-3-106014", "CISCOFW106014")
      .put("ASA-6-106015", "CISCOFW106015")
      .put("ASA-1-106021", "CISCOFW106021")
      .put("ASA-4-106023", "CISCOFW106023")
      .put("ASA-5-106100", "CISCOFW106100")
      .put("ASA-6-110002", "CISCOFW110002")
      .put("ASA-6-302010", "CISCOFW302010")
      .put("ASA-6-302013", "CISCOFW302013_302014_302015_302016")
      .put("ASA-6-302014", "CISCOFW302013_302014_302015_302016")
      .put("ASA-6-302015", "CISCOFW302013_302014_302015_302016")
      .put("ASA-6-302016", "CISCOFW302013_302014_302015_302016")
      .put("ASA-6-302020", "CISCOFW302020_302021")
      .put("ASA-6-302021", "CISCOFW302020_302021")
      .put("ASA-6-305011", "CISCOFW305011")
      .put("ASA-3-313001", "CISCOFW313001_313004_313008")
      .put("ASA-3-313004", "CISCOFW313001_313004_313008")
      .put("ASA-3-313008", "CISCOFW313001_313004_313008")
      .put("ASA-4-313005", "CISCOFW313005")
      .put("ASA-4-402117", "CISCOFW402117")
      .put("ASA-4-402119", "CISCOFW402119")
      .put("ASA-4-419001", "CISCOFW419001")
      .put("ASA-4-419002", "CISCOFW419002")
      .put("ASA-4-500004", "CISCOFW500004")
      .put("ASA-6-602303", "CISCOFW602303_602304")
      .put("ASA-6-602304", "CISCOFW602303_602304")
      .put("ASA-7-710001", "CISCOFW710001_710002_710003_710005_710006")
      .put("ASA-7-710002", "CISCOFW710001_710002_710003_710005_710006")
      .put("ASA-7-710003", "CISCOFW710001_710002_710003_710005_710006")
      .put("ASA-7-710005", "CISCOFW710001_710002_710003_710005_710006")
      .put("ASA-7-710006", "CISCOFW710001_710002_710003_710005_710006")
      .put("ASA-6-713172", "CISCOFW713172")
      .put("ASA-4-733100", "CISCOFW733100")
      .put("ASA-6-305012", "CISCOFW305012")
      .put("ASA-7-609001", "CISCOFW609001")
      .put("ASA-7-609002", "CISCOFW609002")
      .put("ASA-5-713041", "CISCOFW713041")
      .build();

  private Map<String, Grok> grokers = new HashMap<String, Grok>(patternMap.size());

  @Override
  public void configure(Map<String, Object> parserConfig) {
    String timeZone = (String) parserConfig.get("deviceTimeZone");
    if (timeZone != null)
      deviceClock = Clock.system(ZoneId.of(timeZone));
    else {
      deviceClock = Clock.systemUTC();
      LOG.warn("[Metron] No device time zone provided; defaulting to UTC");
    }
  }

  private void addGrok(String key, String pattern) throws GrokException {
    Grok grok = new Grok();
    InputStream patternStream = this.getClass().getResourceAsStream("/patterns/asa");
    grok.addPatternFromReader(new InputStreamReader(patternStream));
    grok.compile("%{" + pattern + "}");
    grokers.put(key, grok);
  }

  @Override
  public void init() {
    syslogGrok = new Grok();
    InputStream syslogStream = this.getClass().getResourceAsStream("/patterns/asa");
    try {
      syslogGrok.addPatternFromReader(new InputStreamReader(syslogStream));
      syslogGrok.compile(syslogPattern);
    } catch (GrokException e) {
      LOG.error("[Metron] Failed to load grok patterns from jar", e);
      throw new RuntimeException(e.getMessage(), e);
    }

    for (Entry<String, String> pattern : patternMap.entrySet()) {
      try {
        addGrok(pattern.getKey(), pattern.getValue());
      } catch (GrokException e) {
        LOG.error("[Metron] Failed to load grok pattern {} for ASA tag {}", pattern.getValue(), pattern.getKey());
      }
    }

    LOG.info("[Metron] CISCO ASA Parser Initialized");
  }

  @Override
  public List<JSONObject> parse(byte[] rawMessage) {
    String logLine = "";
    String messagePattern = "";
    JSONObject metronJson = new JSONObject();
    List<JSONObject> messages = new ArrayList<>();
    Map<String, Object> syslogJson = new HashMap<String, Object>();

    try {
      logLine = new String(rawMessage, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.error("[Metron] Could not read raw message", e);
      throw new RuntimeException(e.getMessage(), e);
    }

    try {
      LOG.debug("[Metron] Started parsing raw message: {}", logLine);
      Match syslogMatch = syslogGrok.match(logLine);
      syslogMatch.captures();
      if (!syslogMatch.isNull()) {
	syslogJson = syslogMatch.toMap();
	LOG.trace("[Metron] Grok CISCO ASA syslog matches: {}", syslogMatch::toJson);

	metronJson.put(Constants.Fields.ORIGINAL.getName(), logLine);
	metronJson.put(Constants.Fields.TIMESTAMP.getName(),
	    SyslogUtils.parseTimestampToEpochMillis((String) syslogJson.get("CISCOTIMESTAMP"), deviceClock));
	metronJson.put("ciscotag", syslogJson.get("CISCOTAG"));
	metronJson.put("syslog_severity", SyslogUtils.getSeverityFromPriority((int) syslogJson.get("syslog_pri")));
	metronJson.put("syslog_facility", SyslogUtils.getFacilityFromPriority((int) syslogJson.get("syslog_pri")));

	if (syslogJson.get("syslog_host") != null) {
	  metronJson.put("syslog_host", syslogJson.get("syslog_host"));
	}
	if (syslogJson.get("syslog_prog") != null) {
	  metronJson.put("syslog_prog", syslogJson.get("syslog_prog"));
	}

      } else
	throw new RuntimeException(
	    String.format("[Metron] Message '%s' does not match pattern '%s'", logLine, syslogPattern));
    } catch (ParseException e) {
      LOG.error("[Metron] Could not parse message timestamp", e);
      throw new RuntimeException(e.getMessage(), e);
    } catch (RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }

    try {
      messagePattern = (String) syslogJson.get("CISCOTAG");
      Grok asaGrok = grokers.get(messagePattern);

      if (asaGrok == null)
	LOG.info("[Metron] No pattern for ciscotag '{}'", syslogJson.get("CISCOTAG"));
      else {

	String messageContent = (String) syslogJson.get("message");
	Match messageMatch = asaGrok.match(messageContent);
	messageMatch.captures();
	if (!messageMatch.isNull()) {
	  Map<String, Object> messageJson = messageMatch.toMap();
	  LOG.trace("[Metron] Grok CISCO ASA message matches: {}", messageMatch::toJson);

	  String src_ip = (String) messageJson.get("src_ip");
	  if (src_ip != null)
	    metronJson.put(Constants.Fields.SRC_ADDR.getName(), src_ip);

	  Integer src_port = (Integer) messageJson.get("src_port");
	  if (src_port != null)
	    metronJson.put(Constants.Fields.SRC_PORT.getName(), src_port);

	  String dst_ip = (String) messageJson.get("dst_ip");
	  if (dst_ip != null)
	    metronJson.put(Constants.Fields.DST_ADDR.getName(), dst_ip);

	  Integer dst_port = (Integer) messageJson.get("dst_port");
	  if (dst_port != null)
	    metronJson.put(Constants.Fields.DST_PORT.getName(), dst_port);

	  String protocol = (String) messageJson.get("protocol");
	  if (protocol != null)
	    metronJson.put(Constants.Fields.PROTOCOL.getName(), protocol.toLowerCase());

	  String action = (String) messageJson.get("action");
	  if (action != null)
	    metronJson.put("action", action.toLowerCase());
	} else
	  LOG.warn("[Metron] Message '{}' did not match pattern for ciscotag '{}'", logLine,
	      syslogJson.get("CISCOTAG"));
      }

      LOG.debug("[Metron] Final normalized message: {}", metronJson::toString);

    } catch (RuntimeException e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }

    messages.add(metronJson);
    return messages;
  }
}
