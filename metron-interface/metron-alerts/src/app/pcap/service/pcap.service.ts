import {Injectable, NgZone} from '@angular/core';
import {Observable, Subject} from 'rxjs/Rx';
import {Http, Headers, RequestOptions, Response} from '@angular/http';
import {HttpUtil} from '../../utils/httpUtil';

import 'rxjs/add/operator/map';

import {PcapRequest} from '../model/pcap.request';
import {Pdml} from '../model/pdml'

@Injectable()
export class PcapService {

    private statusInterval = 4;
    defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};

    constructor(private http: Http, private ngZone: NgZone) {
    }

    public pollStatus(id: string): Observable<string> {
        return this.ngZone.runOutsideAngular(() => {
            return this.ngZone.run(() => {
                return Observable.interval(this.statusInterval * 1000).switchMap(() => {
                    return this.getStatus(id);
                });
            });
        });
    }

    public submitRequest(pcapRequest: PcapRequest): Observable<string> {
        return this.http.post('/api/v1/pcap/pcapqueryfilterasync/submit', pcapRequest, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
            .map(HttpUtil.extractString)
            .catch(HttpUtil.handleError)
            .onErrorResumeNext();
    }

    public getStatus(id: string): Observable<string> {
        return this.http.get('/api/v1/pcap/pcapqueryfilterasync/status?idQuery=' + id,
            new RequestOptions({headers: new Headers(this.defaultHeaders)}))
            .map(HttpUtil.extractString)
            .catch(HttpUtil.handleError)
    }

    public getPackets(id: string): Observable<Pdml> {
        return this.http.get('/api/v1/pcap/pcapqueryfilterasync/resultJson?idQuery=' + id, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
            .map(HttpUtil.extractData)
            .catch(HttpUtil.handleError)
            .onErrorResumeNext();
    }

    public getTestPackets(request: PcapRequest): Observable<Pdml> {
        return Observable.create((o) => o.next(JSON.parse(pdml_json2())))
    }
}

function pdml_json2() {
    return `{
  "version": "0",
  "creator": "wireshark/1.8.10",
  "time": "Thu Apr 12 19:41:33 2018",
  "capture_file": "/tmp/pcapQuery_116205077406675/pcap-data-201804121940-9d557e044ec6445aa395414feceba2f3+0001.pcap",
  "packets": [
    {
      "protos": [
        {
          "name": "geninfo",
          "pos": "0",
          "showname": "General information",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "num",
              "pos": "0",
              "showname": "Number",
              "size": "66",
              "value": "1",
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "len",
              "pos": "0",
              "showname": "Frame Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "caplen",
              "pos": "0",
              "showname": "Captured Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "timestamp",
              "pos": "0",
              "showname": "Captured Time",
              "size": "66",
              "value": "1522244608.113998000",
              "show": "Mar 28, 2018 13:43:28.113998000 UTC",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "frame",
          "pos": "0",
          "showname": "Frame 1: 66 bytes on wire (528 bits), 66 bytes captured (528 bits)",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "frame.dlt",
              "pos": "0",
              "showname": "WTAP_ENCAP: 1",
              "size": "0",
              "value": null,
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time",
              "pos": "0",
              "showname": "Arrival Time: Mar 28, 2018 13:43:28.113998000 UTC",
              "size": "0",
              "value": null,
              "show": "Mar 28, 2018 13:43:28.113998000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.offset_shift",
              "pos": "0",
              "showname": "Time shift for this packet: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_epoch",
              "pos": "0",
              "showname": "Epoch Time: 1522244608.113998000 seconds",
              "size": "0",
              "value": null,
              "show": "1522244608.113998000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta",
              "pos": "0",
              "showname": "Time delta from previous captured frame: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta_displayed",
              "pos": "0",
              "showname": "Time delta from previous displayed frame: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_relative",
              "pos": "0",
              "showname": "Time since reference or first frame: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.number",
              "pos": "0",
              "showname": "Frame Number: 1",
              "size": "0",
              "value": null,
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.len",
              "pos": "0",
              "showname": "Frame Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.cap_len",
              "pos": "0",
              "showname": "Capture Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.marked",
              "pos": "0",
              "showname": "Frame is marked: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.ignored",
              "pos": "0",
              "showname": "Frame is ignored: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.protocols",
              "pos": "0",
              "showname": "Protocols in frame: eth:ip:tcp",
              "size": "0",
              "value": null,
              "show": "eth:ip:tcp",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "eth",
          "pos": "0",
          "showname": "Ethernet II, Src: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1), Dst: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
          "size": "14",
          "hide": null,
          "fields": [
            {
              "name": "eth.dst",
              "pos": "0",
              "showname": "Destination: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
              "size": "6",
              "value": "fa163e04cd37",
              "show": "fa:16:3e:04:cd:37",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "0",
                  "showname": "Address: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
                  "size": "6",
                  "value": "fa163e04cd37",
                  "show": "fa:16:3e:04:cd:37",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "0",
                  "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                  "size": "3",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "0",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.src",
              "pos": "6",
              "showname": "Source: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
              "size": "6",
              "value": "8478ac5be8c1",
              "show": "84:78:ac:5b:e8:c1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "6",
                  "showname": "Address: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
                  "size": "6",
                  "value": "8478ac5be8c1",
                  "show": "84:78:ac:5b:e8:c1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "6",
                  "showname": ".... ..0. .... .... .... .... = LG bit: Globally unique address (factory default)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "6",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.type",
              "pos": "12",
              "showname": "Type: IP (0x0800)",
              "size": "2",
              "value": "0800",
              "show": "0x0800",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "ip",
          "pos": "14",
          "showname": "Internet Protocol Version 4, Src: 10.200.10.172 (10.200.10.172), Dst: 172.26.215.106 (172.26.215.106)",
          "size": "20",
          "hide": null,
          "fields": [
            {
              "name": "ip.version",
              "pos": "14",
              "showname": "Version: 4",
              "size": "1",
              "value": "45",
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.hdr_len",
              "pos": "14",
              "showname": "Header length: 20 bytes",
              "size": "1",
              "value": "45",
              "show": "20",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dsfield",
              "pos": "15",
              "showname": "Differentiated Services Field: 0x00 (DSCP 0x00: Default; ECN: 0x00: Not-ECT (Not ECN-Capable Transport))",
              "size": "1",
              "value": "00",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.dsfield.dscp",
                  "pos": "15",
                  "showname": "0000 00.. = Differentiated Services Codepoint: Default (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.dsfield.ecn",
                  "pos": "15",
                  "showname": ".... ..00 = Explicit Congestion Notification: Not-ECT (Not ECN-Capable Transport) (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.len",
              "pos": "16",
              "showname": "Total Length: 52",
              "size": "2",
              "value": "0034",
              "show": "52",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.id",
              "pos": "18",
              "showname": "Identification: 0x0000 (0)",
              "size": "2",
              "value": "0000",
              "show": "0x0000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.flags",
              "pos": "20",
              "showname": "Flags: 0x02 (Don't Fragment)",
              "size": "1",
              "value": "40",
              "show": "0x02",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.flags.rb",
                  "pos": "20",
                  "showname": "0... .... = Reserved bit: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.df",
                  "pos": "20",
                  "showname": ".1.. .... = Don't fragment: Set",
                  "size": "1",
                  "value": "40",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.mf",
                  "pos": "20",
                  "showname": "..0. .... = More fragments: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.frag_offset",
              "pos": "20",
              "showname": "Fragment offset: 0",
              "size": "2",
              "value": "4000",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.ttl",
              "pos": "22",
              "showname": "Time to live: 62",
              "size": "1",
              "value": "3e",
              "show": "62",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.proto",
              "pos": "23",
              "showname": "Protocol: TCP (6)",
              "size": "1",
              "value": "06",
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.checksum",
              "pos": "24",
              "showname": "Header checksum: 0xa3cb [correct]",
              "size": "2",
              "value": "a3cb",
              "show": "0xa3cb",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.checksum_good",
                  "pos": "24",
                  "showname": "Good: True",
                  "size": "2",
                  "value": "a3cb",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.checksum_bad",
                  "pos": "24",
                  "showname": "Bad: False",
                  "size": "2",
                  "value": "a3cb",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.src",
              "pos": "26",
              "showname": "Source: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "26",
              "showname": "Source or Destination Address: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.src_host",
              "pos": "26",
              "showname": "Source Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "26",
              "showname": "Source or Destination Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst",
              "pos": "30",
              "showname": "Destination: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "30",
              "showname": "Source or Destination Address: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst_host",
              "pos": "30",
              "showname": "Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "30",
              "showname": "Source or Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "tcp",
          "pos": "34",
          "showname": "Transmission Control Protocol, Src Port: 52834 (52834), Dst Port: ssh (22), Seq: 1, Ack: 1, Len: 0",
          "size": "32",
          "hide": null,
          "fields": [
            {
              "name": "tcp.srcport",
              "pos": "34",
              "showname": "Source port: 52834 (52834)",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.dstport",
              "pos": "36",
              "showname": "Destination port: ssh (22)",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "34",
              "showname": "Source or Destination Port: 52834",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "36",
              "showname": "Source or Destination Port: 22",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.stream",
              "pos": "34",
              "showname": "Stream index: 0",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.len",
              "pos": "46",
              "showname": "TCP Segment Len: 0",
              "size": "1",
              "value": "80",
              "show": "0",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.seq",
              "pos": "38",
              "showname": "Sequence number: 1    (relative sequence number)",
              "size": "4",
              "value": "3e7a345c",
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.ack",
              "pos": "42",
              "showname": "Acknowledgment number: 1    (relative ack number)",
              "size": "4",
              "value": "342f6f2e",
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.hdr_len",
              "pos": "46",
              "showname": "Header length: 32 bytes",
              "size": "1",
              "value": "80",
              "show": "32",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.flags",
              "pos": "46",
              "showname": "Flags: 0x010 (ACK)",
              "size": "2",
              "value": "10",
              "show": "0x0010",
              "unmaskedvalue": "8010",
              "hide": null,
              "fields": [
                {
                  "name": "tcp.flags.res",
                  "pos": "46",
                  "showname": "000. .... .... = Reserved: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ns",
                  "pos": "46",
                  "showname": "...0 .... .... = Nonce: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.cwr",
                  "pos": "47",
                  "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ecn",
                  "pos": "47",
                  "showname": ".... .0.. .... = ECN-Echo: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.urg",
                  "pos": "47",
                  "showname": ".... ..0. .... = Urgent: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ack",
                  "pos": "47",
                  "showname": ".... ...1 .... = Acknowledgment: Set",
                  "size": "1",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.push",
                  "pos": "47",
                  "showname": ".... .... 0... = Push: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.reset",
                  "pos": "47",
                  "showname": ".... .... .0.. = Reset: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.syn",
                  "pos": "47",
                  "showname": ".... .... ..0. = Syn: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.fin",
                  "pos": "47",
                  "showname": ".... .... ...0 = Fin: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.window_size_value",
              "pos": "48",
              "showname": "Window size value: 4094",
              "size": "2",
              "value": "0ffe",
              "show": "4094",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size",
              "pos": "48",
              "showname": "Calculated window size: 4094",
              "size": "2",
              "value": "0ffe",
              "show": "4094",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size_scalefactor",
              "pos": "48",
              "showname": "Window size scaling factor: -1 (unknown)",
              "size": "2",
              "value": "0ffe",
              "show": "-1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.checksum",
              "pos": "50",
              "showname": "Checksum: 0xf22c [validation disabled]",
              "size": "2",
              "value": "f22c",
              "show": "0xf22c",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "tcp.checksum_good",
                  "pos": "50",
                  "showname": "Good Checksum: False",
                  "size": "2",
                  "value": "f22c",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.checksum_bad",
                  "pos": "50",
                  "showname": "Bad Checksum: False",
                  "size": "2",
                  "value": "f22c",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.options",
              "pos": "54",
              "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
              "size": "12",
              "value": "0101080a1ec843d604758fd9",
              "show": "01:01:08:0a:1e:c8:43:d6:04:75:8f:d9",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "",
                  "pos": "54",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "54",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "54",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "54",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "54",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "55",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "55",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "55",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "55",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "55",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "56",
                  "showname": null,
                  "size": "10",
                  "value": "080a1ec843d604758fd9",
                  "show": "Timestamps: TSval 516441046, TSecr 74813401",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "tcp.option_kind",
                      "pos": "56",
                      "showname": "Kind: Timestamp (8)",
                      "size": "1",
                      "value": "08",
                      "show": "8",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.option_len",
                      "pos": "57",
                      "showname": "Length: 10",
                      "size": "1",
                      "value": "0a",
                      "show": "10",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsval",
                      "pos": "58",
                      "showname": "Timestamp value: 516441046",
                      "size": "4",
                      "value": "1ec843d6",
                      "show": "516441046",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsecr",
                      "pos": "62",
                      "showname": "Timestamp echo reply: 74813401",
                      "size": "4",
                      "value": "04758fd9",
                      "show": "74813401",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    }
                  ],
                  "protos": null
                }
              ],
              "protos": null
            }
          ]
        }
      ]
    },
    {
      "protos": [
        {
          "name": "geninfo",
          "pos": "0",
          "showname": "General information",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "num",
              "pos": "0",
              "showname": "Number",
              "size": "66",
              "value": "2",
              "show": "2",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "len",
              "pos": "0",
              "showname": "Frame Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "caplen",
              "pos": "0",
              "showname": "Captured Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "timestamp",
              "pos": "0",
              "showname": "Captured Time",
              "size": "66",
              "value": "1522244608.165212000",
              "show": "Mar 28, 2018 13:43:28.165212000 UTC",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "frame",
          "pos": "0",
          "showname": "Frame 2: 66 bytes on wire (528 bits), 66 bytes captured (528 bits)",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "frame.dlt",
              "pos": "0",
              "showname": "WTAP_ENCAP: 1",
              "size": "0",
              "value": null,
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time",
              "pos": "0",
              "showname": "Arrival Time: Mar 28, 2018 13:43:28.165212000 UTC",
              "size": "0",
              "value": null,
              "show": "Mar 28, 2018 13:43:28.165212000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.offset_shift",
              "pos": "0",
              "showname": "Time shift for this packet: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_epoch",
              "pos": "0",
              "showname": "Epoch Time: 1522244608.165212000 seconds",
              "size": "0",
              "value": null,
              "show": "1522244608.165212000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta",
              "pos": "0",
              "showname": "Time delta from previous captured frame: 0.051214000 seconds",
              "size": "0",
              "value": null,
              "show": "0.051214000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta_displayed",
              "pos": "0",
              "showname": "Time delta from previous displayed frame: 0.051214000 seconds",
              "size": "0",
              "value": null,
              "show": "0.051214000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_relative",
              "pos": "0",
              "showname": "Time since reference or first frame: 0.051214000 seconds",
              "size": "0",
              "value": null,
              "show": "0.051214000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.number",
              "pos": "0",
              "showname": "Frame Number: 2",
              "size": "0",
              "value": null,
              "show": "2",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.len",
              "pos": "0",
              "showname": "Frame Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.cap_len",
              "pos": "0",
              "showname": "Capture Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.marked",
              "pos": "0",
              "showname": "Frame is marked: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.ignored",
              "pos": "0",
              "showname": "Frame is ignored: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.protocols",
              "pos": "0",
              "showname": "Protocols in frame: eth:ip:tcp",
              "size": "0",
              "value": null,
              "show": "eth:ip:tcp",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "eth",
          "pos": "0",
          "showname": "Ethernet II, Src: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1), Dst: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
          "size": "14",
          "hide": null,
          "fields": [
            {
              "name": "eth.dst",
              "pos": "0",
              "showname": "Destination: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
              "size": "6",
              "value": "fa163e04cd37",
              "show": "fa:16:3e:04:cd:37",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "0",
                  "showname": "Address: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
                  "size": "6",
                  "value": "fa163e04cd37",
                  "show": "fa:16:3e:04:cd:37",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "0",
                  "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                  "size": "3",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "0",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.src",
              "pos": "6",
              "showname": "Source: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
              "size": "6",
              "value": "8478ac5be8c1",
              "show": "84:78:ac:5b:e8:c1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "6",
                  "showname": "Address: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
                  "size": "6",
                  "value": "8478ac5be8c1",
                  "show": "84:78:ac:5b:e8:c1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "6",
                  "showname": ".... ..0. .... .... .... .... = LG bit: Globally unique address (factory default)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "6",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.type",
              "pos": "12",
              "showname": "Type: IP (0x0800)",
              "size": "2",
              "value": "0800",
              "show": "0x0800",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "ip",
          "pos": "14",
          "showname": "Internet Protocol Version 4, Src: 10.200.10.172 (10.200.10.172), Dst: 172.26.215.106 (172.26.215.106)",
          "size": "20",
          "hide": null,
          "fields": [
            {
              "name": "ip.version",
              "pos": "14",
              "showname": "Version: 4",
              "size": "1",
              "value": "45",
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.hdr_len",
              "pos": "14",
              "showname": "Header length: 20 bytes",
              "size": "1",
              "value": "45",
              "show": "20",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dsfield",
              "pos": "15",
              "showname": "Differentiated Services Field: 0x00 (DSCP 0x00: Default; ECN: 0x00: Not-ECT (Not ECN-Capable Transport))",
              "size": "1",
              "value": "00",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.dsfield.dscp",
                  "pos": "15",
                  "showname": "0000 00.. = Differentiated Services Codepoint: Default (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.dsfield.ecn",
                  "pos": "15",
                  "showname": ".... ..00 = Explicit Congestion Notification: Not-ECT (Not ECN-Capable Transport) (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.len",
              "pos": "16",
              "showname": "Total Length: 52",
              "size": "2",
              "value": "0034",
              "show": "52",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.id",
              "pos": "18",
              "showname": "Identification: 0x0000 (0)",
              "size": "2",
              "value": "0000",
              "show": "0x0000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.flags",
              "pos": "20",
              "showname": "Flags: 0x02 (Don't Fragment)",
              "size": "1",
              "value": "40",
              "show": "0x02",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.flags.rb",
                  "pos": "20",
                  "showname": "0... .... = Reserved bit: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.df",
                  "pos": "20",
                  "showname": ".1.. .... = Don't fragment: Set",
                  "size": "1",
                  "value": "40",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.mf",
                  "pos": "20",
                  "showname": "..0. .... = More fragments: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.frag_offset",
              "pos": "20",
              "showname": "Fragment offset: 0",
              "size": "2",
              "value": "4000",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.ttl",
              "pos": "22",
              "showname": "Time to live: 62",
              "size": "1",
              "value": "3e",
              "show": "62",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.proto",
              "pos": "23",
              "showname": "Protocol: TCP (6)",
              "size": "1",
              "value": "06",
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.checksum",
              "pos": "24",
              "showname": "Header checksum: 0xa3cb [correct]",
              "size": "2",
              "value": "a3cb",
              "show": "0xa3cb",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.checksum_good",
                  "pos": "24",
                  "showname": "Good: True",
                  "size": "2",
                  "value": "a3cb",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.checksum_bad",
                  "pos": "24",
                  "showname": "Bad: False",
                  "size": "2",
                  "value": "a3cb",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.src",
              "pos": "26",
              "showname": "Source: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "26",
              "showname": "Source or Destination Address: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.src_host",
              "pos": "26",
              "showname": "Source Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "26",
              "showname": "Source or Destination Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst",
              "pos": "30",
              "showname": "Destination: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "30",
              "showname": "Source or Destination Address: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst_host",
              "pos": "30",
              "showname": "Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "30",
              "showname": "Source or Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "tcp",
          "pos": "34",
          "showname": "Transmission Control Protocol, Src Port: 52834 (52834), Dst Port: ssh (22), Seq: 1, Ack: 165, Len: 0",
          "size": "32",
          "hide": null,
          "fields": [
            {
              "name": "tcp.srcport",
              "pos": "34",
              "showname": "Source port: 52834 (52834)",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.dstport",
              "pos": "36",
              "showname": "Destination port: ssh (22)",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "34",
              "showname": "Source or Destination Port: 52834",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "36",
              "showname": "Source or Destination Port: 22",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.stream",
              "pos": "34",
              "showname": "Stream index: 0",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.len",
              "pos": "46",
              "showname": "TCP Segment Len: 0",
              "size": "1",
              "value": "80",
              "show": "0",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.seq",
              "pos": "38",
              "showname": "Sequence number: 1    (relative sequence number)",
              "size": "4",
              "value": "3e7a345c",
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.ack",
              "pos": "42",
              "showname": "Acknowledgment number: 165    (relative ack number)",
              "size": "4",
              "value": "342f6fd2",
              "show": "165",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.hdr_len",
              "pos": "46",
              "showname": "Header length: 32 bytes",
              "size": "1",
              "value": "80",
              "show": "32",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.flags",
              "pos": "46",
              "showname": "Flags: 0x010 (ACK)",
              "size": "2",
              "value": "10",
              "show": "0x0010",
              "unmaskedvalue": "8010",
              "hide": null,
              "fields": [
                {
                  "name": "tcp.flags.res",
                  "pos": "46",
                  "showname": "000. .... .... = Reserved: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ns",
                  "pos": "46",
                  "showname": "...0 .... .... = Nonce: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.cwr",
                  "pos": "47",
                  "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ecn",
                  "pos": "47",
                  "showname": ".... .0.. .... = ECN-Echo: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.urg",
                  "pos": "47",
                  "showname": ".... ..0. .... = Urgent: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ack",
                  "pos": "47",
                  "showname": ".... ...1 .... = Acknowledgment: Set",
                  "size": "1",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.push",
                  "pos": "47",
                  "showname": ".... .... 0... = Push: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.reset",
                  "pos": "47",
                  "showname": ".... .... .0.. = Reset: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.syn",
                  "pos": "47",
                  "showname": ".... .... ..0. = Syn: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.fin",
                  "pos": "47",
                  "showname": ".... .... ...0 = Fin: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.window_size_value",
              "pos": "48",
              "showname": "Window size value: 4090",
              "size": "2",
              "value": "0ffa",
              "show": "4090",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size",
              "pos": "48",
              "showname": "Calculated window size: 4090",
              "size": "2",
              "value": "0ffa",
              "show": "4090",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size_scalefactor",
              "pos": "48",
              "showname": "Window size scaling factor: -1 (unknown)",
              "size": "2",
              "value": "0ffa",
              "show": "-1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.checksum",
              "pos": "50",
              "showname": "Checksum: 0xf127 [validation disabled]",
              "size": "2",
              "value": "f127",
              "show": "0xf127",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "tcp.checksum_good",
                  "pos": "50",
                  "showname": "Good Checksum: False",
                  "size": "2",
                  "value": "f127",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.checksum_bad",
                  "pos": "50",
                  "showname": "Bad Checksum: False",
                  "size": "2",
                  "value": "f127",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.options",
              "pos": "54",
              "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
              "size": "12",
              "value": "0101080a1ec844090475900b",
              "show": "01:01:08:0a:1e:c8:44:09:04:75:90:0b",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "",
                  "pos": "54",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "54",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "54",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "54",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "54",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "55",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "55",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "55",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "55",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "55",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "56",
                  "showname": null,
                  "size": "10",
                  "value": "080a1ec844090475900b",
                  "show": "Timestamps: TSval 516441097, TSecr 74813451",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "tcp.option_kind",
                      "pos": "56",
                      "showname": "Kind: Timestamp (8)",
                      "size": "1",
                      "value": "08",
                      "show": "8",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.option_len",
                      "pos": "57",
                      "showname": "Length: 10",
                      "size": "1",
                      "value": "0a",
                      "show": "10",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsval",
                      "pos": "58",
                      "showname": "Timestamp value: 516441097",
                      "size": "4",
                      "value": "1ec84409",
                      "show": "516441097",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsecr",
                      "pos": "62",
                      "showname": "Timestamp echo reply: 74813451",
                      "size": "4",
                      "value": "0475900b",
                      "show": "74813451",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    }
                  ],
                  "protos": null
                }
              ],
              "protos": null
            }
          ]
        }
      ]
    },
    {
      "protos": [
        {
          "name": "geninfo",
          "pos": "0",
          "showname": "General information",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "num",
              "pos": "0",
              "showname": "Number",
              "size": "66",
              "value": "3",
              "show": "3",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "len",
              "pos": "0",
              "showname": "Frame Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "caplen",
              "pos": "0",
              "showname": "Captured Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "timestamp",
              "pos": "0",
              "showname": "Captured Time",
              "size": "66",
              "value": "1522244608.165282000",
              "show": "Mar 28, 2018 13:43:28.165282000 UTC",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "frame",
          "pos": "0",
          "showname": "Frame 3: 66 bytes on wire (528 bits), 66 bytes captured (528 bits)",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "frame.dlt",
              "pos": "0",
              "showname": "WTAP_ENCAP: 1",
              "size": "0",
              "value": null,
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time",
              "pos": "0",
              "showname": "Arrival Time: Mar 28, 2018 13:43:28.165282000 UTC",
              "size": "0",
              "value": null,
              "show": "Mar 28, 2018 13:43:28.165282000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.offset_shift",
              "pos": "0",
              "showname": "Time shift for this packet: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_epoch",
              "pos": "0",
              "showname": "Epoch Time: 1522244608.165282000 seconds",
              "size": "0",
              "value": null,
              "show": "1522244608.165282000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta",
              "pos": "0",
              "showname": "Time delta from previous captured frame: 0.000070000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000070000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta_displayed",
              "pos": "0",
              "showname": "Time delta from previous displayed frame: 0.000070000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000070000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_relative",
              "pos": "0",
              "showname": "Time since reference or first frame: 0.051284000 seconds",
              "size": "0",
              "value": null,
              "show": "0.051284000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.number",
              "pos": "0",
              "showname": "Frame Number: 3",
              "size": "0",
              "value": null,
              "show": "3",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.len",
              "pos": "0",
              "showname": "Frame Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.cap_len",
              "pos": "0",
              "showname": "Capture Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.marked",
              "pos": "0",
              "showname": "Frame is marked: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.ignored",
              "pos": "0",
              "showname": "Frame is ignored: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.protocols",
              "pos": "0",
              "showname": "Protocols in frame: eth:ip:tcp",
              "size": "0",
              "value": null,
              "show": "eth:ip:tcp",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "eth",
          "pos": "0",
          "showname": "Ethernet II, Src: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1), Dst: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
          "size": "14",
          "hide": null,
          "fields": [
            {
              "name": "eth.dst",
              "pos": "0",
              "showname": "Destination: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
              "size": "6",
              "value": "fa163e04cd37",
              "show": "fa:16:3e:04:cd:37",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "0",
                  "showname": "Address: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
                  "size": "6",
                  "value": "fa163e04cd37",
                  "show": "fa:16:3e:04:cd:37",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "0",
                  "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                  "size": "3",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "0",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.src",
              "pos": "6",
              "showname": "Source: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
              "size": "6",
              "value": "8478ac5be8c1",
              "show": "84:78:ac:5b:e8:c1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "6",
                  "showname": "Address: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
                  "size": "6",
                  "value": "8478ac5be8c1",
                  "show": "84:78:ac:5b:e8:c1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "6",
                  "showname": ".... ..0. .... .... .... .... = LG bit: Globally unique address (factory default)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "6",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.type",
              "pos": "12",
              "showname": "Type: IP (0x0800)",
              "size": "2",
              "value": "0800",
              "show": "0x0800",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "ip",
          "pos": "14",
          "showname": "Internet Protocol Version 4, Src: 10.200.10.172 (10.200.10.172), Dst: 172.26.215.106 (172.26.215.106)",
          "size": "20",
          "hide": null,
          "fields": [
            {
              "name": "ip.version",
              "pos": "14",
              "showname": "Version: 4",
              "size": "1",
              "value": "45",
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.hdr_len",
              "pos": "14",
              "showname": "Header length: 20 bytes",
              "size": "1",
              "value": "45",
              "show": "20",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dsfield",
              "pos": "15",
              "showname": "Differentiated Services Field: 0x00 (DSCP 0x00: Default; ECN: 0x00: Not-ECT (Not ECN-Capable Transport))",
              "size": "1",
              "value": "00",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.dsfield.dscp",
                  "pos": "15",
                  "showname": "0000 00.. = Differentiated Services Codepoint: Default (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.dsfield.ecn",
                  "pos": "15",
                  "showname": ".... ..00 = Explicit Congestion Notification: Not-ECT (Not ECN-Capable Transport) (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.len",
              "pos": "16",
              "showname": "Total Length: 52",
              "size": "2",
              "value": "0034",
              "show": "52",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.id",
              "pos": "18",
              "showname": "Identification: 0x0000 (0)",
              "size": "2",
              "value": "0000",
              "show": "0x0000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.flags",
              "pos": "20",
              "showname": "Flags: 0x02 (Don't Fragment)",
              "size": "1",
              "value": "40",
              "show": "0x02",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.flags.rb",
                  "pos": "20",
                  "showname": "0... .... = Reserved bit: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.df",
                  "pos": "20",
                  "showname": ".1.. .... = Don't fragment: Set",
                  "size": "1",
                  "value": "40",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.mf",
                  "pos": "20",
                  "showname": "..0. .... = More fragments: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.frag_offset",
              "pos": "20",
              "showname": "Fragment offset: 0",
              "size": "2",
              "value": "4000",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.ttl",
              "pos": "22",
              "showname": "Time to live: 62",
              "size": "1",
              "value": "3e",
              "show": "62",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.proto",
              "pos": "23",
              "showname": "Protocol: TCP (6)",
              "size": "1",
              "value": "06",
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.checksum",
              "pos": "24",
              "showname": "Header checksum: 0xa3cb [correct]",
              "size": "2",
              "value": "a3cb",
              "show": "0xa3cb",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.checksum_good",
                  "pos": "24",
                  "showname": "Good: True",
                  "size": "2",
                  "value": "a3cb",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.checksum_bad",
                  "pos": "24",
                  "showname": "Bad: False",
                  "size": "2",
                  "value": "a3cb",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.src",
              "pos": "26",
              "showname": "Source: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "26",
              "showname": "Source or Destination Address: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.src_host",
              "pos": "26",
              "showname": "Source Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "26",
              "showname": "Source or Destination Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst",
              "pos": "30",
              "showname": "Destination: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "30",
              "showname": "Source or Destination Address: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst_host",
              "pos": "30",
              "showname": "Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "30",
              "showname": "Source or Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "tcp",
          "pos": "34",
          "showname": "Transmission Control Protocol, Src Port: 52834 (52834), Dst Port: ssh (22), Seq: 1, Ack: 241, Len: 0",
          "size": "32",
          "hide": null,
          "fields": [
            {
              "name": "tcp.srcport",
              "pos": "34",
              "showname": "Source port: 52834 (52834)",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.dstport",
              "pos": "36",
              "showname": "Destination port: ssh (22)",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "34",
              "showname": "Source or Destination Port: 52834",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "36",
              "showname": "Source or Destination Port: 22",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.stream",
              "pos": "34",
              "showname": "Stream index: 0",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.len",
              "pos": "46",
              "showname": "TCP Segment Len: 0",
              "size": "1",
              "value": "80",
              "show": "0",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.seq",
              "pos": "38",
              "showname": "Sequence number: 1    (relative sequence number)",
              "size": "4",
              "value": "3e7a345c",
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.ack",
              "pos": "42",
              "showname": "Acknowledgment number: 241    (relative ack number)",
              "size": "4",
              "value": "342f701e",
              "show": "241",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.hdr_len",
              "pos": "46",
              "showname": "Header length: 32 bytes",
              "size": "1",
              "value": "80",
              "show": "32",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.flags",
              "pos": "46",
              "showname": "Flags: 0x010 (ACK)",
              "size": "2",
              "value": "10",
              "show": "0x0010",
              "unmaskedvalue": "8010",
              "hide": null,
              "fields": [
                {
                  "name": "tcp.flags.res",
                  "pos": "46",
                  "showname": "000. .... .... = Reserved: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ns",
                  "pos": "46",
                  "showname": "...0 .... .... = Nonce: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.cwr",
                  "pos": "47",
                  "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ecn",
                  "pos": "47",
                  "showname": ".... .0.. .... = ECN-Echo: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.urg",
                  "pos": "47",
                  "showname": ".... ..0. .... = Urgent: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ack",
                  "pos": "47",
                  "showname": ".... ...1 .... = Acknowledgment: Set",
                  "size": "1",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.push",
                  "pos": "47",
                  "showname": ".... .... 0... = Push: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.reset",
                  "pos": "47",
                  "showname": ".... .... .0.. = Reset: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.syn",
                  "pos": "47",
                  "showname": ".... .... ..0. = Syn: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.fin",
                  "pos": "47",
                  "showname": ".... .... ...0 = Fin: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.window_size_value",
              "pos": "48",
              "showname": "Window size value: 4093",
              "size": "2",
              "value": "0ffd",
              "show": "4093",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size",
              "pos": "48",
              "showname": "Calculated window size: 4093",
              "size": "2",
              "value": "0ffd",
              "show": "4093",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size_scalefactor",
              "pos": "48",
              "showname": "Window size scaling factor: -1 (unknown)",
              "size": "2",
              "value": "0ffd",
              "show": "-1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.checksum",
              "pos": "50",
              "showname": "Checksum: 0xf0d7 [validation disabled]",
              "size": "2",
              "value": "f0d7",
              "show": "0xf0d7",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "tcp.checksum_good",
                  "pos": "50",
                  "showname": "Good Checksum: False",
                  "size": "2",
                  "value": "f0d7",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.checksum_bad",
                  "pos": "50",
                  "showname": "Bad Checksum: False",
                  "size": "2",
                  "value": "f0d7",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.options",
              "pos": "54",
              "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
              "size": "12",
              "value": "0101080a1ec844090475900c",
              "show": "01:01:08:0a:1e:c8:44:09:04:75:90:0c",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "",
                  "pos": "54",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "54",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "54",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "54",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "54",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "55",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "55",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "55",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "55",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "55",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "56",
                  "showname": null,
                  "size": "10",
                  "value": "080a1ec844090475900c",
                  "show": "Timestamps: TSval 516441097, TSecr 74813452",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "tcp.option_kind",
                      "pos": "56",
                      "showname": "Kind: Timestamp (8)",
                      "size": "1",
                      "value": "08",
                      "show": "8",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.option_len",
                      "pos": "57",
                      "showname": "Length: 10",
                      "size": "1",
                      "value": "0a",
                      "show": "10",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsval",
                      "pos": "58",
                      "showname": "Timestamp value: 516441097",
                      "size": "4",
                      "value": "1ec84409",
                      "show": "516441097",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsecr",
                      "pos": "62",
                      "showname": "Timestamp echo reply: 74813452",
                      "size": "4",
                      "value": "0475900c",
                      "show": "74813452",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    }
                  ],
                  "protos": null
                }
              ],
              "protos": null
            }
          ]
        }
      ]
    },
    {
      "protos": [
        {
          "name": "geninfo",
          "pos": "0",
          "showname": "General information",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "num",
              "pos": "0",
              "showname": "Number",
              "size": "66",
              "value": "4",
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "len",
              "pos": "0",
              "showname": "Frame Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "caplen",
              "pos": "0",
              "showname": "Captured Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "timestamp",
              "pos": "0",
              "showname": "Captured Time",
              "size": "66",
              "value": "1522244690.657009000",
              "show": "Mar 28, 2018 13:44:50.657009000 UTC",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "frame",
          "pos": "0",
          "showname": "Frame 4: 66 bytes on wire (528 bits), 66 bytes captured (528 bits)",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "frame.dlt",
              "pos": "0",
              "showname": "WTAP_ENCAP: 1",
              "size": "0",
              "value": null,
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time",
              "pos": "0",
              "showname": "Arrival Time: Mar 28, 2018 13:44:50.657009000 UTC",
              "size": "0",
              "value": null,
              "show": "Mar 28, 2018 13:44:50.657009000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.offset_shift",
              "pos": "0",
              "showname": "Time shift for this packet: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_epoch",
              "pos": "0",
              "showname": "Epoch Time: 1522244690.657009000 seconds",
              "size": "0",
              "value": null,
              "show": "1522244690.657009000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta",
              "pos": "0",
              "showname": "Time delta from previous captured frame: 82.491727000 seconds",
              "size": "0",
              "value": null,
              "show": "82.491727000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta_displayed",
              "pos": "0",
              "showname": "Time delta from previous displayed frame: 82.491727000 seconds",
              "size": "0",
              "value": null,
              "show": "82.491727000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_relative",
              "pos": "0",
              "showname": "Time since reference or first frame: 82.543011000 seconds",
              "size": "0",
              "value": null,
              "show": "82.543011000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.number",
              "pos": "0",
              "showname": "Frame Number: 4",
              "size": "0",
              "value": null,
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.len",
              "pos": "0",
              "showname": "Frame Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.cap_len",
              "pos": "0",
              "showname": "Capture Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.marked",
              "pos": "0",
              "showname": "Frame is marked: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.ignored",
              "pos": "0",
              "showname": "Frame is ignored: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.protocols",
              "pos": "0",
              "showname": "Protocols in frame: eth:ip:tcp",
              "size": "0",
              "value": null,
              "show": "eth:ip:tcp",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "eth",
          "pos": "0",
          "showname": "Ethernet II, Src: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1), Dst: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
          "size": "14",
          "hide": null,
          "fields": [
            {
              "name": "eth.dst",
              "pos": "0",
              "showname": "Destination: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
              "size": "6",
              "value": "fa163e04cd37",
              "show": "fa:16:3e:04:cd:37",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "0",
                  "showname": "Address: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
                  "size": "6",
                  "value": "fa163e04cd37",
                  "show": "fa:16:3e:04:cd:37",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "0",
                  "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                  "size": "3",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "0",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.src",
              "pos": "6",
              "showname": "Source: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
              "size": "6",
              "value": "8478ac5be8c1",
              "show": "84:78:ac:5b:e8:c1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "6",
                  "showname": "Address: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
                  "size": "6",
                  "value": "8478ac5be8c1",
                  "show": "84:78:ac:5b:e8:c1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "6",
                  "showname": ".... ..0. .... .... .... .... = LG bit: Globally unique address (factory default)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "6",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.type",
              "pos": "12",
              "showname": "Type: IP (0x0800)",
              "size": "2",
              "value": "0800",
              "show": "0x0800",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "ip",
          "pos": "14",
          "showname": "Internet Protocol Version 4, Src: 10.200.10.172 (10.200.10.172), Dst: 172.26.215.106 (172.26.215.106)",
          "size": "20",
          "hide": null,
          "fields": [
            {
              "name": "ip.version",
              "pos": "14",
              "showname": "Version: 4",
              "size": "1",
              "value": "45",
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.hdr_len",
              "pos": "14",
              "showname": "Header length: 20 bytes",
              "size": "1",
              "value": "45",
              "show": "20",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dsfield",
              "pos": "15",
              "showname": "Differentiated Services Field: 0x00 (DSCP 0x00: Default; ECN: 0x00: Not-ECT (Not ECN-Capable Transport))",
              "size": "1",
              "value": "00",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.dsfield.dscp",
                  "pos": "15",
                  "showname": "0000 00.. = Differentiated Services Codepoint: Default (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.dsfield.ecn",
                  "pos": "15",
                  "showname": ".... ..00 = Explicit Congestion Notification: Not-ECT (Not ECN-Capable Transport) (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.len",
              "pos": "16",
              "showname": "Total Length: 52",
              "size": "2",
              "value": "0034",
              "show": "52",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.id",
              "pos": "18",
              "showname": "Identification: 0x0000 (0)",
              "size": "2",
              "value": "0000",
              "show": "0x0000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.flags",
              "pos": "20",
              "showname": "Flags: 0x02 (Don't Fragment)",
              "size": "1",
              "value": "40",
              "show": "0x02",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.flags.rb",
                  "pos": "20",
                  "showname": "0... .... = Reserved bit: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.df",
                  "pos": "20",
                  "showname": ".1.. .... = Don't fragment: Set",
                  "size": "1",
                  "value": "40",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.mf",
                  "pos": "20",
                  "showname": "..0. .... = More fragments: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.frag_offset",
              "pos": "20",
              "showname": "Fragment offset: 0",
              "size": "2",
              "value": "4000",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.ttl",
              "pos": "22",
              "showname": "Time to live: 62",
              "size": "1",
              "value": "3e",
              "show": "62",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.proto",
              "pos": "23",
              "showname": "Protocol: TCP (6)",
              "size": "1",
              "value": "06",
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.checksum",
              "pos": "24",
              "showname": "Header checksum: 0xa3cb [correct]",
              "size": "2",
              "value": "a3cb",
              "show": "0xa3cb",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.checksum_good",
                  "pos": "24",
                  "showname": "Good: True",
                  "size": "2",
                  "value": "a3cb",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.checksum_bad",
                  "pos": "24",
                  "showname": "Bad: False",
                  "size": "2",
                  "value": "a3cb",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.src",
              "pos": "26",
              "showname": "Source: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "26",
              "showname": "Source or Destination Address: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.src_host",
              "pos": "26",
              "showname": "Source Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "26",
              "showname": "Source or Destination Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst",
              "pos": "30",
              "showname": "Destination: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "30",
              "showname": "Source or Destination Address: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst_host",
              "pos": "30",
              "showname": "Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "30",
              "showname": "Source or Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "tcp",
          "pos": "34",
          "showname": "Transmission Control Protocol, Src Port: 52834 (52834), Dst Port: ssh (22), Seq: 1193, Ack: 24217, Len: 0",
          "size": "32",
          "hide": null,
          "fields": [
            {
              "name": "tcp.srcport",
              "pos": "34",
              "showname": "Source port: 52834 (52834)",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.dstport",
              "pos": "36",
              "showname": "Destination port: ssh (22)",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "34",
              "showname": "Source or Destination Port: 52834",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "36",
              "showname": "Source or Destination Port: 22",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.stream",
              "pos": "34",
              "showname": "Stream index: 0",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.len",
              "pos": "46",
              "showname": "TCP Segment Len: 0",
              "size": "1",
              "value": "80",
              "show": "0",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.seq",
              "pos": "38",
              "showname": "Sequence number: 1193    (relative sequence number)",
              "size": "4",
              "value": "3e7a3904",
              "show": "1193",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.ack",
              "pos": "42",
              "showname": "Acknowledgment number: 24217    (relative ack number)",
              "size": "4",
              "value": "342fcdc6",
              "show": "24217",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.hdr_len",
              "pos": "46",
              "showname": "Header length: 32 bytes",
              "size": "1",
              "value": "80",
              "show": "32",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.flags",
              "pos": "46",
              "showname": "Flags: 0x010 (ACK)",
              "size": "2",
              "value": "10",
              "show": "0x0010",
              "unmaskedvalue": "8010",
              "hide": null,
              "fields": [
                {
                  "name": "tcp.flags.res",
                  "pos": "46",
                  "showname": "000. .... .... = Reserved: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ns",
                  "pos": "46",
                  "showname": "...0 .... .... = Nonce: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.cwr",
                  "pos": "47",
                  "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ecn",
                  "pos": "47",
                  "showname": ".... .0.. .... = ECN-Echo: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.urg",
                  "pos": "47",
                  "showname": ".... ..0. .... = Urgent: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ack",
                  "pos": "47",
                  "showname": ".... ...1 .... = Acknowledgment: Set",
                  "size": "1",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.push",
                  "pos": "47",
                  "showname": ".... .... 0... = Push: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.reset",
                  "pos": "47",
                  "showname": ".... .... .0.. = Reset: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.syn",
                  "pos": "47",
                  "showname": ".... .... ..0. = Syn: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.fin",
                  "pos": "47",
                  "showname": ".... .... ...0 = Fin: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.window_size_value",
              "pos": "48",
              "showname": "Window size value: 4094",
              "size": "2",
              "value": "0ffe",
              "show": "4094",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size",
              "pos": "48",
              "showname": "Calculated window size: 4094",
              "size": "2",
              "value": "0ffe",
              "show": "4094",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size_scalefactor",
              "pos": "48",
              "showname": "Window size scaling factor: -1 (unknown)",
              "size": "2",
              "value": "0ffe",
              "show": "-1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.checksum",
              "pos": "50",
              "showname": "Checksum: 0x0c9b [validation disabled]",
              "size": "2",
              "value": "0c9b",
              "show": "0x0c9b",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "tcp.checksum_good",
                  "pos": "50",
                  "showname": "Good Checksum: False",
                  "size": "2",
                  "value": "0c9b",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.checksum_bad",
                  "pos": "50",
                  "showname": "Bad Checksum: False",
                  "size": "2",
                  "value": "0c9b",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.options",
              "pos": "54",
              "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
              "size": "12",
              "value": "0101080a1ec983b60476d248",
              "show": "01:01:08:0a:1e:c9:83:b6:04:76:d2:48",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "",
                  "pos": "54",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "54",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "54",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "54",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "54",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "55",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "55",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "55",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "55",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "55",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "56",
                  "showname": null,
                  "size": "10",
                  "value": "080a1ec983b60476d248",
                  "show": "Timestamps: TSval 516522934, TSecr 74895944",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "tcp.option_kind",
                      "pos": "56",
                      "showname": "Kind: Timestamp (8)",
                      "size": "1",
                      "value": "08",
                      "show": "8",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.option_len",
                      "pos": "57",
                      "showname": "Length: 10",
                      "size": "1",
                      "value": "0a",
                      "show": "10",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsval",
                      "pos": "58",
                      "showname": "Timestamp value: 516522934",
                      "size": "4",
                      "value": "1ec983b6",
                      "show": "516522934",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsecr",
                      "pos": "62",
                      "showname": "Timestamp echo reply: 74895944",
                      "size": "4",
                      "value": "0476d248",
                      "show": "74895944",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    }
                  ],
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.analysis",
              "pos": "34",
              "showname": "SEQ/ACK analysis",
              "size": "0",
              "value": "",
              "show": "",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "tcp.analysis.flags",
                  "pos": "34",
                  "showname": "TCP Analysis Flags",
                  "size": "0",
                  "value": "",
                  "show": "",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "tcp.analysis.lost_segment",
                      "pos": "34",
                      "showname": "A segment before this frame wasn't captured",
                      "size": "0",
                      "value": "",
                      "show": "",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "expert",
                          "pos": "34",
                          "showname": "Expert Info (Warn/Sequence): Previous segment not captured (common at capture start)",
                          "size": "0",
                          "value": null,
                          "show": null,
                          "unmaskedvalue": null,
                          "hide": null,
                          "fields": [
                            {
                              "name": "expert.message",
                              "pos": "0",
                              "showname": "Message: Previous segment not captured (common at capture start)",
                              "size": "0",
                              "value": null,
                              "show": "Previous segment not captured (common at capture start)",
                              "unmaskedvalue": null,
                              "hide": null,
                              "fields": null,
                              "protos": null
                            },
                            {
                              "name": "expert.severity",
                              "pos": "0",
                              "showname": "Severity level: Warn",
                              "size": "0",
                              "value": null,
                              "show": "0x00600000",
                              "unmaskedvalue": null,
                              "hide": null,
                              "fields": null,
                              "protos": null
                            },
                            {
                              "name": "expert.group",
                              "pos": "0",
                              "showname": "Group: Sequence",
                              "size": "0",
                              "value": null,
                              "show": "0x02000000",
                              "unmaskedvalue": null,
                              "hide": null,
                              "fields": null,
                              "protos": null
                            }
                          ],
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                }
              ],
              "protos": null
            }
          ]
        }
      ]
    },
    {
      "protos": [
        {
          "name": "geninfo",
          "pos": "0",
          "showname": "General information",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "num",
              "pos": "0",
              "showname": "Number",
              "size": "66",
              "value": "5",
              "show": "5",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "len",
              "pos": "0",
              "showname": "Frame Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "caplen",
              "pos": "0",
              "showname": "Captured Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "timestamp",
              "pos": "0",
              "showname": "Captured Time",
              "size": "66",
              "value": "1522244690.730830000",
              "show": "Mar 28, 2018 13:44:50.730830000 UTC",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "frame",
          "pos": "0",
          "showname": "Frame 5: 66 bytes on wire (528 bits), 66 bytes captured (528 bits)",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "frame.dlt",
              "pos": "0",
              "showname": "WTAP_ENCAP: 1",
              "size": "0",
              "value": null,
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time",
              "pos": "0",
              "showname": "Arrival Time: Mar 28, 2018 13:44:50.730830000 UTC",
              "size": "0",
              "value": null,
              "show": "Mar 28, 2018 13:44:50.730830000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.offset_shift",
              "pos": "0",
              "showname": "Time shift for this packet: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_epoch",
              "pos": "0",
              "showname": "Epoch Time: 1522244690.730830000 seconds",
              "size": "0",
              "value": null,
              "show": "1522244690.730830000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta",
              "pos": "0",
              "showname": "Time delta from previous captured frame: 0.073821000 seconds",
              "size": "0",
              "value": null,
              "show": "0.073821000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta_displayed",
              "pos": "0",
              "showname": "Time delta from previous displayed frame: 0.073821000 seconds",
              "size": "0",
              "value": null,
              "show": "0.073821000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_relative",
              "pos": "0",
              "showname": "Time since reference or first frame: 82.616832000 seconds",
              "size": "0",
              "value": null,
              "show": "82.616832000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.number",
              "pos": "0",
              "showname": "Frame Number: 5",
              "size": "0",
              "value": null,
              "show": "5",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.len",
              "pos": "0",
              "showname": "Frame Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.cap_len",
              "pos": "0",
              "showname": "Capture Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.marked",
              "pos": "0",
              "showname": "Frame is marked: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.ignored",
              "pos": "0",
              "showname": "Frame is ignored: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.protocols",
              "pos": "0",
              "showname": "Protocols in frame: eth:ip:tcp",
              "size": "0",
              "value": null,
              "show": "eth:ip:tcp",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "eth",
          "pos": "0",
          "showname": "Ethernet II, Src: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1), Dst: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
          "size": "14",
          "hide": null,
          "fields": [
            {
              "name": "eth.dst",
              "pos": "0",
              "showname": "Destination: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
              "size": "6",
              "value": "fa163e04cd37",
              "show": "fa:16:3e:04:cd:37",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "0",
                  "showname": "Address: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
                  "size": "6",
                  "value": "fa163e04cd37",
                  "show": "fa:16:3e:04:cd:37",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "0",
                  "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                  "size": "3",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "0",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.src",
              "pos": "6",
              "showname": "Source: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
              "size": "6",
              "value": "8478ac5be8c1",
              "show": "84:78:ac:5b:e8:c1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "6",
                  "showname": "Address: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
                  "size": "6",
                  "value": "8478ac5be8c1",
                  "show": "84:78:ac:5b:e8:c1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "6",
                  "showname": ".... ..0. .... .... .... .... = LG bit: Globally unique address (factory default)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "6",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.type",
              "pos": "12",
              "showname": "Type: IP (0x0800)",
              "size": "2",
              "value": "0800",
              "show": "0x0800",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "ip",
          "pos": "14",
          "showname": "Internet Protocol Version 4, Src: 10.200.10.172 (10.200.10.172), Dst: 172.26.215.106 (172.26.215.106)",
          "size": "20",
          "hide": null,
          "fields": [
            {
              "name": "ip.version",
              "pos": "14",
              "showname": "Version: 4",
              "size": "1",
              "value": "45",
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.hdr_len",
              "pos": "14",
              "showname": "Header length: 20 bytes",
              "size": "1",
              "value": "45",
              "show": "20",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dsfield",
              "pos": "15",
              "showname": "Differentiated Services Field: 0x00 (DSCP 0x00: Default; ECN: 0x00: Not-ECT (Not ECN-Capable Transport))",
              "size": "1",
              "value": "00",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.dsfield.dscp",
                  "pos": "15",
                  "showname": "0000 00.. = Differentiated Services Codepoint: Default (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.dsfield.ecn",
                  "pos": "15",
                  "showname": ".... ..00 = Explicit Congestion Notification: Not-ECT (Not ECN-Capable Transport) (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.len",
              "pos": "16",
              "showname": "Total Length: 52",
              "size": "2",
              "value": "0034",
              "show": "52",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.id",
              "pos": "18",
              "showname": "Identification: 0x0000 (0)",
              "size": "2",
              "value": "0000",
              "show": "0x0000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.flags",
              "pos": "20",
              "showname": "Flags: 0x02 (Don't Fragment)",
              "size": "1",
              "value": "40",
              "show": "0x02",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.flags.rb",
                  "pos": "20",
                  "showname": "0... .... = Reserved bit: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.df",
                  "pos": "20",
                  "showname": ".1.. .... = Don't fragment: Set",
                  "size": "1",
                  "value": "40",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.mf",
                  "pos": "20",
                  "showname": "..0. .... = More fragments: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.frag_offset",
              "pos": "20",
              "showname": "Fragment offset: 0",
              "size": "2",
              "value": "4000",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.ttl",
              "pos": "22",
              "showname": "Time to live: 62",
              "size": "1",
              "value": "3e",
              "show": "62",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.proto",
              "pos": "23",
              "showname": "Protocol: TCP (6)",
              "size": "1",
              "value": "06",
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.checksum",
              "pos": "24",
              "showname": "Header checksum: 0xa3cb [correct]",
              "size": "2",
              "value": "a3cb",
              "show": "0xa3cb",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.checksum_good",
                  "pos": "24",
                  "showname": "Good: True",
                  "size": "2",
                  "value": "a3cb",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.checksum_bad",
                  "pos": "24",
                  "showname": "Bad: False",
                  "size": "2",
                  "value": "a3cb",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.src",
              "pos": "26",
              "showname": "Source: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "26",
              "showname": "Source or Destination Address: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.src_host",
              "pos": "26",
              "showname": "Source Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "26",
              "showname": "Source or Destination Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst",
              "pos": "30",
              "showname": "Destination: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "30",
              "showname": "Source or Destination Address: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst_host",
              "pos": "30",
              "showname": "Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "30",
              "showname": "Source or Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "tcp",
          "pos": "34",
          "showname": "Transmission Control Protocol, Src Port: 52834 (52834), Dst Port: ssh (22), Seq: 1193, Ack: 24381, Len: 0",
          "size": "32",
          "hide": null,
          "fields": [
            {
              "name": "tcp.srcport",
              "pos": "34",
              "showname": "Source port: 52834 (52834)",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.dstport",
              "pos": "36",
              "showname": "Destination port: ssh (22)",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "34",
              "showname": "Source or Destination Port: 52834",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "36",
              "showname": "Source or Destination Port: 22",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.stream",
              "pos": "34",
              "showname": "Stream index: 0",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.len",
              "pos": "46",
              "showname": "TCP Segment Len: 0",
              "size": "1",
              "value": "80",
              "show": "0",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.seq",
              "pos": "38",
              "showname": "Sequence number: 1193    (relative sequence number)",
              "size": "4",
              "value": "3e7a3904",
              "show": "1193",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.ack",
              "pos": "42",
              "showname": "Acknowledgment number: 24381    (relative ack number)",
              "size": "4",
              "value": "342fce6a",
              "show": "24381",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.hdr_len",
              "pos": "46",
              "showname": "Header length: 32 bytes",
              "size": "1",
              "value": "80",
              "show": "32",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.flags",
              "pos": "46",
              "showname": "Flags: 0x010 (ACK)",
              "size": "2",
              "value": "10",
              "show": "0x0010",
              "unmaskedvalue": "8010",
              "hide": null,
              "fields": [
                {
                  "name": "tcp.flags.res",
                  "pos": "46",
                  "showname": "000. .... .... = Reserved: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ns",
                  "pos": "46",
                  "showname": "...0 .... .... = Nonce: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.cwr",
                  "pos": "47",
                  "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ecn",
                  "pos": "47",
                  "showname": ".... .0.. .... = ECN-Echo: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.urg",
                  "pos": "47",
                  "showname": ".... ..0. .... = Urgent: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ack",
                  "pos": "47",
                  "showname": ".... ...1 .... = Acknowledgment: Set",
                  "size": "1",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.push",
                  "pos": "47",
                  "showname": ".... .... 0... = Push: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.reset",
                  "pos": "47",
                  "showname": ".... .... .0.. = Reset: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.syn",
                  "pos": "47",
                  "showname": ".... .... ..0. = Syn: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.fin",
                  "pos": "47",
                  "showname": ".... .... ...0 = Fin: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.window_size_value",
              "pos": "48",
              "showname": "Window size value: 4090",
              "size": "2",
              "value": "0ffa",
              "show": "4090",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size",
              "pos": "48",
              "showname": "Calculated window size: 4090",
              "size": "2",
              "value": "0ffa",
              "show": "4090",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size_scalefactor",
              "pos": "48",
              "showname": "Window size scaling factor: -1 (unknown)",
              "size": "2",
              "value": "0ffa",
              "show": "-1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.checksum",
              "pos": "50",
              "showname": "Checksum: 0x0b69 [validation disabled]",
              "size": "2",
              "value": "0b69",
              "show": "0x0b69",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "tcp.checksum_good",
                  "pos": "50",
                  "showname": "Good Checksum: False",
                  "size": "2",
                  "value": "0b69",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.checksum_bad",
                  "pos": "50",
                  "showname": "Bad Checksum: False",
                  "size": "2",
                  "value": "0b69",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.options",
              "pos": "54",
              "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
              "size": "12",
              "value": "0101080a1ec983ff0476d291",
              "show": "01:01:08:0a:1e:c9:83:ff:04:76:d2:91",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "",
                  "pos": "54",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "54",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "54",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "54",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "54",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "55",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "55",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "55",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "55",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "55",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "56",
                  "showname": null,
                  "size": "10",
                  "value": "080a1ec983ff0476d291",
                  "show": "Timestamps: TSval 516523007, TSecr 74896017",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "tcp.option_kind",
                      "pos": "56",
                      "showname": "Kind: Timestamp (8)",
                      "size": "1",
                      "value": "08",
                      "show": "8",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.option_len",
                      "pos": "57",
                      "showname": "Length: 10",
                      "size": "1",
                      "value": "0a",
                      "show": "10",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsval",
                      "pos": "58",
                      "showname": "Timestamp value: 516523007",
                      "size": "4",
                      "value": "1ec983ff",
                      "show": "516523007",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsecr",
                      "pos": "62",
                      "showname": "Timestamp echo reply: 74896017",
                      "size": "4",
                      "value": "0476d291",
                      "show": "74896017",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    }
                  ],
                  "protos": null
                }
              ],
              "protos": null
            }
          ]
        }
      ]
    },
    {
      "protos": [
        {
          "name": "geninfo",
          "pos": "0",
          "showname": "General information",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "num",
              "pos": "0",
              "showname": "Number",
              "size": "66",
              "value": "6",
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "len",
              "pos": "0",
              "showname": "Frame Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "caplen",
              "pos": "0",
              "showname": "Captured Length",
              "size": "66",
              "value": "42",
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "timestamp",
              "pos": "0",
              "showname": "Captured Time",
              "size": "66",
              "value": "1522244690.731086000",
              "show": "Mar 28, 2018 13:44:50.731086000 UTC",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "frame",
          "pos": "0",
          "showname": "Frame 6: 66 bytes on wire (528 bits), 66 bytes captured (528 bits)",
          "size": "66",
          "hide": null,
          "fields": [
            {
              "name": "frame.dlt",
              "pos": "0",
              "showname": "WTAP_ENCAP: 1",
              "size": "0",
              "value": null,
              "show": "1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time",
              "pos": "0",
              "showname": "Arrival Time: Mar 28, 2018 13:44:50.731086000 UTC",
              "size": "0",
              "value": null,
              "show": "Mar 28, 2018 13:44:50.731086000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.offset_shift",
              "pos": "0",
              "showname": "Time shift for this packet: 0.000000000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000000000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_epoch",
              "pos": "0",
              "showname": "Epoch Time: 1522244690.731086000 seconds",
              "size": "0",
              "value": null,
              "show": "1522244690.731086000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta",
              "pos": "0",
              "showname": "Time delta from previous captured frame: 0.000256000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000256000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_delta_displayed",
              "pos": "0",
              "showname": "Time delta from previous displayed frame: 0.000256000 seconds",
              "size": "0",
              "value": null,
              "show": "0.000256000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.time_relative",
              "pos": "0",
              "showname": "Time since reference or first frame: 82.617088000 seconds",
              "size": "0",
              "value": null,
              "show": "82.617088000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.number",
              "pos": "0",
              "showname": "Frame Number: 6",
              "size": "0",
              "value": null,
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.len",
              "pos": "0",
              "showname": "Frame Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.cap_len",
              "pos": "0",
              "showname": "Capture Length: 66 bytes (528 bits)",
              "size": "0",
              "value": null,
              "show": "66",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.marked",
              "pos": "0",
              "showname": "Frame is marked: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.ignored",
              "pos": "0",
              "showname": "Frame is ignored: False",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "frame.protocols",
              "pos": "0",
              "showname": "Protocols in frame: eth:ip:tcp",
              "size": "0",
              "value": null,
              "show": "eth:ip:tcp",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "eth",
          "pos": "0",
          "showname": "Ethernet II, Src: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1), Dst: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
          "size": "14",
          "hide": null,
          "fields": [
            {
              "name": "eth.dst",
              "pos": "0",
              "showname": "Destination: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
              "size": "6",
              "value": "fa163e04cd37",
              "show": "fa:16:3e:04:cd:37",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "0",
                  "showname": "Address: fa:16:3e:04:cd:37 (fa:16:3e:04:cd:37)",
                  "size": "6",
                  "value": "fa163e04cd37",
                  "show": "fa:16:3e:04:cd:37",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "0",
                  "showname": ".... ..1. .... .... .... .... = LG bit: Locally administered address (this is NOT the factory default)",
                  "size": "3",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "0",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "fa163e",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.src",
              "pos": "6",
              "showname": "Source: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
              "size": "6",
              "value": "8478ac5be8c1",
              "show": "84:78:ac:5b:e8:c1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "eth.addr",
                  "pos": "6",
                  "showname": "Address: Cisco_5b:e8:c1 (84:78:ac:5b:e8:c1)",
                  "size": "6",
                  "value": "8478ac5be8c1",
                  "show": "84:78:ac:5b:e8:c1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.lg",
                  "pos": "6",
                  "showname": ".... ..0. .... .... .... .... = LG bit: Globally unique address (factory default)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "eth.ig",
                  "pos": "6",
                  "showname": ".... ...0 .... .... .... .... = IG bit: Individual address (unicast)",
                  "size": "3",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "8478ac",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "eth.type",
              "pos": "12",
              "showname": "Type: IP (0x0800)",
              "size": "2",
              "value": "0800",
              "show": "0x0800",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "ip",
          "pos": "14",
          "showname": "Internet Protocol Version 4, Src: 10.200.10.172 (10.200.10.172), Dst: 172.26.215.106 (172.26.215.106)",
          "size": "20",
          "hide": null,
          "fields": [
            {
              "name": "ip.version",
              "pos": "14",
              "showname": "Version: 4",
              "size": "1",
              "value": "45",
              "show": "4",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.hdr_len",
              "pos": "14",
              "showname": "Header length: 20 bytes",
              "size": "1",
              "value": "45",
              "show": "20",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dsfield",
              "pos": "15",
              "showname": "Differentiated Services Field: 0x00 (DSCP 0x00: Default; ECN: 0x00: Not-ECT (Not ECN-Capable Transport))",
              "size": "1",
              "value": "00",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.dsfield.dscp",
                  "pos": "15",
                  "showname": "0000 00.. = Differentiated Services Codepoint: Default (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.dsfield.ecn",
                  "pos": "15",
                  "showname": ".... ..00 = Explicit Congestion Notification: Not-ECT (Not ECN-Capable Transport) (0x00)",
                  "size": "1",
                  "value": "0",
                  "show": "0x00",
                  "unmaskedvalue": "00",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.len",
              "pos": "16",
              "showname": "Total Length: 52",
              "size": "2",
              "value": "0034",
              "show": "52",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.id",
              "pos": "18",
              "showname": "Identification: 0x0000 (0)",
              "size": "2",
              "value": "0000",
              "show": "0x0000",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.flags",
              "pos": "20",
              "showname": "Flags: 0x02 (Don't Fragment)",
              "size": "1",
              "value": "40",
              "show": "0x02",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.flags.rb",
                  "pos": "20",
                  "showname": "0... .... = Reserved bit: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.df",
                  "pos": "20",
                  "showname": ".1.. .... = Don't fragment: Set",
                  "size": "1",
                  "value": "40",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.flags.mf",
                  "pos": "20",
                  "showname": "..0. .... = More fragments: Not set",
                  "size": "1",
                  "value": "40",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.frag_offset",
              "pos": "20",
              "showname": "Fragment offset: 0",
              "size": "2",
              "value": "4000",
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.ttl",
              "pos": "22",
              "showname": "Time to live: 62",
              "size": "1",
              "value": "3e",
              "show": "62",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.proto",
              "pos": "23",
              "showname": "Protocol: TCP (6)",
              "size": "1",
              "value": "06",
              "show": "6",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.checksum",
              "pos": "24",
              "showname": "Header checksum: 0xa3cb [correct]",
              "size": "2",
              "value": "a3cb",
              "show": "0xa3cb",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "ip.checksum_good",
                  "pos": "24",
                  "showname": "Good: True",
                  "size": "2",
                  "value": "a3cb",
                  "show": "1",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "ip.checksum_bad",
                  "pos": "24",
                  "showname": "Bad: False",
                  "size": "2",
                  "value": "a3cb",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "ip.src",
              "pos": "26",
              "showname": "Source: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "26",
              "showname": "Source or Destination Address: 10.200.10.172 (10.200.10.172)",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.src_host",
              "pos": "26",
              "showname": "Source Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "26",
              "showname": "Source or Destination Host: 10.200.10.172",
              "size": "4",
              "value": "0ac80aac",
              "show": "10.200.10.172",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst",
              "pos": "30",
              "showname": "Destination: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.addr",
              "pos": "30",
              "showname": "Source or Destination Address: 172.26.215.106 (172.26.215.106)",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.dst_host",
              "pos": "30",
              "showname": "Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "ip.host",
              "pos": "30",
              "showname": "Source or Destination Host: 172.26.215.106",
              "size": "4",
              "value": "ac1ad76a",
              "show": "172.26.215.106",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            }
          ]
        },
        {
          "name": "tcp",
          "pos": "34",
          "showname": "Transmission Control Protocol, Src Port: 52834 (52834), Dst Port: ssh (22), Seq: 1193, Ack: 24457, Len: 0",
          "size": "32",
          "hide": null,
          "fields": [
            {
              "name": "tcp.srcport",
              "pos": "34",
              "showname": "Source port: 52834 (52834)",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.dstport",
              "pos": "36",
              "showname": "Destination port: ssh (22)",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "34",
              "showname": "Source or Destination Port: 52834",
              "size": "2",
              "value": "ce62",
              "show": "52834",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.port",
              "pos": "36",
              "showname": "Source or Destination Port: 22",
              "size": "2",
              "value": "0016",
              "show": "22",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.stream",
              "pos": "34",
              "showname": "Stream index: 0",
              "size": "0",
              "value": null,
              "show": "0",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.len",
              "pos": "46",
              "showname": "TCP Segment Len: 0",
              "size": "1",
              "value": "80",
              "show": "0",
              "unmaskedvalue": null,
              "hide": "yes",
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.seq",
              "pos": "38",
              "showname": "Sequence number: 1193    (relative sequence number)",
              "size": "4",
              "value": "3e7a3904",
              "show": "1193",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.ack",
              "pos": "42",
              "showname": "Acknowledgment number: 24457    (relative ack number)",
              "size": "4",
              "value": "342fceb6",
              "show": "24457",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.hdr_len",
              "pos": "46",
              "showname": "Header length: 32 bytes",
              "size": "1",
              "value": "80",
              "show": "32",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.flags",
              "pos": "46",
              "showname": "Flags: 0x010 (ACK)",
              "size": "2",
              "value": "10",
              "show": "0x0010",
              "unmaskedvalue": "8010",
              "hide": null,
              "fields": [
                {
                  "name": "tcp.flags.res",
                  "pos": "46",
                  "showname": "000. .... .... = Reserved: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ns",
                  "pos": "46",
                  "showname": "...0 .... .... = Nonce: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "80",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.cwr",
                  "pos": "47",
                  "showname": ".... 0... .... = Congestion Window Reduced (CWR): Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ecn",
                  "pos": "47",
                  "showname": ".... .0.. .... = ECN-Echo: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.urg",
                  "pos": "47",
                  "showname": ".... ..0. .... = Urgent: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.ack",
                  "pos": "47",
                  "showname": ".... ...1 .... = Acknowledgment: Set",
                  "size": "1",
                  "value": "1",
                  "show": "1",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.push",
                  "pos": "47",
                  "showname": ".... .... 0... = Push: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.reset",
                  "pos": "47",
                  "showname": ".... .... .0.. = Reset: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.syn",
                  "pos": "47",
                  "showname": ".... .... ..0. = Syn: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.flags.fin",
                  "pos": "47",
                  "showname": ".... .... ...0 = Fin: Not set",
                  "size": "1",
                  "value": "0",
                  "show": "0",
                  "unmaskedvalue": "10",
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.window_size_value",
              "pos": "48",
              "showname": "Window size value: 4093",
              "size": "2",
              "value": "0ffd",
              "show": "4093",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size",
              "pos": "48",
              "showname": "Calculated window size: 4093",
              "size": "2",
              "value": "0ffd",
              "show": "4093",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.window_size_scalefactor",
              "pos": "48",
              "showname": "Window size scaling factor: -1 (unknown)",
              "size": "2",
              "value": "0ffd",
              "show": "-1",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.checksum",
              "pos": "50",
              "showname": "Checksum: 0x0b19 [validation disabled]",
              "size": "2",
              "value": "0b19",
              "show": "0x0b19",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "tcp.checksum_good",
                  "pos": "50",
                  "showname": "Good Checksum: False",
                  "size": "2",
                  "value": "0b19",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.checksum_bad",
                  "pos": "50",
                  "showname": "Bad Checksum: False",
                  "size": "2",
                  "value": "0b19",
                  "show": "0",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                }
              ],
              "protos": null
            },
            {
              "name": "tcp.options",
              "pos": "54",
              "showname": "Options: (12 bytes), No-Operation (NOP), No-Operation (NOP), Timestamps",
              "size": "12",
              "value": "0101080a1ec983ff0476d292",
              "show": "01:01:08:0a:1e:c9:83:ff:04:76:d2:92",
              "unmaskedvalue": null,
              "hide": null,
              "fields": [
                {
                  "name": "",
                  "pos": "54",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "54",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "54",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "54",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "54",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "55",
                  "showname": null,
                  "size": "1",
                  "value": "01",
                  "show": "No-Operation (NOP)",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "ip.opt.type",
                      "pos": "55",
                      "showname": "Type: 1",
                      "size": "1",
                      "value": "01",
                      "show": "1",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": [
                        {
                          "name": "ip.opt.type.copy",
                          "pos": "55",
                          "showname": "0... .... = Copy on fragmentation: No",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.class",
                          "pos": "55",
                          "showname": ".00. .... = Class: Control (0)",
                          "size": "1",
                          "value": "0",
                          "show": "0",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        },
                        {
                          "name": "ip.opt.type.number",
                          "pos": "55",
                          "showname": "...0 0001 = Number: No-Operation (NOP) (1)",
                          "size": "1",
                          "value": "1",
                          "show": "1",
                          "unmaskedvalue": "01",
                          "hide": null,
                          "fields": null,
                          "protos": null
                        }
                      ],
                      "protos": null
                    }
                  ],
                  "protos": null
                },
                {
                  "name": "",
                  "pos": "56",
                  "showname": null,
                  "size": "10",
                  "value": "080a1ec983ff0476d292",
                  "show": "Timestamps: TSval 516523007, TSecr 74896018",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": [
                    {
                      "name": "tcp.option_kind",
                      "pos": "56",
                      "showname": "Kind: Timestamp (8)",
                      "size": "1",
                      "value": "08",
                      "show": "8",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.option_len",
                      "pos": "57",
                      "showname": "Length: 10",
                      "size": "1",
                      "value": "0a",
                      "show": "10",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsval",
                      "pos": "58",
                      "showname": "Timestamp value: 516523007",
                      "size": "4",
                      "value": "1ec983ff",
                      "show": "516523007",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    },
                    {
                      "name": "tcp.options.timestamp.tsecr",
                      "pos": "62",
                      "showname": "Timestamp echo reply: 74896018",
                      "size": "4",
                      "value": "0476d292",
                      "show": "74896018",
                      "unmaskedvalue": null,
                      "hide": null,
                      "fields": null,
                      "protos": null
                    }
                  ],
                  "protos": null
                }
              ],
              "protos": null
            }
          ]
        }
      ]
    }
  ]
    }`
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
