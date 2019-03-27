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

Stellar Interpreter for Apache Zeppelin
=======================================

[Apache Zeppelin](https://zeppelin.apache.org/) is a web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.  This project provides a means to run the Stellar REPL directly within a Zeppelin Notebook.

* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [Usage](#usage)


Prerequisites
-------------

* [Apache Zeppelin](https://zeppelin.apache.org/) 0.7.3

   This is tested with version 0.7.3.  Other versions may work, but are not supported.


Installation
------------

Currently, you need to manually install the Stellar Interpreter in Zeppelin. In the future this step could be automated by the Metron Mpack.

To install the Stellar Interpreter in your Apache Zeppelin installation, follow these instructions.  This is paraphrased from the [Zeppelin docs](https://zeppelin.apache.org/docs/latest/development/writingzeppelininterpreter.html#install-your-interpreter-binary).

1. Build and install Metron. Metron and its dependencies will be retrieved from your local Maven repository.

    ```
    cd $METRON_HOME
    mvn clean install -DskipTests
    ```

1. If you do not already have Zeppelin installed, [download and unpack Apache Zeppelin](https://zeppelin.apache.org/download.html).  Then change directories to the root of your Zeppelin download.

    ```
    cd $ZEPPELIN_HOME
    ```

1. Use Zeppelin's installation utility to install the Stellar Interpreter.

    If Zeppelin was already installed, make sure that it is stopped before running this command.  Update the version, '0.7.1' in the example below, to whatever is appropriate for your environment.

    ```
    bin/install-interpreter.sh --name stellar --artifact org.apache.metron:stellar-zeppelin:0.7.1
    ```

    **Note:** The above command will download maven artifact groupId1:artifact1:version1 (org.apache.metron:stellar-zeppelin:0.7.1) and all of its transitive dependencies into the $ZEPPELIN_HOME/interpreter/stellar directory. `stellar-common`, which contains many of the [Stellar Core Functions](../stellar-common#stellar-core-functions), will be included transitively because `stellar-zeppelin` declares it as a direct dependency in its Maven pom.xml.

    * [3rd Party Zeppelin Interpreter Installation Documentation](https://zeppelin.apache.org/docs/0.7.3/manual/interpreterinstallation.html#3rd-party-interpreters)

1. Start Zeppelin.  

    ```
    bin/zeppelin-daemon.sh start
    ```

1. Navigate to Zeppelin running at [http://localhost:8080/](http://localhost:8080/).  The Stellar Interpreter should be ready for use with a basic set of functions.

Usage
-----

1. Create a new notebook.  

    1. Click on "Notebook" > "Create new note".

    1. Set the default Interpreter to `stellar`.

        When creating the notebook, if you define `stellar` as the default interpreter, then there is no need to enter `%stellar` at the top of each code block.

        If `stellar` is not the default interpreter, then you must enter `%stellar` at the top of a code block containing Stellar code.

1. In the first block, add the following Stellar, then click Run.

    ```
    2 in [2,3,4]
    ```

1. In the next block, check which functions are available to you.

    When executing Stellar's magic functions, you must explicitly define which interpreter should be used in the code block.  If you define 'stellar' as the default interpreter when creating a notebook, then this is only required when using Stellar's magic functions.

    ```
    %stellar

    %functions
    ```

    You will **only** 'see' the functions defined within `stellar-common` since that is the only library that we added to the interpreter.  

1. Add additional Stellar functions to your session.

    1. Go back to the Stellar interpreter configuration and add another dependency as follows.

        ```
        org.apache.metron:metron-statistics:0.7.1
        ```

    1. Go back to your notebook and run `%functions` again.  You will now see the additional functions defined within the `metron-statistics` project.

1. Auto-completion is also available for Stellar expressions.  

    In another block, type 'TO_' then press the <kbd>CTRL</kbd> + <kbd>PERIOD</kbd> keys. This will trigger the auto-complete mechanism in Stellar and display a list of matching functions or variables.
