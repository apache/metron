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

#include "worker.h"

/*
 * Handles interrupt signals.
 */
static void sig_handler(int sig_num)
{
    LOG_INFO(USER1, "Exiting on signal '%d'\n", sig_num);

    // set quit flag for rx thread to exit
    quit_signal = 1;
}

/*
 * Prints packet processing metrics to stdout.
 */
static void print_stats(
    const rx_worker_params *rx_params,
    const unsigned nb_rx_workers,
    const tx_worker_params *tx_params,
    const unsigned nb_tx_workers)
{
    struct rte_eth_stats eth_stats;
    unsigned i;
    uint64_t in, out, depth, drops;
    app_stats stats;

    // header
    printf("\n\n     %15s %15s %15s %15s \n", " ----- in -----", " --- queued ---", "----- out -----", "---- drops  ----");

    // summarize stats from each port
    in = out = depth = drops = 0;
    for (i = 0; i < rte_eth_dev_count(); i++) {
        rte_eth_stats_get(i, &eth_stats);
        in += eth_stats.ipackets;
        drops += eth_stats.ierrors + eth_stats.oerrors + eth_stats.rx_nombuf;
    }
    printf("[nic] %15" PRIu64 " %15" PRIu64 " %15s %15" PRIu64 "\n", in, depth, "-", drops);

    // summarize receive; from network to receive queues
    in = out = depth = drops = 0;
    for (i = 0; i < nb_rx_workers; i++) {
        in += rx_params[i].stats.in;
        out += rx_params[i].stats.out;
        depth += rx_params[i].stats.depth;
        drops += rx_params[i].stats.drops;
    }
    printf("[rx]  %15" PRIu64 " %15s %15" PRIu64 " %15" PRIu64 "\n", in, "-", out, drops);

    // summarize transmit; from receive queues to transmit rings
    in = out = depth = drops = 0;
    for (i = 0; i < nb_tx_workers; i++) {
        in += tx_params[i].stats.in;
        out += tx_params[i].stats.out;
        depth += tx_params[i].stats.depth;
        drops += tx_params[i].stats.drops;
    }
    printf("[tx]  %15" PRIu64 " %15s %15" PRIu64 " %15" PRIu64 "\n", in, "-", out, drops);

    // summarize push to kafka; from transmit rings to librdkafka
    kaf_stats(&stats);
    printf("[kaf] %15" PRIu64 " %15" PRIu64 " %15" PRIu64 " %15" PRIu64 "\n", stats.in, stats.depth, stats.out, stats.drops);
    fflush(stdout);
}

/*
 * Process packets from a single queue.
 */
static int receive_worker(rx_worker_params* params)
{
    const uint8_t nb_ports = rte_eth_dev_count();
    const unsigned socket_id = rte_socket_id();
    const uint16_t rx_burst_size = params->rx_burst_size;
    const uint16_t queue_id = params->queue_id;
    struct rte_ring *ring = params->output_ring;
    int i, dev_socket_id;
    uint8_t port;
    struct rte_mbuf* pkts[MAX_RX_BURST_SIZE];
    //const int attempts = MAX_RX_BURST_SIZE / rx_burst_size;
    const int attempts = 0;

    LOG_INFO(USER1, "Receive worker started; core=%u, socket=%u, queue=%u attempts=%d \n", rte_lcore_id(), socket_id, queue_id, attempts);

    // validate each port
    for (port = 0; port < nb_ports; port++) {

        // skip ports that are not enabled
        if ((params->enabled_port_mask & (1 << port)) == 0) {
            continue;
        }

        // check for cross-socket communication
        dev_socket_id = rte_eth_dev_socket_id(port);
        if (dev_socket_id >= 0 && ((unsigned) dev_socket_id) != socket_id) {
            LOG_WARN(USER1, "Warning: Port %u on different socket from worker; performance will suffer\n", port);
        }
    }

    port = 0;
    while (!quit_signal) {

        // skip to the next enabled port
        if ((params->enabled_port_mask & (1 << port)) == 0) {
            if (++port == nb_ports) {
                port = 0;
            }
            continue;
        }

        // receive a 'burst' of packets. if get back the max number requested, then there
        // are likely more packets waiting. immediately go back and grab some.
        i = 0;
        uint16_t nb_in = 0, nb_in_last = 0;
        do {
            nb_in_last = rte_eth_rx_burst(port, queue_id, &pkts[nb_in], rx_burst_size);
            nb_in += nb_in_last;

        } while (++i < attempts && nb_in_last == rx_burst_size);
        params->stats.in += nb_in;

        // add each packet to the ring buffer
        if(likely(nb_in) > 0) {
          const uint16_t nb_out = rte_ring_enqueue_burst(ring, (void *) pkts, nb_in, NULL);
          params->stats.out += nb_out;
          params->stats.drops += (nb_in - nb_out);
        }

        // clean-up the packet buffer
        for (i = 0; i < nb_in; i++) {
          rte_pktmbuf_free(pkts[i]);
        }

        // wrap-around to the first port
        if (++port == nb_ports) {
            port = 0;
        }
    }

    LOG_INFO(USER1, "Receive worker finished; core=%u, socket=%u, queue=%u \n", rte_lcore_id(), socket_id, queue_id);
    return 0;
}

