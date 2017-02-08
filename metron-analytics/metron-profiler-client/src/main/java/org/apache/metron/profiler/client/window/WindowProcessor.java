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
package org.apache.metron.profiler.client.window;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.metron.common.dsl.ErrorListener;
import org.apache.metron.common.dsl.GrammarUtils;
import org.apache.metron.common.dsl.ParseException;
import org.apache.metron.common.dsl.Token;
import org.apache.metron.common.utils.ConversionUtils;
import org.apache.metron.profiler.client.window.generated.WindowBaseListener;
import org.apache.metron.profiler.client.window.generated.WindowLexer;
import org.apache.metron.profiler.client.window.generated.WindowParser;
import org.apache.metron.profiler.client.window.predicates.DayPredicates;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public class WindowProcessor extends WindowBaseListener {
  private Throwable throwable;
  private Stack<Token<?>> stack;
  private static final Token<Object> LIST_MARKER = new Token<>(null, Object.class);
  private static final Token<Object> DAY_SPECIFIER_MARKER = new Token<>(null, Object.class);
  private Window window;

  public WindowProcessor() {
    this.stack = new Stack<>();
    this.window = new Window();
  }

  public Window getWindow() {
    return window;
  }

  private void enterList() {
    stack.push(LIST_MARKER);
  }

  private List<Function<Long, Predicate<Long>>> getPredicates() {
    LinkedList<Function<Long, Predicate<Long>>> predicates = new LinkedList<>();
    while (true) {
      Token<?> token = stack.pop();
      if (token == LIST_MARKER) {
        break;
      } else {
        predicates.addFirst((Function<Long, Predicate<Long>>) token.getValue());
      }
    }
    return predicates;
  }


  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitIdentifier(WindowParser.IdentifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    stack.push(new Token<>(ctx.getText().substring(1), String.class));
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void enterSpecifier(WindowParser.SpecifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    stack.push(DAY_SPECIFIER_MARKER);
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitSpecifier(WindowParser.SpecifierContext ctx) {
    LinkedList<String> args = new LinkedList<>();

    while (true) {
      Token<?> token = stack.pop();
      if (token == DAY_SPECIFIER_MARKER) {
        break;
      } else {
        args.addFirst((String) token.getValue());
      }
    }
    String specifier = args.removeFirst();
    List<String> arg = args.size() > 0?args:new ArrayList<>();
    Function<Long, Predicate<Long>> predicate = null;
    try {
      if (specifier.equals("THIS DAY OF THE WEEK") || specifier.equals("THIS DAY OF WEEK")) {
        predicate = now -> DayPredicates.dayOfWeekPredicate(DayPredicates.getDayOfWeek(now));
      } else {
        final Predicate<Long> dayOfWeekPredicate = DayPredicates.create(specifier, arg);
        predicate = now -> dayOfWeekPredicate;
      }
      stack.push(new Token<>(predicate, Function.class));
    }
    catch(Throwable t) {
      throwable = t;
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitDay_specifier(WindowParser.Day_specifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    String specifier = ctx.getText().toUpperCase();
    if(specifier.length() == 0 && ctx.exception != null){
      IllegalStateException ise = new IllegalStateException("Invalid day specifier: " + ctx.getStart().getText(), ctx.exception);
      throwable = ise;
      throw ise;
    }
    if(specifier.endsWith("S")) {
      specifier = specifier.substring(0, specifier.length() - 1);
    }
    stack.push(new Token<>(specifier, String.class));
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void enterExcluding_specifier(WindowParser.Excluding_specifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    enterList();
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitExcluding_specifier(WindowParser.Excluding_specifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    window.setExcludes(getPredicates());
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void enterIncluding_specifier(WindowParser.Including_specifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    enterList();
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitIncluding_specifier(WindowParser.Including_specifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    window.setIncludes(getPredicates());
  }

  private void setFromTo(int from, int to) {
    window.setEndMillis(now -> now - Math.min(to, from));
    window.setStartMillis(now -> now - Math.max(from, to));
  }


  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitFromToDuration(org.apache.metron.profiler.client.window.generated.WindowParser.FromToDurationContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> toInterval = stack.pop();
    Token<?> fromInterval = stack.pop();
    Integer to = (Integer)toInterval.getValue();
    Integer from = (Integer)fromInterval.getValue();
    setFromTo(from, to);
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitFromDuration(org.apache.metron.profiler.client.window.generated.WindowParser.FromDurationContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeInterval = stack.pop();
    Integer from = (Integer)timeInterval.getValue();
    setFromTo(from, 0);
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitSkipDistance(org.apache.metron.profiler.client.window.generated.WindowParser.SkipDistanceContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeInterval = stack.pop();
    Integer width = (Integer)timeInterval.getValue();
    window.setSkipDistance(width);
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitWindowWidth(org.apache.metron.profiler.client.window.generated.WindowParser.WindowWidthContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeInterval = stack.pop();
    Integer width = (Integer)timeInterval.getValue();
    window.setBinWidth(width);
    window.setStartMillis(now -> now - width);
    window.setEndMillis(now -> now);
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitTimeInterval(org.apache.metron.profiler.client.window.generated.WindowParser.TimeIntervalContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeUnit = stack.pop();
    Token<?> timeDuration = stack.pop();
    int duration = ConversionUtils.convert(timeDuration.getValue(), Integer.class);
    TimeUnit unit = (TimeUnit) timeUnit.getValue();
    stack.push(new Token<>((int)unit.toMillis(duration), Integer.class));
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitTimeAmount(org.apache.metron.profiler.client.window.generated.WindowParser.TimeAmountContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    if(ctx.getText().length() == 0) {
      throwable = new IllegalStateException("Unable to parse empty string.");
      return;
    }
    int duration = Integer.parseInt(ctx.getText());
    stack.push(new Token<>(duration, Integer.class));
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   *
   * @param ctx
   */
  @Override
  public void exitTimeUnit(org.apache.metron.profiler.client.window.generated.WindowParser.TimeUnitContext ctx) {
    checkForException(ctx);
    switch(normalizeTimeUnit(ctx.getText())) {
      case "DAY":
        stack.push(new Token<>(TimeUnit.DAYS, TimeUnit.class));
        break;
      case "HOUR":
        stack.push(new Token<>(TimeUnit.HOURS, TimeUnit.class));
        break;
      case "MINUTE":
        stack.push(new Token<>(TimeUnit.MINUTES, TimeUnit.class));
        break;
      case "SECOND":
        stack.push(new Token<>(TimeUnit.SECONDS, TimeUnit.class));
        break;
      default:
        throw new IllegalStateException("Unsupported time unit: " + ctx.getText()
                + ".  Supported units are limited to: day, hour, minute, second "
                + "with any pluralization or capitalization.");
    }
  }

  private boolean checkForException(ParserRuleContext ctx) {
    if(throwable != null)  {
      //throw new ParseException(throwable.getMessage(), throwable);
      return true;
    }
    else if(ctx.exception != null) {
      return true;
      //throw new ParseException(ctx.exception.getMessage(), ctx.exception);
    }
    return false;
  }

  private static String normalizeTimeUnit(String s) {
    String ret = s.toUpperCase().replaceAll("[^A-Z]", "");
    if(ret.endsWith("S")) {
      return ret.substring(0, ret.length() - 1);
    }
    return ret;
  }

  public static Window parse(String statement) throws ParseException {
    if (statement == null || isEmpty(statement.trim())) {
      return null;
    }
    statement = statement.trim();
    ANTLRInputStream input = new ANTLRInputStream(statement);
    WindowLexer lexer = new WindowLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ErrorListener());
    TokenStream tokens = new CommonTokenStream(lexer);
    WindowParser parser = new WindowParser(tokens);
    WindowProcessor treeBuilder = new WindowProcessor();
    parser.addParseListener(treeBuilder);
    parser.removeErrorListeners();
    parser.addErrorListener(new ErrorListener());
    parser.window();
    return treeBuilder.getWindow();
  }

  public static String syntaxTree(String statement) {
    if (statement == null || isEmpty(statement.trim())) {
      return null;
    }
    statement = statement.trim();
    ANTLRInputStream input = new ANTLRInputStream(statement);
    WindowLexer lexer = new WindowLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ErrorListener());
    TokenStream tokens = new CommonTokenStream(lexer);
    WindowParser parser = new WindowParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(new ErrorListener());
    ParseTree tree = parser.window();
    return GrammarUtils.toSyntaxTree(tree) ;
  }
}
