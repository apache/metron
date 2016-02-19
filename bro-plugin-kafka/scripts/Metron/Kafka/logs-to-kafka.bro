#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

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

event bro_init() &priority=-5 {
	if(kafka_broker_list == "" || topic_name == "") {
		return;
	}

	for (stream_id in Log::active_streams) {
		if (stream_id in Kafka::logs_to_send) {
			local stream_str = fmt("%s", stream_id);

			local filter: Log::Filter = [
				$name = fmt("kafka-%s", stream_str),
				$writer = Log::WRITER_KAFKAWRITER
			];

			Log::add_filter(stream_id, filter);
		}
	}
}
