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


package org.apache.metron.enrichment.stellar;

import org.apache.commons.io.IOUtils;
import org.apache.metron.common.utils.SerDeUtils;
import org.apache.metron.integration.utils.TestUtils;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.apache.metron.stellar.dsl.ParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class ObjectGetIntegrationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File file;

    @Before
    public void setup() throws Exception {
        File tempDir = TestUtils.createTempDir(this.getClass().getName());
        file = new File(tempDir, "object.ser");
        try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            IOUtils.write(SerDeUtils.toBytes("object get data"), bos);
        }
    }

    @Test
    public void shouldReturnEnrichment() {
        String expression = String.format("OBJECT_GET('%s')", file.getAbsolutePath());
        String value = (String) StellarProcessorUtils.run(expression, new HashMap<>());
        assertEquals("object get data", value);
    }

    @Test
    public void shouldThrowExceptionOnInvalidPath() {
        thrown.expect(ParseException.class);
        thrown.expectMessage("Unable to parse OBJECT_GET('/some/path'): Unable to parse: OBJECT_GET('/some/path') due to: Path '/some/path' could not be found in HDFS");

        String expression = String.format("OBJECT_GET('%s')", "/some/path");
        StellarProcessorUtils.run(expression, new HashMap<>());
    }
}
