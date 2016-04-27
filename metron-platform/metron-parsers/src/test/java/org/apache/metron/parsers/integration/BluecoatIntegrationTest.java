package org.apache.metron.parsers.integration;

import org.apache.metron.TestConstants;

public class BluecoatIntegrationTest extends ParserIntegrationTest {

  @Override
  public String getFluxPath() {
    return "./src/main/flux/bluecoat/test.yaml";
  }

  @Override
  public String getSampleInputPath() {
    return TestConstants.SAMPLE_DATA_INPUT_PATH + "BluecoatSyslog.txt";
  }

  @Override
  public String getSampleParsedPath() {
    return TestConstants.SAMPLE_DATA_PARSED_PATH + "BluecoatParsed";
  }

  @Override
  public String getSensorType() {
    return "bluecoat";
  }

  @Override
  public String getFluxTopicProperty() {
    return "spout.kafka.topic.yaf";
  }
}
