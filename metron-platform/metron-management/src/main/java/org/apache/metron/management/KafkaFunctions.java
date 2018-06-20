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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.metron.common.system.Clock;
import org.apache.metron.stellar.common.LambdaExpression;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;

/**
 * Defines the following Kafka-related functions available in Stellar.
 *
 *  KAFKA_GET
 *  KAFKA_PUT
 *  KAFKA_TAIL
 *  KAFKA_FIND
 *  KAFKA_PROPS
 */
public class KafkaFunctions {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * The key for the property that defines the maximum amount of time
   * to wait to receive messages.
   */
  public static final String POLL_TIMEOUT_PROPERTY = "stellar.kafka.poll.timeout";

  /**
   * How long to wait on each poll request in milliseconds.
   *
   * <p>One each function call, there will likely be multiple poll requests, each
   * waiting this period of time.
   */
  private static final int DEFAULT_POLL_TIMEOUT = 500;

  /**
   * The key for the property that defines the maximum amount of time
   * to wait to receive messages in milliseconds.
   */
  public static final String MAX_WAIT_PROPERTY = "stellar.kafka.max.wait.millis";

  /**
   * The default max wait time in milliseconds.
   */
  public static final int DEFAULT_MAX_WAIT = 5000;

  /**
   * The key for the global property that defines how a message is returned
   * from the set of KAFKA functions.
   *
   * <p>simple - The result contains only the message value as a string.
   * <p>rich - The result contains the message value, topic, partition, and offset.
   */
  public static final String MESSAGE_VIEW_PROPERTY = "stellar.kafka.message.view";

  /**
   * An acceptable value for the 'stellar.kafka.message.view' property. The result
   * provided will contain only the message value as a string.
   */
  public static final String MESSAGE_VIEW_SIMPLE = "simple";

  /**
   * An acceptable value for the 'stellar.kafka.message.view' property.
   *
   * <p>Provides a view of each message with more detailed metadata beyond just the
   * message value.  The result provided will contain the message value, topic, partition,
   * and offset.
   */
  public static final String MESSAGE_VIEW_RICH = "rich";

  /**
   * The default set of Kafka properties.
   */
  private static Properties defaultProperties = defaultKafkaProperties();

  /**
   * A clock to tell time.
   *
   * Allows any functions that depend on the system clock to be more readily tested.
   */
  protected static Clock clock = new Clock();

