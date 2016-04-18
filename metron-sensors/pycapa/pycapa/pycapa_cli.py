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
from producer import producer
from consumer import consumer


def make_parser():
    parser = argparse.ArgumentParser()
    parser.add_argument('-p',
                        '--producer',
                        help='sniff packets and send to kafka',
                        dest='producer',
                        action='store_true',
                        default=False)
    parser.add_argument('-c',
                        '--consumer',
                        help='read packets from kafka',
                        dest='consumer',
                        action='store_true',
                        default=False)
    parser.add_argument('-k',
                        '--kafka',
                        help='kafka broker(s)',
                        dest='kafka_brokers')
    parser.add_argument('-t',
                        '--topic',
                        help='kafka topic',
                        dest='topic')
    parser.add_argument('-n',
                        '--number',
                        help='number of packets to consume',
                        dest='packet_count',
                        type=int)
    parser.add_argument('-d',
                        '--debug',
                        help='debug every X packets',
                        dest='debug',
                        type=int,
                        default=0)
    parser.add_argument('-i',
                        '--interface',
                        help='interface to listen on',
                        dest='interface')
    return parser


def valid_args(args):
    if args.producer and args.kafka_brokers and args.topic and args.interface:
        return True
    elif args.consumer and args.kafka_brokers and args.topic:
        return True
    else:
        return False


def main():
    parser = make_parser()
    args = parser.parse_args()

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
