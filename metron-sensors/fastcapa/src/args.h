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
    uint32_t enabled_port_mask;
    char* kafka_topic;
    char* kafka_config_path;
} __rte_cache_aligned;

/*
 * Contains all application parameters.
 */
struct app_params app;

/*
 * Print usage information to the user.
 */
void print_usage(const char* prgname);

/*
 * Parse the 'portmask' command line argument.
 */
int parse_portmask(const char* portmask);

/**
 * Parse the command line arguments passed to the application.
 */
int parse_args(int argc, char** argv);

#endif
