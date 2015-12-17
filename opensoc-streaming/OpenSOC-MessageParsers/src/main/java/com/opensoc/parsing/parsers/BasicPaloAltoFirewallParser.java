package com.opensoc.parsing.parsers;


import org.json.simple.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

import com.opensoc.parser.interfaces.MessageParser;

public class BasicPaloAltoFirewallParser extends AbstractParser implements MessageParser{

	private static final long serialVersionUID = 3147090149725343999L;
	public static final String PaloAltoDomain  = "palo_alto_domain";
	public static final String ReceiveTime  = "receive_time";
	public static final String SerialNum  = "serial_num";
	public static final String Type  = "type";
	public static final String ThreatContentType  = "threat_content_type";
	public static final String ConfigVersion  = "config_version";
	public static final String GenerateTime  = "generate_time";
	public static final String SourceAddress  = "source_address";
	public static final String DestinationAddress  = "destination_address";
	public static final String NATSourceIP  = "nat_source_ip";
	public static final String NATDestinationIP  = "nat_destination_ip";
	public static final String Rule  = "rule";
	public static final String SourceUser  = "source_user";
	public static final String DestinationUser  = "destination_user";
	public static final String Application  = "application";
	public static final String VirtualSystem  = "virtual_system";
	public static final String SourceZone  = "source_zone";
	public static final String DestinationZone  = "destination_zone";
	public static final String InboundInterface  = "inbound_interface";
	public static final String OutboundInterface  = "outbound_interface";
	public static final String LogAction  = "log_action";
	public static final String TimeLogged  = "time_logged";
	public static final String SessionID  = "session_id";
	public static final String RepeatCount  = "repeat_count";
	public static final String SourcePort  = "source_port";
	public static final String DestinationPort  = "destination_port";
	public static final String NATSourcePort  = "nats_source_port";
	public static final String NATDestinationPort  = "nats_destination_port";
	public static final String Flags  = "flags";
	public static final String IPProtocol  = "ip_protocol";
	public static final String Action  = "action";
	
	//Threat
	public static final String URL  = "url";
	public static final String HOST  = "host";
	public static final String ThreatContentName  = "threat_content_name";
	public static final String Category  = "category";
	public static final String Direction  = "direction";
	public static final String Seqno  = "seqno";
	public static final String ActionFlags  = "action_flags";
	public static final String SourceCountry  = "source_country";
	public static final String DestinationCountry  = "destination_country";
	public static final String Cpadding  = "cpadding";
	public static final String ContentType = "content_type";
	
	//Traffic
	public static final String Bytes = "content_type";
	public static final String BytesSent = "content_type";
	public static final String BytesReceived = "content_type";
	public static final String Packets = "content_type";
	public static final String StartTime = "content_type";
	public static final String ElapsedTimeInSec = "content_type";
	public static final String Padding = "content_type";
	public static final String PktsSent = "pkts_sent";
	public static final String PktsReceived = "pkts_received";
	

	@SuppressWarnings({ "unchecked", "unused" })
	public JSONObject parse(byte[] msg) {

		JSONObject outputMessage = new JSONObject();
		String toParse = "";

		try {

			toParse = new String(msg, "UTF-8");
			_LOG.debug("Received message: " + toParse);
			
			
			parseMessage(toParse,outputMessage);
			
				outputMessage.put("timestamp", System.currentTimeMillis());
				outputMessage.put("ip_src_addr", outputMessage.remove("source_address"));
				outputMessage.put("ip_src_port", outputMessage.remove("source_port"));
				outputMessage.put("ip_dst_addr", outputMessage.remove("destination_address"));
				outputMessage.put("ip_dst_port", outputMessage.remove("destination_port"));
				outputMessage.put("protocol", outputMessage.remove("ip_protocol"));
				
				outputMessage.put("original_string", toParse);
			return outputMessage;
		} catch (Exception e) {
			e.printStackTrace();
			_LOG.error("Failed to parse: " + toParse);
			return null;
		}
	}
		
