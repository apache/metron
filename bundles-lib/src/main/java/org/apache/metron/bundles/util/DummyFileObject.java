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

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.operations.FileOperations;

/**
 * This is a dummy FileObject Implementation
 * It will return true if exists() is called
 */
public class DummyFileObject implements FileObject{

  @Override
  public boolean canRenameTo(FileObject fileObject) {
    return false;
  }

  @Override
  public void close() throws FileSystemException {

  }

  @Override
  public void copyFrom(FileObject fileObject, FileSelector fileSelector)
      throws FileSystemException {

  }

  @Override
  public void createFile() throws FileSystemException {

  }

  @Override
  public void createFolder() throws FileSystemException {

  }

  @Override
  public boolean delete() throws FileSystemException {
    return false;
  }

  @Override
  public int delete(FileSelector fileSelector) throws FileSystemException {
    return 0;
  }

  @Override
  public int deleteAll() throws FileSystemException {
    return 0;
  }

  @Override
  public boolean exists() throws FileSystemException {
    return true;
  }

  @Override
  public FileObject[] findFiles(FileSelector fileSelector) throws FileSystemException {
    return new FileObject[0];
  }

  @Override
  public void findFiles(FileSelector fileSelector, boolean b, List<FileObject> list)
      throws FileSystemException {

  }

  @Override
  public FileObject getChild(String s) throws FileSystemException {
    return null;
  }

  @Override
  public FileObject[] getChildren() throws FileSystemException {
    return new FileObject[0];
  }

  @Override
  public FileContent getContent() throws FileSystemException {
    return null;
  }

  @Override
  public FileOperations getFileOperations() throws FileSystemException {
    return null;
  }

  @Override
  public FileSystem getFileSystem() {
    return null;
  }

  @Override
  public FileName getName() {
    return null;
  }

  @Override
  public FileObject getParent() throws FileSystemException {
    return null;
  }

  @Override
  public String getPublicURIString() {
    return null;
  }

  @Override
  public FileType getType() throws FileSystemException {
    return null;
  }

  @Override
  public URL getURL() throws FileSystemException {
    return null;
  }

  @Override
  public boolean isAttached() {
    return false;
  }

  @Override
  public boolean isContentOpen() {
    return false;
  }

  @Override
  public boolean isExecutable() throws FileSystemException {
    return false;
  }

  @Override
  public boolean isFile() throws FileSystemException {
    return false;
  }

  @Override
  public boolean isFolder() throws FileSystemException {
    return false;
  }

  @Override
  public boolean isHidden() throws FileSystemException {
    return false;
  }

  @Override
  public boolean isReadable() throws FileSystemException {
    return false;
  }

  @Override
  public boolean isWriteable() throws FileSystemException {
    return false;
  }

  @Override
  public void moveTo(FileObject fileObject) throws FileSystemException {

  }

  @Override
  public void refresh() throws FileSystemException {

  }

  @Override
  public FileObject resolveFile(String s) throws FileSystemException {
    return null;
  }

  @Override
  public FileObject resolveFile(String s, NameScope nameScope) throws FileSystemException {
    return null;
  }

  @Override
  public boolean setExecutable(boolean b, boolean b1) throws FileSystemException {
    return false;
  }

  @Override
  public boolean setReadable(boolean b, boolean b1) throws FileSystemException {
    return false;
  }

  @Override
  public boolean setWritable(boolean b, boolean b1) throws FileSystemException {
    return false;
  }

  @Override
  public int compareTo(FileObject o) {
    return 0;
  }

  @Override
  public Iterator<FileObject> iterator() {
    return null;
  }
}
