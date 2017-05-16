package org.apache.metron.spout.pcap;

import org.apache.metron.spout.pcap.deserializer.KeyValueDeserializer;
import org.apache.storm.kafka.EmitContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

public class HDFSWriterCallbackTest {

  @Mock
  private EmitContext context;
  @Mock
  private HDFSWriterConfig config;
  @Mock
  private KeyValueDeserializer deserializer;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(config.getDeserializer()).thenReturn(deserializer);
  }

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void empty_or_null_key_throws_illegal_argument_exception() {
    HDFSWriterCallback callback = new HDFSWriterCallback().withConfig(config);
    List<Object> tuples = new ArrayList<>();
    tuples.add(null);
    tuples.add(null);

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Expected a key but none provided");

    callback.apply(tuples, context);
  }
}
