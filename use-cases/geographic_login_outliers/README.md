<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
# Problem Statement

One way to find anomalous behavior in a network is by inspecting user
login behavior.  In particular, if a user is logging in via vastly
differing geographic locations in a short period of time, this may be
evidence of malicious behavior.

More formally, we can encode this potentially malicious event in terms
of how far from the geographic centroid of the user's historic logins
as compared to all users.  For instance, if we track all users and the
median distance from the central geographic location of all of their
logins for the last 2 hours is 3 km and the standard deviation is 1 km,
if we see a user logging in 1700 km from the central geographic location of
their logins for the last 2 hours, then they MAY be exhibiting a
deviation that we want to monitor since it would be hard to travel that
distance in 4 hours.  On the other hand, the user may have
just used a VPN or proxy.  Ultimately, this sort of analytic must be
considered only one piece of evidence in addition to many others before
we want to indicate an alert.

# Demonstration Design
For the purposes of demonstration, we will construct synthetic data
whereby 2 users are logging into a system rather quickly (once per
second) from various hosts.  Each user's locations share the same first
2 octets, but will choose the last 2 randomly.  We will then inject a
data point indicating `user1` is logging in via a russian IP address.

## Preliminaries
We assume that the following environment variables are set:
* `METRON_HOME` - the home directory for metron
* `ZOOKEEPER` - The zookeeper quorum (comma separated with port specified: e.g. `node1:2181` for full-dev)
* `BROKERLIST` - The Kafka broker list (comma separated with port specified: e.g. `node1:6667` for full-dev)
* `ES_HOST` - The elasticsearch master (and port) e.g. `node1:9200` for full-dev.

Also, this does not assume that you are using a kerberized cluster.  If you are, then the parser start command will adjust slightly to include the security protocol.

Before editing configurations, be sure to pull the configs from zookeeper locally via
```
$METRON_HOME/bin/zk_load_configs.sh --mode PULL -z $ZOOKEEPER -o $METRON_HOME/config/zookeeper/ -f
```

## Configure the Profiler
First, we'll configure the profiler to emit a profiler every 1 minute:
* In Ambari, set the profiler period duration to `1` minute via the Profiler config section.
* Adjust `$METRON_HOME/config/zookeeper/global.json` to adjust the capture duration:
```
 "profiler.client.period.duration" : "1",
 "profiler.client.period.duration.units" : "MINUTES"
```

## Create the Data Generator
We want to create a new sensor for our synthetic data called `auth`.  To
feed it, we need a synthetic data generator.  In particular, we want a
process which will feed authentication events per second for a set of
users where the IPs are randomly chosen, but each user's login ip
addresses share the same first 2 octets.

Edit `~/gen_data.py` and paste the following into it:
```
#!/usr/bin/python

import random
import sys
import time

domains = { 'user1' : '173.90', 'user2' : '156.33' }

def get_ip(base):
  return base + '.' + str(random.randint(1,255)) + '.' + str(random.randint(1, 255))

def main():
  freq_s = 1
  while True:
    user='user' + str(random.randint(1,len(domains)))
    epoch_time = int(time.time())
    ip=get_ip(domains[user])
    print user + ',' + ip + ',' + str(epoch_time)
    sys.stdout.flush()
    time.sleep(freq_s)

if __name__ == '__main__':
  main()
```

## Create the `auth` Parser

The message format for our simple synthetic data is a CSV with:
* username
* login ip address
* timestamp

We will need to parse this via our `CSVParser` and add the geohash of the login ip address.

* To create this parser, edit `$METRON_HOME/config/zookeeper/parsers/auth.json` and paste the following:
```
{
  "parserClassName" : "org.apache.metron.parsers.csv.CSVParser"
 ,"sensorTopic" : "auth"
 ,"parserConfig" : {
    "columns" : {
      "user" : 0,
      "ip" : 1,
      "timestamp" : 2
                }
                   }
 ,"fieldTransformations" : [
    {
    "transformation" : "STELLAR"
   ,"output" : [ "hash" ]
   ,"config" : {
      "hash" : "GEOHASH_FROM_LOC(GEO_GET(ip))"
               }
    }
                           ]
}
```
* Create the kafka topic via:
```
/usr/hdp/current/kafka-broker/bin/kafka-topics.sh --zookeeper $ZOOKEEPER --create --topic auth --partitions 1 --replication-factor 1
```

## Create the Profiles for Enrichment

We will need to track 2 profiles to accomplish this task:
* `locations_by_user` - The geohashes of the locations the user has logged in from.  This is a multiset of geohashes per user.  Note that the multiset in this case is effectively a map of geohashes to occurrance counts.
* `geo_distribution_from_centroid` - The statistical distribution of the distance between a login location and the geographic centroid of the user's previous logins from the last 2 minutes. Note, in a real installation this would be a larger temporal lookback.

