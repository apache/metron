/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.stellar.common.timing;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

/**
 * The tree node to track time and children.
 * {@code StopWatch} class from the Apache Commons is used
 * for time tracking.
 */
public class TimeRecordNode {

  private static final String pathFmt = "%s/%s";

  // the name of our parent node
  private String parentName;
  // the name of this node
  private String name;


  // the children of this node
  private List<TimeRecordNode> children = new LinkedList<>();
  // this node's StopWatch
  private StopWatch stopWatch = new StopWatch();

  /**
   * Constructor.
   * Creates a new TimeRecordNode for a given parent name, with a given name.
   * @param parentName name of the parent, may be null
   * @param name the name of the node
   * @throws IllegalArgumentException if the node name is null or empty.
   */
  public TimeRecordNode(String parentName, String name) {
    if (StringUtils.isEmpty(name)) {
      throw new IllegalArgumentException("Argument name is missing");
    }
    this.name = name;

    if (StringUtils.isNotEmpty(parentName)) {
      this.parentName = parentName;
    }
  }

  /**
   * Returns the node's parent's name.
   * The parent node name may be null
   * @return the parent node name
   */
  public String getParentName() {
    return parentName;
  }

  /**
   * Returns the node's name.
   * @return the node name
   */
  public String getName() {
    return name;
  }

  /**
   * Return if the node's StopWatch is running.
   * @return true if it is running, false if not
   */
  public boolean isRunning() {
    return stopWatch.isStarted();
  }

  /**
   * Starts the StopWatch.
   */
  public void start() {
    stopWatch.start();
  }

  /**
   * Stops the StopWatch.
   */
  public void stop() {
    stopWatch.stop();
  }

  /**
   * Returns the runtime of the node.
   * @return the runtime in milliseconds
   */
  public long getTime() {
    return stopWatch.getTime();
  }

  /**
   * Returns the runtime of the node in nanoseconds.
   *
   * @return the runtime in nanoseconds
   */
  public long getNanoTime() {
    return stopWatch.getNanoTime();
  }


  /**
   * Returns the node's path, made up by combining it's parent's name and the node's name.
   * The path is '/' delimited.
   * <p>
   * If the parent path is null, then the name only is returned.
   * </p>
   * @return the path as String
   */
  public String getPath() {
    if (parentName == null) {
      return name;
    }
    return String.format(pathFmt, parentName, name);
  }

  /**
   * Returns the child nodes of this node.
   * @return Iterable of the child nodes.
   */
  public Iterable<TimeRecordNode> getChildren() {
    return children;
  }

  /**
   * Creates a new child node to this node.
   * If the current node is not started, then this operation results in an
   * {@code IllegalStateException}
   * @param childName the name of the child
   * @return the child node created
   * @throws IllegalStateException if the current node is not started.
   * @throws IllegalArgumentException if the node name is null or empty.
   */
  public TimeRecordNode createChild(String childName) throws IllegalStateException {
    if (!stopWatch.isStarted()) {
      throw new IllegalStateException("Adding a child to a non-started parent");
    }
    TimeRecordNode child = new TimeRecordNode(this.getPath(), childName);
    children.add(child);
    return child;
  }

  /**
   * Visits the current node and each of it's children in turn.
   * The provided {@code TimeRecordNodeVisitor} will be called this node, and passed to each
   * child node in descent.
   * @param level The level of this node.
   * @param visitor the visitor callback
   */
  protected void visit(int level, TimeRecordNodeVisitor visitor) {
    visitor.visitRecord(level, this);
    children.forEach((n) -> n.visit(level + 1, visitor));
  }
}
