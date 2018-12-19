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

This script automates the process of merging a pull request into `apache/metron` or `apache/metron-bro-plugin-kafka`.  The script will prompt for the pull request number.  Most of the remaining information is automatically extracted from Github or the Apache JIRA.

When prompted the `[value in brackets]` is used by default.  To accept the default, simply press `enter`.  If you would like to change the default, type it in and hit `enter` when done.

In the following example, I enter the pull request number when prompted.   Using the pull request number, the script can extract most of the remaining required information.

1. Execute the script.  

    The first time the script is run, you will be prompted for additional information including your Apache username, Apache email, and Github username.  These values are persisted in `~/.metron-prepare-commit`.  Subsequent executions of the script will retrieve these values, rather than prompting you again for them.

    ```
    $ ./prepare-commit
      your github username []: jonzeolla
      your apache userid []: jonzeolla
      your apache email [jonzeolla@apache.org]:
    ```

1. Select a repository and enter a GitHub pull request number.

    ```
        [1] metron
        [2] metron-bro-plugin-kafka
      which repo? [1]: 1
      pull request: 946
      local working directory [/Users/jzeolla/tmp/metron-pr946]:
      origin repo [https://github.com/apache/metron]:
    Cloning into '/Users/jzeolla/tmp/metron-pr946'...
    remote: Counting objects: 37861, done.
    remote: Compressing objects: 100% (71/71), done.
    remote: Total 37861 (delta 27), reused 47 (delta 4), pack-reused 37757
    Receiving objects: 100% (37861/37861), 58.18 MiB | 4.38 MiB/s, done.
    Resolving deltas: 100% (14439/14439), done.
    From https://gitbox.apache.org/repos/asf/metron.git
     * branch              master     -> FETCH_HEAD
     * [new branch]        master     -> upstream/master
    Already on 'master'
    Your branch is up to date with 'origin/master'.
    Already up to date.
    remote: Counting objects: 82, done.
    remote: Compressing objects: 100% (22/22), done.
    remote: Total 82 (delta 28), reused 48 (delta 28), pack-reused 26
    Unpacking objects: 100% (82/82), done.
    From https://github.com/apache/metron
     * [new ref]           refs/pull/946/head -> pr-946
    ```

1. Enter contribution details.

    The contributor's username, email, along with information about the associated Apache JIRA is extracted from the commit history.

    ```
    github contributor's username [wardbekker]:
    github contributor's email [ward@wardbekker.com]:
    issue identifier in jira [METRON-1465]:
    issue description [X-pack support for Elasticsearch]:
    commit message [METRON-1465 X-pack support for Elasticsearch (wardbekker via jonzeolla) closes apache/metron#946]:
    ```

1. The contribution is then merged with master as a single commit.  The changes that have been made along with the commit message are displayed.

    ```
    Updating b48ab93c..4fbc166d
    Fast-forward
    Squash commit -- not updating HEAD
    <snip>
    [master f0190d57] METRON-1465 X-pack support for Elasticsearch (wardbekker via jonzeolla) closes apache/metron#946
     Author: wardbekker <ward@wardbekker.com>
    <snip>
     11 files changed, 48 insertions(+), 10 deletions(-)
    
    
    f0190d57 (HEAD -> master) METRON-1465 X-pack support for Elasticsearch (wardbekker via jonzeolla) closes apache/metron#946
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
        cd /Users/jzeolla/tmp/metron-pr946
        git push upstream master
    ```
