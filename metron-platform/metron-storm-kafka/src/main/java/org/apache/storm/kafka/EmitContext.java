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
package org.apache.storm.kafka;

import org.apache.storm.kafka.spout.KafkaSpoutConfig;
import org.apache.storm.task.TopologyContext;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The context for the emit call.  This allows us to pass static information into the spout callback.
 */
public class EmitContext implements Cloneable,Serializable {
    static final long serialVersionUID = 0xDEADBEEFL;

    /**
     * The static information to be tracked.
     */
    public enum Type{
        STREAM_ID(String.class)
        ,PARTITION(Integer.class)
        ,TASK_ID(Integer.class)
        ,UUID(String.class)
        ,SPOUT_CONFIG(KafkaSpoutConfig.class)
        ,OPEN_CONFIG(Map.class)
        ,TOPOLOGY_CONTEXT(TopologyContext.class)
        ;
        Class<?> clazz;
        Type(Class<?> clazz) {
            this.clazz=  clazz;
        }

        public Class<?> clazz() {
           return clazz;
        }
    }
    public EmitContext() {
        this(new EnumMap<>(Type.class));
    }
    public EmitContext(EnumMap<Type, Object> context) {
        _context = context;
    }
    private EnumMap<Type, Object> _context;

    public <T> EmitContext with(Type t, T o ) {
        _context.put(t, t.clazz().cast(o));
        return this;
    }
    public <T> void add(Type t, T o ) {
        with(t, o);
    }

    public <T> T get(Type t) {
        Object o = _context.get(t);
        if(o == null) {
            return null;
        }
        else {
            return (T) o;
        }
    }

    public EmitContext cloneContext() {
        try {
            return (EmitContext)this.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Unable to clone emit context.", e);
        }
    }


    @Override
    protected Object clone() throws CloneNotSupportedException {
        EmitContext context = new EmitContext(_context.clone());
        return context;
    }
}
