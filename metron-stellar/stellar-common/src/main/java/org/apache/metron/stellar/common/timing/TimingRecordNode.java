/*
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

/**
 * The tree node to track time and children.
 * The {@code StopWatch} class is used for timings
 */
public class TimingRecordNode {

  /**
   * The format String for creating paths.
   */
  private static final String PATH_FMT = "%s/%s";

  /**
   * This nodes parent's path.
   */
  private String parentTimingPath;

  /**
   * The name of this node.
   */
  private String timingName;

  /**
   * The tags associated with this timing.
   */
  private String[] tags;

  /**
   * The child nodes of this node.
   */
  private List<TimingRecordNode> children = new ArrayList<>();

  /**
   * The {@code StopWatch} for this node.
   */
  private StopWatch stopWatch = new StopWatch();

  /**
   * <p>
   * Constructor.
   * </p>
   * <p>
   * Creates a new TimingRecordNode for a given parent name, with a given name.
   * </p>
   *
   * @param parentTimingPath the path of the parent, may be null
   * @param timingName the name of the timing
   * @param tags the tags to associate with this timing
   * @throws IllegalArgumentException if the timingName is null or empty.
   */
  public TimingRecordNode(String parentTimingPath, String timingName, String... tags) {
    if (StringUtils.isEmpty(timingName)) {
      throw new IllegalArgumentException("Argument name is missing");
    }
    this.timingName = timingName;

    if (StringUtils.isNotEmpty(parentTimingPath)) {
      this.parentTimingPath = parentTimingPath;
    }

    this.tags = tags;
  }

  /**
   * Returns the node's parent's path.
   * The parent node path may be null
   *
   * @return the parent node path
   */
  public String getParentPath() {
    return parentTimingPath;
  }

  /**
   * Returns the node's timing name.
   *
   * @return the node timing name
   */
  public String getTimingName() {
    return timingName;
  }

  /**
   * Return if the node's StopWatch is running.
   *
   * @return true if it is running, false if not
   */
  public boolean isRunning() {
    return stopWatch.isStarted();
  }

  /**
   * Starts the StopWatch.
   */
  public void start() {
    if (!stopWatch.isStarted()) {
      stopWatch.start();
    }
  }

  /**
   * <p>
   * Stops the StopWatch.
   * </p>
   * <p>
   * If this node has running children, an {@code IllegalStateException} will result.
   * </p>
   *
   * @throws IllegalStateException if stop is called on a node with running children
   */
  public void stop() {
    for (TimingRecordNode child : children) {
      if (child.isRunning()) {
        throw new IllegalStateException("Cannot stop a timing with running children");
      }
    }
    stopWatch.stop();
  }

  /**
   * Returns the {@code StopWatch} for this node.
   *
   * @return {@code StopWatch}
   */
  public StopWatch getStopWatch() {
    return stopWatch;
  }

  /**
   * The tags associated with this timing.
   *
   * @return tags array
   */
  public String[] getTags() {
    // variable parameters are never null
    // no need for null check here
    return ArrayUtils.clone(tags);
  }

  /**
   * Returns the node's path, made up by combining it's parent's name and the node's name.
   * The path is '/' delimited.
   * <p>
   * If the parent path is null, then the name only is returned.
   * </p>
   *
   * @return the path as String
   */
  public String getPath() {
    if (parentTimingPath == null) {
      return timingName;
    }
    return String.format(PATH_FMT, parentTimingPath, timingName);
  }

  /**
   * Returns the child nodes of this node.
   *
   * @return Iterable of the child nodes.
   */
  public Iterable<TimingRecordNode> getChildren() {
    return Collections.unmodifiableList(children);
  }

  /**
   * Creates a new child node to this node.
   * If the current node is not started, then this operation results in an
   * {@code IllegalStateException}
   *
   * @param childName the name of the child
   * @param tags the tags for this timing
   * @return the child node created
   * @throws IllegalStateException if the current node is not started.
   * @throws IllegalArgumentException if the node name is null or empty.
   */
  public TimingRecordNode createChild(String childName, String... tags)
      throws IllegalStateException {
    if (!stopWatch.isStarted()) {
      throw new IllegalStateException("Adding a child to a non-started parent");
    }
    TimingRecordNode child = new TimingRecordNode(this.getPath(), childName, tags);
    children.add(child);
    return child;
  }

  /**
   * Visits the current node and each of it's children in turn.
   * The provided {@code TimingRecordNodeVisitor} will be called this node, and passed to each
   * child node in descent.
   *
   * @param level The level of this node.
   * @param visitor the visitor callback
   */
  protected void visit(int level, TimingRecordNodeVisitor visitor) {
    visitor.visitRecord(level, this);
    for (TimingRecordNode child : children) {
      child.visit(level + 1, visitor);
    }
  }
}
