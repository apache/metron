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

* [Installation](#installation)
* [Usage](#usage)


Installation
------------

Currently, you need to manually install the Stellar Interpreter in Zeppelin. In the future this step could be automated by the Metron Mpack.

To install the Stellar Interpreter in your Apache Zeppelin installation, follow these instructions.  This is paraphrased from the [Zeppelin docs](https://zeppelin.apache.org/docs/latest/development/writingzeppelininterpreter.html#install-your-interpreter-binary).

1. Build and install Metron. Metron and its dependencies will be retrieved from your local Maven repository.

    ```
    cd $METRON_HOME
    mvn clean install -DskipTests
    ```

1. If you do not already have Zeppelin installed, [download and unpack Apache Zeppelin](https://zeppelin.apache.org/download.html).  The directory in which you unpack Zeppelin will be referred to as `$ZEPPELIN_HOME`.

1. If Zeppelin was already installed, make sure that it is not running.

1. Create a settings directory for the Stellar interpreter.

    ```
    mkdir $ZEPPELIN_HOME/interpreter/stellar
    cat <<EOF > $ZEPPELIN_HOME/interpreter/stellar/interpreter-setting.json
    [
      {
        "group": "stellar",
        "name": "stellar",
        "className": "org.apache.metron.stellar.zeppelin.StellarInterpreter",
        "properties": {
        }
      }
    ]
    EOF
    ```

1. Create a Zeppelin Site file.

    ```
    cp $ZEPPELIN_HOME/conf/zeppelin-site.xml.template $ZEPPELIN_HOME/conf/zeppelin-site.xml
    ```

1. Add the following to the bottom of your Zeppelin Site file (yet still above the closing `</configuration>` tag) located at `$ZEPPELIN_HOME/conf/zeppelin-site.xml`.

    ```
    <property>
        <name>zeppelin.interpreters</name>
        <value>org.apache.metron.stellar.zeppelin.StellarInterpreter</value>
    </property>
    ```

1. Start Zeppelin.  

    ```
    $ZEPPELIN_HOME/bin/zeppelin-daemon.sh start
    ```

1. Navigate to Zeppelin running at [http://localhost:8080/](http://localhost:8080/).

1. Register the Stellar interpreter in Zeppelin.

    1. Click on the top-right menu item labelled "Anonymous" then choose "Interpreter" in the drop-down that opens.    

1. Configure the Stellar interpreter.

    1. Click on '**+ Create**' near the top-right.

    1. Define the following values.
        * **Interpreter Name** = `stellar`
        * **Interpreter Group** = `stellar`

    1. Under **Options**, set the following values.
        * The interpreter will be instantiated **Per Note**  in **isolated** process.

    1. Under **Dependencies**, define the following fields, then click the "+" icon.  Replace the Metron version as required.
        * **Artifact** = `org.apache.metron:stellar-zeppelin:0.4.3`

    1. Click "Save"

1. Wait for the intrepreter to start.

    1. Near the title '**stellar**', will be a status icon.  This will indicate that it is downloading the dependencies.  

    1. Once the icon is shown as green, the interpreter is ready to work.

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

    ```
    %functions
    ```
        
    You will **only** 'see' the functions defined within `stellar-common` since that is the only library that we added to the interpreter.  
    
1. To see how additional functions can be added, go back to the Stellar interpreter configuration and add another dependency as follows.
    
    ```
    org.apache.metron:metron-statistics:0.4.3
    ```
    
    Reload the Stellar interpreter and run `%functions` again.  You will see the additional functions defined within the `metron-statistics` project.
    
1. Auto-completion is also available for Stellar expressions.  

    In another block, type 'TO_' then press the <kbd>CTRL</kbd> + <kbd>PERIOD</kbd> keys. This will trigger the auto-complete mechanism in Stellar and display a list of matching functions or variables.
