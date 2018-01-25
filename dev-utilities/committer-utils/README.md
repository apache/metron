<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Committer Tools

This project contains tools to assist Apache Metron project committers.

## Prepare Commit

This script automates the process of merging a pull request into `apache/master`.  The script will prompt for the pull request number.  Most of the remaining information is automatically extracted from Github or the Apache JIRA.

When prompted the `[value in brackets]` is used by default.  To accept the default, simply press `enter`.  If you would like to change the default, type it in and hit `enter` when done.

In the following example, I enter the pull request number when prompted.   Using the pull request number, the script can extract most of the remaining required information.

1. Execute the script.  

    The first time the script is run, you will be prompted for additional information including your Apache username, Apache email, and Github username.  These values are persisted in `~/.metron-prepare-commit`.  Subsequent executions of the script will retrieve these values, rather than prompting you again for them.

    ```
    $ prepare-commit
      your github username []: nickwallen
      your apache userid []: nickallen
      your apache email [nickallen@apache.org]:
    ```

1. Enter the Github pull request number.

    ```
      pull request: 897
      local working directory [/Users/nallen/tmp/metron-pr897]:
      origin repo [https://github.com/apache/metron]:

    Cloning into '/Users/nallen/tmp/metron-pr897'...
    remote: Counting objects: 36277, done.
    remote: Compressing objects: 100% (108/108), done.
    remote: Total 36277 (delta 38), reused 54 (delta 20), pack-reused 36138
    Receiving objects: 100% (36277/36277), 57.85 MiB | 7.36 MiB/s, done.
    Resolving deltas: 100% (13653/13653), done.
    From https://git-wip-us.apache.org/repos/asf/metron
     * branch              master     -> FETCH_HEAD
     * [new branch]        master     -> upstream/master
    Already on 'master'
    Your branch is up to date with 'origin/master'.
    Already up to date.
    remote: Counting objects: 5, done.
    remote: Total 5 (delta 3), reused 3 (delta 3), pack-reused 2
    Unpacking objects: 100% (5/5), done.
    From https://github.com/apache/metron
     * [new ref]           refs/pull/897/head -> pr-897
    ```

1. Enter contribution details.

    The contributor's username, email, along with information about the associated Apache JIRA is extracted from the commit history.

    ```
      github contributor's username [MohanDV]:
      github contributor's email [mohan.dv@gmail.com]:
      issue identifier in jira [METRON-1395]:
      issue description [Documentation missing for Produce a message to a Kafka topic Rest API endpoint]:
      commit message [METRON-1395 Documentation missing for Produce a message to a Kafka topic Rest API endpoint (MohanDV via nickwallen) closes apache/metron#897]:
    ```

1. The contribution is then merged with master as a single commit.  The changes that have been made along with the commit message are displayed.

    ```
    Squash commit -- not updating HEAD
    Automatic merge went well; stopped before committing as requested
    [master 998f7915] METRON-1410 Some more upgrade fallout... Can&apos;t restart Metron Indexing. (ottobackwards via nickwallen) closes apache/metron#901
     Author: ottobackwards <ottobackwards@gmail.com>
     3 files changed, 3 insertions(+), 3 deletions(-)


     .../metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/enrichment_commands.py       | 2 +-
     .../ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/indexing_commands.py  | 2 +-
     .../ambari/metron-mpack/src/main/resources/common-services/METRON/CURRENT/package/scripts/profiler_commands.py  | 2 +-
     3 files changed, 3 insertions(+), 3 deletions(-)

    998f7915 (HEAD -> master) METRON-1410 Cannot restart Metron Indexing. (ottobackwards via nickwallen) closes apache/metron#901
    ```

1. Run the test suite.

    After the merge is complete, the script will prompt you to run the test suite.  By default this is skipped, but by typing 'y' the test suite will be run.

    ```
      run test suite? [yN]
    ```

1. Finalize the changes.

    To this point changes have only been made to your local repository.  The script itself will not push changes to Apache.  You are given instructions on how to do so.  Review the summary and enter `y` at the prompt, if you are satisfied.   If you are not happy, simply start over.

    ```
    Review commit carefully then run...
        cd /Users/nallen/tmp/metron-pr897
        git push upstream master
    ```
