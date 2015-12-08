/* global _ */

/*
 * Complex scripted Logstash dashboard
 * This script generates a dashboard object that Kibana can load. It also takes a number of user
 * supplied URL parameters, none are required:
 *
 * index :: Which index to search? If this is specified, interval is set to 'none'
 * pattern :: Does nothing if index is specified. Set a timestamped index pattern. Default: [logstash-]YYYY.MM.DD
 * interval :: Sets the index interval (eg: day,week,month,year), Default: day
 *
 * split :: The character to split the queries on Default: ','
 * query :: By default, a comma separated list of queries to run. Default: *
 *
 * from :: Search this amount of time back, eg 15m, 1h, 2d. Default: 15m
 * timefield :: The field containing the time to filter on, Default: message.timestamp
 *
 * fields :: comma separated list of fields to show in the table
 * sort :: comma separated field to sort on, and direction, eg sort=message.timestamp,desc
 *
 */

'use strict';

// Setup some variables
var dashboard, queries, _d_timespan;

// All url parameters are available via the ARGS object
var ARGS;

// Set a default timespan if one isn't specified
_d_timespan = '3d';

// Intialize a skeleton with nothing but a rows array and service object
dashboard = {
  rows : [],
  services : {}
};

// Set a title
dashboard.title = 'GoodBig Co. with PCAP';

dashboard.failover = false;
dashboard.index = {
  default: 'bro,fireeye,qosmos,sourcefire,qradar,lancope',
  interval: 'none'
};


// In this dashboard we let users pass queries as comma separated list to the query parameter.
// Or they can specify a split character using the split aparameter
// If query is defined, split it into a list of query objects
// NOTE: ids must be integers, hence the parseInt()s
if(!_.isUndefined(ARGS.query)) {
  queries = _.object(_.map(ARGS.query.split(ARGS.split||','), function(v,k) {
    return [k,{
      query: v,
      id: parseInt(k,10),
      alias: v
    }];
  }));
} else {
  // No queries passed? Initialize a single query to match everything
  queries = {
    0: {
          "query": "enrichment.alert.priority:1",
          "alias": "Severe",
          "color": "#FF0000",
          "id": 0,
          "pin": true,
          "type": "lucene",
          "enable": true
    },
    1: {
          "query": "enrichment.alert.priority:2",
          "alias": "Urgent",
          "color": "#FF9900",
          "id": 1,
          "pin": true,
          "type": "lucene",
          "enable": true
    },
    2: {
          "query": "enrichment.alert.priority:3",
          "alias": "Warning",
          "color": "#FFFF00",
          "id": 2,
          "pin": true,
          "type": "lucene",
          "enable": true
    }
  };
}


// Now populate the query service with our objects
dashboard.services.query = {
  list : queries,
  ids : _.map(_.keys(queries),function(v){return parseInt(v,10);})
};

// Lets also add a default time filter, the value of which can be specified by the user

dashboard.services.filter = {
  list: {
    0: {
      from: "now-"+(ARGS.from||_d_timespan),
      to: "now",
      field: ARGS.timefield||"enrichment.message.timestamp",
      type: "time",
      active: true,
      id: 0,
    }
  },
  ids: [0]
};




// Ok, lets make some rows. The Filters row is collapsed by default
dashboard.rows = [
  {
    title: "Alerts Queue Overview",
    height: "200px"
  },
  {
    title: "Alerts Query Results",
    height: "200px"
  }
];

// And a table row where you can specify field and sort order
dashboard.rows[0].panels = [
  {
    title: 'Alerts Count type',
    type: 'terms',
    field: '_type',
    span: 2,
    other: false,
    missing: false,
    chart: 'table',
    locked: true
  },
  {
    title: 'Alerts Count priority',
    type: 'terms',
    field: 'enrichment.alert.priority',
    span: 2,
    other: false,
    missing: false,
    chart: 'table',
    locked: true
  },
  {
    title: 'Source of All Alerts',
    type: 'terms',
    fields: !_.isUndefined(ARGS.fields) ? ARGS.fields.split(',') : ['_type'],
    chart: 'pie',
    span: 4,
    other: false,
    missing: false,
    locked: true
  },
  {
    "span": 3,
    "editable": true,
    "type": "hits",
    "loadingEditor": false,
    "style": {
      "font-size": "10pt"
    },
    "arrangement": "horizontal",
    "chart": "bar",
    "counter_pos": "above",
    "donut": false,
    "tilt": false,
    "labels": true,
    "spyable": true,
    "queries": {
      "mode": "selected",
      "ids": [
        0,
        1,
        2
      ]
    },
    "locked": true,
    "title": "Alerts Severity"
  },
  {
    title: 'Alerts Severity Timeline',
    type: 'histogram',
    fields: !_.isUndefined(ARGS.fields) ? ARGS.fields.split(',') : ['_type'],
    mode: 'count',
    span: 10,
    time_field: 'enrichment.message.timestamp'
  },
  {
    title: 'Top Alerts',
    type: 'table',
    fields: !_.isUndefined(ARGS.fields) ? ARGS.fields.split(',') : [
            "enrichment.message.timestamp",
            "enrichment.alert.priority",
            "_type",
            "enrichment.message.ip_src_addr",
            "enrichment.message.ip_dst_addr",
            "enrichment.message.original_string"
          ],
    size: 10,
    sort: !_.isUndefined(ARGS.sort) ? ARGS.sort.split(',') : [ARGS.timefield||'enrichment.alert.priority','desc'],
    overflow: 'expand',
    span: 12,
    field_list: false,
    timeField: 'enrichment.message.timestamp',
    localTime: true,
    locked: true,
    sortable: false
  }
];

// And Query Results
dashboard.rows[1].panels = [
  {
    title: 'Alert Query Resuts',
    type: 'table',
    fields: !_.isUndefined(ARGS.fields) ? ARGS.fields.split(',') : [
            "enrichment.message.timestamp",
            "enrichment.alert.priority",
            "_type",
            "enrichment.message.ip_src_addr",
            "enrichment.message.ip_dst_addr",
            "enrichment.message.original_string"
          ],
    size: 30,
    sort: !_.isUndefined(ARGS.sort) ? ARGS.sort.split(',') : [ARGS.timefield||'enrichment.message.timestamp','asc'],
    overflow: 'expand',
    span: 12,
    field_list: false,
    timeField: 'enrichment.message.timestamp',
    localTime: true
  },
  {
    title: 'PCAP Panel',
    type: 'pcap',
    span: 12
  }
];

// And a PCAP Panel
/*dashboard.rows[2].panels = [
  {
    title: 'PCAP Panel',
    type: 'pcap',
    span: 12
  }
];*/



// Now return the object and we're good!
return dashboard;
