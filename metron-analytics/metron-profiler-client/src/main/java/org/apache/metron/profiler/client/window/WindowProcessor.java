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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.metron.profiler.client.window.generated.WindowBaseListener;
import org.apache.metron.profiler.client.window.generated.WindowLexer;
import org.apache.metron.profiler.client.window.generated.WindowParser;
import org.apache.metron.profiler.client.window.predicates.DayPredicates;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.dsl.ErrorListener;
import org.apache.metron.stellar.dsl.GrammarUtils;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Token;

/**
 * The WindowProcessor instance provides the parser callbacks for the Window selector language.  This constructs
 * a Window object to be used to compute sparse window intervals across time.
 */
public class WindowProcessor extends WindowBaseListener {
  private Throwable throwable;
  private Deque<Token<?>> stack;
  private static final Token<Object> LIST_MARKER = new Token<>(null, Object.class);
  private static final Token<Object> SPECIFIER_MARKER = new Token<>(null, Object.class);
  private Window window;

  public WindowProcessor() {
    this.stack = new ArrayDeque<>();
    this.window = new Window();
  }

  /**
   * Retrieve the window constructed from the window selector statement.
   * @return window returns the window constructed from the window selector statement
   */
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
   * If we see an identifier, an argument for an inclusion/exclusion predicate, then we want to just push it onto the
   * stack without its ':'.
   * @param ctx
   */
  @Override
  public void exitIdentifier(WindowParser.IdentifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    stack.push(new Token<>(ctx.getText().substring(1), String.class ));
  }

  /**
   * When we enter a specifier then we want to push onto the stack the specifier marker so we know when
   * the specifier parameters end.
   * @param ctx
   */
  @Override
  public void enterSpecifier(WindowParser.SpecifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    stack.push(SPECIFIER_MARKER);
  }

