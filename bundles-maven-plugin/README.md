<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at
      http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Apache Metron Bundle Maven Plugin

Apache Metron Bundles Maven Plugin helps to build Bundles Archives to support the classloader isolation model.

## Table of Contents

- [Requirements](#requirements)
- [Getting Started](#getting-started)
- [Getting Help](#getting-help)
- [License](#license)

## Requirements
* JDK 1.7 or higher
* Apache Maven 3.1.0 or higher

## Getting Started

Building the bundles-maven-plugin module should be rare since it will be released infrequently compared to
the main 'metron' code tree.

- Build with `mvn clean install`
- Presuming you need to make use of changes to the nifi-nar-maven-plugin module, you should next
  go to the [incubator-metron](../incubator-metron) directory and follow its instructions. 


## Settings and configuration

There are several properties that can be set to change the behavior of this plugin.
Two of special interest are:

####type
type defines the type of archive to be produced and evaluated for special dependencies.  The default type is bundle.  This property should be changed if you have need to 
customize the file extension of archives produced by this plugin.  This plugin will build archives with .bundle extentions, and look for othe .bundle dependencies by definition.
Changing this value, for example to 'foo', will have the effect of having the plugin produce archives with .foo as the extension, and look for .foo files
as nar dependencies.
 
####packageIDPrefix 
The archives produced by this plugin have the following entries added to the manifest ( where packageIDPrefix defaults to 'Bundle'):

-  {packageIDPrefix}-Id
-  {packageIDPrefix}-Group
-  {packageIDPrefix}-Version
-  {packageIDPrefix}-Dependency-Group
-  {packageIDPrefix}-Dependency-Id
-  {packageIDPrefix}-Dependency-Version

This property can be used to change the name of these manifest entries.  One convention being to continue as the original by capitalizing the
type, such as:
Bundle to bundle

 

## Getting Help
If you have questions, you can reach out to our mailing list: dev@metron.incubator.apache.org
We're also often available in IRC: #nifi on
[irc.freenode.net](http://webchat.freenode.net/?channels=#apache-metron).


## License

Except as otherwise noted this software is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