  /**
   * KAFKA_GET
   *
   * <p>Retrieves messages from a Kafka topic.  Subsequent calls will continue retrieving messages
   * sequentially from the original offset.
   *
   * <p>Example: Retrieve one message from a topic.
   * <pre>
   *   {@code
   *   KAFKA_GET('topic')
   *   }
   * </pre>
   *
   * <p>Example: Retrieve 10 messages from a topic.
   * <pre>
   *   {@code
   *   KAFKA_GET('topic', 10)
   *   }
   * </pre>
   *
   * <p>Example: Retrieve the first message from a topic.  This must be the first retrieval
   * from the topic, otherwise the messages will be retrieved starting from the
   * previously stored consumer offset.
   * <pre>
   *   {@code
   *   KAFKA_GET('topic', 1, { "auto.offset.reset": "earliest" })
   *   }
   * </pre>
   *
   * <p>By default, only the message value is returned. By setting the global property
   * 'stellar.kafka.message.view' = 'rich' the function will return additional Kafka metadata
   * including the topic, partition, offset, key, and timestamp contained in a map. Setting
   * this property value to 'simple' or simply not setting the property value, will result
   * in the default view behavior.
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
          returns = "The messages as a list of strings"
  )
  public static class KafkaGet implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // required - name of the topic to retrieve messages from
      String topic = getArg("topic", 0, String.class, args);

      // optional - how many messages should be retrieved?
      int count = 1;
      if(args.size() > 1) {
        count = getArg("count", 1, Integer.class, args);
      }

      // optional - property overrides provided by the user
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 2) {
        overrides = getArg("overrides", 2, Map.class, args);
      }

      // build the properties for kafka
      Properties properties = buildKafkaProperties(overrides, context);
      properties.put("max.poll.records", count);

      return getMessages(topic, count, properties);
    }

    /**
     * Gets messages from a Kafka topic.
     *
     * @param topic The Kafka topic.
     * @param count The maximum number of messages to get.
     * @param properties The function properties.
     * @return
     */
    private Object getMessages(String topic, int count, Properties properties) {

      int maxWait = getMaxWait(properties);
      int pollTimeout = getPollTimeout(properties);
      List<Object> messages = new ArrayList<>();

      // read some messages
      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

        manualPartitionAssignment(topic, consumer);

        // continue until we have enough messages or exceeded the max wait time
        long wait = 0L;
        final long start = clock.currentTimeMillis();
        while(messages.size() < count && wait < maxWait) {

          for(ConsumerRecord<String, String> record: consumer.poll(pollTimeout)) {
            Object viewOfMessage = render(record, properties);
            messages.add(viewOfMessage);
          }

          // how long have we waited?
          wait = clock.currentTimeMillis() - start;
          consumer.commitSync();

          LOG.debug("KAFKA_GET polled for messages; topic={}, count={}, waitTime={} ms",
                  topic, messages.size(), wait);
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
   * <p>Tails messages from a Kafka topic always starting with the most recently received message.
   *
   * <p>Example: Retrieve the latest message from a topic.
   * <pre>
   *   {@code
   *   KAFKA_TAIL('topic')
   *   }
   * </pre>
   *
   * <p>Example: Retrieve 10 messages from a topic starting with the latest.
   * <pre>
   *   {@code
   *   KAFKA_TAIL('topic', 10)
   *   }
   * </pre>
   *
   * <p>By default, only the message value is returned. By setting the global property
   * 'stellar.kafka.message.view' = 'rich' the function will return additional Kafka metadata
   * including the topic, partition, offset, key, and timestamp contained in a map. Setting
   * this property value to 'simple' or simply not setting the property value, will result
   * in the default view behavior.
   */
  @Stellar(
          namespace = "KAFKA",
          name = "TAIL",
          description = "Tails messages from a Kafka topic always starting with the most recently received message.",
          params = {
                  "topic - The name of the Kafka topic",
                  "count - The number of Kafka messages to retrieve",
                  "config - Optional map of key/values that override any global properties."
          },
          returns = "The messages as a list of strings"
  )
  public static class KafkaTail implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      // required - name of the topic to retrieve messages from
      String topic = getArg("topic", 0, String.class, args);

      // optional - how many messages should be retrieved?
      int count = 1;
      if(args.size() > 1) {
        count = getArg("count", 1, Integer.class, args);
      }

      // optional - property overrides provided by the user
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 2) {
        overrides = getArg("overrides", 2, Map.class, args);
      }

      Properties properties = buildKafkaProperties(overrides, context);
      properties.put("max.poll.records", count);

      return tailMessages(topic, count, properties);
    }

