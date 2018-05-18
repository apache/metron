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

# bundles-testing

Bundles testing is a project for testing the basic functionality of the bundle-lib and bundles.
The idea is that we have an integration test module (bundles-test-integration), that deplends on the interface module
(bundles-test-integration) but not the implementation.

The interface is attributed with `@IndexSubclasses`, from the ClassIndex system.  Any implementing classes
will be exposed through the bundle system.

The implementation (bundles-test-implementation) is packaged as a bundle (bundles-test-bundle).

The integration test loads the implementation through the bundle system, by interface, and executes its methods.
The demonstrates the main use case.

> NOTE: the code for the implementation and bundle are provided, but

> the bundle produced is in the test/resources directory of the integration project

> If you want to modify you need to build with your changes, and replace the bundle file in the test/resources location.