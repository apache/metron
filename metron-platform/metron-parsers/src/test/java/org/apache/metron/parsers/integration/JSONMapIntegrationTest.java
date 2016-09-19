package org.apache.metron.parsers.integration;

import org.apache.metron.parsers.integration.validation.SampleDataValidation;

import java.util.ArrayList;
import java.util.List;

public class JSONMapIntegrationTest extends ParserIntegrationTest {
  @Override
  String getSensorType() {
    return "jsonMap";
  }

  @Override
  List<ParserValidation> getValidations() {
    return new ArrayList<ParserValidation>() {{
      add(new SampleDataValidation());
    }};
  }
}
