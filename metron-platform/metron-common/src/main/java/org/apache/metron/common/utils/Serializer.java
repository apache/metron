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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.reflectasm.ConstructorAccess;
import de.javakaffee.kryoserializers.*;
import de.javakaffee.kryoserializers.cglib.CGLibProxySerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaDateTimeSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateSerializer;
import de.javakaffee.kryoserializers.jodatime.JodaLocalDateTimeSerializer;
import de.javakaffee.kryoserializers.protobuf.ProtobufSerializer;
import de.javakaffee.kryoserializers.wicket.MiniMapSerializer;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.storm.shade.org.joda.time.DateTime;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.esotericsoftware.kryo.util.Util.className;

/**
 * Provides basic functionality to serialize and deserialize the allowed
 * value types for a ProfileMeasurement.
 */
public class Serializer {
  protected static final Logger LOG = LoggerFactory.getLogger(Serializer.class);
  private static ThreadLocal<Kryo> kryo = new ThreadLocal<Kryo>() {
    @Override
    protected Kryo initialValue() {
      Kryo ret = new Kryo();
      ret.setReferences(true);
      //ret.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

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
      ret.register(DateTime.class, new JodaDateTimeSerializer());
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

  private Serializer() {
    // do not instantiate
  }

  /**
   * Serialize a profile measurement's value.
   *
   * The value produced by a Profile definition can be any numeric data type.  The data
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
      LOG.error("Unable to serialize: " + value + " because " + t.getMessage(), t);
      throw new IllegalStateException("Unable to serialize " + value + " because " + t.getMessage(), t);
    }
  }

  /**
   * Deserialize a profile measurement's value.
   *
   * The value produced by a Profile definition can be any numeric data type.  The data
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
      LOG.error("Unable to deserialize  because " + t.getMessage(), t);
      throw t;
    }
  }
}
