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
package org.apache.metron.integration;


import java.util.LinkedHashMap;
import java.util.Map;

public class ComponentRunner {
    public static class Builder {
        LinkedHashMap<String, InMemoryComponent> components;
        String[] startupOrder;
        String[] shutdownOrder;
        long timeBetweenAttempts = 1000;
        int numRetries = 5;
        long maxTimeMS = 120000;
        public Builder() {
            components = new LinkedHashMap<String, InMemoryComponent>();
        }

        public Builder withNumRetries(int numRetries) {
            this.numRetries = numRetries;
            return this;
        }

        public Builder withMaxTimeMS(long maxTimeMS) {
            this.maxTimeMS = maxTimeMS;
            return this;
        }

        public Builder withComponent(String name, InMemoryComponent component) {
            components.put(name, component);
            return this;
        }

        public Builder withCustomStartupOrder(String[] startupOrder) {
            this.startupOrder = startupOrder;
            return this;
        }
        public Builder withCustomShutdownOrder(String[] shutdownOrder) {
            this.shutdownOrder = shutdownOrder;
            return this;
        }
        public Builder withMillisecondsBetweenAttempts(long timeBetweenAttempts) {
            this.timeBetweenAttempts = timeBetweenAttempts;
            return this;
        }
        private static String[] toOrderedList(Map<String, InMemoryComponent> components) {
            String[] ret = new String[components.size()];
            int i = 0;
            for(String component : components.keySet()) {
                ret[i++] = component;
            }
            return ret;
        }
        public ComponentRunner build() {
            if(shutdownOrder == null) {
                shutdownOrder = toOrderedList(components);
            }
            if(startupOrder == null) {
                startupOrder = toOrderedList(components);
            }
            return new ComponentRunner(components, startupOrder, shutdownOrder, timeBetweenAttempts, numRetries, maxTimeMS);
        }

    }

    LinkedHashMap<String, InMemoryComponent> components;
    String[] startupOrder;
    String[] shutdownOrder;
    long timeBetweenAttempts;
    int numRetries;
    long maxTimeMS;
    public ComponentRunner( LinkedHashMap<String, InMemoryComponent> components
                          , String[] startupOrder
                          , String[] shutdownOrder
                          , long timeBetweenAttempts
                          , int numRetries
                          , long maxTimeMS
                          )
    {
        this.components = components;
        this.startupOrder = startupOrder;
        this.shutdownOrder = shutdownOrder;
        this.timeBetweenAttempts = timeBetweenAttempts;
        this.numRetries = numRetries;
        this.maxTimeMS = maxTimeMS;
    }

    public <T extends InMemoryComponent> T getComponent(String name, Class<T> clazz) {
        return clazz.cast(getComponents().get(name));
    }

    public LinkedHashMap<String, InMemoryComponent> getComponents() {
        return components;
    }

    public void start() throws UnableToStartException {
        for(String componentName : startupOrder) {
            components.get(componentName).start();
        }
    }
    public void stop() {
        for(String componentName : shutdownOrder) {
            components.get(componentName).stop();
        }
    }


    public <T> T process(Processor<T> successState) {
        int retryCount = 0;
        long start = System.currentTimeMillis();
        while(true) {
            long duration = System.currentTimeMillis() - start;
            if(maxTimeMS > 0 && duration > maxTimeMS) {
                throw new RuntimeException("Took too long to complete: " + duration + " > " + maxTimeMS);
            }
            ReadinessState state = successState.process(this);
            if(state == ReadinessState.READY) {
                return successState.getResult();
            }
            else if(state == ReadinessState.NOT_READY) {
                retryCount++;
                if(numRetries > 0 && retryCount > numRetries) {
                    throw new RuntimeException("Too many retries: " + retryCount);
                }
            }
            try {
                Thread.sleep(timeBetweenAttempts);
            } catch (InterruptedException e) {
                throw new RuntimeException("Unable to sleep", e);
            }
        }
    }


}
