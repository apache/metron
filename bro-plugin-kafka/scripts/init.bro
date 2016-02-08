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

module Kafka;

export {
  #
  # the kafka broker(s) to which data will be sent
  #
  const kafka_broker_list: string = "localhost:9092" &redef;

  #
  # the name of the kafka topic
  #
  const topic_name: string = "bro" &redef;

  #
  # the maximum amount of time in milliseconds to wait for
  # kafka message delivery upon shutdown
  #
  const max_wait_on_delivery: count = 3000 &redef;
}
