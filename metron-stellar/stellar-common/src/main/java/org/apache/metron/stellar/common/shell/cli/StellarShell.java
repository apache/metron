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

package org.apache.metron.stellar.common.shell.cli;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.PropertyConfigurator;
import org.apache.metron.stellar.common.shell.DefaultStellarAutoCompleter;
import org.apache.metron.stellar.common.shell.DefaultStellarShellExecutor;
import org.apache.metron.stellar.common.shell.StellarAutoCompleter;
import org.apache.metron.stellar.common.shell.StellarResult;
import org.apache.metron.stellar.common.shell.StellarShellExecutor;
import org.apache.metron.stellar.common.utils.JSONUtils;
import org.apache.metron.stellar.dsl.StellarFunctions;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.complete.Completion;
import org.jboss.aesh.console.AeshConsoleCallback;
import org.jboss.aesh.console.Console;
import org.jboss.aesh.console.ConsoleOperation;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.TerminalCharacter;
import org.jboss.aesh.terminal.TerminalColor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.apache.metron.stellar.dsl.Context.Capabilities.CONSOLE;
import static org.apache.metron.stellar.dsl.Context.Capabilities.GLOBAL_CONFIG;
import static org.apache.metron.stellar.dsl.Context.Capabilities.SHELL_VARIABLES;

/**
 * A REPL environment for Stellar.
 *
 * Useful for debugging Stellar expressions.
 */
public class StellarShell extends AeshConsoleCallback implements Completion {

  static final String WELCOME = "Stellar, Go!\n" +
          "Functions are loading lazily in the background and will be unavailable until loaded fully.";

  private List<TerminalCharacter> EXPRESSION_PROMPT = new ArrayList<TerminalCharacter>()
  {{
    add(new TerminalCharacter('[', new TerminalColor(Color.RED, Color.DEFAULT)));
    add(new TerminalCharacter('S', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.BOLD));
    add(new TerminalCharacter('t', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.BOLD));
    add(new TerminalCharacter('e', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.BOLD));
    add(new TerminalCharacter('l', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.BOLD));
    add(new TerminalCharacter('l', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.BOLD));
    add(new TerminalCharacter('a', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.BOLD));
    add(new TerminalCharacter('r', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.BOLD));
    add(new TerminalCharacter(']', new TerminalColor(Color.RED, Color.DEFAULT)));
    add(new TerminalCharacter('>', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.UNDERLINE));
    add(new TerminalCharacter('>', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.UNDERLINE));
    add(new TerminalCharacter('>', new TerminalColor(Color.GREEN, Color.DEFAULT), CharacterType.UNDERLINE));
    add(new TerminalCharacter(' ', new TerminalColor(Color.DEFAULT, Color.DEFAULT)));
  }};

  public static final String ERROR_PROMPT = "[!] ";
  public static final String STELLAR_PROPERTIES_FILENAME = "stellar.properties";

  /**
   * Executes Stellar expressions for the shell environment.
   */
  private StellarShellExecutor executor;

  /**
   * The Aesh shell console.
   */
  private Console console;

  /**
   * Provides auto-complete functionality.
   */
  private StellarAutoCompleter autoCompleter;

  /**
   * Execute the Stellar REPL.
   */
  public static void main(String[] args) throws Exception {
    StellarShell shell = new StellarShell(args);
    shell.run();
  }

