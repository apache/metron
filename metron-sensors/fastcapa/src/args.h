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

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <inttypes.h>
#include <string.h>
#include <sys/types.h>
#include <sys/queue.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <errno.h>
#include <getopt.h>
#include <glib.h>
#include <rte_common.h>
#include <rte_byteorder.h>
#include <rte_log.h>
#include <rte_memory.h>
#include <rte_memcpy.h>
#include <rte_memzone.h>
#include <rte_eal.h>
#include <rte_per_lcore.h>
#include <rte_launch.h>
#include <rte_atomic.h>
#include <rte_cycles.h>
#include <rte_prefetch.h>
#include <rte_lcore.h>
#include <rte_per_lcore.h>
#include <rte_branch_prediction.h>
#include <rte_interrupts.h>
#include <rte_pci.h>
#include <rte_random.h>
#include <rte_debug.h>
#include <rte_ether.h>
#include <rte_ethdev.h>
#include <rte_ring.h>
#include <rte_mempool.h>
#include <rte_mbuf.h>
#include <rte_ip.h>
#include <rte_tcp.h>
#include <rte_lpm.h>
#include <rte_string_fns.h>

#define DEFAULT_BURST_SIZE 32
#define DEFAULT_PORT_MASK 0x01
#define DEFAULT_KAFKA_TOPIC pcap
#define DEFAULT_NB_RX_QUEUE 2
#define DEFAULT_NB_RX_DESC 1024
#define DEFAULT_TX_RING_SIZE 2048
#define DEFAULT_KAFKA_STATS_PATH 0

#define MAX_BURST_SIZE 1024

#define STR_EXPAND(tok) #tok
#define STR(tok) STR_EXPAND(tok)

/*
 * Logging definitions
 */
#define LOG_ERROR(log_type, fmt, args...) RTE_LOG(ERR, log_type, fmt, ##args);
#define LOG_WARN(log_type, fmt, args...) RTE_LOG(WARNING, log_type, fmt, ##args);
#define LOG_INFO(log_type, fmt, args...) RTE_LOG(INFO, log_type, fmt, ##args);

#ifdef DEBUG
#define LOG_LEVEL RTE_LOG_DEBUG
#define LOG_DEBUG(log_type, fmt, args...) RTE_LOG(DEBUG, log_type, fmt, ##args);
#else
#define LOG_LEVEL RTE_LOG_INFO
#define LOG_DEBUG(log_type, fmt, args...) do {} while (0)
#endif

/**
 * Application configuration parameters.
 */
struct app_params {

    /* The number of receive descriptors to allocate for the receive ring. */
    uint16_t nb_rx_desc;

    /* The number of receive queues to set up for each ethernet device. */
    uint16_t nb_rx_queue;

    /* The size of the transmit ring (must be a power of 2). */
    uint16_t tx_ring_size;

    /* The maximum number of packets to retrieve at a time. */
    uint16_t burst_size;

    /* Defines which ports packets will be consumed from. */
    uint32_t enabled_port_mask;

    /* The name of the Kafka topic that packet data is sent to. */
    const char* kafka_topic;

    /* A file containing configuration values for the Kafka client. */
    char* kafka_config_path;

    /* A file to which the Kafka stats are appended to. */
    char* kafka_stats_path;

} __rte_cache_aligned;

/*
 * Contains all application parameters.
 */
struct app_params app;

/**
 * Parse the command line arguments passed to the application.
 */
int parse_args(int argc, char** argv);

#endif

