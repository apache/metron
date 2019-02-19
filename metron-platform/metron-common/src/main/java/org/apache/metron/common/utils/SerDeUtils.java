/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.metron.common.utils;

import static com.esotericsoftware.kryo.util.Util.className;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Util;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyMapSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptySetSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonListSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonMapSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonSetSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.cglib.CGLibProxySerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateTimeSerializer;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.function.Function;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides basic functionality to serialize and deserialize the allowed
 * value types for a ProfileMeasurement.
 */
public class SerDeUtils {
  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>() {
    @Override
    protected Kryo initialValue() {
      Kryo ret = new Kryo();
      ret.setReferences(true);
      ret.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

      ret.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
      ret.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
      ret.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
      ret.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
      ret.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
      ret.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
      ret.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
      ret.register(GregorianCalendar.class, new GregorianCalendarSerializer());
      ret.register(InvocationHandler.class, new JdkProxySerializer());
      UnmodifiableCollectionsSerializer.registerSerializers(ret);
      SynchronizedCollectionsSerializer.registerSerializers(ret);

// custom serializers for non-jdk libs

// register CGLibProxySerializer, works in combination with the appropriate action in handleUnregisteredClass (see below)
      ret.register(CGLibProxySerializer.CGLibProxyMarker.class, new CGLibProxySerializer());
// joda DateTime, LocalDate and LocalDateTime
      ret.register(LocalDate.class, new JodaLocalDateSerializer());
      ret.register(LocalDateTime.class, new JodaLocalDateTimeSerializer());
// guava ImmutableList, ImmutableSet, ImmutableMap, ImmutableMultimap, UnmodifiableNavigableSet
      ImmutableListSerializer.registerSerializers(ret);
      ImmutableSetSerializer.registerSerializers(ret);
      ImmutableMapSerializer.registerSerializers(ret);
      ImmutableMultimapSerializer.registerSerializers(ret);
      return ret;
    }
  };

  /**
   * This was backported from a more recent version of kryo than we currently run.  The reason why it exists is
   * that we want a strategy for instantiation of classes which attempts a no-arg constructor first and THEN falls
   * back to reflection for performance reasons alone (this is, after all, in the critical path).
   *
   */
  static private class DefaultInstantiatorStrategy implements org.objenesis.strategy.InstantiatorStrategy {
    private InstantiatorStrategy fallbackStrategy;

    public DefaultInstantiatorStrategy () {
    }

    public DefaultInstantiatorStrategy (InstantiatorStrategy fallbackStrategy) {
      this.fallbackStrategy = fallbackStrategy;
    }

    public void setFallbackInstantiatorStrategy (final InstantiatorStrategy fallbackStrategy) {
      this.fallbackStrategy = fallbackStrategy;
    }

    public InstantiatorStrategy getFallbackInstantiatorStrategy () {
      return fallbackStrategy;
    }

    @Override
    public ObjectInstantiator newInstantiatorOf (final Class type) {
      if (!Util.isAndroid) {
        // Use ReflectASM if the class is not a non-static member class.
        Class enclosingType = type.getEnclosingClass();
        boolean isNonStaticMemberClass = enclosingType != null && type.isMemberClass()
                && !Modifier.isStatic(type.getModifiers());
        if (!isNonStaticMemberClass) {
          try {
            final ConstructorAccess access = ConstructorAccess.get(type);
            return new ObjectInstantiator() {
              @Override
              public Object newInstance () {
                try {
                  return access.newInstance();
                } catch (Exception ex) {
                  throw new KryoException("Error constructing instance of class: " + className(type), ex);
                }
              }
            };
          } catch (Exception ignored) {
          }
        }
      }
      // Reflection.
      try {
        Constructor ctor;
        try {
          ctor = type.getConstructor((Class[])null);
        } catch (Exception ex) {
          ctor = type.getDeclaredConstructor((Class[])null);
          ctor.setAccessible(true);
        }
        final Constructor constructor = ctor;
        return new ObjectInstantiator() {
          @Override
          public Object newInstance () {
            try {
              return constructor.newInstance();
            } catch (Exception ex) {
              throw new KryoException("Error constructing instance of class: " + className(type), ex);
            }
          }
        };
      } catch (Exception ignored) {
      }
      if (fallbackStrategy == null) {
        if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers()))
          throw new KryoException("Class cannot be created (non-static member class): " + className(type));
        else
          throw new KryoException("Class cannot be created (missing no-arg constructor): " + className(type));
      }
      // InstantiatorStrategy.
      return fallbackStrategy.newInstantiatorOf(type);
    }
  }

  public static Serializer SERIALIZER = new Serializer();

  private static class Serializer implements Function<Object, byte[]>, Serializable {
    /**
     * Serializes the given Object into bytes.
     *
     */
    @Override
    public byte[] apply(Object o) {
      return toBytes(o);
    }
  }

  public static class Deserializer<T> implements Function<byte[], T>, Serializable {

    private Class<T> clazz;

    public Deserializer(Class<T> clazz) {
      this.clazz = clazz;
    }
    /**
     * Deserializes the given bytes.
     *
     * @param bytes the function argument
     * @return the function result
     */
    @Override
    public T apply(byte[] bytes) {
      return fromBytes(bytes, clazz);
    }
  }

  private SerDeUtils() {
    // do not instantiate
  }

  /**
   * Serialize a profile measurement's value.
   *
   * <p>The value produced by a Profile definition can be any numeric data type.  The data
   * type depends on how the profile is defined by the user.  The user should be able to
   * choose the data type that is most suitable for their use case.
   *
   * @param value The value to serialize.
   */
  public static byte[] toBytes(Object value) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      Output output = new Output(bos);
      kryo.get().writeClassAndObject(output, value);
      output.flush();
      bos.flush();
      return bos.toByteArray();
    }
    catch(Throwable t) {
      LOG.error("Unable to serialize: {} because {}", value, t.getMessage(), t);
      throw new IllegalStateException("Unable to serialize " + value + " because " + t.getMessage(), t);
    }
  }

  /**
   * Deserialize a profile measurement's value.
   *
   * <p>The value produced by a Profile definition can be any numeric data type.  The data
   * type depends on how the profile is defined by the user.  The user should be able to
   * choose the data type that is most suitable for their use case.
   *
   * @param value The value to deserialize.
   */
  public static <T> T fromBytes(byte[] value, Class<T> clazz) {
    try {
      Input input = new Input(new ByteArrayInputStream(value));
      return clazz.cast(kryo.get().readClassAndObject(input));
    }
    catch(Throwable t) {
      LOG.error("Unable to deserialize  because {}", t.getMessage(), t);
      throw t;
    }
  }
}
