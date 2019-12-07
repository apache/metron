/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.utils;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.Marker;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

/**
 * This set of mocking tests checks that
 * 1. If the log level is disabled then no lambda function is called
 * 2. If the log level is enabled, then all passed lamdas are called and parmeters are passed in the correct order
 * 3. All methods call their wrapped messages correctly
 */
public class LazyLoggerImplTest {
  private static final String logString0 = "logString";
  private static final String logString1 = "logString {}";
  private static final String logString2 = "logString {} {}";
  private static final String logString3 = "logString {} {} {}";

  // stub Marker
  private static final Marker marker = new Marker() {
    @Override
    public String getName() {
      return null;
    }

    @Override
    public void add(Marker reference) {

    }

    @Override
    public boolean remove(Marker reference) {
      return false;
    }

    @Override
    public boolean hasChildren() {
      return false;
    }

    @Override
    public boolean hasReferences() {
      return false;
    }

    @Override
    public Iterator<Marker> iterator() {
      return null;
    }

    @Override
    public boolean contains(Marker other) {
      return false;
    }

    @Override
    public boolean contains(String name) {
      return false;
    }
  };

  // Stub Exception
  private static final Exception exception = new Exception();

  private List<UUID> getGuids(int numGuids) {
    return IntStream.range(0,numGuids)
            .mapToObj( x ->  UUID.randomUUID())
            .collect(Collectors.toList());
  }

  private LazyLogger getDisabledLogger() {
    final Logger loggerMock = mock(Logger.class);
    return LazyLoggerFactory.getLogger(loggerMock);
  }

  private LazyLogger getTraceEnabledLogger() {
    final LazyLogger lazyLogger = getDisabledLogger();
    Mockito.when(lazyLogger.getLogger().isTraceEnabled()).thenReturn(true);
    Mockito.when(lazyLogger.getLogger().isTraceEnabled(any(Marker.class))).thenReturn(true);
    return lazyLogger;
  }

  private LazyLogger getDebugEnabledLogger() {
    final LazyLogger lazyLogger = getDisabledLogger();
    Mockito.when(lazyLogger.getLogger().isDebugEnabled()).thenReturn(true);
    Mockito.when(lazyLogger.getLogger().isDebugEnabled(any(Marker.class))).thenReturn(true);
    return lazyLogger;
  }

  private LazyLogger getInfoEnabledLogger() {
    final LazyLogger lazyLogger = getDisabledLogger();
    Mockito.when(lazyLogger.getLogger().isInfoEnabled()).thenReturn(true);
    Mockito.when(lazyLogger.getLogger().isInfoEnabled(any(Marker.class))).thenReturn(true);
    return lazyLogger;
  }

  private LazyLogger getWarnEnabledLogger() {
    final LazyLogger lazyLogger = getDisabledLogger();
    Mockito.when(lazyLogger.getLogger().isWarnEnabled()).thenReturn(true);
    Mockito.when(lazyLogger.getLogger().isWarnEnabled(any(Marker.class))).thenReturn(true);
    return lazyLogger;
  }

  private LazyLogger getErrorEnabledLogger() {
    final LazyLogger lazyLogger = getDisabledLogger();
    Mockito.when(lazyLogger.getLogger().isErrorEnabled()).thenReturn(true);
    Mockito.when(lazyLogger.getLogger().isErrorEnabled(any(Marker.class))).thenReturn(true);
    return lazyLogger;
  }

  @SuppressWarnings("unchecked") // We assume all objects are of type Object
  private Supplier<Object> getMockedSupplier() {
    return mock(Supplier.class);
  }


  @Test
  public void traceEnabled1() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getTraceEnabledLogger();
    logger.isTraceEnabled();
    Mockito.verify(logger.getLogger()).isTraceEnabled();

    logger.isTraceEnabled(marker);
    Mockito.verify(logger.getLogger()).isTraceEnabled(marker);

    logger.trace(logString0);
    Mockito.verify(logger.getLogger()).trace(logString0);