		@SuppressWarnings("unchecked")
		private void parseMessage(String message,JSONObject outputMessage) {
			
			String[] tokens = message.split(",");
			
			String type = tokens[3].trim();
			
			//populate common objects
			outputMessage.put(PaloAltoDomain, tokens[0].trim());
			outputMessage.put(ReceiveTime, tokens[1].trim());
			outputMessage.put(SerialNum, tokens[2].trim());
			outputMessage.put(Type, type);
			outputMessage.put(ThreatContentType, tokens[4].trim());
			outputMessage.put(ConfigVersion, tokens[5].trim());
			outputMessage.put(GenerateTime, tokens[6].trim());
			outputMessage.put(SourceAddress, tokens[7].trim());
			outputMessage.put(DestinationAddress, tokens[8].trim());
			outputMessage.put(NATSourceIP, tokens[9].trim());
			outputMessage.put(NATDestinationIP, tokens[10].trim());
			outputMessage.put(Rule, tokens[11].trim());
			outputMessage.put(SourceUser, tokens[12].trim());
			outputMessage.put(DestinationUser, tokens[13].trim());
			outputMessage.put(Application, tokens[14].trim());
			outputMessage.put(VirtualSystem, tokens[15].trim());
			outputMessage.put(SourceZone, tokens[16].trim());
			outputMessage.put(DestinationZone, tokens[17].trim());
			outputMessage.put(InboundInterface, tokens[18].trim());
			outputMessage.put(OutboundInterface, tokens[19].trim());
			outputMessage.put(LogAction, tokens[20].trim());
			outputMessage.put(TimeLogged, tokens[21].trim());
			outputMessage.put(SessionID, tokens[22].trim());
			outputMessage.put(RepeatCount, tokens[23].trim());
			outputMessage.put(SourcePort, tokens[24].trim());
			outputMessage.put(DestinationPort, tokens[25].trim());
			outputMessage.put(NATSourcePort, tokens[26].trim());
			outputMessage.put(NATDestinationPort, tokens[27].trim());
			outputMessage.put(Flags, tokens[28].trim());
			outputMessage.put(IPProtocol, tokens[29].trim());
			outputMessage.put(Action, tokens[30].trim());
			
			
			if("THREAT".equals(type.toUpperCase())) {
				outputMessage.put(URL, tokens[31].trim());
				try {
					URL url = new URL(tokens[31].trim());
					outputMessage.put(HOST, url.getHost());
				} catch (MalformedURLException e) {
				}
				outputMessage.put(ThreatContentName, tokens[32].trim());
				outputMessage.put(Category, tokens[33].trim());
				outputMessage.put(Direction, tokens[34].trim());
				outputMessage.put(Seqno, tokens[35].trim());
				outputMessage.put(ActionFlags, tokens[36].trim());
				outputMessage.put(SourceCountry, tokens[37].trim());
				outputMessage.put(DestinationCountry, tokens[38].trim());
				outputMessage.put(Cpadding, tokens[39].trim());
				outputMessage.put(ContentType, tokens[40].trim());
				
			}
			else
			{
				outputMessage.put(Bytes, tokens[31].trim());
				outputMessage.put(BytesSent, tokens[32].trim());
				outputMessage.put(BytesReceived, tokens[33].trim());
				outputMessage.put(Packets, tokens[34].trim());
				outputMessage.put(StartTime, tokens[35].trim());
				outputMessage.put(ElapsedTimeInSec, tokens[36].trim());
				outputMessage.put(Category, tokens[37].trim());
				outputMessage.put(Padding, tokens[38].trim());
				outputMessage.put(Seqno, tokens[39].trim());
				outputMessage.put(ActionFlags, tokens[40].trim());
				outputMessage.put(SourceCountry, tokens[41].trim());
				outputMessage.put(DestinationCountry, tokens[42].trim());
				outputMessage.put(Cpadding, tokens[43].trim());
				outputMessage.put(PktsSent, tokens[44].trim());
				outputMessage.put(PktsReceived, tokens[45].trim());
			}
			
		}

		
		

}