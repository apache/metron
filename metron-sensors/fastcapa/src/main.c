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

#include "main.h"

/*
 * Initialize a port using global settings and with the rx buffers
 * coming from the mbuf_pool passed as parameter
 */
static inline int init_port(const uint8_t port, struct rte_mempool* mbuf_pool)
{
    struct rte_eth_conf port_conf = port_conf_default;
    int retval;
    uint16_t q;
    int retry = 5;
    const uint16_t tx_queues = 1;
    int socket;
    struct rte_eth_dev_info dev_info;

    if (port >= rte_eth_dev_count()) {
        rte_exit(EXIT_FAILURE, "Port does not exist; port=%u \n", port);
        return -1;
    }

    // check that the number of RX queues does not exceed what is supported by the device
    rte_eth_dev_info_get(port, &dev_info);
    if (app.nb_rx_queue > dev_info.max_rx_queues) {
        rte_exit(EXIT_FAILURE, "Too many RX queues for device; port=%u, rx_queues=%u, max_queues=%u \n", 
            port, app.nb_rx_queue, dev_info.max_rx_queues);
        return -EINVAL;
    }

    // check that the number of TX queues does not exceed what is supported by the device
    if (tx_queues > dev_info.max_tx_queues) {
        rte_exit(EXIT_FAILURE, "Too many TX queues for device; port=%u, tx_queues=%u, max_queues=%u \n", 
            port, tx_queues, dev_info.max_tx_queues);
        return -EINVAL;
    }

    retval = rte_eth_dev_configure(port, app.nb_rx_queue, tx_queues, &port_conf);
    if (retval != 0) {
        rte_exit(EXIT_FAILURE, "Cannot configure device; port=%u, err=%s \n", port, strerror(-retval));
        return retval;
    }

    // create the receive queues
    socket = rte_eth_dev_socket_id(port);
    for (q = 0; q < app.nb_rx_queue; q++) {
        retval = rte_eth_rx_queue_setup(port, q, app.nb_rx_desc, socket, NULL, mbuf_pool);
        if (retval != 0) {
            rte_exit(EXIT_FAILURE, "Cannot setup RX queue; port=%u, err=%s \n", port, strerror(-retval));
            return retval;
        }
    }

    // create the transmit queues - at least one TX queue must be setup even though we don't use it
    for (q = 0; q < tx_queues; q++) {
        retval = rte_eth_tx_queue_setup(port, q, TX_QUEUE_SIZE, socket, NULL);
        if (retval != 0) {
            rte_exit(EXIT_FAILURE, "Cannot setup TX queue; port=%u, err=%s \n", port, strerror(-retval));
            return retval;
        }
    }

    // start the receive and transmit units on the device
    retval = rte_eth_dev_start(port);
    if (retval < 0) {
        rte_exit(EXIT_FAILURE, "Cannot start device; port=%u, err=%s \n", port, strerror(-retval));
        return retval;
    }

    // retrieve information about the device
    struct rte_eth_link link;
    do {
        rte_eth_link_get_nowait(port, &link);

    } while (retry-- > 0 && !link.link_status && !sleep(1));

    // if still no link information, must be down
    if (!link.link_status) {
        rte_exit(EXIT_FAILURE, "Link down; port=%u \n", port);
        return 0;
    }

    // enable promisc mode
    rte_eth_promiscuous_enable(port);

    // print diagnostics
    struct ether_addr addr;
    rte_eth_macaddr_get(port, &addr);
    LOG_INFO(USER1, "Device setup successfully; port=%u, mac=%02" PRIx8 " %02" PRIx8 " %02" PRIx8 " %02" PRIx8 " %02" PRIx8 " %02" PRIx8 "\n",
        (unsigned)port,
        addr.addr_bytes[0], addr.addr_bytes[1],
        addr.addr_bytes[2], addr.addr_bytes[3],
        addr.addr_bytes[4], addr.addr_bytes[5]);

    return 0;
}

