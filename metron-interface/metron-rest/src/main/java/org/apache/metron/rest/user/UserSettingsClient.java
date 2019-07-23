/*
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
package org.apache.metron.rest.user;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * A client that interacts with persisted user settings data.  Enables create, read, and
 * delete operations on user settings.
 */
public interface UserSettingsClient extends Closeable {

  /**
   * Initialize the client.
   */
  void init();

  /**
   * Finds all settings for a particular user.
   *
   * <p>There may be more than one type of setting associated with each user. This returns all
   * setting types for a particular user.
   *
   * @param user The user.
   * @return All of the user's settings.
   * @throws IOException
   */
  Map<String, String> findOne(String user) throws IOException;

  /**
   * Finds one setting for a particular user.
   *
   * @param user The user.
   * @param type The setting type.
   * @return The value of the setting.
   * @throws IOException
   */
  Optional<String> findOne(String user, String type) throws IOException;

  /**
   * Finds all settings for all users.
   *
   * @return All settings value for all users.
   * @throws IOException
   */
  Map<String, Map<String, String>> findAll() throws IOException;

  /**
   * Finds the value of a particular setting type for all users.
   *
   * @param type The setting type.
   * @return The value of the setting type for all users.
   * @throws IOException
   */
  Map<String, Optional<String>> findAll(String type) throws IOException;

  /**
   * Save a user setting.
   *
   * @param user The user.
   * @param type The setting type.
   * @param value The value of the user setting.
   * @throws IOException
   */
  void save(String user, String type, String value) throws IOException;

  /**
   * Delete all user settings for a particular user.
   *
   * @param user The user.
   * @throws IOException
   */
  void delete(String user) throws IOException;

  /**
   * Delete one user setting value.
   *
   * @param user The user.
   * @param type The setting type.
   * @throws IOException
   */
  void delete(String user, String type) throws IOException;
}
