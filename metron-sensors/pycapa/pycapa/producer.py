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
import pcapy
import argparse
import kafka
import random
from common import to_date, to_hex, pack_ts


def partitioner(key_bytes, all_parts, avail_parts):
    return random.choice(avail_parts)


def timestamp(pkt_hdr):
    (epoch_secs, delta_micros) = pkt_hdr.getts()
    epoch_micros = (epoch_secs * 1000000.0) + delta_micros
    return epoch_micros


def producer(args):
    # connect to kafka
    producer = kafka.KafkaProducer(
        bootstrap_servers=args.kafka_brokers.split(","),
        partitioner=partitioner)

    # initialize packet capture
    capture = pcapy.open_live(args.interface, 65535, True, 3000)
    packet_count = 0

    # start packet capture
    while True:
        (pkt_hdr, pkt_raw) = capture.next()
        if pkt_hdr is not None:

            # send packet to kafka
            pkt_ts = timestamp(pkt_hdr)
            producer.send(args.topic, key=pack_ts(pkt_ts), value=pkt_raw)

            # debug messages, if needed
            packet_count += 1
            if args.debug > 0 and packet_count % args.debug == 0:
                print 'Sent Packet: count=%s dt=%s topic=%s' % (
                    packet_count, to_date(pkt_ts), args.topic)
                print to_hex(pkt_raw)
