# TODO DELETE THIS
# Elasticsearch Meta-alert POC

## Implementation possibilities examined
### Documents themselves duplicated as nested docs inside a meta alert index.
This does allow for the meeting of requirements, at least from current testing. It does, however, necessitate duplicating any documents that make their way into a meta-alert. It also increases the complexity of managing this duplicated data.

### Meta alerts duplicated as nested docs inside the actual docs
The advantage is the docs themselves aren't merged, only metadata about the docs and their groupings. It also allows easy queries going from alert -> doc. There are also potential advantages in the ease of updating docs, but still requires the management of metadata to the docs.

The disadvantage is that it creates a problem deduplicating the docs at query time, which appears to be mostly unresolvable.

### Linking via references
The advantage of this is that it avoids data duplication and potential issues keeping things in sync.  Although two queries are required to get data in some cases, it does allow for easy back and forth between the two indexes, as each knows about the other.  It also eases updating by limiting the amount of work necessary to manage docs

The disadvantage is that it makes sorting/pagination impossible because the two indexes cannot be joined at query time.


## Creating Indices

### Alert
This is limited to essential fields for testing.
The nested alert field is because ES 2.4 doesn't allow for ignore missing mappings on nested fields.
This causes multi-index queries to fail because the alert field is missing.  This has a param in 5.x
  ```
  PUT alert_test
  {
     "mappings": {
        "alert": {
           "properties": {
              "guid" : {
                  "type": "string",
                  "index": "not_analyzed"
              },
              "threat.triage.level" : {
                  "type": "integer",
                  "index": "not_analyzed"
              },
              "alert": {
                 "type": "nested"
              }
           }
        }
     }
  }
  ```

### Meta alert
Ignoring metadata.  metadata is used for bucketing and should be able to be done via elastic.
  ```
  PUT meta_test
  {
     "mappings": {
        "metaalert": {
           "properties": {
              "guid" : {
                  "type": "string",
                  "index": "not_analyzed"
              },
              "threat.triage.level" : {
                  "type": "integer",
                  "index": "not_analyzed"
              },
              "alert": {
                 "type": "nested"
              }
           }
        }
     }
  }
  ```

## Get all alerts for meta alert
Just get the copies from the alerts field in the meta alert

## Get all alerts and metadata alerts matching a query
Needs to be sortable.
  ```
  POST alert_test,meta_test/_search
  {
     "sort": [
        {
           "threat.triage.level": {
              "order": "desc",
              "ignore_unmapped": true
           }
        }
     ],
     "query": {
        "constant_score": {
           "filter": {
              "bool": {
                 "should": [
                    {
                       "term": {
                          "guid": "a1"
                       }
                    },
                    {
                       "nested": {
                          "path": "alert",
                          "query": {
                             "bool": {
                                "must": [
                                   {
                                      "term": {
                                         "alert.guid": "a1"
                                      }
                                   }
                                ]
                             }
                          }
                      }
                    }
                 ]
              }
           }
        }
     }
  }
  ```

## Get all meta alerts associated with an alert.
  ```
  POST meta_test/_search
  {
     "sort": [
        {
           "threat.triage.level": {
              "order": "desc",
              "ignore_unmapped": true
           }
        }
     ],
     "query": {
        "constant_score": {
           "query": {
              "bool": {
                 "must": {
                    "nested": {
                       "path": "alert",
                       "query": {
                          "bool": {
                             "must": [
                                {
                                   "match": {
                                      "alert.guid": "a1"
                                   }
                                }
                             ]
                          }
                       }
                    }
                 }
              }
           }
        }
     }
  }
  ```

## Create a meta-alert
Create the alert with copies of the alerts contained populating the alert field.
Metadata is ignored for right now, but ES does allow for aggregating nested buckets.  This could also be done client-side.
  ```
  PUT meta_test/metaalert/1
  {
    "guid" : "m1",
    "threat.triage.level" : 6,
    "alert" : [
      {
          "guid" : "a1",
          "threat.triage.level" : "1"
      },
      {
          "guid": "a2",
          "threat.triage.level": "2"
      }
    ]
  }
  ```

## Delete meta-alerts
Just delete the appropriate meta alert
  ```
  DELETE meta_alert/my_type/1
  ```

## Update a meta alert
Nested docs require reindexing the entire document (although that could potentially be a partial update to a nested field, which is just an ease of use thing.  Under the hood it's the same).

To update the meta alert itself, it's necessary to recalculate scores and then update nested docs and scores.

## Update a child alert
1. The plain update to the alert
  ```
  POST alert_test/alert/1/_update
  {
    "doc" : {
      "threat.triage.level" : "2"
    }
  }
  ```

2. Need to recalculate scores and update

3. Profit

## Delete an alert in a meta alert
Same as an update.  Ensure we update the nested docs and the scores
