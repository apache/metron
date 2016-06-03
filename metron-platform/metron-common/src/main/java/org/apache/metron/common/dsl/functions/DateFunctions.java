/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.dsl.functions;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class DateFunctions {

  private static class TimezonedFormat {
    private String format;
    private Optional<String> timezone;
    public TimezonedFormat(String format, String timezone) {
      this.format = format;
      this.timezone = Optional.of(timezone);
    }

    public TimezonedFormat(String format) {
      this.format = format;
      this.timezone = Optional.empty();
    }
    public SimpleDateFormat toDateFormat() {
      return createFormat(format, timezone);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TimezonedFormat that = (TimezonedFormat) o;

      if (format != null ? !format.equals(that.format) : that.format != null) return false;
      return timezone != null ? timezone.equals(that.timezone) : that.timezone == null;

    }

    @Override
    public int hashCode() {
      int result = format != null ? format.hashCode() : 0;
      result = 31 * result + (timezone != null ? timezone.hashCode() : 0);
      return result;
    }
  }

  private static LoadingCache<TimezonedFormat, ThreadLocal<SimpleDateFormat>> formatCache
          = CacheBuilder.newBuilder().build(new CacheLoader<TimezonedFormat, ThreadLocal<SimpleDateFormat>>() {
            @Override
            public ThreadLocal<SimpleDateFormat> load(final TimezonedFormat format) throws Exception {
              return new ThreadLocal<SimpleDateFormat>() {
                @Override
                public SimpleDateFormat initialValue() {
                  return format.toDateFormat();
                }
              };
            }
          }
                        );

  public static SimpleDateFormat createFormat(String format, Optional<String> timezone) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    if(timezone.isPresent()) {
      sdf.setTimeZone(TimeZone.getTimeZone(timezone.get()));
    }
    return sdf;
  }
  public static long getEpochTime(String date, String format, Optional<String> timezone) throws ExecutionException, ParseException {
    TimezonedFormat fmt = null;
    if(timezone.isPresent()) {
      fmt = new TimezonedFormat(format, timezone.get());
    }
    else {
      fmt = new TimezonedFormat(format);
    }
    SimpleDateFormat sdf = formatCache.get(fmt).get();
    return sdf.parse(date).getTime();
  }


  public static class ToTimestamp implements Function<List<Object>, Object> {
    @Override
    public Object apply(List<Object> objects) {
      Object dateObj = objects.get(0);
      Object formatObj = objects.get(1);
      Object tzObj = null;
      if(objects.size() >= 3) {
        tzObj = objects.get(2);
      }
      if(dateObj != null && formatObj != null) {
        try {
          return getEpochTime(dateObj.toString()
                             , formatObj.toString()
                             , tzObj == null?Optional.empty():Optional.of(tzObj.toString())
                             );
        } catch (ExecutionException e) {
          return null;
        } catch (ParseException e) {
          return null;
        }
      }
      return null;
    }
  }
}
