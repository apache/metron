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
package org.apache.metron.rest.service.impl;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.metron.rest.service.HdfsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class HdfsServiceImpl implements HdfsService {

    @Autowired
    private Configuration configuration;

    @Override
    public byte[] read(Path path) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.copyBytes(FileSystem.get(configuration).open(path), byteArrayOutputStream, configuration);
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public void write(Path path, byte[] contents) throws IOException {
        FSDataOutputStream fsDataOutputStream = FileSystem.get(configuration).create(path, true);
        fsDataOutputStream.write(contents);
        fsDataOutputStream.close();
    }

    @Override
    public FileStatus[] list(Path path) throws IOException {
        return FileSystem.get(configuration).listStatus(path);
    }

    @Override
    public boolean delete(Path path, boolean recursive) throws IOException {
        return FileSystem.get(configuration).delete(path, recursive);
    }
 }
