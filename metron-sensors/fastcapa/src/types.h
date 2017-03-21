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

#ifndef METRON_TYPES_H
#define METRON_TYPES_H

/**
 * Tracks packet processing stats.
 */
struct app_stats {
    uint64_t in;
    uint64_t out;
    uint64_t depth;
    uint64_t drops;
} __rte_cache_aligned;

/**
 * The parameters required by a receive worker.
 */
struct rx_worker_params {

    /* worker identifier */
    uint16_t worker_id;

    /* queue identifier from which packets are fetched */
    uint16_t queue_id;

    /* how many packets are pulled off the queue at a time */
    uint16_t burst_size;

    /* the ring onto which the packets are enqueued */
    struct rte_ring *output_ring;

    /* metrics */
    struct app_stats stats;

} __rte_cache_aligned;

/**
 * The parameters required by a transmit worker.
 */
struct tx_worker_params {

    /* worker identifier */
    uint16_t worker_id;

    /* how many packets are pulled off the ring at a time */
    uint16_t burst_size;

    /* the ring from which packets are dequeued */
    struct rte_ring *input_ring;

    /* identifies the kafka client connection used by the worker */
    int kafka_id;
 
    /* worker metrics */
    struct app_stats stats;

} __rte_cache_aligned;


#endif

