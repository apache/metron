package org.apache.metron.parsing.parsers;

import org.apache.hadoop.mapreduce.lib.input.InvalidInputException;
import org.apache.metron.parser.interfaces.MessageParser;
import org.json.simple.JSONObject;

@SuppressWarnings("serial")
public class BasicSnortParser extends AbstractParser implements MessageParser {

	/**
	 * The default field names for Snort Alerts.
	 */
	static final String[] fieldNames = new String[] {
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
	static final String recordDelimiter = ",";

	@Override
	public JSONObject parse(byte[] rawMessage) {

		JSONObject jsonMessage = new JSONObject();
		try {
			// snort alerts expected as csv records
			String csvMessage = new String(rawMessage, "UTF-8");
			String[] records = csvMessage.split(recordDelimiter);

			// validate the number of fields
			if (records.length != fieldNames.length) {
				throw new IllegalArgumentException("Unexpected number of fields, expected: " + fieldNames.length + " got: " + records.length);
			}

			// TODO convert timestamp to epoch?

			// build the json record from each field
			for (int i=0; i<records.length; i++) {
				jsonMessage.put(fieldNames[i], records[i]);
			}

		} catch (Exception e) {

            _LOG.error("unable to parse message: " + rawMessage);
            e.printStackTrace();
            return null;
        }

		return jsonMessage;
	}
}
