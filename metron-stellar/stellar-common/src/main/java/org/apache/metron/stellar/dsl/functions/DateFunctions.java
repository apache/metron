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

package org.apache.metron.stellar.dsl.functions;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.metron.stellar.dsl.BaseStellarFunction;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.common.utils.ConversionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

/**
 * Stellar data functions.
 */
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

  private static LoadingCache<TimezonedFormat, ThreadLocal<SimpleDateFormat>> formatCache =
          Caffeine.newBuilder().build(
                  new CacheLoader<TimezonedFormat, ThreadLocal<SimpleDateFormat>>() {
                    @Override
                    public ThreadLocal<SimpleDateFormat> load(final TimezonedFormat format) throws Exception {
                      return new ThreadLocal<SimpleDateFormat>() {
                        @Override
                        public SimpleDateFormat initialValue() {
                        return format.toDateFormat();
                        }
                      };
                    }
                  });

  public static SimpleDateFormat createFormat(String format, Optional<String> timezone) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    if(timezone.isPresent()) {
      sdf.setTimeZone(TimeZone.getTimeZone(timezone.get()));
    }
    return sdf;
  }

  public static long getEpochTime(String date, String format, Optional<String> timezone) throws ExecutionException, ParseException {
    TimezonedFormat fmt;
    if(timezone.isPresent()) {
      fmt = new TimezonedFormat(format, timezone.get());
    } else {
      fmt = new TimezonedFormat(format);
    }
    SimpleDateFormat sdf = formatCache.get(fmt).get();
    return sdf.parse(date).getTime();
  }


  /**
   * Stellar Function: TO_EPOCH_TIMESTAMP
   */
  @Stellar( name="TO_EPOCH_TIMESTAMP"
          , description="Returns the epoch timestamp of the dateTime in the specified format. " +
                        "If the format does not have a timestamp and you wish to assume a " +
                        "given timestamp, you may specify the timezone optionally."
          , params = { "dateTime - DateTime in String format"
                     , "format - DateTime format as a String"
                     , "timezone - Optional timezone in String format"
                     }
          , returns = "Epoch timestamp")
  public static class ToTimestamp extends BaseStellarFunction {
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
          Optional<String> tz = (tzObj == null) ? Optional.empty() : Optional.of(tzObj.toString());
          return getEpochTime(dateObj.toString(), formatObj.toString(), tz);

        } catch (ExecutionException | ParseException e) {
          return null;
        }
      }
      return null;
    }
  }

  /**
   * Gets the value from a list of arguments.
   *
   * If the argument at the specified position does not exist, a default value will be returned.
   * If the argument at the specified position exists, but cannot be coerced to the right type, null is returned.
   * Otherwise, the argument value is returned.
   *
   * @param args A list of arguments.
   * @param position The position of the argument to get.
   * @param clazz The type of class expected.
   * @param defaultValue The default value.
   * @param <T> The expected type of the argument.
   */
  private static <T> T getOrDefault(List<Object> args, int position, Class<T> clazz, T defaultValue) {
      T result = defaultValue;
      if(args.size() > position) {
        result = ConversionUtils.convert(args.get(position), clazz);
      }
      return result;
  }

  /**
   * Stellar Function: DAY_OF_WEEK
   *
   * The numbered day within the week.  The first day of the week, Sunday, has a value of 1.
   *
   * If no argument is supplied, returns the current day of week.
   */
  @Stellar( name="DAY_OF_WEEK"
          , description="The numbered day within the week.  The first day of the week, Sunday, has a value of 1."
          , params = { "dateTime - The datetime as a long representing the milliseconds since unix epoch"
                     }
          , returns = "The numbered day within the week.")
  public static class DayOfWeek extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // expects epoch millis, otherwise defaults to current time
      Long epochMillis = getOrDefault(args, 0, Long.class, System.currentTimeMillis());
      if(epochMillis == null) {
        return null;  // invalid argument
      }

      // create a calendar
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(epochMillis);

      return calendar.get(Calendar.DAY_OF_WEEK);
    }
  }

  /**
   * Stellar Function: DAY_OF_MONTH
   *
   * The day within the month.  The first day within the month has a value of 1.
   */
  @Stellar( name="DAY_OF_MONTH"
          , description="The numbered day within the month.  The first day within the month has a value of 1."
          , params = { "dateTime - The datetime as a long representing the milliseconds since unix epoch"
                     }
          , returns = "The numbered day within the month.")
  public static class DayOfMonth extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // expects epoch millis, otherwise defaults to current time
      Long epochMillis = getOrDefault(args, 0, Long.class, System.currentTimeMillis());
      if(epochMillis == null) {
        return null;  // invalid argument
      }

      // create a calendar
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(epochMillis);

      return calendar.get(Calendar.DAY_OF_MONTH);
    }
  }

  /**
   * Stellar Function: WEEK_OF_MONTH
   *
   * The numbered week within the month.  The first week has a value of 1.
   */
  @Stellar( name="WEEK_OF_MONTH"
          , description="The numbered week within the month.  The first week within the month has a value of 1."
          , params = { "dateTime - The datetime as a long representing the milliseconds since unix epoch"
                     }
          , returns = "The numbered week within the month.")
  public static class WeekOfMonth extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // expects epoch millis, otherwise defaults to current time
      Long epochMillis = getOrDefault(args, 0, Long.class, System.currentTimeMillis());
      if(epochMillis == null) {
        return null;  // invalid argument
      }

      // create a calendar
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(epochMillis);

      return calendar.get(Calendar.WEEK_OF_MONTH);
    }
  }

  /**
   * Stellar Function: WEEK_OF_YEAR
   *
   * The numbered week within the year.  The first week in the year has a value of 1.
   */
  @Stellar( name="WEEK_OF_YEAR"
          , description="The numbered week within the year.  The first week in the year has a value of 1."
          , params = { "dateTime - The datetime as a long representing the milliseconds since unix epoch"
                     }
          , returns = "The numbered week within the year.")
  public static class WeekOfYear extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // expects epoch millis, otherwise defaults to current time
      Long epochMillis = getOrDefault(args, 0, Long.class, System.currentTimeMillis());
      if(epochMillis == null) {
        return null;  // invalid argument
      }

      // create a calendar
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(epochMillis);

      return calendar.get(Calendar.WEEK_OF_YEAR);
    }
  }

  /**
   * Stellar Function: MONTH
   *
   * A number representing the month.  The first month, January, has a value of 0.
   */
  @Stellar( name="MONTH"
          , description="The number representing the month.  The first month, January, has a value of 0."
          , params = { "dateTime - The datetime as a long representing the milliseconds since unix epoch"
                     }
          , returns = "The current month (0-based).")
  public static class MonthOfYear extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // expects epoch millis, otherwise defaults to current time
      Long epochMillis = getOrDefault(args, 0, Long.class, System.currentTimeMillis());
      if(epochMillis == null) {
        return null;  // invalid argument
      }

      // create a calendar
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(epochMillis);

      return calendar.get(Calendar.MONTH);
    }
  }

  /**
   * Stellar Function: YEAR
   *
   * The calendar year.
   */
  @Stellar( name="YEAR"
          , description="The number representing the year. "
          , params = { "dateTime - The datetime as a long representing the milliseconds since unix epoch"
                     }
          , returns = "The current year"
          )
  public static class Year extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // expects epoch millis, otherwise defaults to current time
      Long epochMillis = getOrDefault(args, 0, Long.class, System.currentTimeMillis());
      if(epochMillis == null) {
        return null;  // invalid argument
      }

      // create a calendar
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(epochMillis);

      return calendar.get(Calendar.YEAR);
    }
  }

  /**
   * Stellar Function: DAY_OF_YEAR
   *
   * The day number within the year.  The first day of the year has value of 1.
   */
  @Stellar( name="DAY_OF_YEAR"
          , description="The day number within the year.  The first day of the year has value of 1."
          , params = { "dateTime - The datetime as a long representing the milliseconds since unix epoch"
                     }
          , returns = "The day number within the year."
          )
  public static class DayOfYear extends BaseStellarFunction {
    @Override
    public Object apply(List<Object> args) {

      // expects epoch millis, otherwise defaults to current time
      Long epochMillis = getOrDefault(args, 0, Long.class, System.currentTimeMillis());
      if(epochMillis == null) {
        return null;  // invalid argument
      }

      // create a calendar
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(epochMillis);

      return calendar.get(Calendar.DAY_OF_YEAR);
    }
  }
}

