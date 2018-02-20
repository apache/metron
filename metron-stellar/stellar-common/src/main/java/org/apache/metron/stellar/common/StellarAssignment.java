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
package org.apache.metron.stellar.common;


import java.util.Map;

public class StellarAssignment implements Map.Entry<String, Object>{
  private String variable;
  private String statement;

  public StellarAssignment(String variable, String statement) {
    this.variable = variable;
    this.statement = statement;
  }

  public String getVariable() {
    return variable;
  }

  public String getStatement() {
    return statement;
  }

  public static boolean isAssignment(String statement) {
    return statement != null &&
            statement.contains(":=") && // has the assignment operator
            !statement.trim().startsWith("%"); // not a magic like %define x := 2
  }

  public static StellarAssignment from(String statement) {
    if(statement == null || statement.length() == 0) {
      return new StellarAssignment(null, null);
    }
    char prev = statement.charAt(0);
    char curr;
    String variable = "" + prev;
    String s = null;
    boolean isAssignment = false;
    for(int i = 1;i < statement.length();++i,prev=curr) {
      curr = statement.charAt(i);
      if(prev == ':' && curr == '=') {
        isAssignment = true;
        variable = variable.substring(0, variable.length() - 1);
        s = "";
        continue;
      }
      if(!isAssignment) {
        variable += curr;
      }
      else {
        s += curr;
      }
    }

    if(!isAssignment) {
      s = variable;
      variable = null;
    }

    if(s != null) {
      s = s.trim();
    }
    if(variable != null) {
      variable = variable.trim();
    }
    return new StellarAssignment(variable, s);
  }

  /**
   * Returns the key corresponding to this entry.
   *
   * @return the key corresponding to this entry
   * @throws IllegalStateException implementations may, but are not
   *                               required to, throw this exception if the entry has been
   *                               removed from the backing map.
   */
  @Override
  public String getKey() {
    return variable;
  }

  /**
   * Returns the value corresponding to this entry.  If the mapping
   * has been removed from the backing map (by the iterator's
   * <tt>remove</tt> operation), the results of this call are undefined.
   *
   * @return the value corresponding to this entry
   * @throws IllegalStateException implementations may, but are not
   *                               required to, throw this exception if the entry has been
   *                               removed from the backing map.
   */
  @Override
  public Object getValue() {
    return statement;
  }

  /**
   * Replaces the value corresponding to this entry with the specified
   * value (optional operation).  (Writes through to the map.)  The
   * behavior of this call is undefined if the mapping has already been
   * removed from the map (by the iterator's <tt>remove</tt> operation).
   *
   * @param value new value to be stored in this entry
   * @return old value corresponding to the entry
   * @throws UnsupportedOperationException if the <tt>put</tt> operation
   *                                       is not supported by the backing map
   * @throws ClassCastException            if the class of the specified value
   *                                       prevents it from being stored in the backing map
   * @throws NullPointerException          if the backing map does not permit
   *                                       null values, and the specified value is null
   * @throws IllegalArgumentException      if some property of this value
   *                                       prevents it from being stored in the backing map
   * @throws IllegalStateException         implementations may, but are not
   *                                       required to, throw this exception if the entry has been
   *                                       removed from the backing map.
   */
  @Override
  public String setValue(Object value) {
    throw new UnsupportedOperationException("Assignments are immutable.");
  }
}
