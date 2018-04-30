/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.common.dsl.functions;

import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunctions;
import org.apache.metron.common.stellar.StellarProcessor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static java.lang.String.format;

/**
 * Tests the ExternalFunctions class.
 */
public class ExternalFunctionsTest {
    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    private Map<String, Object> variables = new HashMap<>();

    /**
     * Runs a Stellar expression.
     * @param expr The expression to run.
     */
    private Object run(String expr) {
        StellarProcessor processor = new StellarProcessor();
        assertTrue(processor.validate(expr));
        return processor.parse(expr, x -> variables.get(x), StellarFunctions.FUNCTION_RESOLVER(), Context.EMPTY_CONTEXT());
    }

    @Test
    public void testBashFunction() throws Exception {
        Object result = run("EXEC_SCRIPT(\"bash\", \"bashTest.sh\")");
        Assert.assertNotNull(result);
        Assert.assertEquals(0, ((int) result);
    }

    @Test
    public void testShellFunction() throws Exception {
        Object result = run();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, ((int) result);
    }

    @Test
    public void testPythonFunction() throws Exception {
        Object result = run();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, ((int) result);
    }

    @Test
    public void testJavaFunction() throws Exception {
        Object result = run();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, ((int) result);
    }
}

