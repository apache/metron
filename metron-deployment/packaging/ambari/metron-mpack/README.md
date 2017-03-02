# Overview

Contains the definitions of the various services for the Ambari Management Pack.

More info can be found at https://cwiki.apache.org/confluence/display/AMBARI/Management+Packs

## Kibana Dashboards

The dashboards installed by the Kibana custom action are managed by the dashboard.p file.  This file is created by exporting existing dashboards from a running Kibana instance.

To create a new version of the file, make any necessary changes to Kibana (e.g. on quick-dev), and export with the appropriate command.
```
python src/main/resources/common-services/KIBANA/4.5.1/package/scripts/dashboard/dashboardindex.py $KIBANA_URL 9200 dashboard.p -s
mv dashboard.p src/main/resources/common-services/KIBANA/4.5.1/package/scripts/dashboard/dashboard.p
```

Build the Ambari Mpack to get the dashboard updated appropriately.

Once the MPack is installed, run the Kibana service's action "Load Template" to install dashboards.  This will completely overwrite the .kibana in Elasticsearch, so use with caution.

