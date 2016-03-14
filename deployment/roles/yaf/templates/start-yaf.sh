#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# a very simply metron probe that captures the output of yaf - yet another
# flowmeter - and sends the output to kafka so that it can be consumed
# by metron
#
{{ yaf_bin }} --in {{ sniff_interface }} --live pcap ${@:2} | {{ yafscii_bin }} --tabular | {{ kafka_prod }} --broker-list {{ kafka_broker_url }} --topic {{ yaf_topic }}
