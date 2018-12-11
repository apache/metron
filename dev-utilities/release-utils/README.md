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

# Release Tools

This project contains tools to assist Apache Metron project committers.

## Prepare Release Candidate

This script automates the process of creating a release candidate from `apache/metron` or `apache/metron-bro-plugin-kafka`. The script will prompt for various information necessary.  Ensure your signing key is setup per [Release Signing](https://www.apache.org/dev/release-signing.html) and [Apache GnuPGP Instructions](https://www.apache.org/dev/openpgp.html#gnupg)

When prompted the `[value in brackets]` is used by default.  To accept the default, simply press `enter`.  If you would like to change the default, type it in and hit `enter` when done.

In the following example, enter the appropriate information

1. Execute the script.  

    The first time the script is run, you will be prompted for additional information including your Apache username and Apache email.  These values are persisted in `~/.metron-prepare-release-candidate`.  Subsequent executions of the script will retrieve these values, rather than prompting you again for them.

    ```
    $ ./prepare-release-candidate
      your apache userid []: leet
      your apache email [leet@apache.org]:
    ```

1. Select a repository we're creating an RC for.

    ```
        [1] metron
        [2] metron-bro-plugin-kafka
      which repo? [1]: 1
    ```

1. Enter the current version number.  This will be the base for the CHANGES file

    ```
      current version: 0.6.0
    ```

1. Enter the version being built.

    ```
      version being built: 0.6.1
    ```

1. Enter the current RC number

    ```
      release candidate number: 1
    ```

1. Enter the branch we're releasing from. In most cases, this will be master, but for maintenance releases it can be another branch.

    ```
      base revision branch or hash for release candidate [master]:
    ```
    
1. Enter the signing key id.

    ```
      signing key id in 8-byte format (e.g. BADDCAFEDEADBEEF):
    ```
    
1. Enter if this is a practice run. In a practice run, nothing is pushed to SVN, but everything is setup and built otherwise.

    ```
      do a live run (push to remote repositories?) [y/n]
    ```

1. Wait for all repos to be checked out to complete.  There will be some additional work done, e.g. along with branch and tag creation. In a live run, you may be prompted for Git credentials to push a branch.

    ```
      Checking out repo: https://dist.apache.org/repos/dist/dev/metron
      Checking out repo: dev
      Checking out repo:  https://dist.apache.org/repos/dist/release/metron
      Checking out repo: release
      Checking out git repo: https://gitbox.apache.org/repos/asf/metron.git
      Cloning into '/Users/justinleet/tmp/metron-0.6.1/metron'...
      remote: Counting objects: 46146, done.
      remote: Compressing objects: 100% (15568/15568), done.
      remote: Total 46146 (delta 21513), reused 43696 (delta 19489)
      Receiving objects: 100% (46146/46146), 56.00 MiB | 1.04 MiB/s, done.
      Resolving deltas: 100% (21513/21513), done.
      Creating branch: Metron_0.6.1
      Using git rev: master
      Already on 'master'
      Your branch is up to date with 'origin/master'.
      Switched to a new branch 'Metron_0.6.1'
      This is a practice run. Not running <git push --set-upstream origin Metron_0.6.1>
      Creating tentative git tag <0.6.1-rc1>. Do not push this tag until RC is ready for community review.
      Already on 'Metron_0.6.1'
      Creating the RC tarball for tag apache-metron-0.6.1-rc1
      Creating the SHA hash files
    ```

1. Provide the passphrase to `gpg` to sign the artifacts.

   ```
     Signing the release tarball
     Copying release artifacts
   ```

1. Shortly afterwards the RC will be finalized. In a practice run, this will not be pushed back to SVN.
   ```
     Creating CHANGES file
     Extracting LICENSE, NOTICE, and KEYS from tarball
     x LICENSE
     x NOTICE
     This is a practice run. Not running the following commands:
     <svn add 0.6.1-RC1>
     <svn commit -m "Adding artifacts for metron 0.6.1-RC1">
   ```

At this point, all RC artifacts have been created.  In a live run, these will have been pushed to the appropriate repositories and are ready for community review.