  /**
   * Read the specifier params off the stack in FIFO order until we get to the specifier marker.  Now we can
   * construct the specifier, which is a Function which constructs a Selector Predicate based on the args
   * passed to the selector e.g. holidays:us:nyc would have 2 args us and nyc.
   *
   * @param ctx
   */
  @Override
  public void exitSpecifier(WindowParser.SpecifierContext ctx) {
    LinkedList<String> args = new LinkedList<>();

    while (true) {
      Token<?> token = stack.pop();
      if (token == SPECIFIER_MARKER) {
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
   * Normalize the day specifier e.g. tuesdays -{@literal >} tuesday and push onto the stack.
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
   * When we're beginning an exclusion specifier list, then we push the list token so we
   * know when we're done processing
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
   * And when we're done with the exclusions specifier, then we set the exclusions
   * to the predicates we've put on the stack.
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
   * When we're beginning an inclusion specifier list, then we push the list token so we
   * know when we're done processing
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
   * And when we're done with the inclusions specifier, then we set the exclusions
   * to the predicates we've put on the stack.
   * @param ctx
   */
  @Override
  public void exitIncluding_specifier(WindowParser.Including_specifierContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    window.setIncludes(getPredicates());
  }

  private void setFromTo(long from, long to) {
    window.setEndMillis(now -> now - Math.min(to, from));
    window.setStartMillis(now -> now - Math.max(from, to));
  }

  /**
   * If we have a total time interval that we've specified, then we want to set the interval.
   * NOTE: the interval will be set based on the smallest to largest being the start and end time respectively.
   * Thus 'from 1 hour ago to 1 day ago' and 'from 1 day ago to 1 hour ago' are equivalent.
   * @param ctx
   */
  @Override
  public void exitFromToDuration(org.apache.metron.profiler.client.window.generated.WindowParser.FromToDurationContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> toInterval = stack.pop();
    Token<?> fromInterval = stack.pop();
    Long to = (Long)toInterval.getValue();
    Long from = (Long)fromInterval.getValue();
    setFromTo(from, to);
  }

  /**
   * When we've done specifying a from, then we want to set it.
   * @param ctx
   */
  @Override
  public void exitFromDuration(org.apache.metron.profiler.client.window.generated.WindowParser.FromDurationContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeInterval = stack.pop();
    Long from = (Long)timeInterval.getValue();
    setFromTo(from, 0);
  }

  /**
   * We've set a skip distance.
   * @param ctx
   */
  @Override
  public void exitSkipDistance(org.apache.metron.profiler.client.window.generated.WindowParser.SkipDistanceContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeInterval = stack.pop();
    Long width = (Long)timeInterval.getValue();
    window.setSkipDistance(width);
  }

  /**
   * We've set a window width.
   * @param ctx
   */
  @Override
  public void exitWindowWidth(org.apache.metron.profiler.client.window.generated.WindowParser.WindowWidthContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeInterval = stack.pop();
    Long width = (Long)timeInterval.getValue();
    window.setBinWidth(width);
    window.setStartMillis(now -> now - width);
    window.setEndMillis(now -> now);
  }

  /**
   * We've set a time interval, which is a value along with a unit.
   * @param ctx
   */
  @Override
  public void exitTimeInterval(org.apache.metron.profiler.client.window.generated.WindowParser.TimeIntervalContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    Token<?> timeUnit = stack.pop();
    Token<?> timeDuration = stack.pop();
    long duration = ConversionUtils.convert(timeDuration.getValue(), Long.class);
    TimeUnit unit = (TimeUnit) timeUnit.getValue();
    stack.push(new Token<>(unit.toMillis(duration), Long.class));
  }

  /**
   * We've set a time amount, which is integral.
   * @param ctx
   */
  @Override
  public void exitTimeAmount(org.apache.metron.profiler.client.window.generated.WindowParser.TimeAmountContext ctx) {
    if(checkForException(ctx)) {
      return;
    }
    if(ctx.getText().length() == 0) {
      throwable = new IllegalStateException("Unable to process empty string.");
      return;
    }
    long duration = Long.parseLong(ctx.getText());
    stack.push(new Token<>(duration, Long.class));
  }

  /**
   * We've set a time unit.  We support the timeunits provided by java.util.concurrent.TimeUnit
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
      return true;
    }
    else if(ctx.exception != null) {
      return true;
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

  private static TokenStream createTokenStream(String statement) {
    if (statement == null || isEmpty(statement.trim())) {
      return null;
    }
    statement = statement.trim();
    ANTLRInputStream input = new ANTLRInputStream(statement);
    WindowLexer lexer = new WindowLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ErrorListener());
    TokenStream tokens = new CommonTokenStream(lexer);
    return tokens;
  }

  private static WindowParser createParser(TokenStream tokens, Optional<WindowProcessor> windowProcessor) {
    WindowParser parser = new WindowParser(tokens);
    if(windowProcessor.isPresent()) {
      parser.addParseListener(windowProcessor.get());
    }
    parser.removeErrorListeners();
    parser.addErrorListener(new ErrorListener());
    return parser;
  }

  /**
   * Create a reusable Window object (parameterized by time) from a statement specifying the window intervals
   * conforming to the Window grammar.
   *
   * @param statement
   * @return Window returns a Window object (parameterized by time) from a statement specifying the window
   * intervals conforming to the Window grammar.
   * @throws ParseException
   */
  public static Window process(String statement) throws ParseException {
    TokenStream tokens = createTokenStream(statement);
    if(tokens == null) {
      return null;
    }
    WindowProcessor treeBuilder = new WindowProcessor();
    WindowParser parser = createParser(tokens, Optional.of(treeBuilder));
    parser.window();
    if(treeBuilder.throwable != null) {
      throw new ParseException(treeBuilder.throwable.getMessage(), treeBuilder.throwable);
    }
    return treeBuilder.getWindow();
  }

  /**
   * Create a textual representation of the syntax tree.  This is useful for those intrepid souls
   * who wish to extend the window selector language.  God speed.
   * @param statement
   * @return  A string representation of the syntax tree.
   */
  public static String syntaxTree(String statement) {
    TokenStream tokens = createTokenStream(statement);
    if(tokens == null) {
      return null;
    }
    WindowParser parser = createParser(tokens, Optional.empty());
    ParseTree tree = parser.window();
    return GrammarUtils.toSyntaxTree(tree) ;
  }
}
