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
package org.apache.metron.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.util.IOUtils;

@Component
public class KnoxSSOAuthenticationFilterTests {

    @Test
    public void testParsePemWithHeaders() throws CertificateException, ServletException, IOException {
        RSAPublicKey parseRSAPublicKey = KnoxSSOAuthenticationFilter
                .parseRSAPublicKey(readFile("org/apache/metron/ui/headers.pem"));
        assertNotNull(parseRSAPublicKey);
    }

    @Test
    public void testParsePemWithoutHeaders() throws CertificateException, ServletException, IOException {
        RSAPublicKey parseRSAPublicKey = KnoxSSOAuthenticationFilter.parseRSAPublicKey(readFile("org/apache/metron/ui/noheaders.pem"));
        assertNotNull(parseRSAPublicKey);
    }

    @Test(expected = ServletException.class)
    public void testInvalidPem() throws CertificateException, ServletException, IOException {
        @SuppressWarnings("unused")
        RSAPublicKey parseRSAPublicKey = KnoxSSOAuthenticationFilter.parseRSAPublicKey(readFile("org/apache/metron/ui/invalid.pem"));
        fail();
    }

    private String readFile(String file) throws IOException {
        ClassLoader cl = this.getClass().getClassLoader();
        try (InputStream resourceAsStream = cl.getResourceAsStream(file)) {
            return IOUtils.readInputStreamToString(resourceAsStream, Charset.defaultCharset());
        }
    }
}
