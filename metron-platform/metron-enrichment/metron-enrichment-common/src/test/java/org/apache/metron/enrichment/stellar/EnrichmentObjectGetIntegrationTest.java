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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class EnrichmentObjectGetIntegrationTest {
    private File file;

    @BeforeEach
    public void setup() throws Exception {
        File tempDir = TestUtils.createTempDir(this.getClass().getName());
        file = new File(tempDir, "enrichment.ser");
        try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            IOUtils.write(SerDeUtils.toBytes(new HashMap<String, Object>() {{
                put("key", "value");
            }}), bos);
        }
    }

    @Test
    public void shouldReturnEnrichment() {
        String expression = String.format("ENRICHMENT_OBJECT_GET('%s', '%s')", file.getAbsolutePath(), "key");
        String value = (String) StellarProcessorUtils.run(expression, new HashMap<>());
        assertEquals("value", value);
    }

    @Test
    public void shouldThrowExceptionOnInvalidPath() {
        String expression = String.format("ENRICHMENT_OBJECT_GET('%s', '%s')", "/some/path", "key");
        ParseException e = assertThrows(ParseException.class, () -> StellarProcessorUtils.run(expression, new HashMap<>()));
        assertTrue(e.getMessage().contains("Unable to parse ENRICHMENT_OBJECT_GET('/some/path', 'key'): Unable to parse: ENRICHMENT_OBJECT_GET('/some/path', 'key') due to: Path '/some/path' could not be found in HDFS"));
    }
}
