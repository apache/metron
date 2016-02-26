package org.apache.metron.integration.util.integration.util;


import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;

import java.util.Map;

public class KafkaUtil {
    public static <K,V> void send(Producer<K,V> producer, K key, V value, String topic) {
        producer.send(new KeyedMessage<>(topic, key,value));
    }

    public static <K,V> void send(Producer<K,V> producer, Iterable<Map.Entry<K,V>> messages, String topic) {
        for(Map.Entry<K,V> kv : messages) {
            send(producer, kv.getKey(), kv.getValue(), topic);
        }
    }
}
