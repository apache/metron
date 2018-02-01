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
package org.apache.metron.maas.config;

/**
 * A container for a request for a model.
 */
public class ModelRequest {

  private String name;
  private String version;
  private int numInstances;
  private int memory;
  private Action action;
  private String path;

  /**
   * The path in the model store (right now HDFS) for the directory containing model's bits.
   * @return path
   */
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  /**
   * The action to perform
   * @return action
   */
  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  /**
   * The name of the model
   * @return name
   */
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * The version of the model
   * @return version
   */
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * The number of instances of the model to start
   * @return numInstances
   */
  public int getNumInstances() {
    return numInstances;
  }

  public void setNumInstances(int numInstances) {
    this.numInstances = numInstances;
  }

  /**
   * The amount of memory for the containers holding the model in megabytes
   * @return memory
   */
  public int getMemory() {
    return memory;
  }

  public void setMemory(int memory) {
    this.memory = memory;
  }

}
