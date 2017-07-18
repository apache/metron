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
package org.apache.metron.management;

import com.jakewharton.fliptables.FlipTable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.slf4j.LoggerFactory;

public class FileSystemFunctions {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public interface FileSystemGetter {
    FileSystem getSystem() throws IOException ;
  }

  public static enum FS_TYPE implements FileSystemGetter {
    LOCAL(() -> {
      FileSystem fs = new LocalFileSystem();
      fs.initialize(URI.create("file:///"), new Configuration());
      return fs;
    })
    ,HDFS(() -> FileSystem.get(new Configuration()))
    ;
    FileSystemGetter _func;
    FS_TYPE(FileSystemGetter func) {
      _func = func;
    }


    @Override
    public FileSystem getSystem() throws IOException {
      return _func.getSystem();
    }
  }

  private abstract static class FileSystemFunction implements StellarFunction {
    protected FileSystem fs;
    private FileSystemGetter getter;
    FileSystemFunction(FileSystemGetter getter) {
      this.getter = getter;
    }
    @Override
    public void initialize(Context context) {
      try {
        fs = getter.getSystem();
      } catch (IOException e) {
        String message = "Unable to get FileSystem: " + e.getMessage();
        LOG.error(message, e);
        throw new IllegalStateException(message, e);
      }
    }

    @Override
    public boolean isInitialized() {
      return fs != null;
    }

  }

  static class FileSystemGetList extends FileSystemFunction {

    FileSystemGetList(FileSystemGetter getter) {
      super(getter);
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String path = (String) args.get(0);
      if(path == null) {
        return null;
      }
      try(FSDataInputStream is = fs.open(new Path(path))) {
        return IOUtils.readLines(is);
      } catch (IOException e) {
        String message = "Unable to read " + path + ": " + e.getMessage();
        LOG.error(message, e);
        return null;
      }
    }
  }

  static class FileSystemGet extends FileSystemFunction {

    FileSystemGet(FileSystemGetter getter) {
      super(getter);
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String path = (String) args.get(0);
      if(path == null) {
        return null;
      }
      try(FSDataInputStream is = fs.open(new Path(path))) {
        return IOUtils.toString(is);
      } catch (IOException e) {
        String message = "Unable to read " + path + ": " + e.getMessage();
        LOG.error(message, e);
        return null;
      }
    }
  }

  static class FileSystemRm extends FileSystemFunction {

    FileSystemRm(FileSystemGetter getter) {
      super(getter);
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {

      String path = (String) args.get(0);
      if(path == null) {
        return false;
      }

      boolean recursive = false;
      if(args.size() > 1) {
        recursive = ConversionUtils.convert(args.get(1), Boolean.class);
      }

      try {
        fs.delete(new Path(path), recursive);
        return true;
      } catch (IOException e) {
        String message = "Unable to remove " + path + (recursive?" recursively":"") + ": " + e.getMessage();
        LOG.error(message, e);
        return false;
      }
    }
  }

  static class FileSystemPut extends FileSystemFunction {

    FileSystemPut(FileSystemGetter getter) {
      super(getter);
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      String content = (String)args.get(0);
      if(content == null) {
        return false;
      }
      String path = (String) args.get(1);
      if(path == null) {
        return false;
      }

      try(FSDataOutputStream os = fs.create(new Path(path))) {
        os.writeBytes(content);
        os.flush();
        return true;
      } catch (IOException e) {
        String message = "Unable to write " + path + ": " + e.getMessage();
        LOG.error(message, e);
        return false;
      }
    }
  }

  static class FileSystemLs extends FileSystemFunction {
    private static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {

      @Override
      protected DateFormat initialValue() {
        return DateFormat.getDateTimeInstance(
                DateFormat.DEFAULT,
                DateFormat.DEFAULT,
                Locale.getDefault()
                );
      }
    };

    FileSystemLs(FileSystemGetter getter) {
      super(getter);
    }

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      Path path = null;
      String[] headers = new String[] {"PERMISSION", "OWNER", "GROUP", "SIZE", "LAST MOD TIME", "NAME"};
      if(args.size() == 0) {
        path = fs.getHomeDirectory();
      }
      else {
        String pathStr = (String) args.get(0);
        if (pathStr == null) {
          return FlipTable.of(headers, new String[][]{});
        }
        else {
          try {
            path = new Path(pathStr);
          }
          catch(IllegalArgumentException iae) {
            LOG.error("Unable to convert {} to a path {}", pathStr, iae.getMessage(), iae);
            return FlipTable.of(headers, new String[][]{});
          }
        }
      }
      try {
        List<String[]> dataList = new ArrayList<>();
        for(FileStatus status : fs.listStatus(path)) {
          dataList.add(new String[] {
            status.getPermission().toString()
           ,status.getOwner()
           ,status.getGroup()
           ,status.getLen() + ""
           ,dateFormat.get().format(new Date(status.getModificationTime()))
           ,status.getPath().getName()
          });
        }
        Collections.sort(dataList, (o1, o2) -> {
            try {
              Date left = dateFormat.get().parse(o1[4]);
              Date right = dateFormat.get().parse(o2[4]);
              int ret = left.compareTo(right);
              //first sort by date, then by name.
              if(ret == 0) {
                return o1[5].compareTo(o2[5]);
              }
              else {
                return ret;
              }
            } catch (java.text.ParseException e) {
              String message = "Unable to parse " + Arrays.toString(o1) + " or " + Arrays.toString(o2) + " : " + e.getMessage();
              LOG.error(message, e);
              throw new IllegalStateException(message, e);
            }
        });
        String[][] data = new String[dataList.size()][headers.length];
        for(int i = 0;i < dataList.size();++i) {
          data[i] = dataList.get(i);
        }
        return FlipTable.of(headers, data);
      } catch (IOException e) {
        String message = "Unable to list" + path + " : " + e.getMessage();
        LOG.error(message, e);
        return FlipTable.of(headers, new String[][]{});
      }
    }
  }

