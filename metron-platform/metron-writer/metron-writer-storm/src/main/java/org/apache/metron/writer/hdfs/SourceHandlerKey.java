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

package org.apache.metron.writer.hdfs;

class SourceHandlerKey {
  private String sourceType;
  private String stellarResult;

  SourceHandlerKey(String sourceType, String stellarResult) {
    this.sourceType = sourceType;
    this.stellarResult = stellarResult;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getStellarResult() {
    return stellarResult;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SourceHandlerKey that = (SourceHandlerKey) o;

    if (sourceType != null ? !sourceType.equals(that.sourceType) : that.sourceType != null) {
      return false;
    }
    return stellarResult != null ? stellarResult.equals(that.stellarResult) : that.stellarResult == null;
  }

  @Override
  public int hashCode() {
    int result = sourceType != null ? sourceType.hashCode() : 0;
    result = 31 * result + (stellarResult != null ? stellarResult.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SourceHandlerKey{" +
        "sourceType='" + sourceType + '\'' +
        ", stellarResult='" + stellarResult + '\'' +
        '}';
  }
}

