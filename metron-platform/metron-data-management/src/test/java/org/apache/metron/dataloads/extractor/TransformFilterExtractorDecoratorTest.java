package org.apache.metron.dataloads.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.curator.framework.CuratorFramework;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;
import org.apache.metron.enrichment.lookup.LookupKV;
import org.apache.metron.enrichment.lookup.LookupKey;
import org.apache.metron.enrichment.lookup.LookupValue;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.*;

public class TransformFilterExtractorDecoratorTest {

  LinkedHashMap<String,Object> config1;
  @Mock
  CuratorFramework zkClient;
  @Mock
  Extractor extractor;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    config1 = new ObjectMapper().readValue(config1Contents, LinkedHashMap.class);
//    HashMap<String,Object> config2 = new ObjectMapper().readValue(config1Contents, HashMap.class);
  }

  /**
   *  {
   *    "zk_quorum" : "blah",
   *    "columns" : {
   *      "foo" : 0,
   *      "bar" : 1,
   *      "baz" : 2
   *    },
   *    "value_transform" : {
   *      "foo" : "TO_UPPER(foo)",
   *      "newvar" : "foo",
   *      "lowernewvar" : "TO_LOWER(newvar)"
   *    },
   *    "value_filter" : "LENGTH(baz) > 0",
   *    "indicator_column" : "bar",
   *    "indicator_transform" : {
   *      "somevar" : "indicator",
   *      "indicator" : "TO_UPPER(somevar)"
   *    },
   *    "indicator_filter" : "LENGTH(indicator) > 0",
   *    "type" : "testenrichment",
   *    "separator" : ","
   *  }
   *}
   */
  @Multiline
  public static String config1Contents;

  @Test
  public void simple_transform_value() throws IOException {
    LookupKey lookupKey = new EnrichmentKey("testenrichment", "val2");
    LookupValue lookupValue = new EnrichmentValue(new HashMap() {{
      put("foo", "val1");
      put("bar", "val2");
      put("baz", "val3");
    }});
    LookupKV lkv = new LookupKV(lookupKey, lookupValue);
    List<LookupKV> lkvs = new ArrayList<>();
    lkvs.add(lkv);
    Mockito.when(extractor.extract("val1,val2,val3")).thenReturn(lkvs);
    TransformFilterExtractorDecorator decorator = new TransformFilterExtractorDecorator(extractor);
    decorator.setZkClient(Optional.of(zkClient));
    decorator.initialize(config1);
    Iterable<LookupKV> extracted = decorator.extract("val1,val2,val3");
    Assert.assertThat(extracted, CoreMatchers.equalTo(lkvs));
  }

  // TODO

  // simple filter value

  // simple transform indicator

  // simple filter indicator

}
