Kibana 4
--------

This role installs Kibana along with the default Metron Dashboard.

### FAQ

#### How do I change Metron's default dashboard?

Kibana stores all configuration elements within an Elasticsearch index called `.kibana`.  To deploy Kibana in a desired state, including the Metron Dashboard, we simply take an extract from a functioning Kibana instance and store that in `templates/kibana-index.json`.  The deployment process then restores the index from this extract.

(1) Stand-up an instance of Apache Metron and create the Kibana index patterns, visualizations, and dashboard as you see fit.

(2) Run the following command to extract the definitions for all the components that you have created.  Be sure to delete anything that you don't want to be part of this extract.  It will include all artifacts present in your `.kibana` index.

  ```
  elasticdump --input=http://ec2-52-41-121-175.us-west-2.compute.amazonaws.com:9200/.kibana \
    --output=kibana-index.json \
    --type=data \
    --searchBody='{"filter": { "or": [ {"type": {"value": "search"}}, {"type": {"value":"dashboard"}}, {"type": {"value":"visualization"}},{"type": {"value": "config"}},{"type": {"value": "url"}},{"type": {"value": "index-pattern"}} ] }}'
  ```

(3) This will result in a file containing the JSON-based definitions.  Overwrite `templates/kibana-index.json`.

(4) After redeploying the code, your changes should now be a part of the default Metron dashboard.

#### Why do my dashboard components change their order when reloading the dashboard?

This has been a problem in Kibana 4.5.1 and perhaps other versions too.  To address this problem find the definition for your dashboard in the Kibana index extract.  It will look like the following.

```
{"_index":".kibana","_type":"dashboard","_id":"Metron-Dashboard",...
```

Extract the `panelsJSON` field from the dashboard definition.  Reorder the definition of these panels so that they are ordered by row and column.  The component in row 1 should come before the component in row 2, etc.  After you have ordered the components in this way, Kibana will maintain the order of components in the dashboard.
