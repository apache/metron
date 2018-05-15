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

package org.apache.metron.common.utils;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

/*
 * Factory to provide various compression strategies.
 */
public enum CompressionStrategies implements CompressionStrategy {

  GZIP(new CompressionStrategy() {
    @Override
    public void compress(File inFile, File outFile) throws IOException {
      try (FileInputStream fis = new FileInputStream(inFile);
          FileOutputStream fos = new FileOutputStream(outFile);
          GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) != -1) {
          gzipOS.write(buffer, 0, len);
        }
      }
    }

    @Override
    public void decompress(File inFile, File outFile) throws IOException {
      try (FileInputStream fis = new FileInputStream(inFile);
          GZIPInputStream gis = new GZIPInputStream(fis);
          FileOutputStream fos = new FileOutputStream(outFile)) {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = gis.read(buffer)) != -1) {
          fos.write(buffer, 0, len);
        }
      }

    }

    @Override
    public boolean test(File gzipFile) {
      try (FileInputStream fis = new FileInputStream(gzipFile);
          GZIPInputStream gis = new GZIPInputStream(fis)) {
        byte[] buffer = new byte[1024];
        // this will throw an exception on malformed file
        gis.read(buffer);
      } catch (ZipException | EOFException e) {
        return false;
      } catch (IOException e) {
        throw new IllegalStateException("Error occurred while attempting to validate gzip file", e);
      }
      return true;
    }
  });

  private CompressionStrategy strategy;

  CompressionStrategies(CompressionStrategy strategy) {
    this.strategy = strategy;
  }

  @Override
  public void compress(File inFile, File outFile) throws IOException {
    strategy.compress(inFile, outFile);
  }

  @Override
  public void decompress(File inFile, File outFile) throws IOException {
    strategy.decompress(inFile, outFile);
  }

  @Override
  public boolean test(File gzipFile) {
    return strategy.test(gzipFile);
  }

}
