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
package org.apache.metron.common.dsl.functions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.lang.ProcessBuilder;
import java.lang.ClassLoader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.StellarFunction;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Stellar;

/**
 * Executes external script on server via stellar process
 */
public class ExternalFunctions {

	public static class ExecuteScript implements StellarFunction {

        private ThreadedStreamHandler inStream;
        private ThreadedStreamHandler errStream;
        private boolean isOnTheList = false;

        @Stellar(name="EXEC_SCRIPT",
                description = "Executes an external shell function via stellar.",
                params = {
                        "exec - the executing cmd (ie. bash, sh, python)",
                        "name - name of the script, located in /scripts " +
                                "Do NOT include any special chars except(_), Do include file extension"
                },
                returns = "the return value of the function"
        )

	    @Override
        public Object apply(List<Object> args, Context context) throws ParseException {
            String exec = "";
            String name = "";
            String path = "";

            // if args are provided, get args, only if in whitelist
            if (args.size() >= 1) {
                Object execObj = args.get(0);
                if (!(execObj instanceof String)) { //check if string
                    return null;
                }
                else if (((String) execObj).length() > 0) {
                    exec = (String) execObj;
                }
                else {
                    return null;
                }

                Object nameObj = args.get(1);
                if (!(nameObj instanceof String)) { //check if string
                    return null;
                }
                else if (((String) nameObj).length() > 0) {
                    name = (String) nameObj;
                }
                else {
                    return null;
                }

                if (!Pattern.matches("[0-9A-Za-z.]+", name)) {
                    return null; //if not on whitelist
                }

                path = "/scripts" + name;
                try {
                    File script = new File(path);
                    if (!script.exists() || script.isDirectory()) {
                        return null;
                    }
                }
                catch (NullPointerException e)  {
                    System.err.println("Error: " + e.toString());
                    return null;
                }

                switch (exec) { //check if matches extension
                    case "bash":
                        if (name.contains(".sh")) {
                            isOnTheList = true;
                        }
                        break;
                    case "sh":
                        if (name.contains(".sh")) {
                            isOnTheList = true;
                        }
                        break;
                    case "python":
                        if (name.contains(".py")) {
                            isOnTheList = true;
                        }
                        break;
                    case "java":
                        if (name.contains(".class")) {
                            isOnTheList = true;
                            // java cmd needs called w/o .class
                            name.replace(".class", "");
                        }
                        break;
                }
            }
            if (!(isOnTheList)) {
                return null;
            }

            try { // Create and start Process with ProcessBuilder.
                ProcessBuilder pb = new ProcessBuilder(exec, name);
                //Map<String, String> env = pb.environment();
                //env.put("VAR1", "myValue");

                File log = new File("/scripts/log");
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
                Process p = pb.start();
                int exitCode = p.waitFor();

                assert pb.redirectInput() == ProcessBuilder.Redirect.PIPE;
                assert pb.redirectOutput().file() == log;
                assert p.getInputStream().read() == -1;

                return exitCode;
            }
            catch (NullPointerException e) {
                System.err.println("Error: " + e.toString());
                return null;
            }
            catch (IndexOutOfBoundsException e) {
                System.err.println("Error: " + e.toString());
                return null;
            }
            catch (SecurityException e) {
                System.err.println("Error: " + e.toString());
                return null;
            }
            catch (IOException e) {
                System.err.println("Error: " + e.toString());
                return null;
            } catch (InterruptedException e) {
                System.err.println("Error: " + e.toString());
                return null;
            }
        }
	
	    @Override
        public void initialize(Context context) {

        }
	
	    @Override
	    public boolean isInitialized() {
            return true;
	    }
	}

    /**
     * Special thanks to alvin j. alexander at devdaily.com
     */
    class ThreadedStreamHandler extends Thread
    {
        InputStream inputStream;
        String adminPassword;
        OutputStream outputStream;
        PrintWriter printWriter;
        StringBuilder outputBuffer = new StringBuilder();
        private boolean sudoIsRequested = false;

        /**
         * A simple constructor for when the sudo command is not necessary.
         * This constructor will just run the command you provide, without
         * running sudo before the command, and without expecting a password.
         *
         * @param inputStream
         */
        ThreadedStreamHandler(InputStream inputStream)
        {
            this.inputStream = inputStream;
        }

        /**
         * Use this constructor when you want to invoke the 'sudo' command.
         * The outputStream must not be null. If it is, you'll regret it. :)
         *
         * TODO this currently hangs if the admin password given for the sudo command is wrong.
         *
         * @param inputStream
         * @param outputStream
         * @param adminPassword
         */
        ThreadedStreamHandler(InputStream inputStream, OutputStream outputStream, String adminPassword)
        {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            this.printWriter = new PrintWriter(outputStream);
            this.adminPassword = adminPassword;
            this.sudoIsRequested = true;
        }

        public void run()
        {
            // on mac os x 10.5.x, when i run a 'sudo' command, i need to write
            // the admin password out immediately; that's why this code is
            // here.
            if (sudoIsRequested)
            {
                //doSleep(500);
                printWriter.println(adminPassword);
                printWriter.flush();
            }

            BufferedReader bufferedReader = null;
            try
            {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = null;
                while ((line = bufferedReader.readLine()) != null)
                {
                    outputBuffer.append(line + "\n");
                }
            }
            catch (IOException ioe)
            {
                // TODO handle this better
                ioe.printStackTrace();
            }
            catch (Throwable t)
            {
                // TODO handle this better
                t.printStackTrace();
            }
            finally
            {
                try
                {
                    bufferedReader.close();
                }
                catch (IOException e)
                {
                    // ignore this one
                }
            }
        }

        private void doSleep(long millis)
        {
            try
            {
                Thread.sleep(millis);
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }

        public StringBuilder getOutputBuffer()
        {
            return outputBuffer;
        }
    }

}