package org.apache.metron.common.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import java.util.function.Function;

public enum SerializationUtils implements Function<Object, byte[]> {
  INSTANCE;
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
    Output output = new Output();
    kryo.get().writeObject(output, t);
    return output.toBytes();
  }
}
