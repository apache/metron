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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.common.configuration.profiler.ProfileConfig;
import org.apache.metron.stellar.common.DefaultStellarStatefulExecutor;
import org.apache.metron.stellar.common.StellarStatefulExecutor;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
public class DefaultProfileBuilder implements ProfileBuilder, Serializable {

  protected static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
  private StellarStatefulExecutor executor;

  /**
   * Has the profile been initialized?
   */
  private boolean isInitialized;

  /**
   * The duration of each period in milliseconds.
   */
  private long periodDurationMillis;

  /**
   * Tracks the latest timestamp for use when flushing the profile.
   */
  private long maxTimestamp;

  /**
   * Private constructor.  Use the {@link Builder} to create a new {@link ProfileBuilder).
   */
  private DefaultProfileBuilder(ProfileConfig definition,
                                String entity,
                                long periodDurationMillis,
                                Context stellarContext) {

    this.isInitialized = false;
    this.definition = definition;
    this.profileName = definition.getProfile();
    this.entity = entity;
    this.periodDurationMillis = periodDurationMillis;
    this.executor = new DefaultStellarStatefulExecutor();
    StellarFunctions.initialize(stellarContext);
    this.executor.setContext(stellarContext);
    this.maxTimestamp = 0;
  }

  /**
   * Apply a message to the profile.
   *
   * @param message The message to apply.
   * @param timestamp The timestamp of the message.
   */
  @Override
  public void apply(JSONObject message, long timestamp) {
    LOG.debug("Applying message to profile; profile={}, entity={}, timestamp={}",
            profileName, entity, timestamp);

    try {
      if (!isInitialized()) {
        LOG.debug("Initializing profile; profile={}, entity={}, timestamp={}",
                profileName, entity, timestamp);

        // execute each 'init' expression
        assign(definition.getInit(), message, "init");
        isInitialized = true;
      }

      // execute each 'update' expression
      assign(definition.getUpdate(), message, "update");

      // keep track of the 'latest' timestamp seen for use when flushing the profile
      if(timestamp > maxTimestamp) {
        maxTimestamp = timestamp;
      }

    } catch(Throwable e) {
      LOG.error(format("Unable to apply message to profile: %s", e.getMessage()), e);
    }
  }

  /**
   * Flush the Profile.
   *
   * <p>Completes and emits the {@link ProfileMeasurement}.  Clears all state in preparation for
   * the next window period.
   *
   * @return Returns the completed {@link ProfileMeasurement}.
   */
  @Override
  public Optional<ProfileMeasurement> flush() {
    Optional<ProfileMeasurement> result;
    ProfilePeriod period = ProfilePeriod.fromTimestamp(maxTimestamp, periodDurationMillis, TimeUnit.MILLISECONDS);
    try {
      // execute the 'profile' expression
      String profileExpression = definition
              .getResult()
              .getProfileExpressions()
              .getExpression();
      Object profileValue = execute(profileExpression, "result/profile");

      // execute the 'triage' expression(s)
      Map<String, Object> triageValues = definition
              .getResult()
              .getTriageExpressions()
              .getExpressions()
              .entrySet()
              .stream()
              .collect(Collectors.toMap(
                      e -> e.getKey(),
                      e -> execute(e.getValue(), "result/triage")));

      // the state that will be made available to the `groupBy` expression
      Map<String, Object> state = new HashMap<>();
      state.put("profile", profileName);
      state.put("entity", entity);
      state.put("start", period.getStartTimeMillis());
      state.put("end", period.getEndTimeMillis());
      state.put("duration", period.getDurationMillis());
      state.put("result", profileValue);

      // execute the 'groupBy' expression(s) - can refer to value of 'result' expression
      List<Object> groups = execute(definition.getGroupBy(), state, "groupBy");

      result = Optional.of(new ProfileMeasurement()
              .withProfileName(profileName)
              .withEntity(entity)
              .withGroups(groups)
              .withPeriod(period)
              .withProfileValue(profileValue)
              .withTriageValues(triageValues)
              .withDefinition(definition));

    } catch(Throwable e) {

      // if any of the Stellar expressions fail, a measurement should NOT be returned
      LOG.error(format("Unable to flush profile: error=%s", e.getMessage()), e);
      result = Optional.empty();
    }

    LOG.debug("Flushed profile: profile={}, entity={}, maxTime={}, period={}, start={}, end={}, duration={}",
            profileName,
            entity,
            maxTimestamp,
            period.getPeriod(),
            period.getStartTimeMillis(),
            period.getEndTimeMillis(),
            period.getDurationMillis());

    isInitialized = false;
    return result;
  }