static void print_stats(struct rx_worker_params *rx_params, unsigned nb_rx_workers, struct tx_worker_params *tx_params, unsigned nb_tx_workers)
{
    struct rte_eth_stats eth_stats;
    unsigned i;
    uint64_t in, out, depth, drops;
    struct app_stats stats;

    // header
    printf("\n\n     %15s %15s %15s %15s \n", " ----- in -----", " --- queued ---", "----- out -----", "---- drops ----");

    // summarize stats from each port
    in = 0;
    for (i = 0; i < rte_eth_dev_count(); i++) {
        rte_eth_stats_get(i, &eth_stats);
        in += eth_stats.ipackets;
    }
    printf("[nic] %15" PRIu64 " %15s %15s %15s \n", in, "-", "-", "-");

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
    in = out = depth = 0;
    for (i = 0; i < nb_tx_workers; i++) {
        in += tx_params[i].stats.in;
        out += tx_params[i].stats.out;
        depth += tx_params[i].stats.depth;
    }
    printf("[tx]  %15" PRIu64 " %15s %15" PRIu64 " %15" PRIu64 "\n", in, "-", out, in - out);

    // summarize push to kafka; from transmit rings to librdkafka
    kaf_stats(&stats);
    printf("[kaf] %15" PRIu64 " %15" PRIu64 " %15" PRIu64 " %15" PRIu64 "\n", stats.in, stats.depth, stats.out, stats.drops);

    // summarize any errors on the ports
    for (i = 0; i < rte_eth_dev_count(); i++) {
        rte_eth_stats_get(i, &eth_stats);

        if(eth_stats.ierrors > 0 || eth_stats.oerrors > 0 || eth_stats.rx_nombuf > 0) {
            printf("\nErrors: Port %u \n", i);
            printf(" - In Errs:   %" PRIu64 "\n", eth_stats.ierrors);
            printf(" - Out Errs:  %" PRIu64 "\n", eth_stats.oerrors);
            printf(" - Mbuf Errs: %" PRIu64 "\n", eth_stats.rx_nombuf);
        }
    }
}

/*
 * Handles interrupt signals.
 */
static void sig_handler(int sig_num)
{
    LOG_INFO(USER1, "Exiting on signal '%d'\n", sig_num);

    // set quit flag for rx thread to exit
    quit_signal = 1;
}

static int monitor_workers(struct rx_worker_params *rx_params, unsigned nb_rx_workers, struct tx_worker_params *tx_params, unsigned nb_tx_workers)
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

/**
 * Process packets from a single queue.
 */
