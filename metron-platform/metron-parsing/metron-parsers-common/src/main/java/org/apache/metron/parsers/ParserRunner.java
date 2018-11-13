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
package org.apache.metron.parsers;

import org.apache.metron.common.configuration.ParserConfigurations;
import org.apache.metron.common.message.metadata.RawMessage;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.dsl.Context;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A ParserRunner is responsible for initializing MessageParsers and parsing messages with the appropriate MessageParser.
 * The information needed to initialize a MessageParser is supplied by the parser config supplier.  After the parsers
 * are initialized, the execute method can then be called for each message and will return a ParserRunnerResults object
 * that contains a list of parsed messages and/or a list of errors.
 * @param <T> The type of a successfully parsed message.
 */
public interface ParserRunner<T> {

  /**
   * Return a list of all sensor types that can be parsed with this ParserRunner.
   * @return Sensor types
   */
  Set<String> getSensorTypes();

  /**
   *
   * @param parserConfigSupplier Supplies parser configurations
   * @param stellarContext Stellar context used to apply Stellar functions during field transformations
   */
  void init(Supplier<ParserConfigurations> parserConfigSupplier, Context stellarContext);

  /**
   * Parses a message and either returns the message or an error.
   * @param sensorType Sensor type of the message
   * @param rawMessage Raw message including metadata
   * @param parserConfigurations Parser configurations
   * @return ParserRunnerResults containing a list of messages and a list of errors
   */
  ParserRunnerResults<T> execute(String sensorType, RawMessage rawMessage, ParserConfigurations parserConfigurations);

}
