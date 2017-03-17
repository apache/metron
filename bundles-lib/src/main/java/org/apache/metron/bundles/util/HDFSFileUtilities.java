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
import org.apache.commons.vfs2.FileSystemException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclStatus;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;

import java.io.*;

public class HDFSFileUtilities extends VFSFileUtilities {
  private FileSystem fileSystem;
  private boolean aclEnabled;
  public HDFSFileUtilities(FileSystem fileSystem){
    super();
    this.fileSystem = fileSystem;

    aclEnabled = Boolean.parseBoolean(fileSystem.getConf().get("dfs.namenode.acls.enabled"));
  }
  @Override
  public void close(){
    if(fileSystem != null){
      try {
        fileSystem.close();
      }catch(Exception e){}
      fileSystem = null;
    }
  }
  @Override
  public void createFile(FileObject file, InputStream inputStream) throws FileSystemException{
    try{
      try(BufferedOutputStream os = new BufferedOutputStream(fileSystem.create( new Path(file.getName().getPath())))) {
        byte[] buffer = new byte[65536];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          os.write(buffer, 0, bytesRead);
        }
      }
    }catch(IOException ioe){
      throw new FileSystemException("Cannot create folder " + file.getName().getPath(),ioe);
    }
  }

  @Override
  public void createFile(FileObject file, byte[] bytes) throws FileSystemException{
    try{
      try(BufferedOutputStream os = new BufferedOutputStream(FileSystem.create(fileSystem, new Path(file.getName().getPath()),new FsPermission(FsAction.ALL,FsAction.ALL,FsAction.ALL)))) {
          os.write(bytes, 0, bytes.length);
      }
    }catch(IOException ioe){
      throw new FileSystemException("Cannot create folder " + file.getName().getPath(),ioe);
    }
  }
  @Override
  protected boolean doDeleteFile(FileObject file) throws FileSystemException{
    try{
      fileSystem.delete(new Path(file.getName().getPath()),false);
    }catch(IOException ioe){
      throw new FileSystemException("Cannot delete " + file.getName().getPath(),ioe);
    }
    return true;
  }

  @Override
  protected void doCreateFolder(FileObject folder) throws FileSystemException{
    try{
      FileSystem.mkdirs(fileSystem,new Path(folder.getName().getPath()),new FsPermission(FsAction.ALL,FsAction.ALL,FsAction.ALL));
    }catch(IOException ioe){
      throw new FileSystemException("Cannot create folder " + folder.getName().getPath(),ioe);
    }

  }

  @Override
  protected boolean doIsReadable(FileObject fileObject) throws FileSystemException{
    if(!aclEnabled){
      return true;
    }
    try{
      AclStatus status = fileSystem.getAclStatus(new Path(fileObject.getName().getPath()));
      return status.getPermission().getUserAction().implies(FsAction.READ);
    }catch(IOException ioe){
      throw new FileSystemException(ioe);
    }
  }

  @Override
  protected boolean doIsWriteable(FileObject fileObject) throws FileSystemException{
    if(!aclEnabled){
      return true;
    }
    try{
      AclStatus status = fileSystem.getAclStatus(new Path(fileObject.getName().getPath()));
      return status.getPermission().getUserAction().implies(FsAction.WRITE);
    }catch(IOException ioe){
      throw new FileSystemException(ioe);
    }
  }

  @Override
  protected boolean doCanReadWrite(FileObject fileObject) throws FileSystemException{
    if(!aclEnabled){
      return true;
    }
    try{

      AclStatus status = fileSystem.getAclStatus(new Path(fileObject.getName().getPath()));
      return status.getPermission().getUserAction().implies(FsAction.READ_WRITE);
    }
    catch(IOException ioe){
      throw new FileSystemException(ioe);
    }
  }

}
