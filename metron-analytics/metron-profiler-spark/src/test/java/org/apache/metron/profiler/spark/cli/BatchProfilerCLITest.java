/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.metron.profiler.spark.cli;

import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.junit.Test;

/**
 * Tests the {@link BatchProfilerCLI} class.
 */
public class BatchProfilerCLITest {

  /**
   * The user must provide a Profiler configuration that defines the 'timestampField'.  The
   * Batch Profiler only operates using event time, not processing time.
   */
  @Test(expected = IllegalArgumentException.class)
  public void mustDefineTimestampField() throws Exception {
    String[] args = new String[] {
      "--profiles", "src/test/resources/profiles-no-timestamp-field.json"
    };
    BatchProfilerCLI.main(args);
  }

  /**
   * The user must define the -p, --profiles, -z, --zookeeper options.
   * The Profiler cannot work without profiles.
   */
  @Test(expected = MissingOptionException.class)
  public void mustDefineProfilesOption() throws Exception {
    String[] args = new String[] {};
    BatchProfilerCLI.main(args);
  }

  /**
   * The user must define one of  -p, --profiles, -z, --zookeeper options.
   */
  @Test(expected = IllegalArgumentException.class)
  public void mustDefineOnlyOneProfilesOption() throws Exception {
    String[] args = new String[] {
            "--profiles", "src/test/resources/profiles-no-timestamp-field.json",
            "--zookeeper", "node1:2181"
    };
    BatchProfilerCLI.main(args);
  }

  /**
   * If a timestamp option is given, it must contain a field name
   */
  @Test(expected = MissingArgumentException.class)
  public void mustDefineFieldnametoGoWithTimestamp() throws Exception {
    String[] args = new String[] {
            "--timestampfield"
    };
    BatchProfilerCLI.main(args);
  }


  /**
   * If the profile definition contains no valid profiles, we have a problem.
   */
  @Test(expected = IllegalArgumentException.class)
  public void mustDefineProfiles() throws Exception {
    String[] args = new String[] {
            "--profiles", "src/test/resources/profiles-empty.json"
    };
    BatchProfilerCLI.main(args);
  }
}
