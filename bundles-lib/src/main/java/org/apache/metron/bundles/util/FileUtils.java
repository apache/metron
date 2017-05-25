/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.bundles.util;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileUtils  {

  private static FileUtilities utilities;
  private static AtomicBoolean inited = new AtomicBoolean(false);

  public static void init(FileUtilities utils){
    utilities = utils;
    inited.set(true);
  }

  public static void reset(){
    if(utilities != null){
      utilities.close();
      utilities = null;
    }
    inited.set(false);
  }

  /* Superseded by renamed class bellow */
  @Deprecated
  public static void ensureDirectoryExistAndCanAccess(FileObject dir) throws FileSystemException  {
    isInited();
    utilities.ensureDirectoryExistAndCanAccess(dir);
  }

  public static void ensureDirectoryExistAndCanReadAndWrite(FileObject dir) throws FileSystemException {
    isInited();
    utilities.ensureDirectoryExistAndCanReadAndWrite(dir);
  }

  public static void ensureDirectoryExistAndCanRead(FileObject dir) throws FileSystemException {
    isInited();
    utilities.ensureDirectoryExistAndCanRead(dir);
  }

  public static void createFile(FileObject file, InputStream stream) throws FileSystemException{ utilities.createFile(file, stream);}
  public static void createFile(FileObject file, byte[] bytes) throws FileSystemException{ utilities.createFile(file, bytes);}

  /**
   * Deletes the given file. If the given file exists but could not be deleted
   * this will be printed as a warning to the given logger
   *
   * @param file to delete
   * @param logger to notify
   * @return true if deleted
   */
  public static boolean deleteFile(FileObject file, Logger logger) {
    return utilities.deleteFile(file, logger);
  }

  /**
   * Deletes the given file. If the given file exists but could not be deleted
   * this will be printed as a warning to the given logger
   *
   * @param file to delete
   * @param logger to notify
   * @param attempts indicates how many times an attempt to delete should be
   * made
   * @return true if given file no longer exists
   */
  public static boolean deleteFile(FileObject file, Logger logger, int attempts) {
    isInited();
    return utilities.deleteFile(file, logger, attempts);
  }

  /**
   * Deletes all files (not directories..) in the given directory (non
   * recursive) that match the given filename filter. If any file cannot be
   * deleted then this is printed at warn to the given logger.
   *
   * @param directory to delete contents of
   * @param filter if null then no filter is used
   * @param logger to notify
   * @throws FileSystemException if abstract pathname does not denote a directory, or
   * if an I/O error occurs
   */
  public static void deleteFilesInDirectory(FileObject directory, FileSelector filter, Logger logger) throws FileSystemException {
    isInited();
    utilities.deleteFilesInDirectory(directory, filter, logger);
  }

  /**
   * Deletes all files (not directories) in the given directory (recursive)
   * that match the given filename filter. If any file cannot be deleted then
   * this is printed at warn to the given logger.
   *
   * @param directory to delete contents of
   * @param filter if null then no filter is used
   * @param logger to notify
   * @param recurse true if should recurse
   * @throws FileSystemException if abstract pathname does not denote a directory, or
   * if an I/O error occurs
   */
  public static void deleteFilesInDirectory(FileObject directory, FileSelector filter, Logger logger, boolean recurse) throws FileSystemException {
    isInited();
    utilities.deleteFilesInDirectory(directory, filter, logger, recurse);
  }

  /**
   * Deletes all files (not directories) in the given directory (recursive)
   * that match the given filename filter. If any file cannot be deleted then
   * this is printed at warn to the given logger.
   *
   * @param directory to delete contents of
   * @param filter if null then no filter is used
   * @param logger to notify
   * @param recurse will look for contents of sub directories.
   * @param deleteEmptyDirectories default is false; if true will delete
   * directories found that are empty
   * @throws FileSystemException if abstract pathname does not denote a directory, or
   * if an I/O error occurs
   */
  public static void deleteFilesInDirectory(FileObject directory, FileSelector filter, Logger logger, boolean recurse, boolean deleteEmptyDirectories) throws FileSystemException {
    isInited();
    utilities.deleteFilesInDirectory(directory, filter, logger, recurse, deleteEmptyDirectories);
  }

  /**
   * Deletes given files.
   *
   * @param files to delete
   * @param recurse will recurse
   * @throws FileSystemException if issues deleting files
   */
  public static void deleteFiles(Collection<FileObject> files, boolean recurse) throws FileSystemException {
    isInited();
    utilities.deleteFiles(files, recurse);
  }

  public static void deleteFile(FileObject file, boolean recurse) throws FileSystemException {
    isInited();
    utilities.deleteFile(file, recurse);
  }

  public static void isInited() {
    if(inited.get() == false){
      utilities = new VFSFileUtilities();
      inited.set(true);
    }
  }
}
