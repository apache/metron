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
package org.apache.metron.parsers.leef;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.metron.common.Constants.Fields;
import org.apache.metron.parsers.BasicParser;
import org.apache.metron.parsers.DefaultMessageParserResult;
import org.apache.metron.parsers.ParseException;
import org.apache.metron.parsers.cef.CEFParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.parsers.utils.DateUtils;
import org.apache.metron.parsers.utils.SyslogUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LEEF Parser
 *
 * Based primarily on the IBM LEEF guide at https://www.ibm.com/support/knowledgecenter/SS42VS_DSM/c_LEEF_Format_Guide_intro.html
 * with support for other found in the wild examples shown in tests.
 *
 */
public class LEEFParser extends BasicParser {
  private static final long serialVersionUID = 1L;

  public enum HeaderFields {
    DEVICE_VENDOR("DeviceVendor"),
    DEVICE_PRODUCT("DeviceProduct"),
    DEVICE_VERSION("DeviceVersion"),
    DEVICE_EVENT("DeviceEvent"),
    DELIMITER("Delimiter"),
    VERSION("Version")
    ;

    private String name;

    HeaderFields(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  // Field name for custom device time in LEEF
  private static final String DEV_TIME = "devTime";
  private static final String DEV_TIME_FORMAT = "devTimeFormat";

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String HEADER_CAPTURE_PATTERN = "[^\\|]*";

  private Pattern pattern;

  public void init() {

    // LEEF Headers: Version|Device Vendor|Device Product|Device Version|Device Event|Delimiter
    String syslogTime = "(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\b +(?:(?:0[1-9])|(?:[12][0-9])|(?:3[01])|[1-9]) (?!<[0-9])(?:2[0123]|[01]?[0-9]):(?:[0-5][0-9])(?::(?:(?:[0-5]?[0-9]|60)(?:[:.,][0-9]+)?))(?![0-9])?";
    String syslogTime5424 = "(?:\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2}))";
    String syslogPriority = "<(?:[0-9]+)>";
    String syslogHost = "[a-z0-9\\.\\\\-_]+";

    StringBuilder sb = new StringBuilder("");
    sb.append("(?<syslogPriority>");
    sb.append(syslogPriority);
    sb.append(")?");
    sb.append("(?<syslogTime>");
    sb.append(syslogTime);
    sb.append("|");
    sb.append(syslogTime5424);
    sb.append(")?");

    sb.append("(?<syslogHost>");
    sb.append(syslogHost);
    sb.append(")?");

    sb.append(".*");

    sb.append("LEEF:(?<");
    sb.append(HeaderFields.VERSION.getName());
    sb.append(">1.0|2.0|0)?\\|");

    headerBlock(HeaderFields.DEVICE_VENDOR.getName(), sb);
    sb.append("\\|");
    headerBlock(HeaderFields.DEVICE_PRODUCT.getName(), sb);
    sb.append("\\|");
    headerBlock(HeaderFields.DEVICE_VERSION.getName(), sb);
    sb.append("\\|");
    headerBlock(HeaderFields.DEVICE_EVENT.getName(), sb);
    sb.append("\\|");

    // add optional delimiter header (only applicable for LEEF 2.0)
    sb.append("(");
    headerBlock(HeaderFields.DELIMITER.getName(), sb);
    sb.append("\\|");
    sb.append(")?");

    // extension capture:
    sb.append(" ?(?<extensions>.*)");
    pattern = Pattern.compile(sb.toString());
  }

  public Optional<MessageParserResult<JSONObject>> parseOptionalResult(byte[] rawMessage) {
    List<JSONObject> messages = new ArrayList<>();
    Map<Object,Throwable> errors = new HashMap<>();
    String originalMessage = null;

    try (BufferedReader reader = new BufferedReader(new StringReader(new String(rawMessage, StandardCharsets.UTF_8)))) {
      while ((originalMessage = reader.readLine()) != null) {
        Matcher matcher = pattern.matcher(originalMessage);
        while (matcher.find()) {
          JSONObject obj = new JSONObject();
          if (!matcher.matches()) {
            break;
          }
          LOG.debug("Found %d groups", matcher.groupCount());
          obj.put(HeaderFields.DEVICE_VENDOR.getName(),
              matcher.group(HeaderFields.DEVICE_VENDOR.getName()));
          obj.put(HeaderFields.DEVICE_PRODUCT.getName(),
              matcher.group(HeaderFields.DEVICE_PRODUCT.getName()));
          obj.put(HeaderFields.DEVICE_VERSION.getName(),
              matcher.group(HeaderFields.DEVICE_VERSION.getName()));
          obj.put(HeaderFields.DEVICE_EVENT.getName(),
              matcher.group(HeaderFields.DEVICE_EVENT.getName()));

          String ext = matcher.group("extensions");

          // In LEEF 2.0 the delimiter can be specified
          String version = matcher.group(HeaderFields.VERSION.getName());
          if (version.equals("2.0")) {
            String delimiter = matcher.group(HeaderFields.DELIMITER.getName());
            if (delimiter == null || delimiter.length() == 0) {
              delimiter = "\\t";
            }
            delimiter = "(?<!\\\\)[" + delimiter.replace("^", "\\^").replace("\t", "\\t") + "]";

            String[] kvs = ext.split(delimiter);
            for (String kv : kvs) {
              String[] a = kv.split("=");
              obj.put(a[0], a[1]);
            }
          } else if (version.equals("1.0") || version.isEmpty()) {
            String delimiter = "\t";
            String[] kvs = ext.split(delimiter);
            for (String kv : kvs) {
              String[] a = kv.split("=");
              obj.put(a[0], a[1]);
            }
          } else {
            // Found in the wild examples using CEF rules, which need to handle the processing per the CEFParser
            // Note that technically LEEF does not support the CEF approach to numbered custom variables.
            // We however do here, due to some found in the wild exceptions to the standard.
            CEFParser.parseExtensions(ext, obj);
          }

          // Rename standard CEF fields to comply with Metron standards
          obj = mutate(obj, "dst", Fields.DST_ADDR.getName());
          obj = mutate(obj, "dstPort", Fields.DST_PORT.getName());
          obj = convertToInt(obj, Fields.DST_PORT.getName());

          obj = mutate(obj, "src", Fields.SRC_ADDR.getName());
          obj = mutate(obj, "srcPort", Fields.SRC_PORT.getName());
          obj = convertToInt(obj, Fields.SRC_PORT.getName());

          obj.put(Fields.ORIGINAL.getName(), originalMessage);

          // add the host
          String host = matcher.group("syslogHost");
          if (!(host == null || host.isEmpty())) {
            obj.put("host", host);
          }

          // apply timestamp from message if present, using devTime, syslog
          // timestamp,
          // default to current system time
          //devTime, devTimeFormat, calLanguage, calCountryOrRegion
          if (obj.containsKey(DEV_TIME)) {
            String devTime = (String) obj.get(DEV_TIME);
            try {
              // DateFormats allowed in LEEF
              // epoch
              // MMM dd yyyy HH:mm:ss
              // MMM dd yyyy HH:mm:ss.SSS
              // MMM dd yyyy HH:mm:ss.SSS zzz
              // custom in devTimeFormat field
              final String devTimeFormat = (String) obj.get(DEV_TIME_FORMAT);

              List<SimpleDateFormat> formats = (obj.containsKey(DEV_TIME_FORMAT)) ?
                  new ArrayList<SimpleDateFormat>() {{
                    add(new SimpleDateFormat(devTimeFormat));
                  }} :
                  DateUtils.DATE_FORMATS_LEEF;
              obj.put(Fields.TIMESTAMP.getName(), DateUtils.parseMultiformat(devTime, formats));
            } catch (java.text.ParseException e) {
              errors.put(originalMessage,
                  new IllegalStateException("devTime field present in LEEF but cannot be parsed",
                      e));
              continue;
            }
          } else {
            String logTimestamp = matcher.group("syslogTime");
            if (!(logTimestamp == null || logTimestamp.isEmpty())) {
              try {
                obj.put(Fields.TIMESTAMP.getName(),
                    SyslogUtils.parseTimestampToEpochMillis(logTimestamp, Clock.systemUTC()));
              } catch (ParseException e) {
                errors.put(originalMessage,
                    new IllegalStateException("Cannot parse syslog timestamp", e));
                continue;
              }
            } else {
              obj.put(Fields.TIMESTAMP.getName(), System.currentTimeMillis());
            }
          }
          messages.add(obj);
        }
      }
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      Exception innerException = new IllegalStateException("LEEF parser Error: "
          + e.getMessage()
          + " on "
          + originalMessage, e);
      return Optional.of(new DefaultMessageParserResult<>(innerException));
    }
    return Optional.of(new DefaultMessageParserResult<>(messages, errors));
  }

  @SuppressWarnings("unchecked")
  private JSONObject convertToInt(JSONObject obj, String key) {
    if (obj.containsKey(key)) {
      obj.put(key, Integer.valueOf((String) obj.get(key)));
    }
    return obj;
  }

  private void headerBlock(String name, StringBuilder sb) {
    sb.append("(?<").append(name).append(">").append(HEADER_CAPTURE_PATTERN).append(")");
  }

  @Override
  public void configure(Map<String, Object> config) {
  }

  @SuppressWarnings("unchecked")
  private JSONObject mutate(JSONObject json, String oldKey, String newKey) {
    if (json.containsKey(oldKey)) {
      json.put(newKey, json.remove(oldKey));
    }
    return json;
  }

}

