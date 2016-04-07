package org.apache.metron.domain;

import junit.framework.Assert;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

import java.io.IOException;

public class ConfigurationsTest {

  @Test
  public void test() throws IOException {
    EqualsVerifier.forClass(Configurations.class).suppress(Warning.NONFINAL_FIELDS, Warning.NULL_FIELDS).usingGetClass().verify();
    Configurations configurations = new Configurations();
    try {
      configurations.updateConfig("someConfig", (byte[]) null);
      Assert.fail("Updating a config with null should throw an IllegalStateException");
    } catch(IllegalStateException e) {}
    Assert.assertTrue(configurations.toString() != null && configurations.toString().length() > 0);
  }
}
