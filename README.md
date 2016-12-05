[![Build Status](https://travis-ci.org/apache/incubator-metron.svg?branch=master)](https://travis-ci.org/apache/incubator-metron)

# Apache Metron (Incubating)
 
Metron integrates a variety of open source big data technologies in order
to offer a centralized tool for security monitoring and analysis. Metron
provides capabilities for log aggregation, full packet capture indexing,
storage, advanced behavioral analytics and data enrichment, while applying
the most current threat intelligence information to security telemetry
within a single platform.
 
Metron can be divided into 4 areas:

1. **A mechanism to capture, store, and normalize any type of security
telemetry at extremely high rates.**  Because security telemetry is constantly
being generated, it requires a method for ingesting the data at high speeds 
and pushing it to various processing units for advanced computation and analytics.  

2. **Real time processing and application of enrichments** such as threat
intelligence, geolocation, and DNS information to telemetry being collected.
The immediate application of this information to incoming telemetry provides
the context and situational awareness, as well as the who and where 
information critical for investigation

3. **Efficient information storage** based on how the information will be used:

   - Logs and telemetry are stored such that they can be efficiently mined and
analyzed for concise security visibility
   - The ability to extract and reconstruct full packets helps an analyst answer 
questions such as who the true attacker was, what data was leaked, and where 
that data was sent
   - Long-term storage not only increases visibility over time, but also enables 
advanced analytics such as machine learning techniques to be used to create 
models on the information.  Incoming data can then be scored against these 
stored models for advanced anomaly detection.  

4. **An interface that gives a security investigator a centralized view of data 
and alerts passed through the system.**  Metron’s interface presents alert 
summaries with threat intelligence and enrichment data specific to that alert 
on one single page.  Furthermore, advanced search capabilities and full packet 
extraction tools are presented to the analyst for investigation without the 
need to pivot into additional tools.   

Big data is a natural fit for powerful security analytics. The Metron
framework integrates a number of elements from the Hadoop ecosystem to provide
a scalable platform for security analytics, incorporating such functionality as
full-packet capture, stream processing, batch processing, real-time search, and
telemetry aggregation.  With Metron, our goal is to tie big data into security
analytics and drive towards an extensible centralized platform to effectively
enable rapid detection and rapid response for advanced security threats.  

# Obtaining Metron

This repository is a collection of submodules for convenience which is regularly
updated to point to the latest versions. Github provides multiple ways to obtain
Metron's code:

1. git clone --recursive https://github.com/apache/incubator-metron
2. [Download ZIP](https://github.com/apache/incubator-metron/archive/master.zip)
3. Clone or download each repository individually

Option 3 is more likely to have the latest code.

# Building Metron

Build the full project and run tests:
```
$ mvn clean install
```

Build without tests:<br>
```
$ mvn clean install -DskipTests
```

Build with the HDP profile:<br>
```
$ mvn clean install -PHDP-2.5.0.0
```

You can swap "install" for "package" in the commands above if you don't want to deploy the artifacts to your local .m2 repo.

# Navigating the Architecture

Metron is at its core a Kappa architecture with Apache Storm as the processing
component and Apache Kafka as the unified data bus.

Some high level links to the relevant subparts of the architecture, for
more information:
* [Parsers](metron-platform/metron-parsers) : Parsing data from kafka into the Metron data model and passing it downstream to Enrichment.  
* [Enrichment](metron-platform/metron-enrichment) : Enriching data post-parsing and providing the ability to tag a message as an alert and assign a risk triage level via a custom rule language.
* [Indexing](metron-platform/metron-indexing) : Indexing the data post-enrichment into HDFS, Elasticsearch or Solr.

Some useful utilities that cross all of these parts of the architecture:
* [Stellar](metron-platform/metron-common) : A custom data transformation language that is used throughout metron from simple field transformation to expressing triage rules.
* [Model as a Service](metron-analytics/metron-maas-service) : A Yarn application which can deploy machine learning and statistical models onto the cluster along with the associated Stellar functions to be able to call out to them in a scalable manner.
* [Data management](metron-platform/metron-data-management) : A set of data management utilities aimed at getting data into HBase in a format which will allow data flowing through metron to be enriched with the results.  Contains integrations with threat intelligence feeds exposed via TAXII as well as simple flat file structures.
* [Profiler](metron-analytics/metron-profiler) : A feature extraction mechanism that can generate a profile describing the behavior of an entity. An entity might be a server, user, subnet or application. Once a profile has been generated defining what normal behavior looks-like, models can be built that identify anomalous behavior.
