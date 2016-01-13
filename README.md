# Metron

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

