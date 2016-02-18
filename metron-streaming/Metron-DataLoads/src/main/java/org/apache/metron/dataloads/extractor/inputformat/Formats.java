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
package org.apache.metron.dataloads.extractor.inputformat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

import java.io.IOException;
import java.util.Map;

public enum Formats implements InputFormatHandler{
    BY_LINE(new InputFormatHandler() {
        @Override
        public void set(Job job, Path input, Map<String, Object> config) throws IOException {

            FileInputFormat.addInputPath(job, input);
        }
    })
    ;
    InputFormatHandler _handler = null;
    Formats(InputFormatHandler handler) {
        this._handler = handler;
    }
    @Override
    public void set(Job job, Path path, Map<String, Object> config) throws IOException {
        _handler.set(job, path, config);
    }

    public static InputFormatHandler create(String handlerName) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        try {
            InputFormatHandler ec = Formats.valueOf(handlerName);
            return ec;
        }
        catch(IllegalArgumentException iae) {
            InputFormatHandler ex = (InputFormatHandler) Class.forName(handlerName).newInstance();
            return ex;
        }
    }
}
