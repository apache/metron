package org.apache.metron.dataloads.extractor;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

public class ExtractorDecoratorTest {

  @Mock
  Extractor extractor;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void sets_member_variables() {
    ExtractorDecorator decorator = new ExtractorDecorator(extractor);
    Assert.assertThat(decorator.decoratedExtractor, Matchers.notNullValue());
  }

  @Test
  public void calls_extractor_methods() throws IOException {
    ExtractorDecorator decorator = new ExtractorDecorator(extractor);
    decorator.initialize(new HashMap());
    decorator.extract("line");
    verify(extractor).initialize(isA(Map.class));
    verify(extractor).extract("line");
  }

}
