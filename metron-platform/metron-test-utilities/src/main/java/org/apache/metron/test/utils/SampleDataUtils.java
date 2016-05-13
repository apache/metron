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
package org.apache.metron.test.utils;

import org.apache.metron.TestConstants;
import org.apache.metron.test.TestDataType;

import java.io.File;
import java.io.FileNotFoundException;

public class SampleDataUtils {

  public static String getSampleDataPath(String sensorType, TestDataType testDataType) throws FileNotFoundException {
    File sensorSampleDataPath = new File(TestConstants.SAMPLE_DATA_PATH, sensorType);
    if (sensorSampleDataPath.exists() && sensorSampleDataPath.isDirectory()) {
      File sampleDataPath = new File(sensorSampleDataPath, testDataType.getDirectoryName());
      if (sampleDataPath.exists() && sampleDataPath.isDirectory()) {
        File[] children = sampleDataPath.listFiles();
        if (children != null && children.length > 0) {
          return children[0].getAbsolutePath();
        }
      }
    }
    throw new FileNotFoundException("Could not find data in " + TestConstants.SAMPLE_DATA_PATH + sensorType + "/" + testDataType.getDirectoryName());
  }
}
