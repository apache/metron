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
import argparse
import logging
import random
import string
from producer import producer
from consumer import consumer


def make_parser():
    """ Creates a command-line argument parser. """

    parser = argparse.ArgumentParser()
    parser.add_argument('-p', '--producer',
                        help='sniff packets and send to kafka',
                        dest='producer',
                        action='store_true',
                        default=False)

    parser.add_argument('-c', '--consumer',
                        help='read packets from kafka',
                        dest='consumer',
                        action='store_true',
                        default=False)

    parser.add_argument('-k', '--kafka-broker',
                        help='kafka broker(s) as host:port',
                        dest='kafka_brokers')

    parser.add_argument('-t', '--kafka-topic',
                        help='kafka topic',
                        dest='kafka_topic')

    parser.add_argument('-o', '--kafka-offset',
                        help='kafka offset to consume from; default=end',
                        dest='kafka_offset',
                        choices=['begin','end','stored'],
                        default='end')

    parser.add_argument('-i', '--interface',
                        help='network interface to listen on',
                        dest='interface',
                        metavar='NETWORK_IFACE')

    parser.add_argument('-m', '--max-packets',
                        help='stop after this number of packets',
                        dest='max_packets',
                        type=int,
                        default=0)

    parser.add_argument('-pp','--pretty-print',
                        help='pretty print every X packets',
                        dest='pretty_print',
                        type=int,
                        default=0)

    parser.add_argument('-ll', '--log-level',
                        help='set the log level; DEBUG, INFO, WARN',
                        dest='log_level',
                        default='INFO')

    parser.add_argument('-X',
                        type=keyval,
                        help='define a kafka client parameter; key=value',
                        dest='kafka_configs',
                        action='append')

    parser.add_argument('-s','--snaplen',
                        help="capture only the first X bytes of each packet; default=65535",
                        dest='snaplen',
                        type=int,
                        default=65535)

    return parser


def keyval(input, delim="="):
    """ Expects a single key=value. """

    keyval = input.split("=")
    if(len(keyval) != 2):
        raise ValueError("expect key=val")

    return keyval


def valid_args(args):
    """ Validates the command-line arguments. """

    if not args.producer and not args.consumer:
        print "error: expected either --consumer or --producer \n"
        return False

    elif args.producer and not (args.kafka_brokers and args.kafka_topic and args.interface):
        print "error: missing required args: expected [--kafka-broker, --kafka-topic, --interface] \n"
        return False

    elif args.consumer and not (args.kafka_brokers and args.kafka_topic):
        print "error: missing required args: expected [--kafka-broker, --kafka-topic] \n"
        return False

    else:
        return True


def clean_kafka_configs(args):
    """ Cleans and transforms the Kafka client configs. """

    # transform 'kafka_configs' args from "list of lists" to dict
    configs = {}
    if(args.kafka_configs is not None):
        for keyval in args.kafka_configs:
            configs[keyval[0]] = keyval[1:][0]

    # boostrap servers can be set as a "-X bootstrap.servers=KAFKA:9092" or "-k KAFKA:9092"
    bootstrap_key = "bootstrap.servers"
    if(bootstrap_key not in configs):
        configs[bootstrap_key] = args.kafka_brokers

    # if no 'group.id', generate a random one
    group_key = "group.id"
    if(group_key not in configs):
        configs[group_key] = ''.join(random.choice(string.ascii_uppercase) for _ in range(12))

    args.kafka_configs = configs


def main():

    parser = make_parser()
    args = parser.parse_args()

    # setup logging
    numeric_log_level = getattr(logging, args.log_level.upper(), None)
    if not isinstance(numeric_log_level, int):
        raise ValueError('invalid log level: %s' % args.log_level)
    logging.basicConfig(level=numeric_log_level)

    clean_kafka_configs(args)
    if not valid_args(args):
        parser.print_help()
    elif args.consumer:
        consumer(args)
    elif args.producer:
        producer(args)
    else:
        parser.print_help()


if __name__ == '__main__':
    main()