We can represent these in the `$METRON_HOME/config/zookeeper/profiler.json` via the following:
```
{
  "profiles": [
    {
      "profile": "geo_distribution_from_centroid",
      "foreach": "'global'",
      "onlyif": "geo_distance != null",
      "init" : {
        "s": "STATS_INIT()"
               },
      "update": {
        "s": "STATS_ADD(s, geo_distance)"
                },
      "result": "s"
    },
    {
      "profile": "locations_by_user",
      "foreach": "user",
      "onlyif": "hash != null && LENGTH(hash) > 0",
      "init" : {
        "s": "MULTISET_INIT()"
               },
      "update": {
        "s": "MULTISET_ADD(s, hash)"
                },
      "result": "s"
    }
  ]
}
```

## Enrich authentication Events

We will need to enrich the authentication records in a couple of ways to use in the threat triage section as well as the profiles:
* `geo_distance`: representing the distance between the current geohash and the geographic centroid for the last 2 minutes.
* `geo_centroid`: representing the geographic centroid for the last 2 minutes

Beyond that, we will need to determine if the authentication event is a geographic outlier by computing the following fields:
* `dist_median` : representing the median distance between a user's login location and the geographic centroid for the last 2 minutes (essentially the median of the `geo_distance` values across all users).
* `dist_sd` : representing the standard deviation of the distance between a user's login location and the geographic centroid for the last 2 minutes (essentially the standard deviation of the `geo_distance` values across all users).
* `geo_outlier` : whether `geo_distance` is more than 5 standard deviations from the median across all users.

We also want to set up a triage rule associating a score and setting an alert if `geo_outlier` is true.  In reality, this would be more complex as this metric is at best circumstantial and would need supporting evidence, but for simplicity we'll deal with the false positives.

* Edit `$METRON_HOME/config/zookeeper/enrichments/auth.json` and paste the following:
```
{
  "enrichment": {
    "fieldMap": {
      "stellar" : {
        "config" : [
          "geo_locations := MULTISET_MERGE( PROFILE_GET( 'locations_by_user', user, PROFILE_FIXED( 2, 'MINUTES')))",
          "geo_centroid := GEOHASH_CENTROID(geo_locations)",
          "geo_distance := TO_INTEGER(GEOHASH_DIST(geo_centroid, hash))",
          "geo_locations := null"
        ]
      }
    }
  ,"fieldToTypeMap": { }
  },
  "threatIntel": {
    "fieldMap": {
      "stellar" : {
        "config" : [
          "geo_distance_distr:= STATS_MERGE( PROFILE_GET( 'geo_distribution_from_centroid', 'global', PROFILE_FIXED( 2, 'MINUTES')))",
          "dist_median := STATS_PERCENTILE(geo_distance_distr, 50.0)",
          "dist_sd := STATS_SD(geo_distance_distr)",
          "geo_outlier := ABS(dist_median - geo_distance) >= 5*dist_sd",
          "is_alert := is_alert || (geo_outlier != null && geo_outlier == true)",
          "geo_distance_distr := null"
        ]
      }

    },
    "fieldToTypeMap": { },
    "triageConfig" : {
      "riskLevelRules" : [
        {
          "name" : "Geographic Outlier",
          "comment" : "Determine if the user's geographic distance from the centroid of the historic logins is an outlier as compared to all users.",
          "rule" : "geo_outlier != null && geo_outlier",
          "score" : 10,
          "reason" : "FORMAT('user %s has a distance (%d) from the centroid of their last login is 5 std deviations (%f) from the median (%f)', user, geo_distance, dist_sd, dist_median)"
        }
      ],
      "aggregator" : "MAX"
    }
  }
}
```

## Execute Demonstration

From here, we've set up our configuration and can push the configs:
* Push the configs to zookeeper via
```
$METRON_HOME/bin/zk_load_configs.sh --mode PUSH -z $ZOOKEEPER -i $METRON_HOME/config/zookeeper/
```
* Start the parser via:
```
$METRON_HOME/bin/start_parser_topology.sh -k $BROKERLIST -z $ZOOKEEPER -s auth
```
* Push synthetic data into the `auth` topic via
```
python ~/gen_data.py |
/usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list $BROKERLIST --topic auth
```
* Wait for about `5` minutes and kill the previous command
* Push a synthetic record indicating `user1` has logged in from a russian IP (`109.252.227.173`):
```
echo -e "import time\nprint 'user1,109.252.227.173,'+str(int(time.time()))" | python | /usr/hdp/current/kafka-broker/bin/kafka-console-producer.sh --broker-list $BROKERLIST --topic auth
```
* Execute the following to search elasticsearch for our geographic login outliers: 
```
curl -XPOST "http://$ES_HOST/auth*/_search?pretty" -d '
{
  "_source" : [ "is_alert", "threat:triage:rules:0:reason", "user", "ip", "geo_distance" ],
  "query": { "exists" : { "field" : "threat:triage:rules:0:reason" } }
}
'
```

You should see, among a few other false positive results, something like the following:
```
{
  "_index" : "auth_index_2017.09.07.20",
    "_type" : "auth_doc",
    "_id" : "f5bdbf76-9d78-48cc-b21d-bc434c96e62e",
    "_score" : 1.0,
    "_source" : {
      "geo_distance" : 7879,
      "threat:triage:rules:0:reason" : "user user1 has a distance (7879) from the centroid of their last login is 5 std deviations (334.814719) from the median (128.000000)",
      "ip" : "109.252.227.173",
      "is_alert" : "true",
      "user" : "user1"
    }
}
```

