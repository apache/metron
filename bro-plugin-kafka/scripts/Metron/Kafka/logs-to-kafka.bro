##! load this script to enable log output to kafka

module Kafka;

export {
	##
	## which log streams should be sent to kafka?
	## example:
	##		redef Kafka::logs_to_send = set(Conn::Log, HTTP::LOG, DNS::LOG);
	##
	const logs_to_send: set[Log::ID] &redef;
}

event bro_init() &priority=-5
{
	if(kafka_broker_list == "" || topic_name == "")
		return;

	for (stream_id in Log::active_streams)
	{
		if (stream_id in Kafka::logs_to_send)
		{

			local stream_str = fmt("%s", stream_id);

			local filter: Log::Filter = [
				$name = fmt("kafka-%s", stream_str),
				$writer = Log::WRITER_KAFKAWRITER
			];

			Log::add_filter(stream_id, filter);
		}
	}
}