/*
 * The transmit worker is responsible for consuming packets from the transmit
 * rings and queueing the packets for bulk delivery to kafka.
 */
static int transmit_worker(tx_worker_params *params)
{
    unsigned i, nb_in, nb_out;
    const unsigned int tx_burst_size = params->tx_burst_size;
    struct rte_ring *ring = params->input_ring;
    const int kafka_id = params->kafka_id;

    LOG_INFO(USER1, "Transmit worker started; core=%u, socket=%u \n", rte_lcore_id(), rte_socket_id());
    while (!quit_signal) {

        // dequeue packets from the ring
        struct rte_mbuf* pkts[params->tx_ring_size];
        nb_in = rte_ring_dequeue_burst(ring, (void*) pkts, tx_burst_size, NULL);

        if(likely(nb_in > 0)) {
            params->stats.in += nb_in;

            // prepare the packets to be sent to kafka
            nb_out = kaf_send(pkts, nb_in, kafka_id);
            params->stats.out += nb_out;

            // clean-up the packet buffer
            for (i = 0; i < nb_in; i++) {
                rte_pktmbuf_free(pkts[i]);
            }
        }
    }

    LOG_INFO(USER1, "Transmit worker finished; core=%u, socket=%u \n", rte_lcore_id(), rte_socket_id());
    return 0;
}

/*
 * Start the receive and transmit works.
 */
int start_workers(
    rx_worker_params* rx_params,
    tx_worker_params* tx_params,
    struct rte_ring **tx_rings,
    app_params *p)
{
    unsigned lcore_id;
    unsigned rx_worker_id = 0;
    unsigned tx_worker_id = 0;

    signal(SIGINT, sig_handler);

    // launch the workers
    RTE_LCORE_FOREACH_SLAVE(lcore_id) {

        if(rx_worker_id < p->nb_rx_workers) {

            LOG_INFO(USER1, "Launching receive worker; worker=%u, core=%u, queue=%u\n", rx_worker_id, lcore_id, rx_worker_id);
            rx_params[rx_worker_id] = (rx_worker_params) {
                .worker_id = rx_worker_id,
                .queue_id = rx_worker_id,
                .rx_burst_size = p->rx_burst_size,
                .enabled_port_mask = p->enabled_port_mask,
                .output_ring = tx_rings[rx_worker_id],
                .stats = {0}
            };
            rte_eal_remote_launch((lcore_function_t*) receive_worker, &rx_params[rx_worker_id], lcore_id);
            rx_worker_id++;

        } else {

            unsigned ring_id = tx_worker_id % p->nb_rx_queue;
            LOG_INFO(USER1, "Launching transmit worker; worker=%u, core=%u ring=%u \n", tx_worker_id, lcore_id, ring_id);
            tx_params[tx_worker_id] = (tx_worker_params) {
                .worker_id = tx_worker_id,
                .tx_burst_size = p->tx_burst_size,
                .tx_ring_size = p->tx_ring_size,
                .input_ring = tx_rings[ring_id],
                .kafka_id = tx_worker_id,
                .stats = {0}
            };
            rte_eal_remote_launch((lcore_function_t*) transmit_worker, &tx_params[tx_worker_id], lcore_id);
            tx_worker_id++;
        }

    }

    return 0;
}

/*
 * Monitors the receive and transmit workers.  Executed by the main thread, while
 * other threads are created to perform the actual packet processing.
 */
int monitor_workers(
    const rx_worker_params *rx_params,
    const unsigned nb_rx_workers,
    const tx_worker_params *tx_params,
    const unsigned nb_tx_workers)
{
    LOG_INFO(USER1, "Starting to monitor workers; core=%u, socket=%u \n", rte_lcore_id(), rte_socket_id());
    while (!quit_signal) {
        kaf_poll();
        print_stats(rx_params, nb_rx_workers, tx_params, nb_tx_workers);
        sleep(5);
    }

    LOG_INFO(USER1, "Finished monitoring workers; core=%u, socket=%u \n", rte_lcore_id(), rte_socket_id());
    return 0;
}
