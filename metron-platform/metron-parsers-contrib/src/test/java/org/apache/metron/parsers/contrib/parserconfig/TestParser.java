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
package org.apache.metron.parsers.contrib.parserconfig;

import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.apache.metron.parsers.contrib.chainparser.ChainParser;
import org.apache.metron.parsers.contrib.utils.JSONUtils;
import org.apache.metron.parsers.contrib.chainlink.ChainLink;
import org.apache.metron.parsers.contrib.chainparser.ChainParser;
import org.apache.metron.parsers.contrib.utils.JSONUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

abstract class TestParser {

    private long lastTime;
    private long lastParserTime;
    private long totalTime;
    private long totalTimeExceptFirst;
    private int runs;

    public String getFolder() {
        throw new NotImplementedException();
    }

    private String readFile(String fileName) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            return sb.toString();
        }
    }

    private Map<String, Object> getConfig() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String json = this.readFile(Objects.requireNonNull(classLoader.getResource(this.getFolder() + "/config.json")).getFile());
        return JSONUtils.JSONToMap(json);
    }

    private String[] getInputLoglines() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String data = this.readFile(Objects.requireNonNull(classLoader.getResource(this.getFolder() + "/data_input")).getFile());
        return data.split("\\r?\\n");
    }

    private String[] getOutputLoglines() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String data = this.readFile(Objects.requireNonNull(classLoader.getResource(this.getFolder() + "/data_output")).getFile());
        return data.split("\\r?\\n");
    }

    @Test
    public void testConfigure() throws IOException {
        ChainParser parser = new ChainParser();
        parser.configure(this.getConfig());
    }

    @Test
    public void testInputAndOutput() throws IOException {
        String inputLines[] = getInputLoglines();
        String outputLines[] = getOutputLoglines();
        assertEquals("The number of input loglines (" + inputLines.length + ") should equal the number of expected output loglines (" + outputLines.length + ").", inputLines.length, outputLines.length);
    }

    public void preLinkHook(ChainLink link) {
        lastTime = System.nanoTime();
        System.out.println("Link:            " + link.getClass().getName());

    }

    public void postLinkHook(ChainLink link) {
        long currentTime = System.nanoTime();
        long duration = currentTime - lastTime;
        this.lastParserTime += duration;
        System.out.println("Duration:        " + Double.toString(duration / 1000000.) + " ms");
        System.out.println("-----------------------------------------------------------------------------------");
    }

    @Test
    public void testParser() throws IOException, NoSuchMethodException {
        JSONParser parseJSON = new JSONParser();

        ChainParser parser = new ChainParser();
        parser.configure(this.getConfig());

        Method preLinkHook = this.getClass().getMethod("preLinkHook", ChainLink.class);
        parser.setPreLinkHook(preLinkHook, this);

        Method postLinkHook = this.getClass().getMethod("postLinkHook", ChainLink.class);
        parser.setPostLinkHook(postLinkHook, this);

        String inputLines[] = getInputLoglines();
        String outputLines[] = getOutputLoglines();

        // By testInputAndOutput we are assured that inputLines.length == outputLines.length
        runs = 0;
        totalTime = 0;
        totalTimeExceptFirst = 0;

        // Make sure that there are at least some amount of runs
        int numEpochs = Math.max(1, (int) Math.ceil(((10. + 1.) / inputLines.length)));

        for (int epoch = 0; epoch < numEpochs; epoch++) {
            for (int line = 0; line < inputLines.length; line++) {
                runs += 1;
                String input = inputLines[line];
                String strExpectedOutput = outputLines[line];
                System.out.println("===================================================================================");
                System.out.println("Start ChainParser:");
                System.out.println("Epoch:           " + Integer.toString(epoch));
                System.out.println("Logline:         " + Integer.toString(line));
                System.out.println("Input:           " + input);
                System.out.println("Expected output: " + strExpectedOutput);
                System.out.println("===================================================================================");
                JSONObject expectedOutput;

                lastParserTime = 0;

                try {
                    expectedOutput = (JSONObject) parseJSON.parse(strExpectedOutput);
                } catch (ParseException exception) {
                    exception.printStackTrace();
                    throw new IllegalStateException("Line " + line + " of the output file is not a valid JSON file.");
                }

                List<JSONObject> outputMessages = parser.parse(input.getBytes());
                assertEquals("There should be exactly one message produced by the parsers.", 1, outputMessages.size());
                JSONObject output = outputMessages.get(0);
                assertEquals("The output by applying the parser on line " + line + " of the data_input file does not match the expected output of line " + line + " in the data_output file.", expectedOutput.toString(), output.toString());

                totalTime += lastParserTime;
                if (line > 0) totalTimeExceptFirst += lastParserTime;
                System.out.println("Parser duration: " + Double.toString(lastParserTime / 1000000.) + " ms");
                System.out.println("-----------------------------------------------------------------------------------");
                System.out.println();
            }
        }

        System.out.println("===================================================================================");
        System.out.println("Overall statistics");
        System.out.println("===================================================================================");
        System.out.println("No. runs:        " + Integer.toString(runs));
        System.out.println("Total duration:  " + Double.toString(totalTime / 1000000.) + " ms");
        System.out.println("Avg. duration:   " + Double.toString(totalTime / 1000000. / (double) runs) + " ms");
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println();

        System.out.println("===================================================================================");
        System.out.println("Statistics (excluding first run since it initializes all classes)");
        System.out.println("===================================================================================");
        double avgDuration = totalTimeExceptFirst / 1000000. / ((double) runs - 1);
        int eps = (int) (1000. / avgDuration);
        System.out.println("No. runs:        " + Integer.toString(runs - 1));
        System.out.println("Total duration:  " + Double.toString(totalTimeExceptFirst / 1000000.) + " ms");
        System.out.println("Avg. duration:   " + Double.toString(avgDuration) + " ms");
        System.out.println("Single-node EPS: " + Integer.toString(eps));
        System.out.println("-----------------------------------------------------------------------------------");
    }

}