  /**
   * Create a Stellar REPL.
   * @param args The commmand-line arguments.
   */
  public StellarShell(String[] args) throws Exception {

    // define valid command-line options
    CommandLineParser parser = new PosixParser();
    Options options = defineCommandLineOptions();
    CommandLine commandLine = parser.parse(options, args);

    // print help
    if(commandLine.hasOption("h")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("stellar", options);
      System.exit(0);
    }

    // validate the command line options
    try {
      StellarShellOptionsValidator.validateOptions(commandLine);

    } catch(IllegalArgumentException e){
      System.err.println(e.getMessage());
      System.exit(1);
    }

    // setup logging, if specified
    if(commandLine.hasOption("l")) {
      PropertyConfigurator.configure(commandLine.getOptionValue("l"));
    }

    console = createConsole(commandLine);
    autoCompleter = new DefaultStellarAutoCompleter();
    Properties props = getStellarProperties(commandLine);
    executor = createExecutor(commandLine, console, props, autoCompleter);
    loadVariables(commandLine, executor);
    console.setPrompt(new Prompt(EXPRESSION_PROMPT));
    console.addCompletion(this);
    console.setConsoleCallback(this);
  }

  /**
   * @return The valid command line options.
   */
  private Options defineCommandLineOptions() {
    Options options = new Options();
    options.addOption(
            "z",
            "zookeeper",
            true,
            "Zookeeper URL fragment in the form [HOSTNAME|IPADDRESS]:PORT");
    options.addOption(
            "v",
            "variables",
            true,
            "File containing a JSON Map of variables");
    options.addOption(
            "irc",
            "inputrc",
            true,
            "File containing the inputrc if not the default ~/.inputrc");
    options.addOption(
            "na",
            "no_ansi",
            false,
            "Make the input prompt not use ANSI colors.");
    options.addOption(
            "h",
            "help",
            false,
            "Print help");
    options.addOption(
            "p",
            "properties",
            true,
            "File containing Stellar properties");
    Option log4j = new Option(
            "l",
            "log4j",
            true,
            "The log4j properties file to load");
    log4j.setArgName("FILE");
    log4j.setRequired(false);
    options.addOption(log4j);

    return options;
  }

  /**
   * Loads any variables defined in an external file.
   * @param commandLine The command line arguments.
   * @param executor The stellar executor.
   * @throws IOException
   */
  private static void loadVariables(
          CommandLine commandLine,
          StellarShellExecutor executor) throws IOException {

    if(commandLine.hasOption("v")) {

      // load variables defined in a file
      String variablePath = commandLine.getOptionValue("v");
      Map<String, Object> variables = JSONUtils.INSTANCE.load(
              new File(variablePath),
              JSONUtils.MAP_SUPPLIER);

      // for each variable...
      for(Map.Entry<String, Object> kv : variables.entrySet()) {
        String variable = kv.getKey();
        Object value = kv.getValue();

        // define the variable - no expression available
        executor.assign(variable, value, Optional.empty());
      }
    }
  }

  /**
   * Creates the Stellar execution environment.
   * @param commandLine The command line arguments.
   * @param console The console which drives the REPL.
   * @param properties Stellar properties.
   */
  private StellarShellExecutor createExecutor(
          CommandLine commandLine,
          Console console,
          Properties properties,
          StellarAutoCompleter autoCompleter) throws Exception {

    // setup zookeeper client
    Optional<String> zookeeperUrl = Optional.empty();
    if(commandLine.hasOption("z")) {
      zookeeperUrl = Optional.of(commandLine.getOptionValue("z"));
    }

    StellarShellExecutor executor = new DefaultStellarShellExecutor(properties, zookeeperUrl);

    // the 'CONSOLE' capability is only available with the CLI REPL
    executor.getContext().addCapability(CONSOLE, () -> console);

    // allows some Stellar functions to access Stellar internals; should probably use %magics instead
    executor.getContext().addCapability(SHELL_VARIABLES, () -> executor.getState());

    // register the auto-completer to be notified when needed
    executor.addSpecialListener(   (special) -> autoCompleter.addCandidateFunction(special.getCommand()));
    executor.addFunctionListener( (function) -> autoCompleter.addCandidateFunction(function.getName()));
    executor.addVariableListener((name, val) -> autoCompleter.addCandidateVariable(name));

    executor.init();
    return executor;
  }

