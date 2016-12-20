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

package org.apache.metron.profiler;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.common.dsl.Context;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.profiler.stellar.StellarExecutor;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Responsible for building and maintaining a Profile.
 *
 * One or more messages are applied to the Profile with `apply` and a profile measurement is
 * produced by calling `flush`.
 *
 * Any one instance is responsible only for building the profile for a specific [profile, entity]
 * pairing.  There will exist many instances, one for each [profile, entity] pair that exists
 * within the incoming telemetry data applied to the profile.
 */
public class ProfileBuilder implements Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(ProfileBuilder.class);

  /**
   * The name of the profile.
   */
  private String profileName;

  /**
   * The name of the entity.
   */
  private String entity;

  /**
   * The definition of the Profile that the bolt is building.
   */
  private ProfileConfig definition;

  /**
   * Executes Stellar code and maintains state across multiple invocations.
   */
  private StellarExecutor executor;

  /**
   * Has the profile been initialized?
   */
  private boolean isInitialized;

  /**
   * The duration of each period in milliseconds.
   */
  private long periodDurationMillis;

  /**
   * Use the ProfileBuilder.Builder to create a new ProfileBuilder.
   */
  private ProfileBuilder(ProfileConfig definition, String entity, long periodDurationMillis, StellarExecutor executor) {

    this.isInitialized = false;
    this.definition = definition;
    this.profileName = definition.getProfile();
    this.entity = entity;
    this.periodDurationMillis = periodDurationMillis;
    this.executor = executor;
  }

  /**
   * Apply a message to the profile.
   * @param message The message to apply.
   * @param context The Stellar execution context.
   */
  @SuppressWarnings("unchecked")
  public void apply(JSONObject message, Context context) {

    if(!isInitialized()) {
      assign(definition.getInit(), message, context, "init");
      isInitialized = true;
    }

    assign(definition.getUpdate(), message, context, "update");
  }

  /**
   * Flush the Profile.
   *
   * Completes and emits the ProfileMeasurement.  Clears all state in preparation for
   * the next window period.
   *
   * @param flushTimeMillis When the flush occurred in epoch milliseconds.
   * @param context The Stellar execution context.
   */
  public ProfileMeasurement flush(long flushTimeMillis, Context context) {
    LOG.debug("Flushing profile: profile={}, entity={}", profileName, entity);

    // execute the 'result' expression
    @SuppressWarnings("unchecked")
    Object value = execute(definition.getResult(), new JSONObject(), context, "result");

    // execute the 'groupBy' expression(s) - can refer to value of 'result' expression
    List<Object> groups = execute(definition.getGroupBy(), ImmutableMap.of("result", value), context,"groupBy");

    // execute the 'tickUpdate' expression(s) - can refer to value of 'result' expression
    assign(definition.getTickUpdate(), ImmutableMap.of("result", value), context, "tickUpdate");

    // save a copy of current state then clear it to prepare for the next window
    Map<String, Object> state = executor.getState();
    executor.clearState();

    // the 'tickUpdate' state is not flushed - make sure to bring that state along to the next period
    definition.getTickUpdate().forEach((var, expr) -> {
      Object val = state.get(var);
      executor.assign(var, val);
    });

    isInitialized = false;
    return new ProfileMeasurement()
            .withProfileName(profileName)
            .withEntity(entity)
            .withGroups(groups)
            .withPeriod(flushTimeMillis, periodDurationMillis, TimeUnit.MILLISECONDS)
            .withValue(value);
  }

  /**
   * Executes an expression contained within the profile definition.
   * @param expression The expression to execute.
   * @param transientState Additional transient state provided to the expression.
   * @param context The Stellar execution context.
   * @param expressionType The type of expression; init, update, result.  Provides additional context if expression execution fails.
   * @return The result of executing the expression.
   */
  private Object execute(String expression, Map<String, Object> transientState, Context context, String expressionType) {
    Object result = null;

    List<Object> allResults = execute(Collections.singletonList(expression), transientState, context, expressionType);
    if(allResults.size() > 0) {
      result = allResults.get(0);
    }

    return result;
  }

  /**
   * Executes a set of expressions whose results need to be assigned to a variable.
   * @param expressions Maps the name of a variable to the expression whose result should be assigned to it.
   * @param transientState Additional transient state provided to the expression.
   * @param context The Stellar execution context.
   * @param expressionType The type of expression; init, update, result.  Provides additional context if expression execution fails.
   */
  private void assign(Map<String, String> expressions, Map<String, Object> transientState, Context context, String expressionType) {
    try {
      // set the execution context
      executor.setContext(context);

      // execute each of the expressions
      MapUtils.emptyIfNull(expressions)
              .forEach((var, expr) -> executor.assign(var, expr, transientState));

    } catch(ParseException e) {

      // make it brilliantly clear that one of the expressions is bad
      String msg = format("Bad '%s' expression: %s, profile=%s, entity=%s", expressionType, e.getMessage(), profileName, entity);
      throw new ParseException(msg, e);
    }
  }

  /**
   * Executes the expressions contained within the profile definition.
   * @param expressions A list of expressions to execute.
   * @param transientState Additional transient state provided to the expressions.
   * @param context The Stellar execution context.
   * @param expressionType The type of expression; init, update, result.  Provides additional context if expression execution fails.
   * @return The result of executing each expression.
   */
  private List<Object> execute(List<String> expressions, Map<String, Object> transientState, Context context, String expressionType) {
    List<Object> results = new ArrayList<>();

    try {
      // set the execution context
      executor.setContext(context);

      ListUtils.emptyIfNull(expressions)
              .forEach((expr) -> results.add(executor.execute(expr, transientState, Object.class)));

    } catch (Throwable e) {
      String msg = format("Bad '%s' expression: %s, profile=%s, entity=%s", expressionType, e.getMessage(), profileName, entity);
      throw new ParseException(msg, e);
    }

    return results;
  }

  /**
   * Returns the current value of a variable.
   * @param variable The name of the variable.
   */
  public Object valueOf(String variable) {
    return executor.getState().get(variable);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public String getProfileName() {
    return profileName;
  }

  public String getEntity() {
    return entity;
  }

  public ProfileConfig getDefinition() {
    return definition;
  }

  @Override
  public String toString() {
    return "ProfileBuilder{" +
            "profileName='" + profileName + '\'' +
            ", entity='" + entity + '\'' +
            ", isInitialized=" + isInitialized +
            '}';
  }

  /**
   * A builder used to construct a new ProfileBuilder.
   */
  public static class Builder {

    private ProfileConfig definition;
    private String entity;
    private long periodDurationMillis;
    private StellarExecutor executor;

    public Builder withExecutor(StellarExecutor executor) {
      this.executor = executor;
      return this;
    }

    /**
     * @param definition The profiler definition.
     */
    public Builder withDefinition(ProfileConfig definition) {
      this.definition = definition;
      return this;
    }

    /**
     * @param entity The name of the entity
     */
    public Builder withEntity(String entity) {
      this.entity = entity;
      return this;
    }

    /**
     * @param duration The duration of each profile period.
     * @param units The units used to specify the duration of the profile period.
     */
    public Builder withPeriodDuration(long duration, TimeUnit units) {
      this.periodDurationMillis = units.toMillis(duration);
      return this;
    }

    /**
     * @param millis The duration of each profile period in milliseconds.
     */
    public Builder withPeriodDurationMillis(long millis) {
      this.periodDurationMillis = millis;
      return this;
    }

    /**
     * Construct a ProfileBuilder.
     */
    public ProfileBuilder build() {

      if(definition == null) {
        throw new IllegalArgumentException("missing profiler definition");
      }
      if(StringUtils.isEmpty(entity)) {
        throw new IllegalArgumentException("missing entity name");
      }
      if(executor == null) {
        throw new IllegalArgumentException("missing stellar executor");
      }

      return new ProfileBuilder(definition, entity, periodDurationMillis, executor);
    }
  }
}
