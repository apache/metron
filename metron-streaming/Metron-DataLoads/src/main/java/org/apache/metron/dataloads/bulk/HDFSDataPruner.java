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
package org.apache.metron.dataloads.bulk;


import org.apache.commons.cli.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class HDFSDataPruner  {

    private static final Logger LOG = LoggerFactory.getLogger(HDFSDataPruner.class);
    private Date startDate;
    private long firstTimeMillis;
    private long lastTimeMillis;
    private Path globPath;
    protected FileSystem fileSystem;

    private HDFSDataPruner() {
    }


    HDFSDataPruner(Date startDate, Integer numDays, String fsUri, String globPath) throws IOException {

        this.startDate = dateAtMidnight(startDate);
        this.lastTimeMillis = startDate.getTime();
        this.firstTimeMillis = lastTimeMillis - TimeUnit.DAYS.toMillis(numDays);
        this.globPath = new Path(globPath);
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", fsUri);
        this.fileSystem = FileSystem.get(conf);

    }


    public static void main(String... argv) throws IOException, java.text.ParseException, ClassNotFoundException, InterruptedException {

        /**
         * Example
         * start=$(date -d '30 days ago' +%m/%d/%Y)
         * yarn jar Metron-DataLoads-0.1BETA.jar org.apache.metron.dataloads.bulk.HDFSDataPruner -f hdfs://ec2-52-36-25-217.us-west-2.compute.amazonaws.com:8020 -g '/apps/metron/enrichment/indexed/bro_doc/*enrichment-*' -s $(date -d '30 days ago' +%m/%d/%Y) -n 1;
         * echo ${start}
         **/

        Options options = new Options();
        Options help = new Options();

        {
            Option o = new Option("h", "help", false, "This screen");
            o.setRequired(false);
            help.addOption(o);
        }
        {
            Option o = new Option("s", "start-date", true, "Starting Date (MM/DD/YYYY)");
            o.setArgName("START_DATE");
            o.setRequired(true);
            options.addOption(o);
        }
        {
            Option o = new Option("f", "filesystem", true, "Filesystem uri - e.g. hdfs://host:8020 or file:///");
            o.setArgName("FILESYSTEM");
            o.setRequired(true);
            options.addOption(o);
        }
        {
            Option o = new Option("n", "numdays", true, "Number of days back to purge");
            o.setArgName("NUMDAYS");
            o.setRequired(true);
            options.addOption(o);
        }
        {
            Option o = new Option("g", "glob-string", true, "Glob filemask for files to delete - e.g. /apps/metron/enrichment/bro_doc/file-*");
            o.setArgName("GLOBSTRING");
            o.setRequired(true);
            options.addOption(o);
        }

        try {

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;

            try {

                cmd = parser.parse(help,argv,true);
                if( cmd.getOptions().length > 0){
                    final HelpFormatter usageFormatter = new HelpFormatter();
                    usageFormatter.printHelp("HDFSDataPruner", null, options, null, true);
                    System.exit(0);
                }

                cmd = parser.parse(options, argv);

            } catch (ParseException pe) {

                final HelpFormatter usageFormatter = new HelpFormatter();
                usageFormatter.printHelp("HDFSDataPruner", null, options, null, true);
                System.exit(-1);

            }

            String start = cmd.getOptionValue("s");
            Date startDate = new SimpleDateFormat("MM/dd/yyyy").parse(start);
            String fileSystemUri = cmd.getOptionValue("f");
            Integer numDays = Integer.parseInt(cmd.getOptionValue("n"));
            String globString = cmd.getOptionValue("g");

            if(LOG.isDebugEnabled()) {
                LOG.debug("Running prune with args: " + startDate + " " + numDays + " " + fileSystemUri + " " + globString);
            }

            HDFSDataPruner pruner = new HDFSDataPruner(startDate, numDays, fileSystemUri, globString);

            LOG.info("Pruned " + pruner.prune() + " files from " + fileSystemUri + globString);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public Long prune() throws IOException {

        Long filesPruned = new Long(0);

        Date today = dateAtMidnight(new Date());

        if (!today.after(startDate)) {
            throw new RuntimeException("Prune Start Date must be prior to today");
        }

        FileStatus[] filesToDelete = fileSystem.globStatus(globPath, new HDFSDataPruner.DateFileFilter(this));

        for (FileStatus fileStatus : filesToDelete) {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Deleting File: " + fileStatus.getPath());
            }

            fileSystem.delete(fileStatus.getPath(), false);

            filesPruned++;
        }

        return filesPruned;
    }

    private Date dateAtMidnight(Date date) {

        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();

    }

    class DateFileFilter extends Configured implements PathFilter {

        HDFSDataPruner pruner;
        Boolean failOnError = false;

        DateFileFilter(HDFSDataPruner pruner) {
            this.pruner = pruner;
        }

        DateFileFilter(HDFSDataPruner pruner, Boolean failOnError) {

            this(pruner);
            this.failOnError = failOnError;

        }

        @Override
        public boolean accept(Path path) {
            try {

                if(pruner.LOG.isDebugEnabled()) {
                    pruner.LOG.debug("ACCEPT - working with file: " + path);
                }

                if (pruner.fileSystem.isDirectory(path)) {
                    return false;

                }
            } catch (IOException e) {

                pruner.LOG.error("IOException", e);

                if (failOnError) {
                    throw new RuntimeException(e);
                }

                return false;
            }

            try {

                FileStatus file = pruner.fileSystem.getFileStatus(path);
                long fileModificationTime = file.getModificationTime();
                boolean accept = false;

                if (fileModificationTime >= pruner.firstTimeMillis && fileModificationTime < pruner.lastTimeMillis) {

                    accept = true;
                }

                return accept;

            } catch (IOException e) {

                pruner.LOG.error("IOException", e);

                if (failOnError) {
                    throw new RuntimeException(e);
                }

                return false;
            }

        }
    }
}