    /**
     * Gets messages from the tail end of a Kafka topic.
     *
     * @param topic The name of the kafka topic.
     * @param count The maximum number of messages to get.
     * @param properties The function configuration properties.
     * @return A list of messages from the tail end of a Kafka topic.
     */
    private Object tailMessages(String topic, int count, Properties properties) {

      List<Object> messages = new ArrayList<>();
      int pollTimeout = getPollTimeout(properties);
      int maxWait = getMaxWait(properties);

      // create the consumer
      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

        // seek to the end of all topic/partitions
        Set<TopicPartition> partitions = manualPartitionAssignment(topic, consumer);
        consumer.seekToEnd(partitions);

        // continue until we have enough messages or exceeded the max wait time
        long wait = 0L;
        final long start = clock.currentTimeMillis();
        while(messages.size() < count && wait < maxWait) {

          for(ConsumerRecord<String, String> record: consumer.poll(pollTimeout)) {
            Object viewOfMessage = render(record, properties);
            messages.add(viewOfMessage);
          }

          // how long have we waited?
          wait = clock.currentTimeMillis() - start;
          consumer.commitSync();

          LOG.debug("KAFKA_TAIL polled for messages; topic={}, count={}, waitTime={} ms",
                  topic, messages.size(), wait);
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
   * <p>Sends messages to a Kafka topic.
   *
   * <p>Example: Put two messages on the topic 'topic'.
   * <pre>
   *  {@code
   *  KAFKA_PUT('topic', ["message1", "message2"])
   *  }
   * </pre>
   *
   * <p>Example: Put a message on a topic and also define an alternative Kafka broker.
   * <pre>
   *  {@code
   *  KAFKA_PUT('topic', ["message1"], { "bootstrap.servers": "kafka-broker-1:6667" })
   *  }
   * </pre>
   *
   */
  @Stellar(
          namespace = "KAFKA",
          name = "PUT",
          description = "Sends messages to a Kafka topic. ",
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

      List<String> messages;
      if(args.get(1) instanceof String) {
        // a single message needs sent
        String msg = getArg("message(s)", 1, String.class, args);
        messages = Collections.singletonList(msg);

      } else {
        // a list of messages; all need sent
        messages = getArg("message(s)", 1, List.class, args);
      }

      // are there any overrides?
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 2) {
        overrides = getArg("overrides", 2, Map.class, args);
      }

      // send the messages
      Properties properties = buildKafkaProperties(overrides, context);
      List<RecordMetadata> records = putMessages(topic, messages, properties);

      // render a view of the messages that were written for the user
      Object view = render(records, properties);
      return view;
    }

    /**
     * Render a view of the {@link RecordMetadata} that resulted from writing
     * messages to Kafka.
     *
     * @param records The record metadata.
     * @param properties The properties.
     * @return
     */
    private Object render(List<RecordMetadata> records, Properties properties) {

      Object view;
      if(MESSAGE_VIEW_RICH.equals(getMessageView(properties))) {

        // build a 'rich' view of the messages that were written
        List<Object> responses = new ArrayList<>();
        for(RecordMetadata record: records) {

          // render the 'rich' view of the record
          Map<String, Object> richView = new HashMap<>();
          richView.put("topic", record.topic());
          richView.put("partition", record.partition());
          richView.put("offset", record.offset());
          richView.put("timestamp", record.timestamp());

          responses.add(richView);
        }

        // the rich view is a list of maps containing metadata about how each message was written
        view = responses;

      } else {

        // otherwise, the view is simply a count of the number of messages written
        view = CollectionUtils.size(records);
      }
      return view;
    }

    /**
     * Put messages to a Kafka topic.
     *
     * <p>Sends each message synchronously.
     *
     * @param topic The topic to send messages to.
     * @param messages The messages to send.
     * @param properties The properties to use with Kafka.
     * @return Metadata about all the records written to Kafka.
     */
    private List<RecordMetadata> putMessages(String topic, List<String> messages, Properties properties) {
      LOG.debug("KAFKA_PUT sending messages; topic={}, count={}", topic, messages.size());
      List<RecordMetadata> records = new ArrayList<>();
      try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {

        List<Future<RecordMetadata>> futures = new ArrayList<>();

        // send each message
        for(String msg : messages) {
          Future<RecordMetadata> future = producer.send(new ProducerRecord<>(topic, msg));
          futures.add(future);
        }

        // wait for the sends to complete
        for(Future<RecordMetadata> future : futures) {
          RecordMetadata record = waitForResponse(future, properties);
          records.add(record);
        }

        producer.flush();
      }

      return records;
    }

    /**
     * Wait for response to the message being sent.
     *
     * @param future The future for the message being sent.
     * @param properties The configuration properties.
     * @return Metadata about the record that was written to Kafka.
     */
    private RecordMetadata waitForResponse(Future<RecordMetadata> future, Properties properties) {
      RecordMetadata record = null;
      int maxWait = getMaxWait(properties);

      try {
        // wait for the record and then render it for the user
        record = future.get(maxWait, TimeUnit.MILLISECONDS);
        LOG.debug("KAFKA_PUT message sent; topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());

      } catch(TimeoutException | InterruptedException | ExecutionException e) {
        LOG.error("KAFKA_PUT message send failure", e);
      }

      return record;
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
        overrides = getArg("overrides", 0, Map.class, args);
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
   * KAFKA_FIND
   *
   * <p>Finds messages that satisfy a given filter expression. Subsequent calls will continue retrieving messages
   * sequentially from the original offset.
   *
   * <p>Example: Retrieve a 'bro' message.
   * <pre>
   * {@code
   * KAFKA_FIND('topic', m -> MAP_GET('source.type', m) == 'bro')
   * }
   * </pre>
   *
   * <p>Example: Find 10 messages that contain geo-location data.
   * <pre>
   * {@code
   * KAFKA_FIND('topic', m -> MAP_EXISTS('geo', m), 10)
   * }
   * </pre>
   *
   * <p>By default, only the message value is returned. By setting the global property
   * 'stellar.kafka.message.view' = 'rich' the function will return additional Kafka metadata
   * including the topic, partition, offset, key, and timestamp contained in a map. Setting
   * this property value to 'simple' or simply not setting the property value, will result
   * in the default view behavior.
   */
  @Stellar(
          namespace = "KAFKA",
          name = "FIND",
          description = "Find messages that satisfy a given filter expression. Messages are filtered starting from " +
                  "the latest offset.",
          params = {
                  "topic - The name of the Kafka topic",
                  "filter - A lambda expression that filters messages. Messages are presented as a map of fields to the expression.",
                  "count - The number of Kafka messages to retrieve",
                  "config - Optional map of key/values that override any global properties."
          },
          returns = "The messages as a list of strings"
  )
  public static class KafkaFind implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      // required - name of the topic to retrieve messages from
      String topic = getArg("topic", 0, String.class, args);

      // required - a lambda which filters the messages
      LambdaExpression filter = getArg("filter", 1, LambdaExpression.class, args);

      // optional - how many messages should be retrieved?
      int count = 1;
      if(args.size() > 2) {
        count = getArg("count", 2, Integer.class, args);
      }

      // optional - property overrides provided by the user
      Map<String, String> overrides = new HashMap<>();
      if(args.size() > 3) {
        overrides = getArg("overrides", 3, Map.class, args);
      }

      Properties properties = buildKafkaProperties(overrides, context);
      properties.put("max.poll.records", 10 * count);

      return findMessages(topic, filter, count, properties);
    }

    /**
     * Find messages in Kafka that satisfy a filter expression.
     *
     * @param topic The kafka topic.
     * @param filter The filter expression.
     * @param count The maximum number of messages to find.
     * @param properties Function configuration values.
     * @return A list of messages that satisfy the filter expression.
     */
    private List<Object> findMessages(String topic, LambdaExpression filter, int count, Properties properties) {
      final int pollTimeout = getPollTimeout(properties);
      final int maxWait = getMaxWait(properties);

      List<Object> messages = new ArrayList<>();
      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {

        // seek to the end of all topic/partitions
        Set<TopicPartition> partitions = manualPartitionAssignment(topic, consumer);
        consumer.seekToEnd(partitions);

        // continue until we have enough messages or exceeded the max wait time
        long wait = 0L;
        final long start = clock.currentTimeMillis();
        while(messages.size() < count && wait < maxWait) {

          // poll kafka for messages
          ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
          for(ConsumerRecord<String, String> record : records) {

            // only keep the message if the filter expression is satisfied
            if(isSatisfied(filter, record.value())) {
              Object view = render(record, properties);
              messages.add(view);

              // do we have enough messages already?
              if(messages.size() >= count) {
                break;
              }
            }
          }

          // how long have we waited?
          wait = clock.currentTimeMillis() - start;
          consumer.commitSync();

          LOG.debug("KAFKA_FIND polled for messages; topic={}, count={}, waitTime={} ms",
                  topic, messages.size(), wait);
        }
      }

      return messages;
    }

    /**
     * Executes a given expression on a message.
     *
     * @param expr The filter expression to execute.
     * @param message The message that the expression is executed on.
     * @return Returns true, only if the expression returns true.  If the expression
     * returns false or fails to execute, false is returned.
     */
    public boolean isSatisfied(LambdaExpression expr, String message) {
      boolean result = false;
      Map<String, Object> messageAsMap;
      try {
        // transform the message to a map of fields
        messageAsMap = JSONUtils.INSTANCE.load(message, JSONUtils.MAP_SUPPLIER);

        // apply the filter expression
        Object out = expr.apply(Collections.singletonList(messageAsMap));
        if(out instanceof Boolean) {
          result = (Boolean) out;

        } else {
          LOG.error("Expected boolean from filter expression, got {}", ClassUtils.getShortClassName(out, "null"));
        }

      } catch(IOException e) {
        LOG.error("Unable to parse message", e);
      }

      return result;
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
   * Renders the Kafka record into a view.
   *
   * <p>A user can customize the way in which a Kafka record is rendered by altering
   * the "stellar.kafka.message.view" property.
   *
   * @param record The Kafka record to render.
   * @param properties The properties which allows a user to customize the rendered view of a record.
   * @return
   */
  private static Object render(ConsumerRecord<String, String> record, Properties properties) {
    LOG.debug("Render message; topic={}, partition={}, offset={}",
            record.topic(), record.partition(), record.offset());

    Object result;
    if(MESSAGE_VIEW_RICH.equals(getMessageView(properties))) {
      // build the detailed view of the record
      Map<String, Object> view = new HashMap<>();
      view.put("value", record.value());
      view.put("topic", record.topic());
      view.put("partition", record.partition());
      view.put("offset", record.offset());
      view.put("timestamp", record.timestamp());
      view.put("key", record.key());

      result = view;

    } else {
      // default to the simple view
      result = record.value();
    }

    return result;
  }

  /**
   * Manually assigns all partitions in a topic to a consumer
   *
   * @param topic The topic whose partitions will be assigned.
   * @param consumer The consumer to assign partitions to.
   * @return A set of topic-partitions that were manually assigned to the consumer.
   */
  private static Set<TopicPartition> manualPartitionAssignment(String topic, KafkaConsumer<String, String> consumer) {
    // find all partitions for the topic
    Set<TopicPartition> partitions = new HashSet<>();
    for(PartitionInfo partition : consumer.partitionsFor(topic)) {
      partitions.add(new TopicPartition(topic, partition.partition()));
    }

    if(partitions.size() == 0) {
      throw new IllegalStateException(format("No partitions available for consumer assignment; topic=%s", topic));
    }

    // manually assign this consumer to each partition in the topic
    consumer.assign(partitions);

    return partitions;
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
   * Return the max wait time setting.
   *
   * @param properties The function configuration properties.
   * @return The mex wait time in milliseconds.
   */
  private static int getMaxWait(Properties properties) {
    int maxWait = DEFAULT_MAX_WAIT;

    Object value = properties.get(MAX_WAIT_PROPERTY);
    if(value != null) {
      maxWait = ConversionUtils.convert(value, Integer.class);
    }

    return maxWait;
  }

  /**
   * Returns the poll timeout setting.
   *
   * <p>The maximum amount of time waited each time that Kafka is polled
   * for messages.
   *
   * @param properties The function configuration properties.
   * @return
   */
  private static int getPollTimeout(Properties properties) {
    int pollTimeout = DEFAULT_POLL_TIMEOUT;

    Object value = properties.get(POLL_TIMEOUT_PROPERTY);
    if(value != null) {
      pollTimeout = ConversionUtils.convert(value, Integer.class);
    }

    return pollTimeout;
  }

  /**
   * Determines how Kafka messages should be rendered for the user.
   *
   * @param properties The properties.
   * @return How the Kafka messages should be rendered.
   */
  private static String getMessageView(Properties properties) {
    // defaults to the simple view
    String messageView = MESSAGE_VIEW_SIMPLE;

    if(properties.containsKey(MESSAGE_VIEW_PROPERTY)) {
      messageView = ConversionUtils.convert(properties.get(MESSAGE_VIEW_PROPERTY), String.class);
    }

    return messageView;
  }

  /**
   * Defines a minimal set of default parameters that can be overridden
   * via the global properties.
   */
  private static Properties defaultKafkaProperties() {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", "localhost:9092");
    properties.put("group.id", "kafka-functions-stellar");

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

    // set the default max time to wait for messages
    properties.put(MAX_WAIT_PROPERTY, DEFAULT_MAX_WAIT);

    // set the default poll timeout
    properties.put(POLL_TIMEOUT_PROPERTY, DEFAULT_POLL_TIMEOUT);

    // set the default message view
    properties.put(MESSAGE_VIEW_PROPERTY, MESSAGE_VIEW_SIMPLE);

    return properties;
  }

  /**
   * Get an argument from a list of arguments.
   *
   * @param argName The name of the argument.
   * @param index The index within the list of arguments.
   * @param clazz The type expected.
   * @param args All of the arguments.
   * @param <T> The type of the argument expected.
   */
  public static <T> T getArg(String argName, int index, Class<T> clazz, List<Object> args) {
    if(index >= args.size()) {
      throw new IllegalArgumentException(format("missing '%s'; expected at least %d argument(s), found %d",
              argName, index+1, args.size()));
    }

    return ConversionUtils.convert(args.get(index), clazz);
  }
}
