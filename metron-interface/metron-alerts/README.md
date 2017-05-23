## Caveats
* UI doesn't have an authentication module yet
* UI uses local storage to save all the data.  A middleware needs to be designed and developed for persisting the data
* We need some good automation to test the UI any help in this regard is most welcome :)
* The UI has a left panel to show filters this is just UI mock for now the functionality is not yet done

##Development Setup
###Prerequisites
* Elastic search should be up and running and should have some alerts populated by metron topologies
* The alerts can be populated using Quick Dev, Full Dev  or any other setup
* UI is developed using angular4 and uses angular-cli
* node.JS >= 7.8.0

Install all the dependent node_modules using the following command
```
cd incubator-metron/metron-interface/metron-alerts
npm install
```
UI can be run by using the following command
```
./scripts/start-dev.sh
```
**NOTE**: *The UI by default connects to ES at http://node1:9200 for fetching data. If you wish to change it you can change the ES url at incubator-metron/metron-interface/metron-alerts/proxy.conf.json*