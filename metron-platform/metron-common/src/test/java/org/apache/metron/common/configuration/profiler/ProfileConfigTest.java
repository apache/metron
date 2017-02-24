package org.apache.metron.common.configuration.profiler;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Ensures that Profile definitions have the expected defaults
 * and can be (de)serialized to and from JSON.
 */
public class ProfileConfigTest {

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String onlyIfDefault;

  /**
   * The 'onlyif' field should default to 'true' when it is not specified.
   */
  @Test
  public void testOnlyIfDefault() throws IOException {
    ProfileConfig profile = JSONUtils.INSTANCE.load(onlyIfDefault, ProfileConfig.class);
    assertEquals("true", profile.getOnlyif());
  }

  /**
   * {
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String nameMissing;

  /**
   * The 'name' of the profile must be defined.
   */
  @Test(expected = JsonMappingException.class)
  public void testNameMissing() throws IOException {
    JSONUtils.INSTANCE.load(nameMissing, ProfileConfig.class);
  }

  /**
   * {
   *    "profile": "test",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String foreachMissing;

  /**
   * The 'foreach' field must be defined.
   */
  @Test(expected = JsonMappingException.class)
  public void testForeachMissing() throws IOException {
    JSONUtils.INSTANCE.load(foreachMissing, ProfileConfig.class);
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {}
   * }
   */
  @Multiline
  private String resultMissing;

  /**
   * The 'result' field must be defined.
   */
  @Test(expected = JsonMappingException.class)
  public void testResultMissing() throws IOException {
    JSONUtils.INSTANCE.load(resultMissing, ProfileConfig.class);
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": {}
   * }
   */
  @Multiline
  private String resultMissingProfileExpression;

  /**
   * The 'result' field must contain the 'profile' expression used to store the profile measurement.
   */
  @Test(expected = JsonMappingException.class)
  public void testResultMissingProfileExpression() throws IOException {
    JSONUtils.INSTANCE.load(resultMissingProfileExpression, ProfileConfig.class);
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": "2 + 2"
   * }
   */
  @Multiline
  private String resultWithExpression;

  /**
   * If the 'result' field has only a single expression, it should be treated as
   * the 'profile' expression used to store the profile measurement.
   */
  @Test
  public void testResultWithExpression() throws IOException {
    ProfileConfig profile = JSONUtils.INSTANCE.load(resultWithExpression, ProfileConfig.class);
    assertEquals("2 + 2", profile.getResult().getProfileExpressions().getExpression());
    assertNull(profile.getResult().getTriageExpressions());
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": {
   *      "profile": "2 + 2"
   *    }
   * }
   */
  @Multiline
  private String resultWithProfileOnly;

  /**
   * The result's 'triage' field is optional.
   */
  @Test
  public void testResultWithProfileOnly() throws IOException {
    ProfileConfig profile = JSONUtils.INSTANCE.load(resultWithProfileOnly, ProfileConfig.class);
    assertEquals("2 + 2", profile.getResult().getProfileExpressions().getExpression());
  }

  /**
   * {
   *    "profile": "test",
   *    "foreach": "ip_src_addr",
   *    "update": {},
   *    "result": {
   *      "profile": "2 + 2",
   *      "triage": {
   *        "eight": "4 + 4",
   *        "sixteen": "8 + 8"
   *      }
   *    }
   * }
   */
  @Multiline
  private String resultWithTriage;

  /**
   * The result's 'triage' field can contain many named expressions.
   */
  @Test
  public void testResultWithTriage() throws IOException {
    ProfileConfig profile = JSONUtils.INSTANCE.load(resultWithTriage, ProfileConfig.class);
    assertEquals("4 + 4", profile.getResult().getTriageExpressions().getExpression("eight"));
    assertEquals("8 + 8", profile.getResult().getTriageExpressions().getExpression("sixteen"));
  }
}