  /**
   * Returns the current value of a variable.
   * @param variable The name of the variable.
   */
  @Override
  public Object valueOf(String variable) {
    return executor.getState().get(variable);
  }

  @Override
  public boolean isInitialized() {
    return isInitialized;
  }

  @Override
  public ProfileConfig getDefinition() {
    return definition;
  }

  /**
   * Executes an expression contained within the profile definition.
   *
   * @param expression The expression to execute.
   * @param transientState Additional transient state provided to the expression.
   * @param expressionType The type of expression; init, update, result.  Provides additional context if expression execution fails.
   * @return The result of executing the expression.
   */
  private Object execute(String expression, Map<String, Object> transientState, String expressionType) {
    Object result = null;

    List<Object> allResults = execute(Collections.singletonList(expression), transientState, expressionType);
    if(allResults.size() > 0) {
      result = allResults.get(0);
    }

    return result;
  }

  /**
   * Executes an expression contained within the profile definition.
   *
   * @param expression The expression to execute.
   * @param expressionType The type of expression; init, update, result.  Provides additional context if expression execution fails.
   * @return The result of executing the expression.
   */
  private Object execute(String expression, String expressionType) {
    return execute(expression, Collections.emptyMap(), expressionType);
  }

  /**
   * Executes a set of expressions whose results need to be assigned to a variable.
   *
   * @param expressions Maps the name of a variable to the expression whose result should be assigned to it.
   * @param transientState Additional transient state provided to the expression.
   * @param expressionType The type of expression; init, update, result.  Provides additional context if expression execution fails.
   */
  private void assign(Map<String, String> expressions, Map<String, Object> transientState, String expressionType) {

    // for each expression...
    for(Map.Entry<String, String> entry : MapUtils.emptyIfNull(expressions).entrySet()) {
      String var = entry.getKey();
      String expr = entry.getValue();

      try {

        // assign the result of the expression to the variable
        executor.assign(var, expr, transientState);

      } catch (Throwable e) {

        // in-scope variables = persistent state maintained by the profiler + the transient state
        Set<String> variablesInScope = new HashSet<>();
        variablesInScope.addAll(transientState.keySet());
        variablesInScope.addAll(executor.getState().keySet());

        String msg = format("Bad '%s' expression: error='%s', expr='%s', profile='%s', entity='%s', variables-available='%s'",
                expressionType, e.getMessage(), expr, profileName, entity, variablesInScope);
        LOG.error(msg, e);
        throw new ParseException(msg, e);
      }
    }
  }

  /**
   * Executes the expressions contained within the profile definition.
   *
   * @param expressions A list of expressions to execute.
   * @param transientState Additional transient state provided to the expressions.
   * @param expressionType The type of expression; init, update, result.  Provides additional context if expression execution fails.
   * @return The result of executing each expression.
   */
  private List<Object> execute(List<String> expressions, Map<String, Object> transientState, String expressionType) {
    List<Object> results = new ArrayList<>();

    for(String expr: ListUtils.emptyIfNull(expressions)) {
      try {

        // execute an expression
        Object result = executor.execute(expr, transientState, Object.class);
        results.add(result);

      } catch (Throwable e) {

        // in-scope variables = persistent state maintained by the profiler + the transient state
        Set<String> variablesInScope = new HashSet<>();
        variablesInScope.addAll(transientState.keySet());
        variablesInScope.addAll(executor.getState().keySet());

        String msg = format("Bad '%s' expression: error='%s', expr='%s', profile='%s', entity='%s', variables-available='%s'",
                expressionType, e.getMessage(), expr, profileName, entity, variablesInScope);
        LOG.error(msg, e);
        throw new ParseException(msg, e);
      }
    }

    return results;
  }

  @Override
  public String getEntity() {
    return entity;
  }

  /**
   * A builder should be used to construct a new {@link ProfileBuilder} object.
   */
  public static class Builder {

    private ProfileConfig definition;
    private String entity;
    private Long periodDurationMillis;
    private Context context;

    public Builder withContext(Context context) {
      this.context = context;
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
        throw new IllegalArgumentException("missing profiler definition; got null");
      }
      if(StringUtils.isEmpty(entity)) {
        throw new IllegalArgumentException(format("missing entity name; got '%s'", entity));
      }
      if(periodDurationMillis == null) {
        throw new IllegalArgumentException("missing period duration");
      }

      return new DefaultProfileBuilder(definition, entity, periodDurationMillis, context);
    }
  }
}
