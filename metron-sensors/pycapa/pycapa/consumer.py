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
import sys
import time
import kafka
import struct
from common import to_date, to_hex, unpack_ts


def global_header(magic=0xa1b2c3d4L, version_major=2, version_minor=4, zone=0,
                  sigfigs=0, snaplen=65535, network=1):
    return struct.pack("IHHIIII", magic, version_major, version_minor, zone,
                       sigfigs, snaplen, network)


def packet_header(pkt_raw, msg_key):
    epoch_micros = struct.unpack_from(">Q", bytes(msg_key), 0)[0]
    secs = epoch_micros / 1000000
    usec = epoch_micros % 1000000
    caplen = wirelen = len(pkt_raw)
    hdr = struct.pack('IIII', secs, usec, caplen, wirelen)
    return hdr


def consumer(args):
    # connect to kafka
    brokers = args.kafka_brokers.split(",")
    kafka_consumer = kafka.KafkaConsumer(args.topic, bootstrap_servers=brokers)

    # if debug not set, write libpcap global header
    if args.debug == 0:
        sys.stdout.write(global_header())

    # start packet capture
    packet_count = 0
    for msg in kafka_consumer:

        # if debug not set, write the packet header and packet
        if args.debug == 0:
            sys.stdout.write(packet_header(msg.value, msg.key))
            sys.stdout.write(msg.value)

        elif packet_count % args.debug == 0:
            print 'Packet: count=%s dt=%s topic=%s' % (
                packet_count, to_date(unpack_ts(msg.key)), args.topic)
            print to_hex(msg.value)

        packet_count += 1
        if args.packet_count > 0 and packet_count >= args.packet_count:
            break
