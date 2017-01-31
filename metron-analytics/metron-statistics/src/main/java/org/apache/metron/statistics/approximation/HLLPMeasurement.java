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
package org.apache.metron.statistics.approximation;

import com.google.common.base.Joiner;
import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.metron.common.utils.SerDeUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Measure HLLP performance given variable cardinality and precision values
 */
public class HLLPMeasurement {
  public static final double NANO_TO_MILLIS = 1e6;
  public static final NumberFormat ERR_FORMAT = new DecimalFormat("#0.000");
  public static final NumberFormat TIME_FORMAT = new DecimalFormat("#0.000");
  public static final NumberFormat SIZE_FORMAT = new DecimalFormat("#0");
  public static final String CHART_SPACER = "-";

  public static void main(String[] args) {
    Options options = new Options();

    try {
      CommandLineParser parser = new PosixParser();
      CommandLine cmd = null;
      try {
        cmd = ParserOptions.parse(parser, args);
      } catch (ParseException pe) {
        pe.printStackTrace();
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("HLLPMeasurement", null, options, null, true);
        System.exit(-1);
      }
      if (cmd.hasOption("h")) {
        final HelpFormatter usageFormatter = new HelpFormatter();
        usageFormatter.printHelp("HLLPMeasurement", null, options, null, true);
        System.exit(0);
      }
      final String chartDelim = ParserOptions.CHART_DELIM.get(cmd, "|");

      final int numTrials = Integer.parseInt(ParserOptions.NUM_TRIALS.get(cmd, "5000"));
      final int cardMin = Integer.parseInt(ParserOptions.CARD_MIN.get(cmd, "200"));
      final int cardMax = Integer.parseInt(ParserOptions.CARD_MAX.get(cmd, "1000"));
      final int cardStep = Integer.parseInt(ParserOptions.CARD_STEP.get(cmd, "200"));
      final int cardStart = (((cardMin - 1) / cardStep) * cardStep) + cardStep;
      final int spMin = Integer.parseInt(ParserOptions.SP_MIN.get(cmd, "4"));
      final int spMax = Integer.parseInt(ParserOptions.SP_MAX.get(cmd, "32"));
      final int spStep = Integer.parseInt(ParserOptions.SP_STEP.get(cmd, "4"));
      final int pMin = Integer.parseInt(ParserOptions.P_MIN.get(cmd, "4"));
      final int pMax = Integer.parseInt(ParserOptions.P_MAX.get(cmd, "32"));
      final int pStep = Integer.parseInt(ParserOptions.P_STEP.get(cmd, "4"));
      final double errorPercentile = Double.parseDouble(ParserOptions.ERR_PERCENTILE.get(cmd, "50"));
      final double timePercentile = Double.parseDouble(ParserOptions.TIME_PERCENTILE.get(cmd, "50"));
      final double sizePercentile = Double.parseDouble(ParserOptions.SIZE_PERCENTILE.get(cmd, "50"));
      final boolean formatErrPercent = Boolean.parseBoolean(ParserOptions.ERR_FORMAT_PERCENT.get(cmd, "true"));
      final int errMultiplier = formatErrPercent ? 100 : 1;
      final Function<Double, String> errorFormatter = (v -> ERR_FORMAT.format(v * errMultiplier));
      final Function<Double, String> timeFormatter = (v -> TIME_FORMAT.format(v / NANO_TO_MILLIS));
      final Function<Double, String> sizeFormatter = (v -> SIZE_FORMAT.format(v));
      final String[] chartKey = new String[]{
              "card: cardinality",
              "sp: sparse precision value",
              "p: normal precision value",
              "err: error as a percent of the expected cardinality; ",
              "time: total time to add all values to the hllp estimator and calculate a cardinality estimate",
              "size: size of the hllp set in bytes once all values have been added for the specified cardinality",
              "l=low, m=mid(based on percentile chosen), h=high, std=standard deviation"};
      final String[] chartHeader = new String[]{"card", "sp", "p", "err l/m/h/std (% of actual)", "time l/m/h/std (ms)", "size l/m/h/std (b)"};
      final int[] chartPadding = new int[]{10, 10, 10, 40, 40, 30};
      if (spMin < pMin) {
        throw new IllegalArgumentException("p must be <= sp");
      }
      if (spMax < pMax) {
        throw new IllegalArgumentException("p must be <= sp");
      }
      println("Options Used");
      println("------------");
      println("num trials: " + numTrials);
      println("card min: " + cardMin);
      println("card max: " + cardMax);
      println("card step: " + cardStep);
      println("card start: " + cardStart);
      println("sp min: " + spMin);
      println("sp max: " + spMax);
      println("sp step: " + spStep);
      println("p min: " + pMin);
      println("p max: " + pMax);
      println("p step: " + pStep);
      println("error percentile: " + errorPercentile);
      println("time percentile: " + timePercentile);
      println("size percentile: " + sizePercentile);
      println("format err as %: " + formatErrPercent);
      println("");
      printHeading(chartKey, chartHeader, chartPadding, chartDelim);

      for (int c = cardStart; c <= cardMax; c += cardStep) {
        for (int sp = spMin; sp <= spMax; sp += spStep) {
          for (int p = pMin; p <= pMax; p += pStep) {
            DescriptiveStatistics errorStats = new DescriptiveStatistics();
            DescriptiveStatistics timeStats = new DescriptiveStatistics();
            DescriptiveStatistics sizeStats = new DescriptiveStatistics();
            for (int i = 0; i < numTrials; i++) {
              List<Object> trialSet = buildTrialSet(c);
              Set unique = new HashSet();
              unique.addAll(trialSet);
              long distinctVals = unique.size();
              HyperLogLogPlus hllp = new HyperLogLogPlus(p, sp);
              long timeStart = System.nanoTime();
              hllp.addAll(trialSet);
              long dvEstimate = hllp.cardinality();
              long timeEnd = System.nanoTime();
              long timeElapsed = timeEnd - timeStart;
              double rawError = Math.abs(dvEstimate - distinctVals) / (double) distinctVals;
              errorStats.addValue(rawError);
              timeStats.addValue(timeElapsed);
              sizeStats.addValue(SerDeUtils.toBytes(hllp).length);
            }
            MeasureResultFormatter errorRF = new MeasureResultFormatter(errorStats, errorFormatter, errorPercentile);
            MeasureResultFormatter timeRF = new MeasureResultFormatter(timeStats, timeFormatter, timePercentile);
            MeasureResultFormatter sizeRF = new MeasureResultFormatter(sizeStats, sizeFormatter, sizePercentile);

            println(formatWithPadding(new String[]{"" + c, "" + sp, "" + p, errorRF.getFormattedResults(), timeRF.getFormattedResults(), sizeRF.getFormattedResults()}, chartPadding, chartDelim));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static void printHeading(String[] key, String[] header, int[] chartPadding, String chartDelim) {
    printHeadingKey(key);
    printDescription();
    printHeaderRow(header, chartPadding, chartDelim);
    printChartSpacer(header.length, chartPadding, chartDelim, CHART_SPACER);
  }

  private static void printHeadingKey(String[] key) {
    println("Table Key");
    println("---------");
    for (String v : key) {
      println(v);
    }
    println("");
  }

  private static void printDescription() {
    println("Metrics Table");
    println("-------------");
  }

  private static void printHeaderRow(String[] header, int[] padding, String delim) {
    String headerPadded = formatWithPadding(header, padding, delim);
    println(headerPadded);
  }

  private static void printChartSpacer(int totlength, int[] padding, String delim, String spacerStr) {
    String[] spacer = new String[totlength];
    Arrays.fill(spacer, spacerStr);
    String spacerPadded = formatWithPadding(spacer, padding, delim, spacerStr);
    println(spacerPadded);
  }

  private static void println(String val) {
    System.out.println(val);
  }

  private static String formatWithPadding(String[] columns, int[] padding, String delim) {
    return formatWithPadding(columns, padding, delim, " ");
  }

  private static String formatWithPadding(String[] columns, int[] padding, String delim, String paddingStr) {
    StringBuilder sb = new StringBuilder();
    sb.append(delim);
    for (int i = 0; i < columns.length; i++) {
      sb.append(org.apache.commons.lang.StringUtils.rightPad(columns[i], padding[i], paddingStr));
      sb.append(delim);
    }
    return sb.toString();
  }

  private static class MeasureResultFormatter {
    private DescriptiveStatistics stats;
    private Function<Double, String> formatter;
    private double percentile;
    private String delim;
    private boolean showStd;
    private String std;

    private MeasureResultFormatter(DescriptiveStatistics stats, Function<Double, String> formatter, double percentile) {
      this(stats, formatter, percentile, true, " / ");
    }

    private MeasureResultFormatter(DescriptiveStatistics stats, Function<Double, String> formatter, double percentile, boolean showStd, String delim) {
      this.stats = stats;
      this.formatter = formatter;
      this.percentile = percentile;
      this.showStd = showStd;
      this.delim = delim;
    }

    public String getMin() {
      return formatter.apply(stats.getMin());
    }

    public String getPercentile() {
      return formatter.apply(stats.getPercentile(percentile));
    }

    public String getMax() {
      return formatter.apply(stats.getMax());
    }

    public MeasureResultFormatter showStandardDeviation(boolean showStd) {
      this.showStd = showStd;
      return this;
    }

    public String getFormattedResults() {
      if (showStd) {
        return Joiner.on(delim).join(getMin(), getPercentile(), getMax(), getStd());
      } else {
        return Joiner.on(delim).join(getMin(), getPercentile(), getMax());
      }
    }

    public String getStd() {
      return formatter.apply(stats.getStandardDeviation());
    }
  }

  private static List<Object> buildTrialSet(int cardinality) {
    List<Object> trialSet = new ArrayList(cardinality);
    for (int i = 0; i < cardinality; i++) {
      trialSet.add(Math.random());
    }
    return trialSet;
  }

  public enum ParserOptions {
    HELP("h", code -> {
      Option o = new Option(code, "help", false, "This screen");
      o.setRequired(false);
      return o;
    }),
    NUM_TRIALS("nt", code -> {
      Option o = new Option(code, "num_trials", true, "Number of trials to run. Default 1000");
      o.setArgName("NUM_TRIALS");
      o.setRequired(false);
      return o;
    }),
    CARD_MIN("cmn", code -> {
      Option o = new Option(code, "card_min", true, "Lowest cardinality to start running trials from. Default 100");
      o.setArgName("CARD_MIN");
      o.setRequired(false);
      return o;
    }),
    CARD_MAX("cmx", code -> {
      Option o = new Option(code, "card_max", true, "Max cardinality to run trials up to. Default 1000");
      o.setArgName("CARD_MAX");
      o.setRequired(false);
      return o;
    }),
    CARD_STEP("cs", code -> {
      Option o = new Option(code, "card_step", true, "Quantity to increment cardinality by for each successive measurement up until the cardinality high value. Default 100");
      o.setArgName("CARD_STEP");
      o.setRequired(false);
      return o;
    }),
    SP_MIN("spmn", code -> {
      Option o = new Option(code, "sp_min", true, "Minimum sparse precision to get measurements for. Default 4");
      o.setArgName("SP_MIN");
      o.setRequired(false);
      return o;
    }),
    SP_MAX("spmx", code -> {
      Option o = new Option(code, "sp_max", true, "Maximum sparse precision to get measurements for. Default 32");
      o.setArgName("SP_MAX");
      o.setRequired(false);
      return o;
    }),
    SP_STEP("sps", code -> {
      Option o = new Option(code, "sp_step", true, "Increment precision values by this step amount when running trials. Default 4");
      o.setArgName("SP_STEP");
      o.setRequired(false);
      return o;
    }),
    P_MIN("pmn", code -> {
      Option o = new Option(code, "p_min", true, "Minimum sparse precision to get measurements for. Default 4");
      o.setArgName("P_MIN");
      o.setRequired(false);
      return o;
    }),
    P_MAX("pmx", code -> {
      Option o = new Option(code, "p_max", true, "Maximum sparse precision to get measurements for. Default 32");
      o.setArgName("P_MAX");
      o.setRequired(false);
      return o;
    }),
    P_STEP("ps", code -> {
      Option o = new Option(code, "p_step", true, "Increment precision values by this step amount when running trials. Default 4");
      o.setArgName("P_STEP");
      o.setRequired(false);
      return o;
    }),
    CHART_DELIM("cd", code -> {
      Option o = new Option(code, "chart_delim", true, "Column delimiter for the chart. Default is pipe '|'");
      o.setArgName("CHART_DELIM");
      o.setRequired(false);
      return o;
    }),
    CHART_PADDING("cp", code -> {
      Option o = new Option(code, "chart_padding", true, "Amount of padding to use for each column. Default 20");
      o.setArgName("CHART_PADDING");
      o.setRequired(false);
      return o;
    }),
    ERR_PERCENTILE("ep", code -> {
      Option o = new Option(code, "error_percentile", true, "What percentile to calculate between min/max for error. Default is the median (50th percentile)");
      o.setArgName("ERR_PERCENTILE");
      o.setRequired(false);
      return o;
    }),
    TIME_PERCENTILE("tp", code -> {
      Option o = new Option(code, "time_percentile", true, "What percentile to calculate between min/max for time. Default is the median (50th percentile)");
      o.setArgName("ERR_PERCENTILE");
      o.setRequired(false);
      return o;
    }),
    SIZE_PERCENTILE("spt", code -> {
      Option o = new Option(code, "size_percentile", true, "What percentile to calculate between min/max for size. Default is the median (50th percentile)");
      o.setArgName("SIZE_PERCENTILE");
      o.setRequired(false);
      return o;
    }),
    ERR_FORMAT_PERCENT("efp", code -> {
      Option o = new Option(code, "error_format_percent", true, "Format error in percent instead of absolute terms. Default true.");
      o.setArgName("ERR_FORMAT_PERCENT");
      o.setRequired(false);
      return o;
    });

    Option option;
    String shortCode;

    ParserOptions(String shortCode, Function<String, Option> optionHandler) {
      this.shortCode = shortCode;
      this.option = optionHandler.apply(shortCode);
    }

    public boolean has(CommandLine cli) {
      return cli.hasOption(shortCode);
    }

    public String get(CommandLine cli) {
      return cli.getOptionValue(shortCode);
    }

    public String get(CommandLine cli, String defaultVal) {
      return has(cli) ? cli.getOptionValue(shortCode) : defaultVal;
    }

    public static CommandLine parse(CommandLineParser parser, String[] args) throws ParseException {
      try {
        CommandLine cli = parser.parse(getOptions(), args);
        if (HELP.has(cli)) {
          printHelp();
          System.exit(0);
        }
        return cli;
      } catch (ParseException e) {
        System.err.println("Unable to parse args: " + Joiner.on(' ').join(args));
        e.printStackTrace(System.err);
        printHelp();
        throw e;
      }
    }

    public static void printHelp() {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("HLLPMeasurement", getOptions());
    }

    public static Options getOptions() {
      Options ret = new Options();
      for (ParserOptions o : ParserOptions.values()) {
        ret.addOption(o.option);
      }
      return ret;
    }
  }
}
