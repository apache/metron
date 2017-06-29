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

Integration tests are more structured.  The intent is that the parser that
you have implemented can be driven successfully from `org.apache.metron.parsers.bolt.ParserBolt`.

The procedure for creating a new test is as follows:
* Create an integration test that extends `org.apache.metron.parsers.integration.ParserIntegrationTest`
  * Override `getSensorType()` to return the sensor type to be used in the test (referred to as `${sensor_type}` at times)
  * Override `getValidations()` to indicate how you want the output of the parser to be validated (more on validations later)
  * Optionally `readSensorConfig(String sensorType)` to read the sensor config
    * By default, we will pull this from `metron-parsers/src/main/config/zookeeper/parsers/${sensor_type}`.  Override if you want to provide your own
  * Optionally `readGlobalConfig()` to return the global config
    * By default, we will pull this from `metron-integration-test/src/main/config/zookeeper/global.json)`.  Override if you want to provide your own
* Place sample input data in `metron-integration-test/src/main/sample/data/${sensor_type}/raw`
  * It should be one line per input record.
* Place expected output based on sample data in `metron-integration-test/src/main/sample/data/${sensor_type}/parsed`
  * Line `k` in the expected data should match with line `k`

The way these tests function is by creating a `ParserBolt` instance with your specified global configuration and
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

### Sample Integration Test

A sample integration test for the `snort` parser is as follows:
```
public class SnortIntegrationTest extends ParserIntegrationTest {
  @Override
  String getSensorType() {
    return "snort";
  }

  @Override
  List<ParserValidation> getValidations() {
    return new ArrayList<ParserValidation>() {{
      add(new SampleDataValidation());
    }};
  }
}
```
