package org.apache.metron.parsing.parsers;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
public class BasicSnortParser extends BasicParser {

	private static final Logger _LOG = LoggerFactory
					.getLogger(BasicSnortParser.class);

	/**
	 * The default field names for Snort Alerts.
	 */
	private String[] fieldNames = new String[] {
			"timestamp",
			"sig_generator",
			"sig_id",
			"sig_rev",
			"msg",
			"proto",
			"src",
			"srcport",
			"dst",
			"dstport",
			"ethsrc",
			"ethdst",
			"ethlen",
			"tcpflags",
			"tcpseq",
			"tcpack",
			"tcplen",
			"tcpwindow",
			"ttl",
			"tos",
			"id",
			"dgmlen",
			"iplen",
			"icmptype",
			"icmpcode",
			"icmpid",
			"icmpseq"
	};

	/**
	 * Snort alerts are received as CSV records
	 */
	private String recordDelimiter = ",";

	@Override
	public void init() {

	}

	@Override
	public List<JSONObject> parse(byte[] rawMessage) {

		JSONObject jsonMessage = new JSONObject();
		List<JSONObject> messages = new ArrayList<>();
		try {
			// snort alerts expected as csv records
			String csvMessage = new String(rawMessage, "UTF-8");
			String[] records = csvMessage.split(recordDelimiter, -1);

			// validate the number of fields
			if (records.length != fieldNames.length) {
				throw new IllegalArgumentException("Unexpected number of fields, expected: " + fieldNames.length + " got: " + records.length);
			}
			long timestamp = 0L;
			// build the json record from each field
			for (int i=0; i<records.length; i++) {
			
				String field = fieldNames[i];
				String record = records[i];
				
				if("timestamp".equals(field)) {

					// convert the timestamp to epoch
					timestamp = toEpoch(record);
					jsonMessage.put("timestamp", timestamp);
					
				} else {
					jsonMessage.put(field, record);
				}
			}

			// add original msg; required by 'checkForSchemaCorrectness'
			jsonMessage.put("original_string", csvMessage);
			messages.add(jsonMessage);
		} catch (Exception e) {

            _LOG.error("unable to parse message: " + rawMessage);
            e.printStackTrace();
            return null;
        }

		return messages;
	}

	/**
	 * Parses Snort's default date-time representation and
	 * converts to epoch.
	 * @param snortDatetime Snort's default date-time as String '01/27-16:01:04.877970'
	 * @return epoch time
	 * @throws java.text.ParseException 
	 */
	private long toEpoch(String snortDatetime) throws ParseException {
		
		/*
		 * TODO how does Snort not embed the year in their default timestamp?! need to change this in 
		 * Snort configuration.  for now, just assume current year.
		 */
		int year = Calendar.getInstance().get(Calendar.YEAR);
		String withYear = Integer.toString(year) + " " + snortDatetime;
		
		// convert to epoch time
		SimpleDateFormat df = new SimpleDateFormat("yyyy MM/dd-HH:mm:ss.S");
		Date date = df.parse(withYear);
		return date.getTime();
	}

	public String getRecordDelimiter() {
		return this.recordDelimiter;
	}

	public void setRecordDelimiter(String recordDelimiter) {
		this.recordDelimiter = recordDelimiter;
	}

	public String[] getFieldNames() {
		return this.fieldNames;
	}

	public void setFieldNames(String[] fieldNames) {
		this.fieldNames = fieldNames;
	}

}
