export class SampleData {

  public static getSnortFieldNames(): any {
    return {
      'mappings': {
        'snort_doc': {
          'properties': {
            'msg': {
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:location_point': {
              'type': 'geo_point'
            },
            'dgmlen': {
              'type': 'integer'
            },
            'enrichments:geo:ip_src_addr:longitude': {
              'type': 'float'
            },
            'enrichmentjoinbolt:joiner:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'enrichments:geo:ip_src_addr:dmaCode': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'adapter:geoadapter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'tcpack': {
              'type': 'string'
            },
            'protocol': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'adapter:threatinteladapter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'enrichments:geo:ip_src_addr:locID': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'original_string': {
              'type': 'string'
            },
            'adapter:geoadapter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'id': {
              'type': 'integer'
            },
            'threat:triage:rules:0:score': {
              'type': 'long'
            },
            'enrichments:geo:ip_src_addr:location_point': {
              'type': 'geo_point'
            },
            'enrichmentsplitterbolt:splitter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'threat:triage:score': {
              'type': 'double'
            },
            'enrichments:geo:ip_dst_addr:city': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'adapter:hostfromjsonlistadapter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'enrichments:geo:ip_src_addr:postalCode': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'ethlen': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'threat:triage:level': {
              'type': 'double'
            },
            'adapter:threatinteladapter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'guid': {
              'type': 'string'
            },
            'tcpflags': {
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:country': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:locID': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:dmaCode': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'ip_dst_port': {
              'type': 'integer'
            },
            'sig_rev': {
              'type': 'string'
            },
            'threatinteljoinbolt:joiner:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'ethsrc': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'tcpseq': {
              'type': 'string'
            },
            'enrichmentsplitterbolt:splitter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'tcpwindow': {
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:latitude': {
              'type': 'float'
            },
            'source:type': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'ip_dst_addr': {
              'type': 'ip'
            },
            'adapter:hostfromjsonlistadapter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'tos': {
              'type': 'integer'
            },
            'enrichments:geo:ip_src_addr:latitude': {
              'type': 'float'
            },
            'ip_src_addr': {
              'type': 'ip'
            },
            'threatintelsplitterbolt:splitter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'enrichments:geo:ip_dst_addr:longitude': {
              'type': 'float'
            },
            'timestamp': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'ethdst': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:postalCode': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'is_alert': {
              'type': 'boolean'
            },
            'enrichments:geo:ip_src_addr:country': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'ttl': {
              'type': 'integer'
            },
            'iplen': {
              'type': 'integer'
            },
            'ip_src_port': {
              'type': 'integer'
            },
            'threatintelsplitterbolt:splitter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'sig_id': {
              'type': 'integer'
            },
            'sig_generator': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'enrichments:geo:ip_src_addr:city': {
              'index': 'not_analyzed',
              'type': 'string'
            }
          }
        }
      }
    };
  }

  public static getBroFieldNames(): any {
    return {
      'mappings': {
        'bro_doc': {
          'properties': {
            'TTLs': {
              'type': 'double'
            },
            'bro_timestamp': {
              'type': 'string'
            },
            'qclass_name': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:location_point': {
              'type': 'geo_point'
            },
            'answers': {
              'type': 'ip'
            },
            'enrichmentjoinbolt:joiner:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'adapter:geoadapter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'resp_mime_types': {
              'type': 'string'
            },
            'protocol': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'adapter:threatinteladapter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'original_string': {
              'type': 'string'
            },
            'host': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'adapter:geoadapter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'AA': {
              'type': 'boolean'
            },
            'method': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'enrichmentsplitterbolt:splitter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'query': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'enrichments:geo:ip_dst_addr:city': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'rcode': {
              'type': 'integer'
            },
            'adapter:hostfromjsonlistadapter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'orig_mime_types': {
              'type': 'string'
            },
            'RA': {
              'type': 'boolean'
            },
            'RD': {
              'type': 'boolean'
            },
            'orig_fuids': {
              'type': 'string'
            },
            'proto': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'adapter:threatinteladapter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'enrichments:geo:ip_dst_addr:country': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'response_body_len': {
              'type': 'integer'
            },
            'enrichments:geo:ip_dst_addr:locID': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'qtype_name': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'status_code': {
              'type': 'integer'
            },
            'enrichments:geo:ip_dst_addr:dmaCode': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'ip_dst_port': {
              'type': 'integer'
            },
            'threatinteljoinbolt:joiner:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'qtype': {
              'type': 'integer'
            },
            'rejected': {
              'type': 'boolean'
            },
            'enrichmentsplitterbolt:splitter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'trans_id': {
              'type': 'integer'
            },
            'enrichments:geo:ip_dst_addr:latitude': {
              'type': 'float'
            },
            'uid': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'source:type': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'trans_depth': {
              'type': 'integer'
            },
            'ip_dst_addr': {
              'type': 'ip'
            },
            'adapter:hostfromjsonlistadapter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'Z': {
              'type': 'integer'
            },
            'ip_src_addr': {
              'type': 'ip'
            },
            'threatintelsplitterbolt:splitter:end:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'enrichments:geo:ip_dst_addr:longitude': {
              'type': 'float'
            },
            'qclass': {
              'type': 'integer'
            },
            'user_agent': {
              'type': 'string'
            },
            'resp_fuids': {
              'type': 'string'
            },
            'timestamp': {
              'format': 'epoch_millis',
              'type': 'date'
            },
            'request_body_len': {
              'type': 'integer'
            },
            'enrichments:geo:ip_dst_addr:postalCode': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'uri': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'TC': {
              'type': 'boolean'
            },
            'rcode_name': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'referrer': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'ip_src_port': {
              'type': 'integer'
            },
            'status_msg': {
              'index': 'not_analyzed',
              'type': 'string'
            },
            'threatintelsplitterbolt:splitter:begin:ts': {
              'format': 'epoch_millis',
              'type': 'date'
            }
          }
        }
      }
    };
  }

  public static getFilters(): any[] {
    return [
      {'aggregations': {
        'Application': {
          'buckets': [
            {'key': 'x-world/x-3dmf', 'doc_count': 22},
            {'key': 'x-bzip2', 'doc_count': 3},
            {'key': 'msword', 'doc_count': 19},
            {'key': 'x-gzip', 'doc_count': 3},
            {'key': 'jpeg', 'doc_count': 292},
            {'key': 'mpeg3', 'doc_count': 45}
          ]
        }}},
      {'aggregations': {
        'Assets': {
          'buckets': [
            {'key': 'laptops', 'doc_count': 22},
            {'key': 'servers', 'doc_count': 3},
            {'key': 'printers', 'doc_count': 19},
            {'key': 'mobile phones', 'doc_count': 37},
            {'key': 'USB memory sticks', 'doc_count': 9}
          ]
        }}},
      {'aggregations': {
        'Severity': {
          'buckets': [
            {'key': 'critical', 'doc_count': 22},
            {'key': 'high', 'doc_count': 3},
            {'key': 'medium', 'doc_count': 19},
            {'key': 'low', 'doc_count': 37}
          ]
        }}}
    ];
  }
}
