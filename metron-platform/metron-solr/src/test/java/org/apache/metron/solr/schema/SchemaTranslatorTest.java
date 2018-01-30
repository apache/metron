package org.apache.metron.solr.schema;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.common.utils.JSONUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;

public class SchemaTranslatorTest {
  /**
   {
  "template": "bro_index*",
  "mappings": {
    "bro_doc": {
      "dynamic_templates": [
      {
        "geo_location_point": {
          "match": "enrichments:geo:*:location_point",
          "match_mapping_type": "*",
          "mapping": {
            "type": "geo_point"
          }
        }
      },
      {
        "geo_country": {
          "match": "enrichments:geo:*:country",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_city": {
          "match": "enrichments:geo:*:city",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_location_id": {
          "match": "enrichments:geo:*:locID",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_dma_code": {
          "match": "enrichments:geo:*:dmaCode",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_postal_code": {
          "match": "enrichments:geo:*:postalCode",
          "match_mapping_type": "*",
          "mapping": {
            "type": "keyword"
          }
        }
      },
      {
        "geo_latitude": {
          "match": "enrichments:geo:*:latitude",
          "match_mapping_type": "*",
          "mapping": {
            "type": "float"
          }
        }
      },
      {
        "geo_longitude": {
          "match": "enrichments:geo:*:longitude",
          "match_mapping_type": "*",
          "mapping": {
            "type": "float"
          }
        }
      },
      {
        "timestamps": {
          "match": "*:ts",
          "match_mapping_type": "*",
          "mapping": {
            "type": "date",
            "format": "epoch_millis"
          }
        }
      },
      {
        "threat_triage_score": {
          "mapping": {
            "type": "float"
          },
          "match": "threat:triage:*score",
          "match_mapping_type": "*"
        }
      },
      {
        "threat_triage_reason": {
          "mapping": {
            "type": "text",
            "fielddata": "true"
          },
          "match": "threat:triage:rules:*:reason",
          "match_mapping_type": "*"
        }
      },
      {
        "threat_triage_name": {
          "mapping": {
            "type": "text",
            "fielddata": "true"
          },
          "match": "threat:triage:rules:*:name",
          "match_mapping_type": "*"
        }
      }
      ],
      "properties": {
        "source:type": {
          "type": "keyword"
        },

        "timestamp": {
          "type": "date",
          "format": "epoch_millis"
        },
        "uid": {
          "type": "keyword"
        },
        "alert": {
          "type": "nested"
        },
        "ip_src_addr": {
          "type": "ip"
        },
        "ip_src_port": {
          "type": "integer"
        },
        "ip_dst_addr": {
          "type": "ip"
        },
        "ip_dst_port": {
          "type": "integer"
        },
        "trans_depth": {
          "type": "integer"
        },
        "method": {
          "type": "keyword"
        },
        "host": {
          "type": "keyword"
        },
        "uri": {
          "type": "keyword",
          "ignore_above": 8191
        },
        "referrer": {
          "type": "keyword"
        },
        "version": {
          "type": "keyword"
        },
        "user_agent": {
          "type": "text",
          "fielddata": "true"
        },
        "request_body_len": {
          "type": "long"
        },
        "response_body_len": {
          "type": "long"
        },
        "status_code": {
          "type": "integer"
        },
        "status_msg": {
          "type": "keyword"
        },
        "info_code": {
          "type": "integer"
        },
        "info_msg": {
          "type": "keyword"
        },
        "tags": {
          "type": "keyword"
        },
        "username": {
          "type": "keyword"
        },
        "password": {
          "type": "keyword"
        },
        "proxied": {
          "type": "keyword"
        },
        "orig_fuids": {
          "type": "text",
          "fielddata": "true"
        },
        "orig_filenames": {
          "type": "text",
          "fielddata": "true"
        },
        "orig_mime_types": {
          "type": "text",
          "fielddata": "true"
        },
        "resp_fuids": {
          "type": "text",
          "fielddata": "true"
        },
        "resp_filenames": {
          "type": "text",
          "fielddata": "true"
        },
        "resp_mime_types": {
          "type": "text",
          "fielddata": "true"
        },
        "proto": {
          "type": "keyword"
        },
        "trans_id": {
          "type": "long"
        },
        "rtt": {
          "type": "keyword"
        },
        "query": {
          "type": "keyword"
        },
        "qclass": {
          "type": "integer"
        },
        "qclass_name": {
          "type": "keyword"
        },
        "qtype": {
          "type": "integer"
        },
        "qtype_name": {
          "type": "keyword"
        },
        "rcode": {
          "type": "integer"
        },
        "rcode_name": {
          "type": "keyword"
        },
        "AA": {
          "type": "boolean"
        },
        "TC": {
          "type": "boolean"
        },
        "RD": {
          "type": "boolean"
        },
        "RA": {
          "type": "boolean"
        },
        "Z": {
          "type": "integer"
        },
        "answers": {
          "type": "text",
          "fielddata": "true"
        },
        "TTLs": {
          "type": "text",
          "fielddata": "true"
        },
        "rejected": {
          "type": "boolean"
        },
        "service": {
          "type": "keyword"
        },
        "duration": {
          "type": "float"
        },
        "orig_bytes": {
          "type": "long"
        },
        "resp_bytes": {
          "type": "long"
        },
        "conn_state": {
          "type": "keyword"
        },
        "local_orig": {
          "type": "boolean"
        },
        "local_resp": {
          "type": "keyword"
        },
        "missed_bytes": {
          "type": "long"
        },
        "history": {
          "type": "keyword"
        },
        "orig_pkts": {
          "type": "long"
        },
        "orig_ip_bytes": {
          "type": "long"
        },
        "resp_pkts": {
          "type": "long"
        },
        "resp_ip_bytes": {
          "type": "long"
        },
        "tunnel_parents": {
          "type": "keyword"
        },
        "analyzer": {
          "type": "keyword"
        },
        "failure_reason": {
          "type": "keyword"
        },
        "user": {
          "type": "keyword"
        },
        "command": {
          "type": "keyword"
        },
        "arg": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "mime_type": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "file_size": {
          "type": "long"
        },
        "reply_code": {
          "type": "integer"
        },
        "reply_msg": {
          "type": "keyword"
        },
        "data_channel:passive": {
          "type": "boolean"
        },
        "data_channel:orig_h": {
          "type": "ip"
        },
        "data_channel:resp_h": {
          "type": "ip"
        },
        "data_channel:resp_p": {
          "type": "integer"
        },
        "cwd": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "passive": {
          "type": "boolean"
        },
        "fuid": {
          "type": "keyword"
        },
        "conn_uids": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "source": {
          "type": "keyword"
        },
        "depth": {
          "type": "integer"
        },
        "analyzers": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "filename": {
          "type": "keyword"
        },
        "is_orig": {
          "type": "boolean"
        },
        "seen_bytes": {
          "type": "long"
        },
        "total_bytes": {
          "type": "long"
        },
        "missing_bytes": {
          "type": "long"
        },
        "overflow_bytes": {
          "type": "long"
        },
        "timedout": {
          "type": "boolean"
        },
        "parent_fuid": {
          "type": "keyword"
        },
        "md5": {
          "type": "keyword"
        },
        "sha1": {
          "type": "keyword"
        },
        "sha256": {
          "type": "keyword"
        },
        "extracted": {
          "type": "keyword"
        },
        "extracted_cutoff": {
          "type": "boolean"
        },
        "extracted_size": {
          "type": "long"
        },
        "port_num": {
          "type": "integer"
        },
        "subject": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "issuer_subject": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "serial": {
          "type": "keyword"
        },
        "helo": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "mailfrom": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "rcptto": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "date": {
          "type": "keyword"
        },
        "from": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "to": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "cc": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "reply_to": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "msg_id": {
          "type": "keyword"
        },
        "in_reply_to": {
          "type": "keyword"
        },
        "x_originating_ip": {
          "type": "ip"
        },
        "first_received": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "second_received": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "last_reply": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "path": {
          "type": "keyword"
        },
        "tls": {
          "type": "boolean"
        },
        "fuids": {
          "type": "keyword"
        },
        "is_webmail": {
          "type": "boolean"
        },
        "cipher": {
          "type": "keyword"
        },
        "curve": {
          "type": "keyword"
        },
        "server_name": {
          "type": "keyword"
        },
        "resumed": {
          "type": "boolean"
        },
        "server_appdata": {
          "type": "keyword"
        },
        "client_appdata": {
          "type": "boolean"
        },
        "last_alert": {
          "type": "keyword"
        },
        "next_protocol": {
          "type": "keyword"
        },
        "established": {
          "type": "boolean"
        },
        "cert_chain_fuids": {
          "type": "text",
          "fielddata": "true"
        },
        "client_cert_chain_fuids": {
          "type": "text",
          "fielddata": "true"
        },
        "issuer": {
          "type": "keyword"
        },
        "client_subject": {
          "type": "keyword"
        },
        "client_issuer": {
          "type": "keyword"
        },
        "validation_status": {
          "type": "keyword"
        },
        "name": {
          "type": "keyword"
        },
        "addl": {
          "type": "keyword"
        },
        "notice": {
          "type": "boolean"
        },
        "peer": {
          "type": "keyword"
        },
        "file_mime_type": {
          "type": "keyword"
        },
        "file_desc": {
          "type": "keyword"
        },
        "note": {
          "type": "keyword"
        },
        "msg": {
          "type": "keyword"
        },
        "sub": {
          "type": "keyword"
        },
        "src": {
          "type": "ip"
        },
        "dst": {
          "type": "ip"
        },
        "p": {
          "type": "integer"
        },
        "n": {
          "type": "integer"
        },
        "src_peer": {
          "type": "ip"
        },
        "peer_descr": {
          "type": "keyword"
        },
        "actions": {
          "type": "keyword"
        },
        "suppress_for": {
          "type": "double"
        },
        "dropped": {
          "type": "boolean"
        },
        "remote_location:country_code": {
          "type": "text",
          "fielddata": "true"
        },
        "remote_location:region": {
          "type": "text",
          "fielddata": "true"
        },
        "remote_location:city": {
          "type": "text",
          "fielddata": "true"
        },
        "remote_location:latitude": {
          "type": "double"
        },
        "remote_location:longitude": {
          "type": "double"
        },
        "mac": {
          "type": "keyword"
        },
        "assigned_ip": {
          "type": "ip"
        },
        "lease_time": {
          "type": "float"
        },
        "auth_success": {
          "type": "boolean"
        },
        "auth_attempts": {
          "type": "integer"
        },
        "direction": {
          "type": "keyword"
        },
        "client": {
          "type": "keyword"
        },
        "server": {
          "type": "keyword"
        },
        "cipher_alg": {
          "type": "keyword"
        },
        "mac_alg": {
          "type": "keyword"
        },
        "compression_alg": {
          "type": "keyword"
        },
        "kex_alg": {
          "type": "keyword"
        },
        "host_key_alg": {
          "type": "keyword"
        },
        "host_key": {
          "type": "keyword"
        },
        "host_p": {
          "type": "integer"
        },
        "software_type": {
          "type": "keyword"
        },
        "version:major": {
          "type": "keyword"
        },
        "version:minor": {
          "type": "keyword"
        },
        "version:minor2": {
          "type": "keyword"
        },
        "version:minor3": {
          "type": "keyword"
        },
        "version:addl": {
          "type": "keyword"
        },
        "unparsed_version": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },

        "framed_addr": {
          "type": "ip"
        },
        "remote_ip": {
          "type": "ip"
        },
        "connect_info": {
          "type": "keyword"
        },
        "reply_msg": {
          "type": "keyword"
        },
        "result": {
          "type": "keyword"
        },
        "ttl": {
          "type": "keyword"
        },
        "id": {
          "type": "keyword"
        },
        "certificate:version": {
          "type": "integer"
        },
        "certificate:serial": {
          "type": "keyword"
        },
        "certificate:subject": {
          "type": "keyword"
        },
        "certificate:issuer": {
          "type": "keyword"
        },
        "certificate:not_valid_before": {
          "type": "keyword"
        },
        "certificate:not_valid_after": {
          "type": "keyword"
        },
        "certificate:key_alg": {
          "type": "keyword"
        },
        "certificate:sig_alg": {
          "type": "keyword"
        },
        "certificate:key_type": {
          "type": "keyword"
        },
        "certificate:key_length": {
          "type": "integer"
        },
        "certificate:exponent": {
          "type": "keyword"
        },
        "certificate:curve": {
          "type": "keyword"
        },
        "san:dns": {
          "type": "keyword"
        },
        "san:uri": {
          "type": "keyword"
        },
        "san:email": {
          "type": "keyword"
        },
        "san:ip": {
          "type": "keyword"
        },
        "basic_constraints:ca": {
          "type": "boolean"
        },
        "basic_constraints:path_len": {
          "type": "integer"
        },

        "dhcp_host_name": {
          "type": "keyword"
        },

        "client_major_version": {
          "type": "keyword"
        },
        "client_minor_version": {
          "type": "keyword"
        },
        "server_major_version": {
          "type": "keyword"
        },
        "server_minor_version": {
          "type": "keyword"
        },
        "authentication_method": {
          "type": "keyword"
        },
        "auth": {
          "type": "boolean"
        },
        "share_flag": {
          "type": "boolean"
        },
        "desktop_name": {
          "type": "keyword"
        },
        "width": {
          "type": "integer"
        },
        "height": {
          "type": "integer"
        },
        "mem": {
          "type": "integer"
        },
        "pkts_proc": {
          "type": "integer"
        },
        "bytes_recv": {
          "type": "integer"
        },
        "pkts_dropped": {
          "type": "integer"
        },
        "pkts_link": {
          "type": "integer"
        },
        "pkt_lag": {
          "type": "keyword"
        },
        "events_proc": {
          "type": "integer"
        },
        "events_queued": {
          "type": "integer"
        },
        "active_tcp_conns": {
          "type": "integer"
        },
        "active_udp_conns": {
          "type": "integer"
        },
        "active_icmp_conns": {
          "type": "integer"
        },
        "tcp_conns": {
          "type": "integer"
        },
        "udp_conns": {
          "type": "integer"
        },
        "icmp_conns": {
          "type": "integer"
        },
        "timers": {
          "type": "integer"
        },
        "active_timers": {
          "type": "integer"
        },
        "files": {
          "type": "integer"
        },
        "active_files": {
          "type": "integer"
        },
        "dns_requests": {
          "type": "integer"
        },
        "active_dns_requests": {
          "type": "integer"
        },
        "reassem_tcp_size": {
          "type": "integer"
        },
        "reassem_file_size": {
          "type": "integer"
        },
        "reassem_frag_size": {
          "type": "integer"
        },
        "reassem_unknown_size": {
          "type": "integer"
        },
        "ts_delta": {
          "type": "keyword"
        },
        "gaps": {
          "type": "integer",
          "index": "not_analyzed"
        },
        "acks": {
          "type": "integer",
          "index": "not_analyzed"
        },
        "percent_lost": {
          "type": "double",
          "index": "not_analyzed"
        },
        "level": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "message": {
          "type": "keyword"
        },
        "location": {
          "type": "keyword"
        },
        "request_from": {
          "type": "keyword"
        },
        "request_to": {
          "type": "keyword"
        },
        "response_from": {
          "type": "keyword"
        },
        "response_to": {
          "type": "keyword"
        },
        "call_id": {
          "type": "keyword"
        },
        "seq": {
          "type": "keyword"
        },
        "request_path": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "response_path": {
          "type": "text",
          "fielddata": "true",
          "analyzer": "simple"
        },
        "warning": {
          "type": "keyword"
        },
        "content_type": {
          "type": "keyword"
        },
        "guid": {
          "type": "keyword"
        }
      }
    }
  }
}
   */
  @Multiline
  public static String broTemplate;
  @Test
  public void test() throws IOException {
    SchemaTranslator.translate(new PrintWriter(System.out), JSONUtils.INSTANCE.load(broTemplate, JSONUtils.MAP_SUPPLIER));
  }

}
