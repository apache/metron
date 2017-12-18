# Stellar Interpreter for Apache Zeppelin

[Apache Zeppelin](https://zeppelin.apache.org/) is a web-based notebook that enables data-driven, interactive data analytics and collaborative documents with SQL, Scala and more.  This project provides a means to run the Stellar REPL directly within a Zeppelin Notebook.

## Installation

Currently, you need to manually install the Stellar Interpreter in Zeppelin. In the future this step could be automated by the Metron Mpack.

To install the Stellar Interpreter in your Apache Zeppelin installation, follow these instructions.  This is excerpted directly from the [Zeppelin docs](https://zeppelin.apache.org/docs/latest/development/writingzeppelininterpreter.html#install-your-interpreter-binary).

1. If you do not already have Zeppelin installed, [download and unpack Apache Zeppelin](https://zeppelin.apache.org/download.html).  The directory in which you unpack Zeppelin will be referred to as `$ZEPPELIN_HOME`.

1. If Zeppelin was already installed, make sure that it is not running.

1. Create a directory for the Stellar interpreter.

    ```
    mkdir $ZEPPELIN_HOME/interpreter/stellar
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
    
1. Copy all necessary Stellar dependencies along with the jar of this project to `$ZEPPELIN_HOME/interpreter/stellar`.  This can be done by executing the following starting from Metron's top-level directory.  

    Of course, this process is a bit of a kludge and is only intended to get you up and running in Zeppelin as quickly as possible.

    ```
    mvn clean install -DskipTests
    cd metron-stellar/stellar-zeppelin/
    mvn install dependency:copy-dependencies
    cp -r target/dependency/ $ZEPPELIN_HOME/interpreter/stellar/
    cp target/stellar-zeppelin-0.4.2.jar $ZEPPELIN_HOME/interpreter/stellar
    ```
    
1. Start Zeppelin.  

    ```
    $ZEPPELIN_HOME/bin/zeppelin-daemon.sh start
    ```
   
1. Navigate to Zeppelin running at [http://localhost:8080/](http://localhost:8080/).
    
1. Click on the top-right menu item labelled "Anonymous" then choose "Interpreter" in the drop-down that opens.    

1. Configure the Stellar interpreter.

    1. On the "Interpreters" page, click on "+ Create" near the top-right.
    1. Set "Interpreter Name" to "stellar".
    1. Set "Interpreter Group" to "Metron".
    1. Click "Save"

1. Create a new notebook.  

    1. Click on "Notebook" > "Create new note".
    1. Set the default Interpreter to Stellar.

1. Execute Stellar in your notebook.

    1. In the first block, add the following Stellar, then click Run.
    
        ```
        %stellar
        
        2 in [2,3,4]
        ```
    1. The execution should result in a value of true.
    
1. Try out auto-completion.

    1. In another block type 'TO_' then press CTRL+. which will trigger the auto-complete mechanism in Stellar.
    
    