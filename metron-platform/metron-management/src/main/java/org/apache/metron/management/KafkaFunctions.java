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

package org.apache.metron.management;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

/**
 * Kafka functions available in Stellar.
 *
 * KAFKA_GET
 * KAFKA_TAIL
 * KAFKA_PUT
 * KAFKA_PROPS
 */
public class KafkaFunctions {

  /**
   * How long to wait on each poll request in milliseconds.  There will be multiple
   * poll requests, each waiting this period of time.  The maximum amount of time
   * that the user is willing to wait for a message will be some multiple of this value.
   */
  private static final int POLL_TIMEOUT = 1000;

  /**
   * The key for the property that defines the maximum amount of time
   * to wait to receive messages.
   */
  private static final String MAX_WAIT_PROPERTY = "stellar.kafka.max.wait";

  /**
   * Maintaining the default Kafka properties as a static class member is
   * critical to consumer offset management.
   *
   * A unique 'group.id' is generated when creating these default properties.  This
   * value is used when storing Kafka consumer offsets.  Multiple executions of any
   * KAFKA_* functions, within the same Stellar REPL session, must maintain the same
   * 'group.id'.  At the same time, different Stellar REPL sessions running
   * simultaneously should each have their own 'group.id'.
   */
  private static Properties defaultProperties = defaultKafkaProperties();

  /**
   * KAFKA_GET
   *
   * Retrieves messages from a Kafka topic.  Subsequent calls will continue retrieving messages
   * sequentially from the original offset.
   *
   * Example: Retrieve one message from a topic.
   *  KAFKA_GET('topic')
   *
   * Example: Retrieve 10 messages from a topic.
   *  KAFKA_GET('topic', 10)
   *
   * Example: Retrieve the first message from a topic.  This must be the first retrieval
   * from the topic, otherwise the messages will be retrieved starting from the
   * previously stored consumer offset.
   *  KAFKA_GET('topic', 1, { "auto.offset.reset": "earliest" })
   */
  @Stellar(
          namespace = "KAFKA",
          name = "GET",
          description = "Retrieves messages from a Kafka topic.  Subsequent calls will" +
                  "continue retrieving messages sequentially from the original offset.",
          params = {
                  "topic - The name of the Kafka topic",
                  "count - The number of Kafka messages to retrieve",
                  "config - Optional map of key/values that override any global properties."
          },
          returns = "List of strings"
  )
  public static class KafkaGet implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      List<String> messages = new ArrayList<>();

      // required - name of the topic to retrieve messages from
      String topic = ConversionUtils.convert(args.get(0), String.class);

      // optional - how many messages should be retrieved?
      int count = 1;
      if(args.size() > 1) {
        count = ConversionUtils.convert(args.get(1), Integer.class);
      }

