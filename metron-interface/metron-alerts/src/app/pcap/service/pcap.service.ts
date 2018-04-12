import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs/Rx';
import { Http, Headers, RequestOptions, Response } from '@angular/http';
import { HttpUtil } from '../../utils/httpUtil';

import 'rxjs/add/operator/map';

import { PcapRequest } from '../model/pcap.request';
import { Pdml } from '../model/pdml'

@Injectable()
export class PcapService {

  constructor(private http: Http) {
  }

  public getPackets(request: PcapRequest): Observable<Pdml> {
    console.log(request)
    return this.http.get('/api/v1/pcap', new RequestOptions({ 
      params: request
    })).map(r => r.json()).catch(HttpUtil.handleError)
  }
  
  public getTestPackets(request: PcapRequest): Observable<Pdml> {
    return Observable.create((o) => o.next(JSON.parse(pdml_json())))
  }
}




function pdml_json() {
  return `{
  "pdml": {
    "$": {
      "version": "0",
      "creator": "wireshark/2.4.2",
      "time": "Tue Mar 27 21:55:25 2018",
      "capture_file": "./metron-platform/metron-api/src/test/resources/test-tcp-packet.pcap"
    },
    "packet": [
      {
        "proto": [
          {
            "$": {
              "name": "geninfo",
              "pos": "0",
              "showname": "General information",
              "size": "104"
            },
            "field": [
              {
                "$": {
                  "name": "num",
                  "pos": "0",
                  "show": "1",
                  "showname": "Number",
                  "value": "1",
                  "size": "104"
                }
              },
              {
                "$": {
                  "name": "len",
                  "pos": "0",
                  "show": "104",
                  "showname": "Frame Length",
                  "value": "68",
                  "size": "104"
                }
              },
              {
                "$": {
                  "name": "caplen",
                  "pos": "0",
                  "show": "104",
                  "showname": "Captured Length",
                  "value": "68",
                  "size": "104"
                }
              },
              {
                "$": {
                  "name": "timestamp",
                  "pos": "0",
                  "show": "Mar 26, 2014 19:59:40.024362000 GMT",
                  "showname": "Captured Time",
                  "value": "1395863980.024362000",
                  "size": "104"
                }
              }
            ]
          },
          {
            "$": {
              "name": "frame",
              "showname": "Frame 1: 104 bytes on wire (832 bits), 104 bytes captured (832 bits)",
              "size": "104",
              "pos": "0"
            },
            "field": [
              {
                "$": {
                  "name": "frame.encap_type",
                  "showname": "Encapsulation type: Ethernet (1)",
                  "size": "0",
                  "pos": "0",
                  "show": "1"
                }
              },
              {
                "$": {
                  "name": "frame.time",
                  "showname": "Arrival Time: Mar 26, 2014 19:59:40.024362000 GMT",
                  "size": "0",
                  "pos": "0",
                  "show": "Mar 26, 2014 19:59:40.024362000 GMT"
                }
              },
              {
                "$": {
                  "name": "frame.offset_shift",
                  "showname": "Time shift for this packet: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.time_epoch",
                  "showname": "Epoch Time: 1395863980.024362000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "1395863980.024362000"
                }
              },
              {
                "$": {
                  "name": "frame.time_delta",
                  "showname": "Time delta from previous captured frame: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.time_delta_displayed",
                  "showname": "Time delta from previous displayed frame: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.time_relative",
                  "showname": "Time since reference or first frame: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.number",
                  "showname": "Frame Number: 1",
                  "size": "0",
                  "pos": "0",
                  "show": "1"
                }
              },
              {
                "$": {
                  "name": "frame.len",
                  "showname": "Frame Length: 104 bytes (832 bits)",
                  "size": "0",
                  "pos": "0",
                  "show": "104"
                }
              },
              {
                "$": {
                  "name": "frame.cap_len",
                  "showname": "Capture Length: 104 bytes (832 bits)",
                  "size": "0",
                  "pos": "0",
                  "show": "104"
                }
              },
              {
                "$": {
                  "name": "frame.marked",
                  "showname": "Frame is marked: False",
                  "size": "0",
                  "pos": "0",
                  "show": "0"
                }
              },
              {
                "$": {
                  "name": "frame.ignored",
                  "showname": "Frame is ignored: False",
                  "size": "0",
                  "pos": "0",
                  "show": "0"
                }
              },
              {
                "$": {
                  "name": "frame.protocols",
                  "showname": "Protocols in frame: eth:ethertype:ip:tcp:smtp",
                  "size": "0",
                  "pos": "0",
                  "show": "eth:ethertype:ip:tcp:smtp"
                }
              }
            ]
          },
          {
            "$": {
              "name": "eth",
              "showname": "Ethernet II, Src: MS-NLB-PhysServer-26_c5:01:00:02 (02:1a:c5:01:00:02), Dst: MS-NLB-PhysServer-26_c5:05:00:02 (02:1a:c5:05:00:02)",
              "size": "14",
              "pos": "0"
            },
            "field": [
              {
                "$": {
                  "name": "eth.dst",
                  "showname": "Destination: MS-NLB-PhysServer-26_c5:05:00:02 (02:1a:c5:05:00:02)",
                  "size": "6",
                  "pos": "0",
                  "show": "02:1a:c5:05:00:02",
                  "value": "021ac5050002"
                },
                "field": [
                  {
                    "$": {
                      "name": "eth.dst_resolved",
                      "showname": "Destination (resolved): MS-NLB-PhysServer-26_c5:05:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "0",
                      "show": "MS-NLB-PhysServer-26_c5:05:00:02",
                      "value": "021ac5050002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr",
                      "showname": "Address: MS-NLB-PhysServer-26_c5:05:00:02 (02:1a:c5:05:00:02)",
                      "size": "6",
                      "pos": "0",
                      "show": "02:1a:c5:05:00:02",
                      "value": "021ac5050002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr_resolved",
                      "showname": "Address (resolved): MS-NLB-PhysServer-26_c5:05:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "0",
                      "show": "MS-NLB-PhysServer-26_c5:05:00:02",
                      "value": "021ac5050002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.lg",
                      "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                      "size": "3",
                      "pos": "0",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "021ac5"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.ig",
                      "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                      "size": "3",
                      "pos": "0",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "021ac5"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "eth.src",
                  "showname": "Source: MS-NLB-PhysServer-26_c5:01:00:02 (02:1a:c5:01:00:02)",
                  "size": "6",
                  "pos": "6",
                  "show": "02:1a:c5:01:00:02",
                  "value": "021ac5010002"
                },
                "field": [
                  {
                    "$": {
                      "name": "eth.src_resolved",
                      "showname": "Source (resolved): MS-NLB-PhysServer-26_c5:01:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "6",
                      "show": "MS-NLB-PhysServer-26_c5:01:00:02",
                      "value": "021ac5010002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr",
                      "showname": "Address: MS-NLB-PhysServer-26_c5:01:00:02 (02:1a:c5:01:00:02)",
                      "size": "6",
                      "pos": "6",
                      "show": "02:1a:c5:01:00:02",
                      "value": "021ac5010002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr_resolved",
                      "showname": "Address (resolved): MS-NLB-PhysServer-26_c5:01:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "6",
                      "show": "MS-NLB-PhysServer-26_c5:01:00:02",
                      "value": "021ac5010002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.lg",
                      "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                      "size": "3",
                      "pos": "6",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "021ac5"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.ig",
                      "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                      "size": "3",
                      "pos": "6",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "021ac5"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "eth.type",
                  "showname": "Type: IPv4 (0x0800)",
                  "size": "2",
                  "pos": "12",
                  "show": "0x00000800",
                  "value": "0800"
                }
              },
              {
                "$": {
                  "name": "eth.fcs",
                  "showname": "Frame check sequence: 0x26469e92 [correct]",
                  "size": "4",
                  "pos": "100",
                  "show": "0x26469e92",
                  "value": "26469e92"
                }
              },
              {
                "$": {
                  "name": "eth.fcs.status",
                  "showname": "FCS Status: Good",
                  "size": "0",
                  "pos": "100",
                  "show": "1"
                }
              }
            ]
          },
          {
            "$": {
              "name": "ip",
              "showname": "Internet Protocol Version 4, Src: 24.0.0.2, Dst: 24.128.0.2",
              "size": "20",
              "pos": "14"
            },
            "field": [
              {
                "$": {
                  "name": "ip.version",
                  "showname": "0100 .... = Version: 4",
                  "size": "1",
                  "pos": "14",
                  "show": "4",
                  "value": "4",
                  "unmaskedvalue": "45"
                }
              },
              {
                "$": {
                  "name": "ip.hdr_len",
                  "showname": ".... 0101 = Header Length: 20 bytes (5)",
                  "size": "1",
                  "pos": "14",
                  "show": "20",
                  "value": "45"
                }
              },
              {
                "$": {
                  "name": "ip.dsfield",
                  "showname": "Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)",
                  "size": "1",
                  "pos": "15",
                  "show": "0x00000000",
                  "value": "00"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.dsfield.dscp",
                      "showname": "0000 00.. = Differentiated Services Codepoint: Default (0)",
                      "size": "1",
                      "pos": "15",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "00"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.dsfield.ecn",
                      "showname": ".... ..00 = Explicit Congestion Notification: Not ECN-Capable Transport (0)",
                      "size": "1",
                      "pos": "15",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "00"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "ip.len",
                  "showname": "Total Length: 86",
                  "size": "2",
                  "pos": "16",
                  "show": "86",
                  "value": "0056"
                }
              },
              {
                "$": {
                  "name": "ip.id",
                  "showname": "Identification: 0xcff6 (53238)",
                  "size": "2",
                  "pos": "18",
                  "show": "0x0000cff6",
                  "value": "cff6"
                }
              },
              {
                "$": {
                  "name": "ip.flags",
                  "showname": "Flags: 0x02 (Don't Fragment)",
                  "size": "1",
                  "pos": "20",
                  "show": "0x00000002",
                  "value": "40"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.flags.rb",
                      "showname": "0... .... = Reserved bit: Not set",
                      "size": "1",
                      "pos": "20",
                      "show": "0",
                      "value": "40"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.flags.df",
                      "showname": ".1.. .... = Don't fragment: Set",
                      "size": "1",
                      "pos": "20",
                      "show": "1",
                      "value": "40"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.flags.mf",
                      "showname": "..0. .... = More fragments: Not set",
                      "size": "1",
                      "pos": "20",
                      "show": "0",
                      "value": "40"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "ip.frag_offset",
                  "showname": "Fragment offset: 0",
                  "size": "2",
                  "pos": "20",
                  "show": "0",
                  "value": "4000"
                }
              },
              {
                "$": {
                  "name": "ip.ttl",
                  "showname": "Time to live: 32",
                  "size": "1",
                  "pos": "22",
                  "show": "32",
                  "value": "20"
                }
              },
              {
                "$": {
                  "name": "ip.proto",
                  "showname": "Protocol: TCP (6)",
                  "size": "1",
                  "pos": "23",
                  "show": "6",
                  "value": "06"
                }
              },
              {
                "$": {
                  "name": "ip.checksum",
                  "showname": "Header checksum: 0x5a28 [validation disabled]",
                  "size": "2",
                  "pos": "24",
                  "show": "0x00005a28",
                  "value": "5a28"
                }
              },
              {
                "$": {
                  "name": "ip.checksum.status",
                  "showname": "Header checksum status: Unverified",
                  "size": "0",
                  "pos": "24",
                  "show": "2"
                }
              },
              {
                "$": {
                  "name": "ip.src",
                  "showname": "Source: 24.0.0.2",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.addr",
                  "showname": "Source or Destination Address: 24.0.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.src_host",
                  "showname": "Source Host: 24.0.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.host",
                  "showname": "Source or Destination Host: 24.0.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.dst",
                  "showname": "Destination: 24.128.0.2",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "ip.addr",
                  "showname": "Source or Destination Address: 24.128.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "ip.dst_host",
                  "showname": "Destination Host: 24.128.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "ip.host",
                  "showname": "Source or Destination Host: 24.128.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "",
                  "show": "Source GeoIP: United States, Woodbridge, NJ, AS7922 Comcast Cable Communications, LLC, United States, Woodbridge, NJ, AS7922 Comcast Cable Communications, LLC, 40.557598, -74.284599",
                  "size": "4",
                  "pos": "26",
                  "value": "18000002"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.geoip.src_country",
                      "showname": "Source GeoIP Country: United States",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_city",
                      "showname": "Source GeoIP City: Woodbridge, NJ",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Woodbridge, NJ",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_asnum",
                      "showname": "Source GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_country",
                      "showname": "Source GeoIP Country: United States",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_city",
                      "showname": "Source GeoIP City: Woodbridge, NJ",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Woodbridge, NJ",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_asnum",
                      "showname": "Source GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_lat",
                      "showname": "Source GeoIP Latitude: 40.557598",
                      "size": "4",
                      "pos": "26",
                      "show": "40.557598",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lat",
                      "showname": "Source or Destination GeoIP Latitude: 40.557598",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "40.557598",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_lon",
                      "showname": "Source GeoIP Longitude: -74.284599",
                      "size": "4",
                      "pos": "26",
                      "show": "-74.284599",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lon",
                      "showname": "Source or Destination GeoIP Longitude: -74.284599",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "-74.284599",
                      "value": "18000002"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "",
                  "show": "Destination GeoIP: United States, Groton, CT, AS7922 Comcast Cable Communications, LLC, United States, Groton, CT, AS7922 Comcast Cable Communications, LLC, 41.353199, -72.038597",
                  "size": "4",
                  "pos": "30",
                  "value": "18800002"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.geoip.dst_country",
                      "showname": "Destination GeoIP Country: United States",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_city",
                      "showname": "Destination GeoIP City: Groton, CT",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Groton, CT",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_asnum",
                      "showname": "Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_country",
                      "showname": "Destination GeoIP Country: United States",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_city",
                      "showname": "Destination GeoIP City: Groton, CT",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Groton, CT",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_asnum",
                      "showname": "Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_lat",
                      "showname": "Destination GeoIP Latitude: 41.353199",
                      "size": "4",
                      "pos": "30",
                      "show": "41.353199",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lat",
                      "showname": "Source or Destination GeoIP Latitude: 41.353199",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "41.353199",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_lon",
                      "showname": "Destination GeoIP Longitude: -72.038597",
                      "size": "4",
                      "pos": "30",
                      "show": "-72.038597",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lon",
                      "showname": "Source or Destination GeoIP Longitude: -72.038597",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "-72.038597",
                      "value": "18800002"
                    }
                  }
                ]
              }
            ]
          },
          {
            "$": {
              "name": "tcp",
              "showname": "Transmission Control Protocol, Src Port: 2137, Dst Port: 25, Seq: 1, Ack: 1, Len: 34",
              "size": "32",
              "pos": "34"
            },
            "field": [
              {
                "$": {
                  "name": "tcp.srcport",
                  "showname": "Source Port: 2137",
                  "size": "2",
                  "pos": "34",
                  "show": "2137",
                  "value": "0859"
                }
              },
              {
                "$": {
                  "name": "tcp.dstport",
                  "showname": "Destination Port: 25",
                  "size": "2",
                  "pos": "36",
                  "show": "25",
                  "value": "0019"
                }
              },
              {
                "$": {
                  "name": "tcp.port",
                  "showname": "Source or Destination Port: 2137",
                  "hide": "yes",
                  "size": "2",
                  "pos": "34",
                  "show": "2137",
                  "value": "0859"
                }
              },
              {
                "$": {
                  "name": "tcp.port",
                  "showname": "Source or Destination Port: 25",
                  "hide": "yes",
                  "size": "2",
                  "pos": "36",
                  "show": "25",
                  "value": "0019"
                }
              },
              {
                "$": {
                  "name": "tcp.stream",
                  "showname": "Stream index: 0",
                  "size": "0",
                  "pos": "34",
                  "show": "0"
                }
              },
              {
                "$": {
                  "name": "tcp.len",
                  "showname": "TCP Segment Len: 34",
                  "size": "1",
                  "pos": "46",
                  "show": "34",
                  "value": "80"
                }
              },
              {
                "$": {
                  "name": "tcp.seq",
                  "showname": "Sequence number: 1    (relative sequence number)",
                  "size": "4",
                  "pos": "38",
                  "show": "1",
                  "value": "f88900ce"
                }
              },
              {
                "$": {
                  "name": "tcp.nxtseq",
                  "showname": "Next sequence number: 35    (relative sequence number)",
                  "size": "0",
                  "pos": "34",
                  "show": "35"
                }
              },
              {
                "$": {
                  "name": "tcp.ack",
                  "showname": "Acknowledgment number: 1    (relative ack number)",
                  "size": "4",
                  "pos": "42",
                  "show": "1",
                  "value": "365aa74f"
                }
              },
              {
                "$": {
                  "name": "tcp.hdr_len",
                  "showname": "1000 .... = Header Length: 32 bytes (8)",
                  "size": "1",
                  "pos": "46",
                  "show": "32",
                  "value": "80"
                }
              },
              {
                "$": {
                  "name": "tcp.flags",
                  "showname": "Flags: 0x018 (PSH, ACK)",
                  "size": "2",
                  "pos": "46",
                  "show": "0x00000018",
                  "value": "18",
                  "unmaskedvalue": "8018"
                },
                "field": [
                  {
                    "$": {
                      "name": "tcp.flags.res",
                      "showname": "000. .... .... = Reserved: Not set",
                      "size": "1",
                      "pos": "46",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "80"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.ns",
                      "showname": "...0 .... .... = Nonce: Not set",
                      "size": "1",
                      "pos": "46",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "80"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.cwr",
                      "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.ecn",
                      "showname": ".... .0.. .... = ECN-Echo: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.urg",
                      "showname": ".... ..0. .... = Urgent: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.ack",
                      "showname": ".... ...1 .... = Acknowledgment: Set",
                      "size": "1",
                      "pos": "47",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.push",
                      "showname": ".... .... 1... = Push: Set",
                      "size": "1",
                      "pos": "47",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.reset",
                      "showname": ".... .... .0.. = Reset: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.syn",
                      "showname": ".... .... ..0. = Syn: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.fin",
                      "showname": ".... .... ...0 = Fin: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.str",
                      "showname": "TCP Flags: \\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7AP\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7",
                      "size": "2",
                      "pos": "46",
                      "show": "\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7AP\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7",
                      "value": "8018"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "tcp.window_size_value",
                  "showname": "Window size value: 7240",
                  "size": "2",
                  "pos": "48",
                  "show": "7240",
                  "value": "1c48"
                }
              },
              {
                "$": {
                  "name": "tcp.window_size",
                  "showname": "Calculated window size: 7240",
                  "size": "2",
                  "pos": "48",
                  "show": "7240",
                  "value": "1c48"
                }
              },
              {
                "$": {
                  "name": "tcp.window_size_scalefactor",
                  "showname": "Window size scaling factor: -1 (unknown)",
                  "size": "2",
                  "pos": "48",
                  "show": "-1",
                  "value": "1c48"
                }
              },
              {
                "$": {
                  "name": "tcp.checksum",
                  "showname": "Checksum: 0x681f [unverified]",
                  "size": "2",
                  "pos": "50",
                  "show": "0x0000681f",
                  "value": "681f"
                }
              },
              {
                "$": {
                  "name": "tcp.checksum.status",
                  "showname": "Checksum Status: Unverified",
                  "size": "0",
                  "pos": "50",
                  "show": "2"
                }
              },
              {
                "$": {
                  "name": "tcp.urgent_pointer",
                  "showname": "Urgent pointer: 0",
                  "size": "2",
                  "pos": "52",
                  "show": "0",
                  "value": "0000"
                }
              },
              {
                "$": {
                  "name": "tcp.options",
                  "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
                  "size": "12",
                  "pos": "54",
                  "show": "01:01:08:0a:eb:83:4b:08:e8:8c:de:cb",
                  "value": "0101080aeb834b08e88cdecb"
                },
                "field": [
                  {
                    "$": {
                      "name": "tcp.options.nop",
                      "showname": "TCP Option - No-Operation (NOP)",
                      "size": "1",
                      "pos": "54",
                      "show": "01",
                      "value": "01"
                    },
                    "field": [
                      {
                        "$": {
                          "name": "tcp.option_kind",
                          "showname": "Kind: No-Operation (1)",
                          "size": "1",
                          "pos": "54",
                          "show": "1",
                          "value": "01"
                        }
                      }
                    ]
                  },
                  {
                    "$": {
                      "name": "tcp.options.nop",
                      "showname": "TCP Option - No-Operation (NOP)",
                      "size": "1",
                      "pos": "55",
                      "show": "01",
                      "value": "01"
                    },
                    "field": [
                      {
                        "$": {
                          "name": "tcp.option_kind",
                          "showname": "Kind: No-Operation (1)",
                          "size": "1",
                          "pos": "55",
                          "show": "1",
                          "value": "01"
                        }
                      }
                    ]
                  },
                  {
                    "$": {
                      "name": "tcp.options.timestamp",
                      "showname": "TCP Option - Timestamps: TSval 3951250184, TSecr 3901546187",
                      "size": "10",
                      "pos": "56",
                      "show": "08:0a:eb:83:4b:08:e8:8c:de:cb",
                      "value": "080aeb834b08e88cdecb"
                    },
                    "field": [
                      {
                        "$": {
                          "name": "tcp.option_kind",
                          "showname": "Kind: Time Stamp Option (8)",
                          "size": "1",
                          "pos": "56",
                          "show": "8",
                          "value": "08"
                        }
                      },
                      {
                        "$": {
                          "name": "tcp.option_len",
                          "showname": "Length: 10",
                          "size": "1",
                          "pos": "57",
                          "show": "10",
                          "value": "0a"
                        }
                      },
                      {
                        "$": {
                          "name": "tcp.options.timestamp.tsval",
                          "showname": "Timestamp value: 3951250184",
                          "size": "4",
                          "pos": "58",
                          "show": "3951250184",
                          "value": "eb834b08"
                        }
                      },
                      {
                        "$": {
                          "name": "tcp.options.timestamp.tsecr",
                          "showname": "Timestamp echo reply: 3901546187",
                          "size": "4",
                          "pos": "62",
                          "show": "3901546187",
                          "value": "e88cdecb"
                        }
                      }
                    ]
                  }
                ]
              },
              {
                "$": {
                  "name": "tcp.analysis",
                  "showname": "SEQ/ACK analysis",
                  "size": "0",
                  "pos": "34",
                  "show": "",
                  "value": ""
                },
                "field": [
                  {
                    "$": {
                      "name": "tcp.analysis.bytes_in_flight",
                      "showname": "Bytes in flight: 34",
                      "size": "0",
                      "pos": "34",
                      "show": "34"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.analysis.push_bytes_sent",
                      "showname": "Bytes sent since last PSH flag: 34",
                      "size": "0",
                      "pos": "34",
                      "show": "34"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "tcp.payload",
                  "showname": "TCP payload (34 bytes)",
                  "size": "34",
                  "pos": "66",
                  "show": "45:48:4c:4f:20:63:6c:69:65:6e:74:2d:31:38:30:30:30:30:30:33:2e:65:78:61:6d:70:6c:65:2e:69:6e:74:0d:0a",
                  "value": "45484c4f20636c69656e742d31383030303030332e6578616d706c652e696e740d0a"
                }
              }
            ]
          },
          {
            "$": {
              "name": "smtp",
              "showname": "Simple Mail Transfer Protocol",
              "size": "34",
              "pos": "66"
            },
            "field": [
              {
                "$": {
                  "name": "smtp.req",
                  "showname": "Request: True",
                  "hide": "yes",
                  "size": "0",
                  "pos": "66",
                  "show": "1"
                }
              },
              {
                "$": {
                  "name": "smtp.command_line",
                  "showname": "Command Line: EHLO client-18000003.example.int\\\\r\\\\n",
                  "size": "34",
                  "pos": "66",
                  "show": "EHLO client-18000003.example.int\\\\xd\\\\xa",
                  "value": "45484c4f20636c69656e742d31383030303030332e6578616d706c652e696e740d0a"
                },
                "field": [
                  {
                    "$": {
                      "name": "smtp.req.command",
                      "showname": "Command: EHLO",
                      "size": "4",
                      "pos": "66",
                      "show": "EHLO",
                      "value": "45484c4f"
                    }
                  },
                  {
                    "$": {
                      "name": "smtp.req.parameter",
                      "showname": "Request parameter: client-18000003.example.int",
                      "size": "27",
                      "pos": "71",
                      "show": "client-18000003.example.int",
                      "value": "636c69656e742d31383030303030332e6578616d706c652e696e74"
                    }
                  }
                ]
              }
            ]
          }
        ]
      },
      {
        "proto": [
          {
            "$": {
              "name": "geninfo",
              "pos": "0",
              "showname": "General information",
              "size": "104"
            },
            "field": [
              {
                "$": {
                  "name": "num",
                  "pos": "0",
                  "show": "1",
                  "showname": "Number",
                  "value": "1",
                  "size": "104"
                }
              },
              {
                "$": {
                  "name": "len",
                  "pos": "0",
                  "show": "104",
                  "showname": "Frame Length",
                  "value": "68",
                  "size": "104"
                }
              },
              {
                "$": {
                  "name": "caplen",
                  "pos": "0",
                  "show": "104",
                  "showname": "Captured Length",
                  "value": "68",
                  "size": "104"
                }
              },
              {
                "$": {
                  "name": "timestamp",
                  "pos": "0",
                  "show": "Mar 26, 2014 19:59:40.024362000 GMT",
                  "showname": "Captured Time",
                  "value": "1395863980.024362000",
                  "size": "104"
                }
              }
            ]
          },
          {
            "$": {
              "name": "frame",
              "showname": "Frame 1: 104 bytes on wire (832 bits), 104 bytes captured (832 bits)",
              "size": "104",
              "pos": "0"
            },
            "field": [
              {
                "$": {
                  "name": "frame.encap_type",
                  "showname": "Encapsulation type: Ethernet (1)",
                  "size": "0",
                  "pos": "0",
                  "show": "1"
                }
              },
              {
                "$": {
                  "name": "frame.time",
                  "showname": "Arrival Time: Mar 26, 2014 19:59:40.024362000 GMT",
                  "size": "0",
                  "pos": "0",
                  "show": "Mar 26, 2014 19:59:40.024362000 GMT"
                }
              },
              {
                "$": {
                  "name": "frame.offset_shift",
                  "showname": "Time shift for this packet: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.time_epoch",
                  "showname": "Epoch Time: 1395863980.024362000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "1395863980.024362000"
                }
              },
              {
                "$": {
                  "name": "frame.time_delta",
                  "showname": "Time delta from previous captured frame: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.time_delta_displayed",
                  "showname": "Time delta from previous displayed frame: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.time_relative",
                  "showname": "Time since reference or first frame: 0.000000000 seconds",
                  "size": "0",
                  "pos": "0",
                  "show": "0.000000000"
                }
              },
              {
                "$": {
                  "name": "frame.number",
                  "showname": "Frame Number: 1",
                  "size": "0",
                  "pos": "0",
                  "show": "1"
                }
              },
              {
                "$": {
                  "name": "frame.len",
                  "showname": "Frame Length: 104 bytes (832 bits)",
                  "size": "0",
                  "pos": "0",
                  "show": "104"
                }
              },
              {
                "$": {
                  "name": "frame.cap_len",
                  "showname": "Capture Length: 104 bytes (832 bits)",
                  "size": "0",
                  "pos": "0",
                  "show": "104"
                }
              },
              {
                "$": {
                  "name": "frame.marked",
                  "showname": "Frame is marked: False",
                  "size": "0",
                  "pos": "0",
                  "show": "0"
                }
              },
              {
                "$": {
                  "name": "frame.ignored",
                  "showname": "Frame is ignored: False",
                  "size": "0",
                  "pos": "0",
                  "show": "0"
                }
              },
              {
                "$": {
                  "name": "frame.protocols",
                  "showname": "Protocols in frame: eth:ethertype:ip:tcp:smtp",
                  "size": "0",
                  "pos": "0",
                  "show": "eth:ethertype:ip:tcp:smtp"
                }
              }
            ]
          },
          {
            "$": {
              "name": "eth",
              "showname": "Ethernet II, Src: MS-NLB-PhysServer-26_c5:01:00:02 (02:1a:c5:01:00:02), Dst: MS-NLB-PhysServer-26_c5:05:00:02 (02:1a:c5:05:00:02)",
              "size": "14",
              "pos": "0"
            },
            "field": [
              {
                "$": {
                  "name": "eth.dst",
                  "showname": "Destination: MS-NLB-PhysServer-26_c5:05:00:02 (02:1a:c5:05:00:02)",
                  "size": "6",
                  "pos": "0",
                  "show": "02:1a:c5:05:00:02",
                  "value": "021ac5050002"
                },
                "field": [
                  {
                    "$": {
                      "name": "eth.dst_resolved",
                      "showname": "Destination (resolved): MS-NLB-PhysServer-26_c5:05:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "0",
                      "show": "MS-NLB-PhysServer-26_c5:05:00:02",
                      "value": "021ac5050002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr",
                      "showname": "Address: MS-NLB-PhysServer-26_c5:05:00:02 (02:1a:c5:05:00:02)",
                      "size": "6",
                      "pos": "0",
                      "show": "02:1a:c5:05:00:02",
                      "value": "021ac5050002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr_resolved",
                      "showname": "Address (resolved): MS-NLB-PhysServer-26_c5:05:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "0",
                      "show": "MS-NLB-PhysServer-26_c5:05:00:02",
                      "value": "021ac5050002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.lg",
                      "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                      "size": "3",
                      "pos": "0",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "021ac5"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.ig",
                      "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                      "size": "3",
                      "pos": "0",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "021ac5"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "eth.src",
                  "showname": "Source: MS-NLB-PhysServer-26_c5:01:00:02 (02:1a:c5:01:00:02)",
                  "size": "6",
                  "pos": "6",
                  "show": "02:1a:c5:01:00:02",
                  "value": "021ac5010002"
                },
                "field": [
                  {
                    "$": {
                      "name": "eth.src_resolved",
                      "showname": "Source (resolved): MS-NLB-PhysServer-26_c5:01:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "6",
                      "show": "MS-NLB-PhysServer-26_c5:01:00:02",
                      "value": "021ac5010002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr",
                      "showname": "Address: MS-NLB-PhysServer-26_c5:01:00:02 (02:1a:c5:01:00:02)",
                      "size": "6",
                      "pos": "6",
                      "show": "02:1a:c5:01:00:02",
                      "value": "021ac5010002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.addr_resolved",
                      "showname": "Address (resolved): MS-NLB-PhysServer-26_c5:01:00:02",
                      "hide": "yes",
                      "size": "6",
                      "pos": "6",
                      "show": "MS-NLB-PhysServer-26_c5:01:00:02",
                      "value": "021ac5010002"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.lg",
                      "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                      "size": "3",
                      "pos": "6",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "021ac5"
                    }
                  },
                  {
                    "$": {
                      "name": "eth.ig",
                      "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                      "size": "3",
                      "pos": "6",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "021ac5"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "eth.type",
                  "showname": "Type: IPv4 (0x0800)",
                  "size": "2",
                  "pos": "12",
                  "show": "0x00000800",
                  "value": "0800"
                }
              },
              {
                "$": {
                  "name": "eth.fcs",
                  "showname": "Frame check sequence: 0x26469e92 [correct]",
                  "size": "4",
                  "pos": "100",
                  "show": "0x26469e92",
                  "value": "26469e92"
                }
              },
              {
                "$": {
                  "name": "eth.fcs.status",
                  "showname": "FCS Status: Good",
                  "size": "0",
                  "pos": "100",
                  "show": "1"
                }
              }
            ]
          },
          {
            "$": {
              "name": "ip",
              "showname": "Internet Protocol Version 4, Src: 24.0.0.2, Dst: 24.128.0.2",
              "size": "20",
              "pos": "14"
            },
            "field": [
              {
                "$": {
                  "name": "ip.version",
                  "showname": "0100 .... = Version: 4",
                  "size": "1",
                  "pos": "14",
                  "show": "4",
                  "value": "4",
                  "unmaskedvalue": "45"
                }
              },
              {
                "$": {
                  "name": "ip.hdr_len",
                  "showname": ".... 0101 = Header Length: 20 bytes (5)",
                  "size": "1",
                  "pos": "14",
                  "show": "20",
                  "value": "45"
                }
              },
              {
                "$": {
                  "name": "ip.dsfield",
                  "showname": "Differentiated Services Field: 0x00 (DSCP: CS0, ECN: Not-ECT)",
                  "size": "1",
                  "pos": "15",
                  "show": "0x00000000",
                  "value": "00"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.dsfield.dscp",
                      "showname": "0000 00.. = Differentiated Services Codepoint: Default (0)",
                      "size": "1",
                      "pos": "15",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "00"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.dsfield.ecn",
                      "showname": ".... ..00 = Explicit Congestion Notification: Not ECN-Capable Transport (0)",
                      "size": "1",
                      "pos": "15",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "00"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "ip.len",
                  "showname": "Total Length: 86",
                  "size": "2",
                  "pos": "16",
                  "show": "86",
                  "value": "0056"
                }
              },
              {
                "$": {
                  "name": "ip.id",
                  "showname": "Identification: 0xcff6 (53238)",
                  "size": "2",
                  "pos": "18",
                  "show": "0x0000cff6",
                  "value": "cff6"
                }
              },
              {
                "$": {
                  "name": "ip.flags",
                  "showname": "Flags: 0x02 (Don't Fragment)",
                  "size": "1",
                  "pos": "20",
                  "show": "0x00000002",
                  "value": "40"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.flags.rb",
                      "showname": "0... .... = Reserved bit: Not set",
                      "size": "1",
                      "pos": "20",
                      "show": "0",
                      "value": "40"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.flags.df",
                      "showname": ".1.. .... = Don't fragment: Set",
                      "size": "1",
                      "pos": "20",
                      "show": "1",
                      "value": "40"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.flags.mf",
                      "showname": "..0. .... = More fragments: Not set",
                      "size": "1",
                      "pos": "20",
                      "show": "0",
                      "value": "40"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "ip.frag_offset",
                  "showname": "Fragment offset: 0",
                  "size": "2",
                  "pos": "20",
                  "show": "0",
                  "value": "4000"
                }
              },
              {
                "$": {
                  "name": "ip.ttl",
                  "showname": "Time to live: 32",
                  "size": "1",
                  "pos": "22",
                  "show": "32",
                  "value": "20"
                }
              },
              {
                "$": {
                  "name": "ip.proto",
                  "showname": "Protocol: TCP (6)",
                  "size": "1",
                  "pos": "23",
                  "show": "6",
                  "value": "06"
                }
              },
              {
                "$": {
                  "name": "ip.checksum",
                  "showname": "Header checksum: 0x5a28 [validation disabled]",
                  "size": "2",
                  "pos": "24",
                  "show": "0x00005a28",
                  "value": "5a28"
                }
              },
              {
                "$": {
                  "name": "ip.checksum.status",
                  "showname": "Header checksum status: Unverified",
                  "size": "0",
                  "pos": "24",
                  "show": "2"
                }
              },
              {
                "$": {
                  "name": "ip.src",
                  "showname": "Source: 24.0.0.2",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.addr",
                  "showname": "Source or Destination Address: 24.0.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.src_host",
                  "showname": "Source Host: 24.0.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.host",
                  "showname": "Source or Destination Host: 24.0.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "26",
                  "show": "24.0.0.2",
                  "value": "18000002"
                }
              },
              {
                "$": {
                  "name": "ip.dst",
                  "showname": "Destination: 24.128.0.2",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "ip.addr",
                  "showname": "Source or Destination Address: 24.128.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "ip.dst_host",
                  "showname": "Destination Host: 24.128.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "ip.host",
                  "showname": "Source or Destination Host: 24.128.0.2",
                  "hide": "yes",
                  "size": "4",
                  "pos": "30",
                  "show": "24.128.0.2",
                  "value": "18800002"
                }
              },
              {
                "$": {
                  "name": "",
                  "show": "Source GeoIP: United States, Woodbridge, NJ, AS7922 Comcast Cable Communications, LLC, United States, Woodbridge, NJ, AS7922 Comcast Cable Communications, LLC, 40.557598, -74.284599",
                  "size": "4",
                  "pos": "26",
                  "value": "18000002"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.geoip.src_country",
                      "showname": "Source GeoIP Country: United States",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_city",
                      "showname": "Source GeoIP City: Woodbridge, NJ",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Woodbridge, NJ",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_asnum",
                      "showname": "Source GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_country",
                      "showname": "Source GeoIP Country: United States",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "United States",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_city",
                      "showname": "Source GeoIP City: Woodbridge, NJ",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Woodbridge, NJ",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "Woodbridge, NJ",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_asnum",
                      "showname": "Source GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_lat",
                      "showname": "Source GeoIP Latitude: 40.557598",
                      "size": "4",
                      "pos": "26",
                      "show": "40.557598",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lat",
                      "showname": "Source or Destination GeoIP Latitude: 40.557598",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "40.557598",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.src_lon",
                      "showname": "Source GeoIP Longitude: -74.284599",
                      "size": "4",
                      "pos": "26",
                      "show": "-74.284599",
                      "value": "18000002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lon",
                      "showname": "Source or Destination GeoIP Longitude: -74.284599",
                      "hide": "yes",
                      "size": "4",
                      "pos": "26",
                      "show": "-74.284599",
                      "value": "18000002"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "",
                  "show": "Destination GeoIP: United States, Groton, CT, AS7922 Comcast Cable Communications, LLC, United States, Groton, CT, AS7922 Comcast Cable Communications, LLC, 41.353199, -72.038597",
                  "size": "4",
                  "pos": "30",
                  "value": "18800002"
                },
                "field": [
                  {
                    "$": {
                      "name": "ip.geoip.dst_country",
                      "showname": "Destination GeoIP Country: United States",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_city",
                      "showname": "Destination GeoIP City: Groton, CT",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Groton, CT",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_asnum",
                      "showname": "Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_country",
                      "showname": "Destination GeoIP Country: United States",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.country",
                      "showname": "Source or Destination GeoIP Country: United States",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "United States",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_city",
                      "showname": "Destination GeoIP City: Groton, CT",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.city",
                      "showname": "Source or Destination GeoIP City: Groton, CT",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "Groton, CT",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_asnum",
                      "showname": "Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.asnum",
                      "showname": "Source or Destination GeoIP AS Number: AS7922 Comcast Cable Communications, LLC",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "AS7922 Comcast Cable Communications, LLC",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_lat",
                      "showname": "Destination GeoIP Latitude: 41.353199",
                      "size": "4",
                      "pos": "30",
                      "show": "41.353199",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lat",
                      "showname": "Source or Destination GeoIP Latitude: 41.353199",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "41.353199",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.dst_lon",
                      "showname": "Destination GeoIP Longitude: -72.038597",
                      "size": "4",
                      "pos": "30",
                      "show": "-72.038597",
                      "value": "18800002"
                    }
                  },
                  {
                    "$": {
                      "name": "ip.geoip.lon",
                      "showname": "Source or Destination GeoIP Longitude: -72.038597",
                      "hide": "yes",
                      "size": "4",
                      "pos": "30",
                      "show": "-72.038597",
                      "value": "18800002"
                    }
                  }
                ]
              }
            ]
          },
          {
            "$": {
              "name": "tcp",
              "showname": "Transmission Control Protocol, Src Port: 2137, Dst Port: 25, Seq: 1, Ack: 1, Len: 34",
              "size": "32",
              "pos": "34"
            },
            "field": [
              {
                "$": {
                  "name": "tcp.srcport",
                  "showname": "Source Port: 2137",
                  "size": "2",
                  "pos": "34",
                  "show": "2137",
                  "value": "0859"
                }
              },
              {
                "$": {
                  "name": "tcp.dstport",
                  "showname": "Destination Port: 25",
                  "size": "2",
                  "pos": "36",
                  "show": "25",
                  "value": "0019"
                }
              },
              {
                "$": {
                  "name": "tcp.port",
                  "showname": "Source or Destination Port: 2137",
                  "hide": "yes",
                  "size": "2",
                  "pos": "34",
                  "show": "2137",
                  "value": "0859"
                }
              },
              {
                "$": {
                  "name": "tcp.port",
                  "showname": "Source or Destination Port: 25",
                  "hide": "yes",
                  "size": "2",
                  "pos": "36",
                  "show": "25",
                  "value": "0019"
                }
              },
              {
                "$": {
                  "name": "tcp.stream",
                  "showname": "Stream index: 0",
                  "size": "0",
                  "pos": "34",
                  "show": "0"
                }
              },
              {
                "$": {
                  "name": "tcp.len",
                  "showname": "TCP Segment Len: 34",
                  "size": "1",
                  "pos": "46",
                  "show": "34",
                  "value": "80"
                }
              },
              {
                "$": {
                  "name": "tcp.seq",
                  "showname": "Sequence number: 1    (relative sequence number)",
                  "size": "4",
                  "pos": "38",
                  "show": "1",
                  "value": "f88900ce"
                }
              },
              {
                "$": {
                  "name": "tcp.nxtseq",
                  "showname": "Next sequence number: 35    (relative sequence number)",
                  "size": "0",
                  "pos": "34",
                  "show": "35"
                }
              },
              {
                "$": {
                  "name": "tcp.ack",
                  "showname": "Acknowledgment number: 1    (relative ack number)",
                  "size": "4",
                  "pos": "42",
                  "show": "1",
                  "value": "365aa74f"
                }
              },
              {
                "$": {
                  "name": "tcp.hdr_len",
                  "showname": "1000 .... = Header Length: 32 bytes (8)",
                  "size": "1",
                  "pos": "46",
                  "show": "32",
                  "value": "80"
                }
              },
              {
                "$": {
                  "name": "tcp.flags",
                  "showname": "Flags: 0x018 (PSH, ACK)",
                  "size": "2",
                  "pos": "46",
                  "show": "0x00000018",
                  "value": "18",
                  "unmaskedvalue": "8018"
                },
                "field": [
                  {
                    "$": {
                      "name": "tcp.flags.res",
                      "showname": "000. .... .... = Reserved: Not set",
                      "size": "1",
                      "pos": "46",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "80"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.ns",
                      "showname": "...0 .... .... = Nonce: Not set",
                      "size": "1",
                      "pos": "46",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "80"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.cwr",
                      "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.ecn",
                      "showname": ".... .0.. .... = ECN-Echo: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.urg",
                      "showname": ".... ..0. .... = Urgent: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.ack",
                      "showname": ".... ...1 .... = Acknowledgment: Set",
                      "size": "1",
                      "pos": "47",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.push",
                      "showname": ".... .... 1... = Push: Set",
                      "size": "1",
                      "pos": "47",
                      "show": "1",
                      "value": "1",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.reset",
                      "showname": ".... .... .0.. = Reset: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.syn",
                      "showname": ".... .... ..0. = Syn: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.fin",
                      "showname": ".... .... ...0 = Fin: Not set",
                      "size": "1",
                      "pos": "47",
                      "show": "0",
                      "value": "0",
                      "unmaskedvalue": "18"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.flags.str",
                      "showname": "TCP Flags: \\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7AP\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7",
                      "size": "2",
                      "pos": "46",
                      "show": "\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7AP\\\\xc2\\\\xb7\\\\xc2\\\\xb7\\\\xc2\\\\xb7",
                      "value": "8018"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "tcp.window_size_value",
                  "showname": "Window size value: 7240",
                  "size": "2",
                  "pos": "48",
                  "show": "7240",
                  "value": "1c48"
                }
              },
              {
                "$": {
                  "name": "tcp.window_size",
                  "showname": "Calculated window size: 7240",
                  "size": "2",
                  "pos": "48",
                  "show": "7240",
                  "value": "1c48"
                }
              },
              {
                "$": {
                  "name": "tcp.window_size_scalefactor",
                  "showname": "Window size scaling factor: -1 (unknown)",
                  "size": "2",
                  "pos": "48",
                  "show": "-1",
                  "value": "1c48"
                }
              },
              {
                "$": {
                  "name": "tcp.checksum",
                  "showname": "Checksum: 0x681f [unverified]",
                  "size": "2",
                  "pos": "50",
                  "show": "0x0000681f",
                  "value": "681f"
                }
              },
              {
                "$": {
                  "name": "tcp.checksum.status",
                  "showname": "Checksum Status: Unverified",
                  "size": "0",
                  "pos": "50",
                  "show": "2"
                }
              },
              {
                "$": {
                  "name": "tcp.urgent_pointer",
                  "showname": "Urgent pointer: 0",
                  "size": "2",
                  "pos": "52",
                  "show": "0",
                  "value": "0000"
                }
              },
              {
                "$": {
                  "name": "tcp.options",
                  "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
                  "size": "12",
                  "pos": "54",
                  "show": "01:01:08:0a:eb:83:4b:08:e8:8c:de:cb",
                  "value": "0101080aeb834b08e88cdecb"
                },
                "field": [
                  {
                    "$": {
                      "name": "tcp.options.nop",
                      "showname": "TCP Option - No-Operation (NOP)",
                      "size": "1",
                      "pos": "54",
                      "show": "01",
                      "value": "01"
                    },
                    "field": [
                      {
                        "$": {
                          "name": "tcp.option_kind",
                          "showname": "Kind: No-Operation (1)",
                          "size": "1",
                          "pos": "54",
                          "show": "1",
                          "value": "01"
                        }
                      }
                    ]
                  },
                  {
                    "$": {
                      "name": "tcp.options.nop",
                      "showname": "TCP Option - No-Operation (NOP)",
                      "size": "1",
                      "pos": "55",
                      "show": "01",
                      "value": "01"
                    },
                    "field": [
                      {
                        "$": {
                          "name": "tcp.option_kind",
                          "showname": "Kind: No-Operation (1)",
                          "size": "1",
                          "pos": "55",
                          "show": "1",
                          "value": "01"
                        }
                      }
                    ]
                  },
                  {
                    "$": {
                      "name": "tcp.options.timestamp",
                      "showname": "TCP Option - Timestamps: TSval 3951250184, TSecr 3901546187",
                      "size": "10",
                      "pos": "56",
                      "show": "08:0a:eb:83:4b:08:e8:8c:de:cb",
                      "value": "080aeb834b08e88cdecb"
                    },
                    "field": [
                      {
                        "$": {
                          "name": "tcp.option_kind",
                          "showname": "Kind: Time Stamp Option (8)",
                          "size": "1",
                          "pos": "56",
                          "show": "8",
                          "value": "08"
                        }
                      },
                      {
                        "$": {
                          "name": "tcp.option_len",
                          "showname": "Length: 10",
                          "size": "1",
                          "pos": "57",
                          "show": "10",
                          "value": "0a"
                        }
                      },
                      {
                        "$": {
                          "name": "tcp.options.timestamp.tsval",
                          "showname": "Timestamp value: 3951250184",
                          "size": "4",
                          "pos": "58",
                          "show": "3951250184",
                          "value": "eb834b08"
                        }
                      },
                      {
                        "$": {
                          "name": "tcp.options.timestamp.tsecr",
                          "showname": "Timestamp echo reply: 3901546187",
                          "size": "4",
                          "pos": "62",
                          "show": "3901546187",
                          "value": "e88cdecb"
                        }
                      }
                    ]
                  }
                ]
              },
              {
                "$": {
                  "name": "tcp.analysis",
                  "showname": "SEQ/ACK analysis",
                  "size": "0",
                  "pos": "34",
                  "show": "",
                  "value": ""
                },
                "field": [
                  {
                    "$": {
                      "name": "tcp.analysis.bytes_in_flight",
                      "showname": "Bytes in flight: 34",
                      "size": "0",
                      "pos": "34",
                      "show": "34"
                    }
                  },
                  {
                    "$": {
                      "name": "tcp.analysis.push_bytes_sent",
                      "showname": "Bytes sent since last PSH flag: 34",
                      "size": "0",
                      "pos": "34",
                      "show": "34"
                    }
                  }
                ]
              },
              {
                "$": {
                  "name": "tcp.payload",
                  "showname": "TCP payload (34 bytes)",
                  "size": "34",
                  "pos": "66",
                  "show": "45:48:4c:4f:20:63:6c:69:65:6e:74:2d:31:38:30:30:30:30:30:33:2e:65:78:61:6d:70:6c:65:2e:69:6e:74:0d:0a",
                  "value": "45484c4f20636c69656e742d31383030303030332e6578616d706c652e696e740d0a"
                }
              }
            ]
          },
          {
            "$": {
              "name": "smtp",
              "showname": "Simple Mail Transfer Protocol",
              "size": "34",
              "pos": "66"
            },
            "field": [
              {
                "$": {
                  "name": "smtp.req",
                  "showname": "Request: True",
                  "hide": "yes",
                  "size": "0",
                  "pos": "66",
                  "show": "1"
                }
              },
              {
                "$": {
                  "name": "smtp.command_line",
                  "showname": "Command Line: EHLO client-18000003.example.int\\\\r\\\\n",
                  "size": "34",
                  "pos": "66",
                  "show": "EHLO client-18000003.example.int\\\\xd\\\\xa",
                  "value": "45484c4f20636c69656e742d31383030303030332e6578616d706c652e696e740d0a"
                },
                "field": [
                  {
                    "$": {
                      "name": "smtp.req.command",
                      "showname": "Command: EHLO",
                      "size": "4",
                      "pos": "66",
                      "show": "EHLO",
                      "value": "45484c4f"
                    }
                  },
                  {
                    "$": {
                      "name": "smtp.req.parameter",
                      "showname": "Request parameter: client-18000003.example.int",
                      "size": "27",
                      "pos": "71",
                      "show": "client-18000003.example.int",
                      "value": "636c69656e742d31383030303030332e6578616d706c652e696e74"
                    }
                  }
                ]
              }
            ]
          }
        ]
      }
    ]
  }
}
`
}