    logger.trace(logString0, exception);
    Mockito.verify(logger.getLogger()).trace(logString0,exception);

    logger.trace(logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).trace(logString1, guids.get(0));

    logger.trace(marker, logString0);
    Mockito.verify(logger.getLogger()).trace(marker, logString0);

    logger.trace(marker, logString0, exception);
    Mockito.verify(logger.getLogger()).trace(marker, logString0, exception);

    logger.trace(marker, logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).trace(marker, logString1, guids.get(0));

  }

  @Test
  public void traceEnabled1Lambda() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getTraceEnabledLogger();
    logger.trace(logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).trace(logString1, guids.get(0));

    logger.trace(marker, logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).trace(marker, logString1, guids.get(0));
  }

  @Test
  public void traceDisabled1Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.trace(logString1, supplier);
    Mockito.verify(supplier, never()).get();

    logger.trace(marker, logString1, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void traceEnabled2() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getTraceEnabledLogger();
    logger.trace(logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger())
            .trace(logString2, guids.get(0), guids.get(1));

    logger.trace(marker, logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger())
            .trace(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void traceEnabled2Lambda() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getTraceEnabledLogger();
    logger.trace(logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).trace(logString2, guids.get(0), guids.get(1));

    logger.trace(marker, logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).trace(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void traceDisabled2Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();

    logger.trace(logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.trace(marker, logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void traceEnabled3() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getTraceEnabledLogger();
    logger.trace(logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).trace(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.trace(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).trace(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
  }

  @Test
  public void traceEnabled3Lambda() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getTraceEnabledLogger();

    logger.trace(logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).trace(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.trace(marker, logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).trace(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
  }

  @Test
  public void traceDisabled3Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();

    logger.trace(logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.trace(marker, logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }
  

  @Test
  public void debugEnabled1() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getDebugEnabledLogger();

    logger.isDebugEnabled();
    Mockito.verify(logger.getLogger()).isDebugEnabled();

    logger.isDebugEnabled(marker);
    Mockito.verify(logger.getLogger()).isDebugEnabled(marker);

    logger.debug(logString0);
    Mockito.verify(logger.getLogger()).debug(logString0);

    logger.debug(logString0,exception);
    Mockito.verify(logger.getLogger()).debug(logString0,exception);

    logger.debug(logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).debug(logString1, guids.get(0));

    logger.debug(marker, logString0);
    Mockito.verify(logger.getLogger()).debug(marker, logString0);

    logger.debug(marker, logString0,exception);
    Mockito.verify(logger.getLogger()).debug(marker, logString0, exception);

    logger.debug(marker, logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).debug(marker, logString1, guids.get(0));
  }

  @Test
  public void debugEnabled1Lambda() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getDebugEnabledLogger();
    logger.debug(logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).debug(logString1, guids.get(0));

    logger.debug(marker, logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).debug(marker, logString1, guids.get(0));
  }

  @Test
  public void debugDisabled1Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.debug(logString1, supplier);
    Mockito.verify(supplier, never()).get();

    logger.debug(marker, logString1, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void debugEnabled2() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getDebugEnabledLogger();
    logger.debug(logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger())
            .debug(logString2, guids.get(0), guids.get(1));

    logger.debug(marker, logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger())
            .debug(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void debugEnabled2Lambda() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getDebugEnabledLogger();
    logger.debug(logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).debug(logString2, guids.get(0), guids.get(1));

    logger.debug(marker, logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).debug(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void debugDisabled2Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();

    logger.debug(logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.debug(marker, logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void debugEnabled3() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getDebugEnabledLogger();
    logger.debug(logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).debug(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.debug(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).debug(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
  }

  @Test
  public void debugEnabled3Lambda() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getDebugEnabledLogger();

    logger.debug(logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).debug(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.debug(marker, logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).debug(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
  }

  @Test
  public void debugDisabled3Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();

    logger.debug(logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.debug(marker, logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  
  @Test
  public void infoEnabled1Lambda() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getInfoEnabledLogger();

    logger.info(logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).info(logString1, guids.get(0));

    logger.info(marker, logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).info(marker, logString1, guids.get(0));
  }

  @Test
  public void infoEnabled1() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getInfoEnabledLogger();
    logger.isInfoEnabled();
    Mockito.verify(logger.getLogger()).isInfoEnabled();

    logger.isInfoEnabled(marker);
    Mockito.verify(logger.getLogger()).isInfoEnabled(marker);

    logger.info(logString0);
    Mockito.verify(logger.getLogger()).info(logString0);

    logger.info(logString0, exception);
    Mockito.verify(logger.getLogger()).info(logString0, exception);

    logger.info(logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).info(logString1, guids.get(0));

    logger.info(marker, logString0);
    Mockito.verify(logger.getLogger()).info(marker, logString0);

    logger.info(marker, logString0, exception);
    Mockito.verify(logger.getLogger()).info(marker, logString0, exception);

    logger.info(marker, logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).info(marker, logString1, guids.get(0));
  }

  @Test
  public void infoDisabled1Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.info(logString1, supplier);
    Mockito.verify(supplier, never()).get();

    logger.info(marker, logString1, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void infoEnabled2Lambda() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getInfoEnabledLogger();
    logger.info(logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).info(logString2, guids.get(0), guids.get(1));

    logger.info(marker, logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).info(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void infoEnabled2() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getInfoEnabledLogger();
    logger.info(logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger()).info(logString2, guids.get(0), guids.get(1));

    logger.info(marker, logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger()).info(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void infoDisabled2Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.info(logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.info(marker, logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void infoEnabled3Lambda() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getInfoEnabledLogger();
    logger.info(logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).info(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.info(marker, logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).info(marker, logString3, guids.get(0), guids.get(1), guids.get(2));

  }

  @Test
  public void infoEnabled3() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getInfoEnabledLogger();
    logger.info(logString3, guids.get(0), guids.get(1),  guids.get(2));
    Mockito.verify(logger.getLogger()).info(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.info(marker, logString3, guids.get(0), guids.get(1),  guids.get(2));
    Mockito.verify(logger.getLogger()).info(marker, logString3, guids.get(0), guids.get(1), guids.get(2));

  }

  @Test
  public void infoDisabled3Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.info(logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.info(marker, logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void warnEnabled1Lambda() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getWarnEnabledLogger();
    logger.warn(logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).warn(logString1, guids.get(0));

    logger.warn(marker, logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).warn(marker, logString1, guids.get(0));

  }

  @Test
  public void warnEnabled1() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getWarnEnabledLogger();
    logger.isWarnEnabled();
    Mockito.verify(logger.getLogger()).isWarnEnabled();

    logger.isWarnEnabled(marker);
    Mockito.verify(logger.getLogger()).isWarnEnabled(marker);

    logger.warn(logString0);
    Mockito.verify(logger.getLogger()).warn(logString0);

    logger.warn(logString0,exception);
    Mockito.verify(logger.getLogger()).warn(logString0,exception);

    logger.warn(logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).warn(logString1, guids.get(0));

    logger.warn(marker, logString0);
    Mockito.verify(logger.getLogger()).warn(marker, logString0);

    logger.warn(marker, logString0,exception);
    Mockito.verify(logger.getLogger()).warn(marker, logString0,exception);

    logger.warn(marker, logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).warn(marker, logString1, guids.get(0));
  }

  @Test
  public void warnDisabled1Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.warn(logString1, supplier);
    Mockito.verify(supplier, never()).get();

    logger.warn(marker, logString1, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void warnEnabled2Lambda() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getWarnEnabledLogger();
    logger.warn(logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).warn(logString2, guids.get(0), guids.get(1));

    logger.warn(marker, logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).warn(marker, logString2, guids.get(0), guids.get(1));

  }

  @Test
  public void warnEnabled2() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getWarnEnabledLogger();
    logger.warn(logString2, guids.get(0),  guids.get(1));
    Mockito.verify(logger.getLogger()).warn(logString2, guids.get(0), guids.get(1));

    logger.warn(marker, logString2, guids.get(0),  guids.get(1));
    Mockito.verify(logger.getLogger()).warn(marker, logString2, guids.get(0), guids.get(1));
  }


  @Test
  public void warnDisabled2Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.warn(logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.warn(marker, logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void warnEnabled3Lambda() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getWarnEnabledLogger();
    logger.warn(logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).warn(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.warn(marker, logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).warn(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
  }

  @Test
  public void warnEnabled3() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getWarnEnabledLogger();
    logger.warn(logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).warn(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.warn(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).warn(marker, logString3, guids.get(0), guids.get(1), guids.get(2));

  }

  @Test
  public void warnDisabled3Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.warn(logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.warn(marker, logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void errorEnabled1Lambda() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getErrorEnabledLogger();
    logger.error(logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).error(logString1, guids.get(0));

    logger.error(marker, logString1, () -> guids.get(0));
    Mockito.verify(logger.getLogger()).error(marker, logString1, guids.get(0));
  }

  @Test
  public void errorEnabled1() {
    final List<UUID> guids = getGuids(1);
    final LazyLogger logger =  getErrorEnabledLogger();

    logger.isErrorEnabled();
    Mockito.verify(logger.getLogger()).isErrorEnabled();

    logger.isErrorEnabled(marker);
    Mockito.verify(logger.getLogger()).isErrorEnabled(marker);

    logger.error(logString0);
    Mockito.verify(logger.getLogger()).error(logString0);

    logger.error(logString0,exception);
    Mockito.verify(logger.getLogger()).error(logString0,exception);

    logger.error(logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).error(logString1, guids.get(0));

    logger.error(marker, logString0);
    Mockito.verify(logger.getLogger()).error(marker, logString0);

    logger.error(marker, logString0, exception);
    Mockito.verify(logger.getLogger()).error(marker, logString0, exception);

    logger.error(marker, logString1, guids.get(0));
    Mockito.verify(logger.getLogger()).error(marker, logString1, guids.get(0));

  }

  @Test
  public void errorDisabled1Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.error(logString1, supplier);
    Mockito.verify(supplier, never()).get();

    logger.error(marker, logString1, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void errorEnabled2Lambda() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getErrorEnabledLogger();
    logger.error(logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).error(logString2, guids.get(0), guids.get(1));

    logger.error(marker, logString2, () -> guids.get(0), () -> guids.get(1));
    Mockito.verify(logger.getLogger()).error(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void errorEnabled2() {
    final List<UUID> guids = getGuids(2);
    final LazyLogger logger =  getErrorEnabledLogger();
    logger.error(logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger()).error(logString2, guids.get(0), guids.get(1));

    logger.error(marker, logString2, guids.get(0), guids.get(1));
    Mockito.verify(logger.getLogger()).error(marker, logString2, guids.get(0), guids.get(1));
  }

  @Test
  public void errorDisabled2Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.error(logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.error(marker, logString2, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }

  @Test
  public void errorEnabled3Lambda() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getErrorEnabledLogger();
    logger.error(logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).error(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.error(marker, logString3, () -> guids.get(0), () -> guids.get(1), () -> guids.get(2));
    Mockito.verify(logger.getLogger()).error(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
  }

  @Test
  public void errorEnabled3() {
    final List<UUID> guids = getGuids(3);
    final LazyLogger logger =  getErrorEnabledLogger();
    logger.error(logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).error(logString3, guids.get(0), guids.get(1), guids.get(2));

    logger.error(marker, logString3, guids.get(0), guids.get(1), guids.get(2));
    Mockito.verify(logger.getLogger()).error(marker, logString3, guids.get(0), guids.get(1), guids.get(2));

  }

  @Test
  public void errorDisabled3Lambda() {
    final Supplier<Object> supplier = getMockedSupplier();
    final LazyLogger logger =  getDisabledLogger();
    logger.error(logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();

    logger.error(marker, logString3, supplier, supplier, supplier);
    Mockito.verify(supplier, never()).get();
  }
}