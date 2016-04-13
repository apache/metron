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

#ifndef METRON_MAIN_H
#define METRON_MAIN_H

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <inttypes.h>
#include <sys/types.h>
#include <string.h>
#include <sys/queue.h>
#include <stdarg.h>
#include <errno.h>
#include <getopt.h>
#include <unistd.h>
#include <signal.h>
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
#include <rte_distributor.h>
#include <rte_malloc.h>

#include "args.h"
#include "kafka.h"

#define RX_RING_SIZE 256
#define TX_RING_SIZE 512
#define NUM_MBUFS ((64 * 1024) - 1)
#define MBUF_CACHE_SIZE 250
#define BURST_SIZE 32
#define RTE_RING_SZ 1024

// uncomment below line to enable debug logs
//#define DEBUG

volatile uint8_t quit_signal;
volatile uint8_t quit_signal_rx;

/**
 * Tracks packet processing stats.
 */
volatile struct app_stats {
    struct {
        uint64_t received_pkts;
        uint64_t enqueued_pkts;
        uint64_t sent_pkts;
    } rx __rte_cache_aligned;
} app_stats;

/**
 * Default port configuration settings.
 */
const struct rte_eth_conf port_conf_default = {
    .rxmode = {
        .mq_mode = ETH_MQ_RX_RSS,
        .max_rx_pkt_len = ETHER_MAX_LEN,
    },
    .txmode = {
        .mq_mode = ETH_MQ_TX_NONE,
    },
    .rx_adv_conf = {
      .rss_conf = {
        .rss_hf = ETH_RSS_IP | ETH_RSS_UDP | ETH_RSS_TCP | ETH_RSS_SCTP,
      }
    },
};

/**
 * Configuration parameters provided to each worker.
 */
struct lcore_params {
    unsigned worker_id;
    unsigned num_workers;
    struct rte_distributor* d;
    struct rte_mempool* mem_pool;
} __rte_cache_aligned;

int main(int argc, char* argv[]);

#endif
