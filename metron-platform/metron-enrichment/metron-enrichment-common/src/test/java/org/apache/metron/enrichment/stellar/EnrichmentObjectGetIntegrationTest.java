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

public class EnrichmentObjectGetIntegrationTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File file;

    @Before
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
        thrown.expect(ParseException.class);
        thrown.expectMessage("Unable to parse ENRICHMENT_OBJECT_GET('/some/path', 'key'): Unable to parse: ENRICHMENT_OBJECT_GET('/some/path', 'key') due to: Path '/some/path' could not be found in HDFS");

        String expression = String.format("ENRICHMENT_OBJECT_GET('%s', '%s')", "/some/path", "key");
        StellarProcessorUtils.run(expression, new HashMap<>());
    }
}
