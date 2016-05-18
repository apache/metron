package org.apache.metron.parsers.integration;

import org.apache.metron.TestConstants;

public class FireEyeIntegrationTest extends ParserIntegrationTest {
	
	  @Override
	  public String getFluxPath() {
	    return "./src/main/flux/fireeye/test.yaml";
	  }

	  @Override
	  public String getSampleInputPath() {
	    return TestConstants.SAMPLE_DATA_INPUT_PATH + "FireEyeOutputSmall";
	  }

	  @Override
	  public String getSampleParsedPath() {
	    return TestConstants.SAMPLE_DATA_PARSED_PATH + "FireEyeParsed";
	  }

	  @Override
	  public String getSensorType() {
	    return "fireeye";
	  }

	  @Override
	  public String getFluxTopicProperty() {
	    return "spout.kafka.topic.fireeye";
	  }

}
