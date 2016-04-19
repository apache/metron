# OpenTAXII

Installs [OpenTAXII](https://github.com/EclecticIQ/OpenTAXII) as a deamon that can be launched via a SysV service script.  The complementary client implementation, [Cabby](https://github.com/EclecticIQ/cabby) is also installed.

OpenTAXII is a robust Python implementation of TAXII Services that delivers a rich feature set and friendly pythonic API.  [TAXII](https://stixproject.github.io/) (Trusted Automated eXchange of Indicator Information) is a collection of specifications defining a set of services and message exchanges used for sharing cyber threat intelligence information between parties.

## Usage

### Service

A standard SysV script has been installed to manage OpenTAXII.  The following functions are available.

- `start` Start the opentaxii service
- `stop` Stop the opentaxii service
- `restart` Restart the opentaxii service
- `status` Current status of the opentaxii service
- `setup` Creates a set of collections and services to mirror the threat data made available at [Hail a TAXII](http://hailataxii.com/).  Running this will destroy all existing data.

### Troubleshooting

Should you need to explore the installation, here are instructions on doing so.

OpenTAXII is installed in a virtual environment.  Before exploring the environment run the following commands to perform the necessary setup.  The specific paths may change depending on your Ansible settings.

```
export LD_LIBRARY_PATH=/opt/rh/python27/root/usr/lib64
cd /usr/local/opentaxii
. opentaxii-venv/bin/activate
```

Explore the available collections.

```
taxii-collections --path http://hailataxii.com/taxii-data
taxii-collections --path http://localhost:9000/services/hailataxii/collection
```

Read data from a collection.

```
taxii-poll --host hailataxii.com --discovery taxii-data -c guest.phishtank_com
taxii-poll --host localhost:9000 --discovery services/hailataxii/collection -c hailataxii-phishtank
```

Discover available services.

```
taxii-discovery --discovery http://hailataxii.com/taxii-data
taxii-discovery --discovery http://localhost:9000/services/hailataxii/discovery
```

Fetch data from a remote service and mirror it locally.

```
taxii-proxy --poll-path http://hailataxii.com/taxii-data \
                     --poll-collection guest.phishtank_com \
                     --inbox-path http://localhost:9000/services/inbox \
                     --inbox-collection hailataxii-phishtank \
                     --begin 2016-04-17 \
                     --binding urn:stix.mitre.org:xml:1.1.1 \
                     --inbox-username guest --inbox-password guest
```