  @Stellar(namespace="LOCAL"
          ,name="RM"
          ,description="Removes the path"
          ,params = { "path - The path of the file or directory."
                    , "recursive - Recursively remove or not (optional and defaulted to false)"
                    }
          ,returns = "boolean - true if successful, false otherwise."

  )
  public static class FileRm extends FileSystemRm {
    public FileRm() {
      super(FS_TYPE.LOCAL);
    }
  }

  @Stellar(namespace="HDFS"
          ,name="RM"
          ,description="Removes the path"
          ,params = { "path - The path in HDFS of the file."
                    , "recursive - Recursively remove or not (optional and defaulted to false)"
                    }
          ,returns = "boolean - true if successful, false otherwise."

  )
  public static class HDFSRm extends FileSystemRm {
    public HDFSRm() {
      super(FS_TYPE.HDFS);
    }
  }

  @Stellar(namespace="LOCAL"
          ,name="LS"
          ,description="Lists the contents of a directory"
          ,params = { "path - The path of the file."
                    }
          ,returns = "The contents of the directory in tabular form sorted by last modification date."

  )
  public static class FileLs extends FileSystemLs {
    public FileLs() {
      super(FS_TYPE.LOCAL);
    }
  }

  @Stellar(namespace="HDFS"
          ,name="LS"
          ,description="Lists the contents of a directory"
          ,params = { "path - The path in HDFS of the file."
                    }
          ,returns = "The contents of the directory in tabular form sorted by last modification date."

  )
  public static class HDFSLs extends FileSystemLs {
    public HDFSLs() {
      super(FS_TYPE.HDFS);
    }
  }

  @Stellar(namespace="HDFS"
          ,name="WRITE"
          ,description="Writes the contents a string to a file in HDFS."
          ,params = { "content - The content to write out"
                    , "path - The path in HDFS of the file."
                    }
          ,returns = "true if the file was written and false otherwise."

  )
  public static class HDFSPut extends FileSystemPut {


    public HDFSPut() {
      super(FS_TYPE.HDFS);
    }
  }

  @Stellar(namespace="LOCAL"
          ,name="WRITE"
          ,description="Writes the contents a string to a local file."
          ,params = { "content - The content to write out"
                    , "path - The path of the file."
                    }
          ,returns = "true if the file was written and false otherwise."

  )
  public static class FilePut extends FileSystemPut {
    public FilePut() {
      super(FS_TYPE.LOCAL);
    }
  }

  @Stellar(namespace="HDFS"
          ,name="READ"
          ,description="Retrieves the contents as a string of a file in HDFS."
          ,params = { "path - The path in HDFS of the file."}
          ,returns = "The contents of the file in the path from HDFS and null otherwise."

  )
  public static class HDFSGet extends FileSystemGet {
    public HDFSGet() {
      super(FS_TYPE.HDFS);
    }
  }

  @Stellar(namespace="LOCAL"
          ,name="READ"
          ,description="Retrieves the contents as a string of a file on the local filesystem."
          ,params = { "path - The path of the file."}
          ,returns = "The contents of the file or null otherwise."

  )
  public static class FileGet extends FileSystemGet {
    public FileGet() {
      super(FS_TYPE.LOCAL);
    }
  }

  @Stellar(namespace="HDFS"
          ,name="READ_LINES"
          ,description="Retrieves the contents of a HDFS file as a list of strings."
          ,params = { "path - The path in HDFS of the file."}
          ,returns = "A list of lines"

  )
  public static class HDFSGetList extends FileSystemGetList {
    public HDFSGetList() {
      super(FS_TYPE.HDFS);
    }
  }

  @Stellar(namespace="LOCAL"
          ,name="READ_LINES"
          ,description="Retrieves the contents of a file as a list of strings."
          ,params = { "path - The path of the file."}
          ,returns = "A list of lines"

  )
  public static class FileGetList extends FileSystemGetList {
    public FileGetList() {
      super(FS_TYPE.LOCAL);
    }
  }
}
