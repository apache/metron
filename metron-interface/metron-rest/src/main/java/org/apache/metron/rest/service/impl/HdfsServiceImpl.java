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
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.metron.rest.RestException;
import org.apache.metron.rest.service.HdfsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class HdfsServiceImpl implements HdfsService {

    private Configuration configuration;

    @Autowired
    public HdfsServiceImpl(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<String> list(Path path) throws RestException {
      try {
          return Arrays.asList(FileSystem.get(configuration).listStatus(path)).stream().map(fileStatus -> fileStatus.getPath().getName()).collect(Collectors.toList());
      } catch (IOException e) {
          throw new RestException(e);
      }
    }

    @Override
    public String read(Path path) throws RestException {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try {
        IOUtils.copyBytes(FileSystem.get(configuration).open(path), byteArrayOutputStream, configuration);
      } catch (FileNotFoundException e) {
        return null;
      } catch (IOException e) {
        throw new RestException(e);
      }
      return new String(byteArrayOutputStream.toByteArray(), UTF_8);
    }

    @Override
    public void write(Path path, byte[] contents) throws RestException {
      FSDataOutputStream fsDataOutputStream;
      try {
        fsDataOutputStream = FileSystem.get(configuration).create(path, true);
        fsDataOutputStream.write(contents);
        fsDataOutputStream.close();
      } catch (IOException e) {
        throw new RestException(e);
      }
    }

    @Override
    public boolean delete(Path path, boolean recursive) throws RestException {
      try {
        return FileSystem.get(configuration).delete(path, recursive);
      } catch (IOException e) {
        throw new RestException(e);
      }
    }
 }