  /**
   * Creates the REPL's console.
   * @param commandLine The command line options.
   */
  private Console createConsole(CommandLine commandLine) {

    // console settings
    boolean useAnsi = !commandLine.hasOption("na");
    SettingsBuilder settings = new SettingsBuilder().enableAlias(true)
                                                    .enableMan(true)
                                                    .ansi(useAnsi)
                                                    .parseOperators(false)
                                                    .inputStream(PausableInput.INSTANCE);

    if(commandLine.hasOption("irc")) {
      settings = settings.inputrc(new File(commandLine.getOptionValue("irc")));
    }

    return new Console(settings.create());
  }

  /**
   * Retrieves the Stellar properties. The properties are either loaded from a file in
   * the classpath or a set of defaults are used.
   */
  private Properties getStellarProperties(CommandLine commandLine) throws IOException {
    Properties properties = new Properties();

    if (commandLine.hasOption("p")) {
      // attempt to load properties from a file specified on the command-line
      try (InputStream in = new FileInputStream(commandLine.getOptionValue("p"))) {
        if(in != null) {
          properties.load(in);
        }
      }

    } else {
      // otherwise attempt to load properties from the classpath
      try (InputStream in = getClass().getClassLoader().getResourceAsStream(STELLAR_PROPERTIES_FILENAME)) {
        if(in != null) {
          properties.load(in);
        }
      }
    }

    return properties;
  }

  /**
   * Handles the main loop for the REPL.
   */
  public void run() {
    // welcome message
    writeLine(WELCOME);

    // print the globals if we got 'em
    executor.getContext()
            .getCapability(GLOBAL_CONFIG, false)
            .ifPresent(conf -> writeLine(conf.toString()));

    console.start();
  }

  /**
   * Quits the console.
   */
  private void handleQuit() {
    try {
      console.stop();
      StellarFunctions.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void writeLine(String out) {
    console.getShell().out().println(out);
  }

  @Override
  public int execute(ConsoleOperation output) throws InterruptedException {

    // grab the user the input
    String expression = StringUtils.trimToEmpty(output.getBuffer());
    if(StringUtils.isNotBlank(expression) ) {

      // execute the expression
      StellarResult result = executor.execute(expression);

      if(result.isSuccess()) {
        // on success
        result.getValue().ifPresent(v -> writeLine(v.toString()));

      } else if (result.isError()) {
        // on error
        result.getException().ifPresent(e -> writeLine(ERROR_PROMPT + e.getMessage()));
        result.getException().ifPresent(e -> e.printStackTrace());

      } else if(result.isTerminate()) {
        // on quit
        handleQuit();

      } else {
        // should never happen
        throw new IllegalStateException("An execution result is neither a success nor a failure. Please file a bug report.");
      }
    }

    return 0;
  }

  /**
   * Performs auto-completion for the shell.
   * @param completeOperation The auto-complete operation.
   */
  @Override
  public void complete(CompleteOperation completeOperation) {
    String buffer = completeOperation.getBuffer();
    final String lastToken = getLastToken(buffer);
    Iterable<String> candidates = autoCompleter.autoComplete(buffer);

    // transform the candidates into valid completions
    if(candidates != null && !Iterables.isEmpty(candidates)) {
      for(String candidate : candidates) {
        String completion = stripOff(buffer, lastToken) + candidate;
        completeOperation.addCompletionCandidate(completion);
      }
    }
  }

  private static String getLastToken(String buffer) {
    String lastToken = Iterables.getLast(Splitter.on(" ").split(buffer), null);
    return lastToken.trim();
  }

  private static String stripOff(String baseString, String lastBit) {
    int index = baseString.lastIndexOf(lastBit);
    if(index < 0) {
      return baseString;
    }
    return baseString.substring(0, index);
  }

  /**
   * @return The executor of Stellar expressions.
   */
  public StellarShellExecutor getExecutor() {
    return executor;
  }

  /**
   * @return The console.
   */
  public Console getConsole() {
    return console;
  }
}
