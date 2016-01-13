#Metron-Alerts

##Module Description

This module enables telemetry alerts.  It splits the mssage stream into two streams.  The original message is emitted on the "message" stream.  The corresponding alert is emitted on the "alerts" stream.  The two are tied together through the alerts UUID.  

##Message Format

Assuming the original message (with enrichments enabled) has the following format:

```json
{
"message": 
{"ip_src_addr": xxxx, 
"ip_dst_addr": xxxx, 
"ip_src_port": xxxx, 
"ip_dst_port": xxxx, 
"protocol": xxxx, 
"timestamp": xxxx.
"original_string": xxxx,
"additional-field 1": xxxx,
},
"enrichment" : {"geo": xxxx, "whois": xxxx, "hosts": xxxxx, "CIF": "xxxxx"}

}
```

The telemetry message will be tagged with a UUID alert tag like so:

```json
{
"message": 
{"ip_src_addr": xxxx, 
"ip_dst_addr": xxxx, 
"ip_src_port": xxxx, 
"ip_dst_port": xxxx, 
"protocol": xxxx, 
"timestamp": xxxx,
"original_string": xxxx,
"additional-field 1": xxxx,
},
"enrichment" : {"geo": xxxx, "whois": xxxx, "hosts": xxxxx, "CIF": "xxxxx"},
"alerts": [UUID1, UUID2, UUID3, etc]

}
```

The alert will be fired on the "alerts" stream and can be customized to have any format as long as it includes the required mandatory fields.  The mandatory fields are:

* timestamp (epoch): The time from the message that triggered the alert
* description: A human friendly string representation of the alert
* alert_id: The UUID generated for the alert. This uniquely identifies an alert

There are other standard but not mandatory fields that can be leveraged by metron-ui and other alert consumers:

* designated_host: The IP address that corresponds to an asset. Ex. The IP address of the company device associated with the alert.
* enrichment: A copy of the enrichment data from the message that triggered the alert
* priority: The priority of the alert. Mustb e set to one of HIGH, MED or LOW

An example of an alert with all mandatory and standard fields would look like so:

```json
{
"timestamp": xxxx,
"alert_id": UUID,
"description": xxxx,
"designated_host": xxxx,
"enrichment": { "geo": xxxx, "whois": xxxx, "cif": xxxx },
"priority": "MED"
}
```

##Alerts Bolt

The bolt can be extended with a variety of alerts adapters.  The ability to stack alerts is currently in beta, but is not currently advisable.  We advice to only have one alerts bolt per topology.  The adapters are rules-based adapters which fire alerts when rules are a match.  Currently only Java adapters are provided, but there are future plans to provide Grok-Based adapters as well.

The signature of the Alerts bolt is as follows:

``` 
TelemetryAlertsBolt alerts_bolt = new TelemetryAlertsBolt()
.withIdentifier(alerts_identifier).withMaxCacheSize(1000)
.withMaxTimeRetain(3600).withAlertsAdapter(alerts_adapter)
.withMetricConfiguration(config);
```
Identifier - JSON key where the alert is attached
TimeRetain & MaxCacheSize - Caching parameters for the bolt
MetricConfiguration - export custom bolt metrics to graphite (if not null)
AlertsAdapter - pick the appropriate adapter for generating the alerts

### Java Adapters

Java adapters are designed for high volume topologies, but are not easily extensible.  The adapters provided are:

* com.apache.metron.alerts.adapters.AllAlertsAdapter - will tag every single message with the static alert (appropriate for topologies like Sourcefire, etc, where every single message is an alert)
* com.apache.metron.alerts.adapters.HbaseWhiteAndBlacklistAdapter - will read white and blacklists from HBase and fire alerts if source or dest IP are not on the whitelist or if any IP is on the blacklist
* com.apache.metron.alerts.adapters.CIFAlertsAdapter - will alert on messages that have results in enrichment.cif.
* com.apache.metron.alerts.adpaters.KeywordsAlertAdapter - will alert on messages that contain any of a list of keywords
###Grok Adapters

Grok alerts adapters for Metron are still under devleopment

###Stacking Alert Adapters

The functionality to stack alerts adapters is still under development
