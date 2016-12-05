# Statistics and Mathematical Functions

A variety of non-trivial and advanced analytics make use of statistics
and advanced mathematical functions.  Particular, capturing the
statistical snapshots in a scalable way can open up doors for more
advanced analytics such as outlier analysis.  As such, this project is
aimed at capturing a robust set of statistical functions and
statistical-based algorithms in the form of Stellar functions.  These
functions can be used from everywhere where Stellar is used.

##Stellar Functions

### Mathematical Functions
* `ABS`
  * Description: Returns the absolute value of a number.
  * Input:
    * number - The number to take the absolute value of
  * Returns: The absolute value of the number passed in.


### Distributional Statistics

* `STATS_ADD`
  * Description: Adds one or more input values to those that are used to calculate the summary statistics.
  * Input:
    * stats - The Stellar statistics object.  If null, then a new one is initialized.
    * value+ - One or more numbers to add
  * Returns: A Stellar statistics object
* `STATS_COUNT`
  * Description: Calculates the count of the values accumulated (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object
  * Returns: The count of the values in the window or NaN if the statistics object is null.
* `STATS_GEOMETRIC_MEAN`
  * Description: Calculates the geometric mean of the accumulated values (or in the window if a window is used). See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The geometric mean of the values in the window or NaN if the statistics object is null.
* `STATS_INIT`
  * Description: Initializes a statistics object
  * Input:
    * window_size - The number of input data values to maintain in a rolling window in memory.  If window_size is equal to 0, then no rolling window is maintained. Using no rolling window is less memory intensive, but cannot calculate certain statistics like percentiles and kurtosis.
  * Returns: A Stellar statistics object
* `STATS_KURTOSIS`
  * Description: Calculates the kurtosis of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The kurtosis of the values in the window or NaN if the statistics object is null.
* `STATS_MAX`
  * Description: Calculates the maximum of the accumulated values (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object
  * Returns: The maximum of the accumulated values in the window or NaN if the statistics object is null.
* `STATS_MEAN`
  * Description: Calculates the mean of the accumulated values (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object
  * Returns: The mean of the values in the window or NaN if the statistics object is null.
* `STATS_MERGE`
  * Description: Merges statistics objects.
  * Input:
    * statistics - A list of statistics objects
  * Returns: A Stellar statistics object
* `STATS_MIN`
  * Description: Calculates the minimum of the accumulated values (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object
  * Returns: The minimum of the accumulated values in the window or NaN if the statistics object is null.
* `STATS_PERCENTILE`
  * Description: Computes the p'th percentile of the accumulated values (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object
    * p - a double where 0 <= p < 1 representing the percentile
  * Returns: The p'th percentile of the data or NaN if the statistics object is null
* `STATS_POPULATION_VARIANCE`
  * Description: Calculates the population variance of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The population variance of the values in the window or NaN if the statistics object is null.
* `STATS_QUADRATIC_MEAN`
  * Description: Calculates the quadratic mean of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The quadratic mean of the values in the window or NaN if the statistics object is null.
* `STATS_SD`
  * Description: Calculates the standard deviation of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The standard deviation of the values in the window or NaN if the statistics object is null.
* `STATS_SKEWNESS`
  * Description: Calculates the skewness of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The skewness of the values in the window or NaN if the statistics object is null.
* `STATS_SUM`
  * Description: Calculates the sum of the accumulated values (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object
  * Returns: The sum of the values in the window or NaN if the statistics object is null.
* `STATS_SUM_LOGS`
  * Description: Calculates the sum of the (natural) log of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The sum of the (natural) log of the values in the window or NaN if the statistics object is null.
* `STATS_SUM_SQUARES`
  * Description: Calculates the sum of the squares of the accumulated values (or in the window if a window is used).
  * Input:
    * stats - The Stellar statistics object
  * Returns: The sum of the squares of the values in the window or NaN if the statistics object is null.
* `STATS_VARIANCE`
  * Description: Calculates the variance of the accumulated values (or in the window if a window is used).  See http://commons.apache.org/proper/commons-math/userguide/stat.html#a1.2_Descriptive_statistics 
  * Input:
    * stats - The Stellar statistics object
  * Returns: The variance of the values in the window or NaN if the statistics object is null.

### Statistical Outlier Detection

* `OUTLIER_MAD_STATE_MERGE`
  * Description: Update the statistical state required to compute the Median Absolute Deviation.
  * Input:
    * [state] - A list of Median Absolute Deviation States to merge.  Generally these are states across time.
    * currentState? - The current state (optional)
  * Returns: The Median Absolute Deviation state
* `OUTLIER_MAD_ADD`
  * Description: Add a piece of data to the state.
  * Input:
    * state - The MAD state
    * value - The numeric value to add
  * Returns: The MAD state
* `OUTLIER_MAD_SCORE`
  * Description: Get the modified z-score normalized by the MAD: scale * | x_i - median(X) | / MAD.  See the first page of http://web.ipac.caltech.edu/staff/fmasci/home/astro_refs/BetterThanMAD.pdf
  * Input:
    * state - The MAD state
    * value - The numeric value to score
    * scale? - Optionally the scale to use when computing the modified z-score.  Default is `0.6745`, see the first page of http://web.ipac.caltech.edu/staff/fmasci/home/astro_refs/BetterThanMAD.pdf
  * Returns: The modified z-score 

# Outlier Analysis

A common desire is to find anomalies in numerical data.  To that end,
we have some simple statistical anomaly detectors.

## Median Absolute Deviation

Much has been written about this robust estimator.  See the first page
of http://web.ipac.caltech.edu/staff/fmasci/home/astro_refs/BetterThanMAD.pdf
for a good coverage of the good and the bad of MAD.  The usage, however
is fairly straightforward:
* Gather the statistical state required to compute the MAD
  * The distribution of the values of a univariate random variable over time.
  * The distribution of the absolute deviations of the values from the median.
* Use this statistical state to score unseen values.  The higher the score, the more unlike the previously seen data the value is.

There are a couple of issues which make MAD a bit hard to compute.
First, the statistical state requires computing median, which can be
computationally expensive to compute exactly.  To get around this, we
use the OnlineStatisticalProvider to compute a sketch rather than the
exact median.  Secondly, the statistical state for seasonal data should
be limited to a fixed, trailing window.  We do this by ensuring that the
MAD state is mergeable and able to be queried from within the Profiler.

### Example

We will create a dummy data stream of gaussian noise to illustrate how
to use the MAD functionality along with the profiler to tag messages as
outliers or not.

To do this, we will create a 
* data generator
* parser
* profiler profile
* enrichment and threat triage

#### Data Generator

We can create a simple python script to generate a stream of gaussian
noise at the frequency of one message per second as a python script
which should be saved at `~/rand_gen.py`:
```
#!/usr/bin/python
import random
import sys
import time
def main():
  mu = float(sys.argv[1])
  sigma = float(sys.argv[2])
  freq_s = int(sys.argv[3])
  while True:
    print str(random.gauss(mu, sigma))
    sys.stdout.flush()
    time.sleep(freq_s)

if __name__ == '__main__':
  main()
```

This script will take the following as arguments:
* The mean of the data generated
* The standard deviation of the data generated
* The frequency (in seconds) of the data generated

If, however, you'd like to test a longer tailed distribution, like the
student t-distribution and have numpy installed, you can use the
following as `~/rand_gen.py`:
```
#!/usr/bin/python
import random
import sys
import time
import numpy as np

def main():
  df = float(sys.argv[1])
  freq_s = int(sys.argv[2])
  while True:
    print str(np.random.standard_t(df))
    sys.stdout.flush()
    time.sleep(freq_s)

if __name__ == '__main__':
  main()
```

This script will take the following as arguments:
* The degrees of freedom for the distribution
* The frequency (in seconds) of the data generated

#### The Parser

We will create a parser that will take the single numbers in and create
a message with a field called `value` in them using the `CSVParser`.

Add the following file to
`$METRON_HOME/config/zookeeper/parsers/mad.json`:
```
{
  "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
 ,"sensorTopic" : "mad"
 ,"parserConfig" : {
    "columns" : {
      "value_str" : 0
                }
                   }
 ,"fieldTransformations" : [
    {
    "transformation" : "STELLAR"
   ,"output" : [ "value" ]
   ,"config" : {
      "value" : "TO_DOUBLE(value_str)"
               }
    }
                           ]
}
```

#### Enrichment and Threat Intel

We will set a threat triage level of `10` if a message generates a outlier score of more than 3.5.
This cutoff will depend on your data and should be adjusted based on the
assumed underlying distribution.  Note that under the assumptions of
normality, MAD will act as a robust estimator of the standard deviation, so the cutoff
should be considered the number of standard deviations away.  For other
distributions, there are other interpretations which will make sense in
the context of measuring the "degree different".  See
http://eurekastatistics.com/using-the-median-absolute-deviation-to-find-outliers/
for a brief discussion of this.

Create the following in
`$METRON_HOME/config/zookeeper/enrichments/mad.json`:

```
{
  "index": "mad",
  "batchSize": 1,
  "enrichment": {
    "fieldMap": {
      "stellar" : {
        "config" : {
          "parser_score" : "OUTLIER_MAD_SCORE(OUTLIER_MAD_STATE_MERGE(
PROFILE_GET( 'sketchy_mad', 'global', 10, 'MINUTES') ), value)"
         ,"is_alert" : "if parser_score > 3.5 then true else is_alert"
        }
      }
    }
  ,"fieldToTypeMap": { }
  },
  "threatIntel": {
    "fieldMap": { },
    "fieldToTypeMap": { },
    "triageConfig" : {
      "riskLevelRules" : {
        "parser_score > 3.5" : 10
      },
      "aggregator" : "MAX"
    }
  }
}
```

#### The Profiler

We can set up the profiler to track the MAD statistical state required
to compute MAD.  For the purposes of this demonstration, we will
configure the profiler to capture statistics on the minute mark.  We
will capture a global statistical state for the `value` field and we
will look back for a 5 minute window when computing the median.

Create the following file at
`$METRON_HOME/config/zookeeper/profiler.json`:

```
{
  "profiles": [
    {
      "profile": "sketchy_mad",
      "foreach": "'global'",
      "onlyif": "true",
      "init" : {
        "s": "OUTLIER_MAD_STATE_MERGE(PROFILE_GET('sketchy_mad',
'global', 5, 'MINUTES'))"
               },
      "tickUpdate": {
        "s": "OUTLIER_MAD_STATE_MERGE(PROFILE_GET('sketchy_mad',
'global', 5, 'MINUTES'), s)"
                },
      "update": {
        "s": "OUTLIER_MAD_ADD(s, value)"
                },
      "result": "s"
    }
  ]
}
```

Adjust `$METRON_HOME/config/zookeeper/global.json` to adjust the capture duration:
```
 "profiler.client.period.duration" : "1",
 "profiler.client.period.duration.units" : "MINUTES"
```

Adjust `$METRON_HOME/config/profiler.properties` to adjust the capture
duration by changing `profiler.period.duration=15` to `profiler.period.duration=1`

#### Execute the Flow

1. Install the elasticsearch head plugin by executing:
`/usr/share/elasticsearch/bin/plugin install mobz/elasticsearch-head`

2. Stopping all other parser topologies via monit

3. Create the `mad` kafka topic by executing:
`/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --zookeeper node1:2181 --create --topic mad --partitions 1 --replication-factor 1`

4. Push the modified configs by executing:
`$METRON_HOME/bin/zk_load_configs.sh --mode PUSH -z node1:2181 -i $METRON_HOME/config/zookeeper/`

5. Start the profiler by executing:
`$METRON_HOME/bin/start_profiler_topology.sh`

6. Start the parser topology by executing:
`$METRON_HOME/bin/start_parser_topology.sh -k node1:6667 -z node1:2181 -s mad`

7. Ensure that the enrichment and indexing topologies are started.  If not, then start those via monit or by hand.

8. Generate data into kafka by executing the following for at least 10 minutes:
`~/rand_gen.py 0 1 1 | /usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list node1:6667 --topic mad`
Note: if you chose the use the t-distribution script above, you would adjust the parameters of the `rand_gen.py` script accordingly.

9. Stop the above with ctrl-c and send in an obvious outlier into kafka:
`echo "1000" | /usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list node1:6667 --topic mad`

You should be able to find the outlier via the elasticsearch head plugin by
searching for the messages where `is_alert` is `true`.
