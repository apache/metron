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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

public class VFSFileUtilities implements FileUtilities{
  long MILLIS_BETWEEN_ATTEMPTS = 50L;

  @Override
  public void close(){

  }

  /* Superseded by renamed class bellow */
  @Deprecated
  @Override
  public void ensureDirectoryExistAndCanAccess(FileObject dir) throws FileSystemException {
    ensureDirectoryExistAndCanReadAndWrite(dir);
  }
  @Override
  public void ensureDirectoryExistAndCanReadAndWrite(FileObject dir) throws FileSystemException {
    if (dir.exists() && !dir.isFolder()) {
      throw new FileSystemException(dir.getURL() + " is not a directory");
    } else if (!dir.exists()) {
      try{
        doCreateFolder(dir);
      }catch(FileSystemException fse)
      {
        throw new FileSystemException(dir.getURL().toString() + " could not be created",fse);
      }
    }
    if (!(doCanReadWrite(dir))) {
      throw new FileSystemException(dir.getURL().toString() + " directory does not have read/write privilege");
    }
  }

  @Override
  public void ensureDirectoryExistAndCanRead(FileObject dir) throws FileSystemException {
    if (dir.exists() && !dir.isFolder()) {
      throw new FileSystemException(dir.getURL() + " is not a directory");
    } else if (!dir.exists()) {
      try{
        doCreateFolder(dir);
      }catch(FileSystemException fse){
        throw new FileSystemException(dir.getURL().toString() + " could not be created",fse);
      }
    }
    if (!doIsReadable(dir)) {
      throw new FileSystemException(dir.getURL().toString() + " directory does not have read privilege");
    }
  }

  @Override
  public void createFile(FileObject file, InputStream inputStream) throws FileSystemException{
    file.createFile();
    try (final OutputStream os = file.getContent().getOutputStream()) {
      byte[] bytes = new byte[65536];
      int numRead;
      while ((numRead = inputStream.read(bytes)) != -1) {
        os.write(bytes, 0, numRead);
      }
    }catch(IOException ioe){
      throw new FileSystemException(ioe);
    }
  }

  @Override
  public void createFile(FileObject file, byte[] bytes) throws FileSystemException{
    file.createFile();
    try (final OutputStream os = file.getContent().getOutputStream()) {
      os.write(bytes, 0, bytes.length);
    }catch(IOException ioe){
      throw new FileSystemException(ioe);
    }
  }

  /**
   * Deletes the given file. If the given file exists but could not be deleted
   * this will be printed as a warning to the given logger
   *
   * @param file to delete
   * @param logger to notify
   * @return true if deleted
   */
  @Override
  public boolean deleteFile(FileObject file, Logger logger) {
    return deleteFile(file, logger, 1);
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
  @Override
  public boolean deleteFile(FileObject file, Logger logger, int attempts) {
    if (file == null) {
      return false;
    }
    boolean isGone = false;
    try {
      if (file.exists()) {
        final int effectiveAttempts = Math.max(1, attempts);
        for (int i = 0; i < effectiveAttempts && !isGone; i++) {
          isGone = doDeleteFile(file) || !file.exists();
          if (!isGone && (effectiveAttempts - i) > 1) {
            sleepQuietly(MILLIS_BETWEEN_ATTEMPTS);
          }
        }
        if (!isGone && logger != null) {
          logger.warn("File appears to exist but unable to delete file: " + file.getURL().toString());
        }
      }
    } catch (final Throwable t) {
      if (logger != null) {
        logger.warn("Unable to delete file: '" + file.getName() + "' due to " + t);
      }
    }
    return isGone;
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
  @Override
  public void deleteFilesInDirectory(FileObject directory, FileSelector filter, Logger logger) throws FileSystemException {
    deleteFilesInDirectory(directory, filter, logger, false);
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
  @Override
  public void deleteFilesInDirectory(FileObject directory, FileSelector filter, Logger logger, boolean recurse) throws FileSystemException {
    deleteFilesInDirectory(directory, filter, logger, recurse, false);
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
  @Override
  public void deleteFilesInDirectory(FileObject directory, FileSelector filter, Logger logger, boolean recurse, boolean deleteEmptyDirectories) throws FileSystemException {
    // ensure the specified directory is actually a directory and that it exists
    if (null != directory && directory.isFolder()) {
      final FileObject ingestFiles[] = directory.findFiles(filter);
      if (ingestFiles == null) {
        // null if abstract pathname does not denote a directory, or if an I/O error occurs
        throw new FileSystemException("Unable to list directory content in: " + directory.getURL());
      }
      for (FileObject ingestFile : ingestFiles) {
        if (ingestFile.isFile()) {
          deleteFile(ingestFile, logger, 3);
        }
        if (ingestFile.isFolder() && recurse) {
          deleteFilesInDirectory(ingestFile, filter, logger, recurse, deleteEmptyDirectories);
          if (deleteEmptyDirectories && ingestFile.getChildren().length == 0) {
            deleteFile(ingestFile, logger, 3);
          }
        }
      }
    }
  }

  /**
   * Deletes given files.
   *
   * @param files to delete
   * @param recurse will recurse
   * @throws FileSystemException if issues deleting files
   */
  @Override
  public void deleteFiles(Collection<FileObject> files, boolean recurse) throws FileSystemException {
    for (final FileObject file : files) {
      deleteFile(file, recurse);
    }
  }

  @Override
  public void deleteFile(FileObject file, boolean recurse) throws FileSystemException {
    if (file.isFolder() && recurse) {
      final FileObject[] list = file.getChildren();
      if(list != null && list.length > 0) {
        deleteFiles(Arrays.asList(list), recurse);
      }
    }
    //now delete the file itself regardless of whether it is plain file or a directory
    if (!deleteFile(file, null, 5)) {
      throw new FileSystemException("Unable to delete " + file.getURL().toString());
    }
  }

  protected static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException ex) {
          /* do nothing */
    }
  }

  protected boolean doDeleteFile(FileObject file) throws FileSystemException{
    return file.delete();
  }

  protected void doCreateFolder(FileObject folder) throws FileSystemException{
    folder.createFolder();
  }

  protected boolean doIsReadable(FileObject fileObject) throws FileSystemException{
    return fileObject.isReadable();
  }

  protected boolean doIsWriteable(FileObject fileObject) throws FileSystemException{
    return fileObject.isWriteable();
  }

  protected boolean doCanReadWrite(FileObject fileObject) throws FileSystemException{
    return fileObject.isReadable() && fileObject.isWriteable();
  }
}
