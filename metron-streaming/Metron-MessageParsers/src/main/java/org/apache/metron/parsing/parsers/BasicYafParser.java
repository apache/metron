package org.apache.metron.parsing.parsers;

import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import oi.thekraken.grok.api.exception.GrokException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by rmerriman on 1/27/16.
 */
public class BasicYafParser extends BasicParser {


  /**
   * The default field names for Snort Alerts.
   */
  private String[] fieldNames = new String[] {
          "start-time",
          "end-time",
          "duration",
          "rtt",
          "proto",
          "sip",
          "sp",
          "dip",
          "dp",
          "iflags",
          "uflags",
          "riflags",
          "ruflags",
          "isn",
          "risn",
          "tag",
          "rtag",
          "pkt",
          "oct",
          "rpkt",
          "roct",
          "app",
          "end-reason"
  };

  private String recordDelimiter = "\\|";

  private transient  Grok  grok;
  private transient InputStream pattern_url;

  public static final String PREFIX = "stream2file";
  public static final String SUFFIX = ".tmp";

  public static File stream2file(InputStream in) throws IOException {
    final File tempFile = File.createTempFile(PREFIX, SUFFIX);
    tempFile.deleteOnExit();
    try (FileOutputStream out = new FileOutputStream(tempFile)) {
      IOUtils.copy(in, out);
    }
    return tempFile;
  }

  protected static final Logger _LOG = LoggerFactory
          .getLogger(BasicBroParser.class);
  private JSONCleaner cleaner = new JSONCleaner();

  @Override
  public void init() {
    // pattern_url = Resources.getResource("patterns/asa");

    pattern_url = getClass().getClassLoader().getResourceAsStream(
            "patterns/yaf");

    File file = null;
    try {
      file = stream2file(pattern_url);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    try {
      grok = Grok.create(file.getPath());
    } catch (GrokException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    try {
      grok.compile("%{YAF_DELIMITED}");
    } catch (GrokException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  public List<JSONObject> parse(byte[] msg) {
    //return parseManual(msg);
    return parseWithGrok(msg);
  }

  private List<JSONObject> parseWithGrok(byte[] msg) {
    _LOG.trace("[Metron] Starting to parse incoming message with grok");
    JSONObject jsonMessage = new JSONObject();
    List<JSONObject> messages = new ArrayList<>();
    try {
      String rawMessage = new String(msg, "UTF-8");

      Match gm = grok.match(rawMessage);
      gm.captures();
      Map grokMap = gm.toMap();
      jsonMessage.putAll(gm.toMap());

      jsonMessage.put("original_string", rawMessage);
      String startTime = (String) grokMap.get("start_time");
      long timestamp = 0L;
      if (startTime != null) {
        timestamp = toEpoch(startTime);
        jsonMessage.put("timestamp", timestamp);
      } else {
        jsonMessage.put("timestamp", "0");
      }
      String endTime = (String) grokMap.get("end_time");
      if (endTime != null) {
        jsonMessage.put("end_time", toEpoch(endTime));
      } else {
        jsonMessage.put("end_time", "0");
      }
      jsonMessage.remove("YAF_DELIMITED");
      jsonMessage.remove("start_time");
      messages.add(jsonMessage);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    return messages;
  }

  private JSONObject parseManual(byte[] msg) {
    _LOG.trace("[Metron] Starting to parse incoming message");

    JSONObject jsonMessage = new JSONObject();
    try {
      String rawMessage = new String(msg, "UTF-8");
      String[] records = rawMessage.split(recordDelimiter, -1);

      // validate the number of fields
      if (records.length != fieldNames.length) {
        throw new IllegalArgumentException("Unexpected number of fields, expected: " + fieldNames.length + " got: " + records.length);
      }

      // build the json record from each field
      for (int i=0; i<records.length; i++) {

        String field = fieldNames[i];
        String record = records[i].trim();

        if("start-time".equals(field) || "end-time".equals(field)) {
          // convert the timestamp to epoch
          String timeFieldName = "start-time".equals(field) ? "timestamp" : field;
          jsonMessage.put(timeFieldName, toEpoch(record));
        } else {
          jsonMessage.put(field, record);
        }
      }

      jsonMessage.put("original_string", rawMessage);

    } catch (Exception e) {

      _LOG.error("Unable to Parse Message: " + msg);
      e.printStackTrace();
      return null;
    }

    return jsonMessage;
  }

  private long toEpoch(String datetime) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
    Date date = sdf.parse(datetime + " UTC");
    return date.getTime();
  }
}