static int receive_worker(struct rx_worker_params* params)
{
    const uint8_t nb_ports = rte_eth_dev_count();
    const unsigned socket_id = rte_socket_id();
    const uint16_t burst_size = app.burst_size;
    const uint16_t queue_id = params->queue_id;
    struct rte_ring *ring = params->output_ring;
    int i, dev_socket_id;
    uint8_t port;
    struct rte_mbuf* pkts[MAX_BURST_SIZE];
    const int attempts = MAX_BURST_SIZE / burst_size;

    LOG_INFO(USER1, "Receive worker started; core=%u, socket=%u, queue=%u attempts=%d \n", rte_lcore_id(), socket_id, queue_id, attempts);

    // validate each port
    for (port = 0; port < nb_ports; port++) {

        // skip ports that are not enabled
        if ((app.enabled_port_mask & (1 << port)) == 0) {
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
        if ((app.enabled_port_mask & (1 << port)) == 0) {
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
            nb_in_last = rte_eth_rx_burst(port, queue_id, &pkts[nb_in], burst_size);
            nb_in += nb_in_last;

        } while (++i < attempts && nb_in_last == burst_size);
        params->stats.in += nb_in;

        // add each packet to the ring buffer
        if(likely(nb_in) > 0) {
          const uint16_t nb_out = rte_ring_enqueue_burst(ring, (void *) pkts, nb_in);
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

/**
 *
 */
static int transmit_worker(struct tx_worker_params *params)
{
    unsigned i, nb_in, nb_out;
    const uint16_t burst_size = params->burst_size;
    struct rte_ring *ring = params->input_ring;
    const int kafka_id = params->kafka_id;

    LOG_INFO(USER1, "Transmit worker started; core=%u, socket=%u \n", rte_lcore_id(), rte_socket_id());
    while (!quit_signal) {

        // dequeue packets from the ring
        struct rte_mbuf* pkts[MAX_BURST_SIZE];
        nb_in = rte_ring_dequeue_burst(ring, (void*) pkts, burst_size);
        
        if(likely(nb_in > 0)) {
            params->stats.in += nb_in;

            // prepare the packets to be sent to kafka
            nb_out = kaf_send(pkts, nb_in, kafka_id);
            params->stats.out += nb_out;
        }

        // clean-up the packet buffer    
        for (i = 0; i < nb_in; i++) {
            rte_pktmbuf_free(pkts[i]);
        }
    }

    LOG_INFO(USER1, "Transmit worker finished; core=%u, socket=%u \n", rte_lcore_id(), rte_socket_id());
    return 0;
}

/**
 * Get it going.
 */
int main(int argc, char* argv[])
{
    unsigned lcore_id;
    unsigned nb_workers;
    uint8_t port_id;
    unsigned nb_ports;
    unsigned nb_ports_available;
    struct rte_mempool* mbuf_pool;
    unsigned n, i;
    unsigned nb_rx_workers, nb_tx_workers;
    unsigned rx_worker_id = 0, tx_worker_id = 0;
    char buf[32];

    // catch interrupt
    signal(SIGINT, sig_handler);

    // initialize the environment
    int ret = rte_eal_init(argc, argv);
    if (ret < 0) {
        rte_exit(EXIT_FAILURE, "Failed to initialize EAL: %i\n", ret);
    }

    // advance past the environmental settings
    argc -= ret;
    argv += ret;

    // parse arguments to the application
    ret = parse_args(argc, argv);
    if (ret < 0) {
        rte_exit(EXIT_FAILURE, "Invalid parameters\n");
    }

    nb_workers = rte_lcore_count() - 1;
    nb_ports_available = nb_ports = rte_eth_dev_count();
    nb_rx_workers = app.nb_rx_queue;
    nb_tx_workers = nb_workers - nb_rx_workers;
    n = NUM_MBUFS * nb_ports;

    // validate the number of workers
    if(nb_tx_workers < nb_rx_workers) {
        rte_exit(EXIT_FAILURE, "Additional lcore(s) required; found=%u, required=%u \n", 
            rte_lcore_count(), (app.nb_rx_queue*2) + 1);
    }

    // create memory pool
    mbuf_pool = rte_pktmbuf_pool_create("mbuf-pool", n, MBUF_CACHE_SIZE, 0, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());
    if (mbuf_pool == NULL) {
        rte_exit(EXIT_FAILURE, "Unable to create memory pool; n=%u, cache_size=%u, data_room_size=%u, socket=%u \n", 
            n, MBUF_CACHE_SIZE, RTE_MBUF_DEFAULT_BUF_SIZE, rte_socket_id());
    }

    // initialize each specified ethernet ports
    for (port_id = 0; port_id < nb_ports; port_id++) {

        // skip over ports that are not enabled
        if ((app.enabled_port_mask & (1 << port_id)) == 0) {
            LOG_INFO(USER1, "Skipping over disabled port '%d'\n", port_id);
            nb_ports_available--;
            continue;
        }

        // initialize the port - creates one receive queue for each worker
        LOG_INFO(USER1, "Initializing port %u\n", (unsigned)port_id);
        if (init_port(port_id, mbuf_pool) != 0) {
            rte_exit(EXIT_FAILURE, "Cannot initialize port %" PRIu8 "\n", port_id);
        }
    }

    // ensure that we were able to initialize enough ports
    if (nb_ports_available < 1) {
        rte_exit(EXIT_FAILURE, "Error: No available enabled ports. Portmask set?\n");
    }

    // each transmit worker has their own kafka client connection
    kaf_init(nb_tx_workers);

    struct rx_worker_params rx_params[nb_rx_workers];
    struct tx_worker_params tx_params[nb_tx_workers];

    // create the transmit rings - 1 for each receive queue
    struct rte_ring *tx_rings[app.nb_rx_queue]; 
    for(i = 0; i < app.nb_rx_queue; i++) {
        sprintf(buf, "tx-ring-%d", i);
        tx_rings[i] = rte_ring_create(buf, app.tx_ring_size, rte_socket_id(), 0);
        if(NULL == tx_rings[i]) {
            rte_exit(EXIT_FAILURE, "Unable to create transmit ring: %s \n", rte_strerror(rte_errno));
        }
    }

    // launch the workers
    RTE_LCORE_FOREACH_SLAVE(lcore_id) {

        if(rx_worker_id < nb_rx_workers) {

            LOG_INFO(USER1, "Launching receive worker; worker=%u, core=%u, queue=%u\n", rx_worker_id, lcore_id, rx_worker_id);
            rx_params[rx_worker_id] = (struct rx_worker_params) { 
                .worker_id = rx_worker_id, 
                .queue_id = rx_worker_id, 
                .burst_size = app.burst_size, 
                .output_ring = tx_rings[rx_worker_id], 
                .stats = {0} 
            };
            rte_eal_remote_launch((lcore_function_t*) receive_worker, &rx_params[rx_worker_id], lcore_id);
            rx_worker_id++;

        } else {

            unsigned ring_id = tx_worker_id % app.nb_rx_queue;
            LOG_INFO(USER1, "Launching transmit worker; worker=%u, core=%u ring=%u \n", tx_worker_id, lcore_id, ring_id);
            tx_params[tx_worker_id] = (struct tx_worker_params) { 
                .worker_id = tx_worker_id, 
                .burst_size = app.burst_size, 
                .input_ring = tx_rings[ring_id], 
                .kafka_id = tx_worker_id,
                .stats = {0} 
            };
            rte_eal_remote_launch((lcore_function_t*) transmit_worker, &tx_params[tx_worker_id], lcore_id);
            tx_worker_id++;
        }

    }

    // allow the master to monitor each of the workers
    monitor_workers(rx_params, nb_rx_workers, tx_params, nb_tx_workers);

    // wait for each worker to complete
    RTE_LCORE_FOREACH_SLAVE(lcore_id) {
        if (rte_eal_wait_lcore(lcore_id) < 0) {
            LOG_WARN(USER1, "Failed to wait for worker; lcore=%u \n", lcore_id);
            return -1;
        }
    }

    kaf_close();
    return 0;
}

