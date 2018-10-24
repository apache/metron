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
# Metron Profiler for the Stellar REPL

This project allows profiles to be executed within the Stellar REPL. This is a port of the Profiler to the Stellar REPL that allows profiles to be tested and debugged within a controlled environment.


* [Introduction](#introduction)
* [Getting Started](#getting-started)
* [Installation](#installation)

## Introduction

Creating and refining profiles is an iterative process.  Iterating against a live stream of data is slow, difficult and error prone.  Running the Profiler in the Stellar REPL provides a controlled and isolated execution environment to create, refine and troubleshoot profiles.

For an introduction to the Profiler, see the [Profiler README](../metron-profiler-common/README.md).

## Getting Started

This section describes how to get started using the Profiler in the Stellar REPL. This outlines a useful workflow for creating, testing, and deploying profiles.

1. Launch the Stellar REPL.
	```
	[root@node1 ~]# $METRON_HOME/bin/stellar
	Stellar, Go!
	[Stellar]>>>
	```

1. The following functions should be accessible.  Documentation is also provided for each function; for example by executing`?PROFILER_FLUSH`.
    ```
    [Stellar]>>> %functions PROFILER
    PROFILER_APPLY, PROFILER_FLUSH, PROFILER_INIT
    ```

1. Create a simple `hello-world` profile that will count the number of messages for each `ip_src_addr`.  The `SHELL_EDIT` function will open an editor in which you can copy/paste the following Profiler configuration.
	```
	[Stellar]>>> conf := SHELL_EDIT()
	[Stellar]>>> conf
	{
	  "profiles": [
	    {
	      "profile": "hello-world",
	      "foreach": "ip_src_addr",
	      "init":    { "count": "0" },
	      "update":  { "count": "count + 1" },
	      "result":  "count"
	    }
	  ]
	}
	```

1. Create the profile execution environment.

	The Profiler will output the number of profiles that have been defined, the number of messages that have been applied and the number of routes that have been followed.

	While the idea of a profile and message may be well understood, a route may need further explanation. A route is defined when a message is applied to a specific profile.
	* If a message is not needed by any profile, then there are no routes.
	* If a message is needed by one profile, then one route has been followed.
	* If a message is needed by two profiles, then two routes have been followed.

	```
	[Stellar]>>> profiler := PROFILER_INIT(conf)
	[Stellar]>>> profiler
	Profiler{1 profile(s), 0 messages(s), 0 route(s)}
	```

1. Create a message that mimics the telemetry that your profile will consume.

	This message can be as simple or complex as you like.  For the `hello-world` profile, all you need is a message containing an `ip_src_addr` field.

	```
	[Stellar]>>> msg := SHELL_EDIT()
	[Stellar]>>> msg
	{
		"ip_src_addr": "10.0.0.1"
	}
	```

1. Apply the message to your Profiler, as many times as you like.

	```
	[Stellar]>>> PROFILER_APPLY(msg, profiler)
	Profiler{1 profile(s), 1 messages(s), 1 route(s)}
	```
	```
	[Stellar]>>> PROFILER_APPLY(msg, profiler)
	Profiler{1 profile(s), 2 messages(s), 2 route(s)}
	```
    ```
    [Stellar]>>> PROFILER_APPLY(msg, profiler)
    Profiler{1 profile(s), 3 messages(s), 3 route(s)}
    ```

1. Flush the Profiler.  

	A flush is what occurs at the end of each 15 minute period in the Profiler.  The result is a list of Profile Measurements. Each measurement is a map containing detailed information about the profile data that has been generated. The `value` field is what is written to HBase when running the Profiler in either Storm or Spark.

	There will always be one measurement for each [profile, entity] pair.  This profile simply counts the number of messages by IP source address. Notice that the value is '3' for the entity '10.0.0.1' as we applied 3 messages with an 'ip_src_addr' of ’10.0.0.1'.

	```
	[Stellar]>>> values := PROFILER_FLUSH(profiler)
	[Stellar]>>> values
	[{period={duration=900000, period=1669628, start=1502665200000, end=1502666100000},
	profile=hello-world, groups=[], value=3, entity=10.0.0.1}]
	```

1. In addition to testing with mock data, you can also apply real, live telemetry to your profile. This can be useful to test your profile against the complexities that exist in real data.  

    This example extracts 10 messages of live, enriched telemetry to test your profile(s).
	```
	[Stellar]>>> msgs := KAFKA_GET("indexing", 10)
	[Stellar]>>> LENGTH(msgs)
	10
	```
	Now apply those 10 messages to your profile.
	```
	[Stellar]>>> PROFILER_APPLY(msgs, profiler)
	  Profiler{1 profile(s), 10 messages(s), 10 route(s)}
	```

1. After you are satisfied with your profile, the next step is to deploy the profile against the live stream of telemetry being capture by Metron. This involves deploying the profile to either the [Storm Profiler](../metron-profiler-storm/README.md) or the [Spark Profiler](../metron-profiler-spark/README.md).


## Installation

This package is installed automatically when installing Metron using the Ambari MPack.

This package can also be installed manually by using either the RPM or DEB package.

### Build the RPM

1. Build Metron.
    ```
    mvn clean package -DskipTests -T2C
    ```

1. Build the RPMs.
    ```
    cd metron-deployment/
    mvn clean package -Pbuild-rpms
    ```

1. Retrieve the package.
    ```
    find ./ -name "metron-profiler-repl*.rpm"
    ```

### Build the DEB

1. Build Metron.
    ```
    mvn clean package -DskipTests -T2C
    ```

1. Build the DEBs.
    ```
    cd metron-deployment/
    mvn clean package -Pbuild-debs
    ```

1. Retrieve the package.
    ```
    find ./ -name "metron-profiler-repl*.deb"
    ```
