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
# Parser Contribution and Testing

So you want to contribute a parser to Apache Metron.  First off, on behalf
of the community, thank you very much!  Now that you have implemented a parser
by writing a java class which implements `org.apache.metron.parsers.interfaces.MessageParser`
what are the testing expectations for a new parser?

It is expected that a new parser have two tests:
* A JUnit test directly testing your parser class.
* An Integration test validating that your parser class can parse messages 
inside the `ParserBolt`.

## The JUnit Test

The JUnit Test should be focused on testing your Parser directly.  You
should feel free to use mocks or stubs or whatever else you need to completely
test that unit of functionality.

## The Integration Test

Integration tests are more structured.  The intent is that the parser that you have implemented can be driven successfully from the appropriate driver, e.g. `org.apache.metron.parsers.bolt.ParserBolt` for Storm.

To add a new test, just add it to the list of sensorTypes in `org.apache.metron.parsers.integration.ParserIntegrationTest`.

To setup the tests for a new platform, extend `ParserIntegrationTest`, e.g. as in `org.apache.metron.parsers.integration.StormParserIntegrationTests`. This should be a parameterized test, so that each sensorType gets its own test.  Use `StormParserIntegrationTests` as a template for the new platform's class.  The test method should just setup the appropriate `ParserDriver` implementation, and simply call back into the parent to run the test.

Customized versions of the tests can be added by extending `ParserIntegrationTest` and performing additional setup or validations as needed.

The way these tests function is by running the `ParserDriver` instance with your specified global configuration and
sensor configuration.  It will then send your specified sample input data in line-by-line.  It will then
perform some basic sanity validation:
* Ensure no errors were logged
* Execute your specified validation methods

### Validations

Validations are functions which indicate how one should validate the parsed messages.  The basic one which is sufficient
for most cases is `org.apache.metron.parsers.integration.validation.SampleDataValidation`.  This will read the expected results
from `metron-integration-test/src/main/sample/data/${sensor_type}/parsed` and validate that the actual parsed message
conforms (excluding timestamp).

If you have special validations required, you may implement your own and return an instance of that in the `getValidations()`
method of your Integration Test.
