# OpenTAXII

Installs [OpenTAXII](https://github.com/EclecticIQ/OpenTAXII) as a deamon that can be launched via a SysV service script.  The complementary client implementation, [Cabby](https://github.com/EclecticIQ/cabby) is also installed.

OpenTAXII is a robust Python implementation of TAXII Services that delivers a rich feature set and friendly pythonic API.  [TAXII](https://stixproject.github.io/) (Trusted Automated eXchange of Indicator Information) is a collection of specifications defining a set of services and message exchanges used for sharing cyber threat intelligence information between parties.

## Getting Started

After deployment completes the OpenTAXII service is installed and running.  A set of [Hail a TAXII](http://hailataxii.com/) threat intel collections have been defined and configured.  Use the `status` option to view the collections that have been defined.

```
$ service opentaxii status
Checking opentaxii...                             Running
guest.phishtank_com                                0
guest.Abuse_ch                                     0
guest.CyberCrime_Tracker                           0
guest.EmergingThreats_rules                        0
guest.Lehigh_edu                                   0
guest.MalwareDomainList_Hostlist                   0
guest.blutmagie_de_torExits                        0
guest.dataForLast_7daysOnly                        0
guest.dshield_BlockList                            0
```

Notice that each collections contain zero records.  None of the data is automatically synced during deployment.  To sync the data manually use the `sync` option as defined below.  The following example does not provide a begin and end time so the data will be fetched for the current day only.

```
# service opentaxii sync guest.blutmagie_de_torExits
2016-04-21 20:34:42,511 INFO: Starting new HTTP connection (1): localhost
2016-04-21 20:34:42,540 INFO: Response received for Inbox_Message from http://localhost:9000/services/inbox
2016-04-21 20:34:42,542 INFO: Sending Inbox_Message to http://localhost:9000/services/inbox
...
2016-04-21 20:34:42,719 INFO: Response received for Poll_Request from http://localhost:9000/services/poll
2016-04-21 20:34:42,719 INFO: Content blocks count: 1618, is partial: False
```

The OpenTAXII service now contains 1,618 threat intel records indicating Tor Exit nodes.

```
[root@source ~]# service opentaxii status
Checking opentaxii...                             Running
guest.phishtank_com                                0
guest.Abuse_ch                                     0
guest.CyberCrime_Tracker                           0
guest.EmergingThreats_rules                        0
guest.Lehigh_edu                                   0
guest.MalwareDomainList_Hostlist                   0
guest.blutmagie_de_torExits                        1618
guest.dataForLast_7daysOnly                        0
guest.dshield_BlockList                            0
```

## Usage

A standard SysV script has been installed to manage OpenTAXII.  The following functions are available.

`start` `stop` `restart` the OpenTAXII service

`status` of the OpenTAXII service.  The command displays the collections that have been defined and the number of records in each.

```
$ service opentaxii status
Checking opentaxii...                             Running
guest.phishtank_com                                984
guest.Abuse_ch                                     45
guest.CyberCrime_Tracker                           482
guest.EmergingThreats_rules                        0
guest.Lehigh_edu                                   1030
guest.MalwareDomainList_Hostlist                   84
guest.blutmagie_de_torExits                        3236
guest.dataForLast_7daysOnly                        3377
guest.dshield_BlockList                            0
```

`setup` Initializes the services and collections required to operate the OpenTAXII service.  This will destroy all existing data.  The user is prompted to continue before any data is destroyed.

```
# service opentaxii setup
WARNING: force reset and destroy all opentaxii data? [Ny]: y
Stopping opentaxii                                ..Ok
2016-04-21T19:56:01.886157Z [opentaxii.server] info: api.persistence.loaded {timestamp=2016-04-21T19:56:01.886157Z, logger=opentaxii.server, api_class=SQLDatabaseAPI, event=api.persistence.loaded, level=info}
2016-04-21T19:56:01.896503Z [opentaxii.server] info: api.auth.loaded {timestamp=2016-04-21T19:56:01.896503Z, logger=opentaxii.server, api_class=SQLDatabaseAPI, event=api.auth.loaded, level=info}
2016-04-21T19:56:01.896655Z [opentaxii.server] info: taxiiserver.configured {timestamp=2016-04-21T19:56:01.896655Z, logger=opentaxii.server, event=taxiiserver.configured, level=info}
...
Ok
```

`sync [collection] [begin-at] [end-at]` Syncs the threat intel data available at [Hail a TAXII](http://hailataxii.com/).  If no begin and end date is provided then data is synced over the current day only.
  - `collection` Name of the collection to sync.
  - `begin-at` Exclusive begin of time window; ISO8601
  - `end-at` Inclusive end of time window; ISO8601

```
$ service opentaxii sync guest.phishtank_com
+ /usr/local/opentaxii/opentaxii-venv/bin/taxii-proxy --poll-path http://hailataxii.com/taxii-data --poll-collection guest.phishtank_com --inbox-path http://localhost:9000/services/guest.phishtank_com-inbox --inbox-collection guest.phishtank_com --binding urn:stix.mitre.org:xml:1.1.1 --begin 2016-04-21 --end 2016-04-22
2016-04-21 17:36:23,778 INFO: Sending Poll_Request to http://hailataxii.com/taxii-data
2016-04-21 17:36:23,784 INFO: Starting new HTTP connection (1): hailataxii.com
2016-04-21 17:36:24,175 INFO: Response received for Poll_Request from http://hailataxii.com/taxii-data
2016-04-21 17:36:24,274 INFO: Sending Inbox_Message to http://localhost:9000/services/guest.phishtank_com-inbox
...
2016-04-21 17:36:34,867 INFO: Response received for Poll_Request from http://localhost:9000/services/guest.phishtank_com-poll
2016-04-21 17:36:34,868 INFO: Content blocks count: 6993, is partial: False
```

### Troubleshooting

Should you need to explore the installation, here are instructions on doing so.

OpenTAXII is installed in a virtual environment.  Before exploring the environment run the following commands to perform the necessary setup.  The specific paths may change depending on your Ansible settings.

```
export LD_LIBRARY_PATH=/opt/rh/python27/root/usr/lib64
export OPENTAXII_CONFIG=/usr/local/opentaxii/etc/opentaxii-conf.yml
cd /usr/local/opentaxii
. opentaxii-venv/bin/activate
```

Discover available services.

```
taxii-discovery --discovery http://localhost:9000/services/discovery
taxii-discovery --discovery http://hailataxii.com/taxii-data
```

Explore available collections.

```
taxii-collections --discovery http://localhost:9000/services/discovery
taxii-collections --discovery http://hailataxii.com/taxii-data
```

Read data from a collection.

```
taxii-poll --discovery http://localhost:9000/services/discovery -c guest.phishtank_com
taxii-poll --discovery http://hailataxii.com/taxii-data -c guest.phishtank_com --begin 2016-04-20
```

Manually load data into a collection.

```
taxii-push \
  --discovery http://localhost:9000/services/discovery \
  --dest phishtank \
  --content-file data.xml \
  --username guest \
  --password guest
```

Fetch data from a remote service and mirror it locally.

```
taxii-proxy --poll-path http://hailataxii.com/taxii-data \
            --poll-collection guest.phishtank_com \
            --inbox-path http://localhost:9000/services/guest.phishtank_com-inbox \
            --inbox-collection guest.phishtank_com \
            --binding urn:stix.mitre.org:xml:1.1.1 \
            --inbox-username guest \
            --inbox-password guest \
            --begin 2016-04-20
```
