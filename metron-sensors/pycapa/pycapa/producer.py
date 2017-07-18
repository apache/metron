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
import threading
import signal
import pcapy
import argparse
import random
import logging
from common import to_date, to_hex, pack_ts, unpack_ts
from confluent_kafka import Producer

finished = threading.Event()
producer_args = None

def signal_handler(signum, frame):
    """ Initiates a clean shutdown for a SIGINT """

    finished.set()
    logging.info("Clean shutdown process started")


def partitioner(key_bytes, all_parts, avail_parts):
    """ Partitions messages randomly across all available partitions. """

    return random.choice(avail_parts)


def timestamp(pkt_hdr):
    """ Returns the timestamp of the packet in epoch milliseconds. """

    (epoch_secs, delta_micros) = pkt_hdr.getts()
    epoch_micros = (epoch_secs * 1000000.0) + delta_micros
    return epoch_micros


def delivery_callback(err, msg):
    """ Callback executed when message delivery either succeeds or fails. """

    # initialize counter, if needed
    if not hasattr(delivery_callback, "pkts_out"):
         delivery_callback.pkts_out = 0

    if err:
        logging.error("message delivery failed: error=%s", err)

    elif msg is not None:
        delivery_callback.pkts_out += 1

        pretty_print = 0
        pretty_print = producer_args.pretty_print

        if pretty_print > 0 and delivery_callback.pkts_out % pretty_print == 0:
            print 'Packet delivered[%s]: date=%s topic=%s partition=%s offset=%s len=%s' % (
                delivery_callback.pkts_out, to_date(unpack_ts(msg.key())), msg.topic(),
                msg.partition(), msg.offset(), len(msg.value()))


def producer(args, sniff_timeout_ms=500, sniff_promisc=True):
    """ Captures packets from a network interface and sends them to a Kafka topic. """

    # setup the signal handler
    signal.signal(signal.SIGINT, signal_handler)

    global producer_args
    producer_args = args

    # connect to kafka
    logging.info("Connecting to Kafka; %s", args.kafka_configs)
    kafka_producer = Producer(args.kafka_configs)

    # initialize packet capture
    logging.info("Starting packet capture")
    capture = pcapy.open_live(args.interface, args.snaplen, sniff_promisc, sniff_timeout_ms)
    pkts_in = 0

    try:
        while not finished.is_set() and (args.max_packets <= 0 or pkts_in < args.max_packets):

            # capture a packet
            (pkt_hdr, pkt_raw) = capture.next()
            if pkt_hdr is not None:
                logging.debug("Packet received: pkts_in=%d, pkt_len=%s", pkts_in, pkt_hdr.getlen())
                pkts_in += 1
                pkt_ts = timestamp(pkt_hdr)
                kafka_producer.produce(args.kafka_topic, key=pack_ts(pkt_ts), value=pkt_raw, callback=delivery_callback)

                # pretty print, if needed
                if args.pretty_print > 0 and pkts_in % args.pretty_print == 0:
                    print 'Packet received[%s]' % (pkts_in)

            # serve the callback queue
            kafka_producer.poll(0)

    finally:
        # flush all messages
        logging.info("Waiting for '%d' message(s) to flush", len(kafka_producer))
        kafka_producer.flush()

        # pkts_out may not be initialized if the callback was never executed
        pkts_out = 0
        if hasattr(delivery_callback, "pkts_out"):
            pkts_out = delivery_callback.pkts_out

        logging.info("'%d' packet(s) in, '%d' packet(s) out", pkts_in, pkts_out)