      // optional - property overrides provided by the user
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 2) {
        overrides = ConversionUtils.convert(args.get(2), Map.class);
      }

      // build the properties for kafka
      Properties properties = buildKafkaProperties(overrides, context);
      properties.put("max.poll.records", count);

      // read some messages
      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
        consumer.subscribe(Arrays.asList(topic));

        int maxAttempts = getMaxAttempts(properties);
        int i = 0;
        while(messages.size() < count && i++ < maxAttempts) {
          consumer.poll(POLL_TIMEOUT).forEach(record -> messages.add(record.value()));
          consumer.commitSync();
        }
      }

      return messages;
    }

    @Override
    public void initialize(Context context) {
      // no initialization required
    }

    @Override
    public boolean isInitialized() {
      // no initialization required
      return true;
    }
  }

  /**
   * KAFKA_TAIL
   *
   * Retrieves messages from a Kafka topic always starting with
   * the most recent message first.
   *
   * Example: Retrieve the latest message from a topic.
   *  KAFKA_TAIL('topic')
   *
   * Example: Retrieve 10 messages from a topic starting with the latest.
   *  KAFKA_TAIL('topic', 10)
   */
  @Stellar(
          namespace = "KAFKA",
          name = "TAIL",
          description = "Retrieves messages from a Kafka topic always starting with the most recent message first.",
          params = {
                  "topic - The name of the Kafka topic",
                  "count - The number of Kafka messages to retrieve",
                  "config - Optional map of key/values that override any global properties."
          },
          returns = "Messages retrieved from the Kafka topic"
  )
  public static class KafkaTail implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      List<String> messages = new ArrayList<>();

      // required - name of the topic to retrieve messages from
      String topic = ConversionUtils.convert(args.get(0), String.class);

      // optional - how many messages should be retrieved?
      int count = 1;
      if(args.size() > 1) {
        count = ConversionUtils.convert(args.get(1), Integer.class);
      }

      // optional - property overrides provided by the user
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 2) {
        overrides = ConversionUtils.convert(args.get(2), Map.class);
      }

      // build the properties for kafka
      Properties properties = buildKafkaProperties(overrides, context);

      // ensures messages pulled from latest offset, versus a previously stored consumer offset
      properties.put("group.id", generateGroupId());
      properties.put("auto.offset.reset", "latest");

      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
        consumer.subscribe(Arrays.asList(topic));

        int maxAttempts = getMaxAttempts(properties);
        int i = 0;
        while(messages.size() < count && i++ < maxAttempts) {
          consumer.poll(POLL_TIMEOUT).forEach(record -> messages.add(record.value()));
          consumer.commitSync();
        }
      }

      return messages;
    }

    @Override
    public void initialize(Context context) {
      // no initialization required
    }

    @Override
    public boolean isInitialized() {
      // no initialization required
      return true;
    }
  }

  /**
   * KAFKA_PUT
   *
   * Sends messages to a Kafka topic.
   *
   * Example: Put two messages on the topic 'topic'.
   *  KAFKA_PUT('topic', ["message1", "message2"])
   *
   * Example: Put a message on a topic and also define an alternative Kafka broker.
   *  KAFKA_PUT('topic', ["message1"], { "bootstrap.servers": "kafka-broker-1:6667" })
   */
  @Stellar(
          namespace = "KAFKA",
          name = "PUT",
          description = "Sends messages to a Kafka topic.",
          params = {
                  "topic - The name of the Kafka topic.",
                  "messages - A list of messages to write.",
                  "config - An optional map of key/values that override any global properties."
          },
          returns = " "
  )
  public static class KafkaPut implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      String topic = ConversionUtils.convert(args.get(0), String.class);
      List<String> messages = ConversionUtils.convert(args.get(1), List.class);

      // build the properties for kafka
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 2) {
        overrides = ConversionUtils.convert(args.get(2), Map.class);
      }
      Properties properties = buildKafkaProperties(overrides, context);

      // send the messages
      try {
        send(topic, messages, properties);

      } catch(InterruptedException | ExecutionException e) {
        throw new ParseException(e.getMessage(), e);
      }

      return null;
    }

    /**
     * Send each message synchronously.
     * @param topic The topic to send messages to.
     * @param messages The messages to send.
     * @param properties The properties to use with Kafka.
     */
    private void send(String topic, List<String> messages, Properties properties) throws InterruptedException, ExecutionException {
      try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {

        // send each message synchronously, hence the get()
        for(String msg : messages) {
          producer.send(new ProducerRecord<>(topic, msg)).get();
        }
        producer.flush();
      }
    }

    @Override
    public void initialize(Context context) {
      // no initialization required
    }

    @Override
    public boolean isInitialized() {
      // no initialization required
      return true;
    }
  }

  /**
   * KAFKA_PROPS
   *
   * Retrieves the Kafka properties that are used by other KAFKA_* functions
   * like KAFKA_GET and KAFKA_PUT.  The Kafka properties are compiled from a
   * set of default properties, the global properties, and any overrides.
   *
   * Example: Retrieve the current Kafka properties.
   *  KAFKA_PROPS()
   *
   * Example: Retrieve the current Kafka properties taking into account a set of overrides.
   *  KAFKA_PROPS({ "max.poll.records": 1 })
   */
  @Stellar(
          namespace = "KAFKA",
          name = "PROPS",
          description = "Retrieves the Kafka properties that are used by other KAFKA_* functions " +
                  "like KAFKA_GET and KAFKA_PUT.  The Kafka properties are compiled from a " +
                  "set of default properties, the global properties, and any overrides.",
          params = { "config - An optional map of key/values that override any global properties." },
          returns = " "
  )
  public static class KafkaProps implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // optional - did the user provide any overrides?
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 0) {
        overrides = ConversionUtils.convert(args.get(0), Map.class);
      }

      return buildKafkaProperties(overrides, context);
    }

    @Override
    public void initialize(Context context) {
      // no initialization required
    }

    @Override
    public boolean isInitialized() {
      // no initialization required
      return true;
    }
  }

  /**
   * Assembles the set of Properties required by the Kafka client.
   *
   * A set of default properties has been defined to provide minimum functionality.
   * Any properties defined in the global configuration override these defaults.
   * Any user-defined overrides then override all others.
   *
   * @param overrides Property overrides provided by the user.
   * @param context The Stellar context.
   */
  private static Properties buildKafkaProperties(Map<String, String> overrides, Context context) {

    // start with minimal set of default properties
    Properties properties = new Properties();
    properties.putAll(defaultProperties);

    // override the default properties with those in the global configuration
    Optional<Object> globalCapability = context.getCapability(GLOBAL_CONFIG, false);
    if(globalCapability.isPresent()) {
      Map<String, Object> global = (Map<String, Object>) globalCapability.get();
      properties.putAll(global);
    }

    // any user-defined properties will override both the defaults and globals
    properties.putAll(overrides);

    return properties;
  }

  /**
   * Determine how many poll attempts should be made based on the user's patience.
   * @param properties
   * @return The maximum number of poll attempts to make.
   */
  private static int getMaxAttempts(Properties properties) {
    int maxWait = ConversionUtils.convert(properties.get(MAX_WAIT_PROPERTY), Integer.class);
    return maxWait / POLL_TIMEOUT;
  }

  /**
   * Defines a minimal set of default parameters that can be overridden
   * via the global properties.
   */
  private static Properties defaultKafkaProperties() {

    Properties properties = new Properties();
    properties.put("bootstrap.servers", "localhost:9092");

    /*
     * A unique 'group.id' is generated when creating these default properties.  This
     * value is used when storing Kafka consumer offsets.  Multiple executions of any
     * KAFKA_* functions, within the same Stellar REPL session, must maintain the same
     * 'group.id'.  At the same time, different Stellar REPL sessions running
     * simultaneously should each have their own 'group.id'.
     */
    properties.put("group.id", generateGroupId());

    /*
     * What to do when there is no initial offset in Kafka or if the current
     * offset does not exist any more on the server (e.g. because that data has been deleted):
     *
     *  "earliest": automatically reset the offset to the earliest offset
     *  "latest": automatically reset the offset to the latest offset
     *  "none": throw exception to the consumer if no previous offset is found or the consumer's group
     *  anything else: throw exception to the consumer.
     */
    properties.put("auto.offset.reset", "latest");

    // limits the number of messages read in a single poll request
    properties.put("max.poll.records", 1);

    // consumer deserialization
    properties.put("key.deserializer", StringDeserializer.class.getName());
    properties.put("value.deserializer", StringDeserializer.class.getName());

    // producer serialization
    properties.put("key.serializer", StringSerializer.class.getName());
    properties.put("value.serializer", StringSerializer.class.getName());

    // the maximum time to wait for messages
    properties.put(MAX_WAIT_PROPERTY, 5000);

    return properties;
  }

  /**
   * Generates a unique 'group.id' for a session.
   */
  private static String generateGroupId() {
    return String.format("stellar-shell-%s", UUID.randomUUID().toString());
  }
}
