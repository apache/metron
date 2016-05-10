package org.apache.metron.parsers.integration;

import org.apache.metron.TestConstants;

public class WebSphereIntegrationTest extends ParserIntegrationTest {
	
	  @Override
	  public String getFluxPath() {
	    return "./src/main/flux/websphere/test.yaml";
	  }

	  @Override
	  public String getSampleInputPath() {
	    return TestConstants.SAMPLE_DATA_INPUT_PATH + "WebsphereOutput.txt";
	  }

	  @Override
	  public String getSampleParsedPath() {
	    return TestConstants.SAMPLE_DATA_PARSED_PATH + "WebsphereParsed";
	  }

	  @Override
	  public String getSensorType() {
	    return "websphere";
	  }

	  @Override
	  public String getFluxTopicProperty() {
	    return "spout.kafka.topic.websphere";
	  }

}
