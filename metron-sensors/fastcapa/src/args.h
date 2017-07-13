/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef METRON_ARGS_H
#define METRON_ARGS_H

#include <unistd.h>
#include <string.h>
#include <getopt.h>
#include <sys/stat.h>
#include <rte_memory.h>
#include <rte_ethdev.h>
#include "types.h"

#define DEFAULT_RX_BURST_SIZE 32
#define DEFAULT_TX_BURST_SIZE 256
#define DEFAULT_PORT_MASK 0x01
#define DEFAULT_KAFKA_TOPIC pcap
#define DEFAULT_NB_RX_QUEUE 1
#define DEFAULT_NB_RX_DESC 1024
#define DEFAULT_TX_RING_SIZE 2048
#define DEFAULT_KAFKA_STATS_PATH 0
#define MAX_RX_BURST_SIZE 1024

/**
 * Parse the command line arguments passed to the application.
 */
int parse_args(int argc, char** argv, app_params* app);

#endif
