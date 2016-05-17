package org.apache.metron.parsers.airmagnet;

import org.apache.metron.common.Constants;
import org.apache.metron.parsers.GrokParser;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by rdy196 on 5/17/16.
 */
public class GrokAirMagnetParser extends GrokParser {

    private static final long serialVersionUID = 4860448408066888358L;
    private final String timestampField = "timestamp_string";

    public GrokAirMagnetParser(String grokHdfsPath, String patternLabel) {
        super(grokHdfsPath, patternLabel);
    }

    @Override
    protected long formatTimestamp(Object value) {
        long epochTimestamp = System.currentTimeMillis();
        if (value != null) {
            try {
                epochTimestamp = toEpoch(Calendar.getInstance().get(Calendar.YEAR)  + " " + value);
            } catch (ParseException e) {
                //default to current time
            }
        }
        return epochTimestamp;
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

    @Override
    protected void postParse(JSONObject message) {
        removeEmptyFields(message);
        System.out.println("dfgdfsjbgjkdbfgjkdbgkdfbjgbdmfgbjkdsgkdfbkjdsfgbskdjgbdsjhfj");
        Object timestamp = message.get(timestampField);
        if (timestamp != null) {
            message.put(Constants.Fields.TIMESTAMP.getName(), formatTimestamp(message.get(timestampField)));
        }
        message.remove("timestamp_string");
    }
}
