package org.apache.metron.common.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.function.Function;

public enum SerializationUtils implements Function<Object, byte[]> {
  INSTANCE;
  protected static final Logger LOG = LoggerFactory.getLogger(SerializationUtils.class);
  ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>() {

    @Override
    protected Kryo initialValue() {
      return new Kryo();
    }
  };

  /**
   * Applies this function to the given argument.
   *
   * @param t the function argument
   * @return the function result
   */
  @Override
  public byte[] apply(Object t) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Output output = new Output(bos);
      kryo.get().writeObject(output, t);
      output.flush();
      byte[] ret = bos.toByteArray();
      return ret;
    }
    catch(Throwable ex) {
      LOG.error("Unable to serialize " + t + ": " + ex.getMessage(), ex);
      throw ex;
    }
  }
}
