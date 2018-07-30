/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PcapPacketComponent } from './pcap-packet.component';
import { PdmlPacket } from '../model/pdml';
import { By } from '@angular/platform-browser';

describe('PcapPacketComponent', () => {
  let component: PcapPacketComponent;
  let fixture: ComponentFixture<PcapPacketComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PcapPacketComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PcapPacketComponent);
    component = fixture.componentInstance;
    component.packet = fakePacket as PdmlPacket;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expand the packet`s proto fieldset', () => {

    const protos = fixture.debugElement.queryAll(By.css('[data-qe-id="proto"]'));

    protos.forEach((proto, i) => {

      expect(proto.query(By.css('[data-qe-id="proto-fields"]'))).toBeFalsy();
      proto.nativeElement.click();
      fixture.detectChanges();
      const fieldsContainer = proto.query(By.css('[data-qe-id="proto-fields"]'));
      expect(fieldsContainer).toBeDefined();

      const fields = fieldsContainer.queryAll(By.css('[data-qe-id="proto-field"]'));

      fields.forEach((field, j) => {
        const name = field.query(By.css('[data-qe-id="proto-field-name"]'));
        expect(name.nativeElement.textContent.trim()).toBe(fakePacket.protos[i].fields[j].name);
        const showname = field.query(By.css('[data-qe-id="proto-field-showname"]'));
        expect(showname.nativeElement.textContent.trim()).toBe(fakePacket.protos[i].fields[j].showname);
      });
    });
  });

  it('should render proto`s showname property', () => {
    const protos = fixture.debugElement.queryAll(By.css('[data-qe-id="proto"]'));
    protos.forEach((proto, i) => {
      expect(
        proto.query(By.css('[data-qe-id="proto-showname"]'))
          .nativeElement
          .textContent.trim()
      ).toBe(fakePacket.protos[i].showname);
    });
  });
});

const fakePacket = {
  name: "something",
  expanded: false,
  protos: [
    {
      "name": "geninfo",
      "pos": "0",
      "showname": "General information",
      "size": "722",
      "hide": null,
      "fields": [
        {
          "name": "num",
          "pos": "0",
          "showname": "Number",
          "size": "722",
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
          "size": "722",
          "value": "2d2",
          "show": "722",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "caplen",
          "pos": "0",
          "showname": "Captured Length",
          "size": "722",
          "value": "2d2",
          "show": "722",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "timestamp",
          "pos": "0",
          "showname": "Captured Time",
          "size": "722",
          "value": "1458240269.373968000",
          "show": "Mar 17, 2016 18:44:29.373968000 UTC",
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
      "showname": "Frame 1: 722 bytes on wire (5776 bits), 722 bytes captured (5776 bits)",
      "size": "722",
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
          "showname": "Arrival Time: Mar 17, 2016 18:44:29.373968000 UTC",
          "size": "0",
          "value": null,
          "show": "Mar 17, 2016 18:44:29.373968000",
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
          "showname": "Epoch Time: 1458240269.373968000 seconds",
          "size": "0",
          "value": null,
          "show": "1458240269.373968000",
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
          "showname": "Frame Length: 722 bytes (5776 bits)",
          "size": "0",
          "value": null,
          "show": "722",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "frame.cap_len",
          "pos": "0",
          "showname": "Capture Length: 722 bytes (5776 bits)",
          "size": "0",
          "value": null,
          "show": "722",
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
          "showname": "Protocols in frame: eth:ip:tcp:ssh",
          "size": "0",
          "value": null,
          "show": "eth:ip:tcp:ssh",
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
      "showname": "Ethernet II, Src: CadmusCo_96:a4:7a (08:00:27:96:a4:7a), Dst: 0a:00:27:00:00:00 (0a:00:27:00:00:00)",
      "size": "14",
      "hide": null,
      "fields": [
        {
          "name": "eth.dst",
          "pos": "0",
          "showname": "Destination: 0a:00:27:00:00:00 (0a:00:27:00:00:00)",
          "size": "6",
          "value": "0a0027000000",
          "show": "0a:00:27:00:00:00",
          "unmaskedvalue": null,
          "hide": null,
          "fields": [
            {
              "name": "eth.addr",
              "pos": "0",
              "showname": "Address: 0a:00:27:00:00:00 (0a:00:27:00:00:00)",
              "size": "6",
              "value": "0a0027000000",
              "show": "0a:00:27:00:00:00",
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
              "unmaskedvalue": "0a0027",
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
              "unmaskedvalue": "0a0027",
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
          "showname": "Source: CadmusCo_96:a4:7a (08:00:27:96:a4:7a)",
          "size": "6",
          "value": "08002796a47a",
          "show": "08:00:27:96:a4:7a",
          "unmaskedvalue": null,
          "hide": null,
          "fields": [
            {
              "name": "eth.addr",
              "pos": "6",
              "showname": "Address: CadmusCo_96:a4:7a (08:00:27:96:a4:7a)",
              "size": "6",
              "value": "08002796a47a",
              "show": "08:00:27:96:a4:7a",
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
              "unmaskedvalue": "080027",
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
              "unmaskedvalue": "080027",
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
      "showname": "Internet Protocol Version 4, Src: 192.168.66.121 (192.168.66.121), Dst: 192.168.66.1 (192.168.66.1)",
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
          "showname": "Differentiated Services Field: 0x10 (DSCP 0x04: Unknown DSCP; ECN: 0x00: Not-ECT (Not ECN-Capable Transport))",
          "size": "1",
          "value": "10",
          "show": "16",
          "unmaskedvalue": null,
          "hide": null,
          "fields": [
            {
              "name": "ip.dsfield.dscp",
              "pos": "15",
              "showname": "0001 00.. = Differentiated Services Codepoint: Unknown (0x04)",
              "size": "1",
              "value": "4",
              "show": "0x04",
              "unmaskedvalue": "10",
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
              "unmaskedvalue": "10",
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
          "showname": "Total Length: 708",
          "size": "2",
          "value": "02c4",
          "show": "708",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.id",
          "pos": "18",
          "showname": "Identification: 0x7cd9 (31961)",
          "size": "2",
          "value": "7cd9",
          "show": "0x7cd9",
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
          "showname": "Time to live: 64",
          "size": "1",
          "value": "40",
          "show": "64",
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
          "showname": "Header checksum: 0xb57f [correct]",
          "size": "2",
          "value": "b57f",
          "show": "0xb57f",
          "unmaskedvalue": null,
          "hide": null,
          "fields": [
            {
              "name": "ip.checksum_good",
              "pos": "24",
              "showname": "Good: True",
              "size": "2",
              "value": "b57f",
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
              "value": "b57f",
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
          "showname": "Source: 192.168.66.121 (192.168.66.121)",
          "size": "4",
          "value": "c0a84279",
          "show": "192.168.66.121",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.addr",
          "pos": "26",
          "showname": "Source or Destination Address: 192.168.66.121 (192.168.66.121)",
          "size": "4",
          "value": "c0a84279",
          "show": "192.168.66.121",
          "unmaskedvalue": null,
          "hide": "yes",
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.src_host",
          "pos": "26",
          "showname": "Source Host: 192.168.66.121",
          "size": "4",
          "value": "c0a84279",
          "show": "192.168.66.121",
          "unmaskedvalue": null,
          "hide": "yes",
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.host",
          "pos": "26",
          "showname": "Source or Destination Host: 192.168.66.121",
          "size": "4",
          "value": "c0a84279",
          "show": "192.168.66.121",
          "unmaskedvalue": null,
          "hide": "yes",
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.dst",
          "pos": "30",
          "showname": "Destination: 192.168.66.1 (192.168.66.1)",
          "size": "4",
          "value": "c0a84201",
          "show": "192.168.66.1",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.addr",
          "pos": "30",
          "showname": "Source or Destination Address: 192.168.66.1 (192.168.66.1)",
          "size": "4",
          "value": "c0a84201",
          "show": "192.168.66.1",
          "unmaskedvalue": null,
          "hide": "yes",
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.dst_host",
          "pos": "30",
          "showname": "Destination Host: 192.168.66.1",
          "size": "4",
          "value": "c0a84201",
          "show": "192.168.66.1",
          "unmaskedvalue": null,
          "hide": "yes",
          "fields": null,
          "protos": null
        },
        {
          "name": "ip.host",
          "pos": "30",
          "showname": "Source or Destination Host: 192.168.66.1",
          "size": "4",
          "value": "c0a84201",
          "show": "192.168.66.1",
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
      "showname": "Transmission Control Protocol, Src Port: ssh (22), Dst Port: 55791 (55791), Seq: 1, Ack: 1, Len: 656",
      "size": "32",
      "hide": null,
      "fields": [
        {
          "name": "tcp.srcport",
          "pos": "34",
          "showname": "Source port: ssh (22)",
          "size": "2",
          "value": "0016",
          "show": "22",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "tcp.dstport",
          "pos": "36",
          "showname": "Destination port: 55791 (55791)",
          "size": "2",
          "value": "d9ef",
          "show": "55791",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "tcp.port",
          "pos": "34",
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
          "name": "tcp.port",
          "pos": "36",
          "showname": "Source or Destination Port: 55791",
          "size": "2",
          "value": "d9ef",
          "show": "55791",
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
          "showname": "TCP Segment Len: 656",
          "size": "1",
          "value": "80",
          "show": "656",
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
          "value": "12903044",
          "show": "1",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "tcp.nxtseq",
          "pos": "34",
          "showname": "Next sequence number: 657    (relative sequence number)",
          "size": "0",
          "value": null,
          "show": "657",
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
          "value": "8b92f3e7",
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
          "showname": "Flags: 0x018 (PSH, ACK)",
          "size": "2",
          "value": "18",
          "show": "0x0018",
          "unmaskedvalue": "8018",
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
              "unmaskedvalue": "18",
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
              "unmaskedvalue": "18",
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
              "unmaskedvalue": "18",
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
              "unmaskedvalue": "18",
              "hide": null,
              "fields": null,
              "protos": null
            },
            {
              "name": "tcp.flags.push",
              "pos": "47",
              "showname": ".... .... 1... = Push: Set",
              "size": "1",
              "value": "1",
              "show": "1",
              "unmaskedvalue": "18",
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
              "unmaskedvalue": "18",
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
              "unmaskedvalue": "18",
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
              "unmaskedvalue": "18",
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
          "showname": "Window size value: 501",
          "size": "2",
          "value": "01f5",
          "show": "501",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "tcp.window_size",
          "pos": "48",
          "showname": "Calculated window size: 501",
          "size": "2",
          "value": "01f5",
          "show": "501",
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
          "value": "01f5",
          "show": "-1",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        },
        {
          "name": "tcp.checksum",
          "pos": "50",
          "showname": "Checksum: 0x0882 [validation disabled]",
          "size": "2",
          "value": "0882",
          "show": "0x0882",
          "unmaskedvalue": null,
          "hide": null,
          "fields": [
            {
              "name": "tcp.checksum_good",
              "pos": "50",
              "showname": "Good Checksum: False",
              "size": "2",
              "value": "0882",
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
              "value": "0882",
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
          "value": "0101080a0014f4f811bdb98f",
          "show": "01:01:08:0a:00:14:f4:f8:11:bd:b9:8f",
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
              "value": "080a0014f4f811bdb98f",
              "show": "Timestamps: TSval 1373432, TSecr 297646479",
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
                  "showname": "Timestamp value: 1373432",
                  "size": "4",
                  "value": "0014f4f8",
                  "show": "1373432",
                  "unmaskedvalue": null,
                  "hide": null,
                  "fields": null,
                  "protos": null
                },
                {
                  "name": "tcp.options.timestamp.tsecr",
                  "pos": "62",
                  "showname": "Timestamp echo reply: 297646479",
                  "size": "4",
                  "value": "11bdb98f",
                  "show": "297646479",
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
              "name": "tcp.analysis.bytes_in_flight",
              "pos": "34",
              "showname": "Bytes in flight: 656",
              "size": "0",
              "value": null,
              "show": "656",
              "unmaskedvalue": null,
              "hide": null,
              "fields": null,
              "protos": null
            }
          ],
          "protos": null
        }
      ]
    },
    {
      "name": "ssh",
      "pos": "66",
      "showname": "SSH Protocol",
      "size": "656",
      "hide": null,
      "fields": [
        {
          "name": "ssh.encrypted_packet",
          "pos": "66",
          "showname": "Encrypted Packet: 5b2bfe1fa006867834412184af9f5b239737763adead7140...",
          "size": "656",
          "value": "5b2bfe1fa006867834412184af9f5b239737763adead71408fc01b88e548b2cc86f341a39771c6ed16f2b0bb3e6ab6109e73c7d68ca7545852f91930e4633c17fb9dc7aa794b0d820d0fa3ac65bf0f013e5449d5953d2943506657e2b76b548e67f5c9ce1a4c53db1b52465bde7208baf11f3fe01975418b4db186a38ad32947d1908b62e532da4b729353a932423d25f5f734484076aa4143c6a74937a4ea49448e261ae1ecb6b07bbdf5c98d0855940a19018c88263b8936f7b3e9a4b6cd98090fa10a10e37ad20fe5d833071ad6d5b2886ba85ec72affb83e316443dbe29dbf643e6aa05595c90765cf85f6da55cc1c09d8dccc7d05da022429ad602a559a044b7e2663b0c153a3011bf824ef8d1fa56cba957c5f5d2276a1c9e92de65782f406848c6e20f634c5d1fea843a8bf1a4058e85553f5838f7299958fbf54be84e46c5a3c3965f8bed7fe03a9a1168a892e0073adeb54deca171a318d11fc1a8179f91632310213da327965a40bc6fe18eae55e8da6b57d7ef9f3a05b42381bcb3db8f8efd6d0c638a2cdd46efb0b8f1274e98672f644b2275947e626b02e5166f86c2dd4a67b81e213f8c064927a396815db589f10e5e521ffedb13f8edbe2de01c6fc8bf0e12c82212e497794aa045e9b6fcca83b4cad0a3b5e6ca2d1feaf8887b4d64f22989396ecfa8f7f1835eed422580505109fed36797bdc10a9168d5148daef6a8710c3df1d6366c9763ab4ebd359d86a8ea14819252fb52ba423422d1f60b0179316b3729e479ba07e88cb886938c8daae65d470dde91e5336e0fc4221a72cc49057d878aa5924875d097483e94bc44a4ea93aee8780e56c50a405932841f50da156e1f90559a7c4f76999442fb433a26fc703dea656bbe03790ac3c9c5318ff5f81d87d483524bbfe7ff167",
          "show": "5b:2b:fe:1f:a0:06:86:78:34:41:21:84:af:9f:5b:23:97:37:76:3a:de:ad:71:40:8f:c0:1b:88:e5:48:b2:cc:86:f3:41:a3:97:71:c6:ed:16:f2:b0:bb:3e:6a:b6:10:9e:73:c7:d6:8c:a7:54:58:52:f9:19:30:e4:63:3c:17:fb:9d:c7:aa:79:4b:0d:82:0d:0f:a3:ac:65:bf:0f:01:3e:54:49:d5:95:3d:29:43:50:66:57:e2:b7:6b:54:8e:67:f5:c9:ce:1a:4c:53:db:1b:52:46:5b:de:72:08:ba:f1:1f:3f:e0:19:75:41:8b:4d:b1:86:a3:8a:d3:29:47:d1:90:8b:62:e5:32:da:4b:72:93:53:a9:32:42:3d:25:f5:f7:34:48:40:76:aa:41:43:c6:a7:49:37:a4:ea:49:44:8e:26:1a:e1:ec:b6:b0:7b:bd:f5:c9:8d:08:55:94:0a:19:01:8c:88:26:3b:89:36:f7:b3:e9:a4:b6:cd:98:09:0f:a1:0a:10:e3:7a:d2:0f:e5:d8:33:07:1a:d6:d5:b2:88:6b:a8:5e:c7:2a:ff:b8:3e:31:64:43:db:e2:9d:bf:64:3e:6a:a0:55:95:c9:07:65:cf:85:f6:da:55:cc:1c:09:d8:dc:cc:7d:05:da:02:24:29:ad:60:2a:55:9a:04:4b:7e:26:63:b0:c1:53:a3:01:1b:f8:24:ef:8d:1f:a5:6c:ba:95:7c:5f:5d:22:76:a1:c9:e9:2d:e6:57:82:f4:06:84:8c:6e:20:f6:34:c5:d1:fe:a8:43:a8:bf:1a:40:58:e8:55:53:f5:83:8f:72:99:95:8f:bf:54:be:84:e4:6c:5a:3c:39:65:f8:be:d7:fe:03:a9:a1:16:8a:89:2e:00:73:ad:eb:54:de:ca:17:1a:31:8d:11:fc:1a:81:79:f9:16:32:31:02:13:da:32:79:65:a4:0b:c6:fe:18:ea:e5:5e:8d:a6:b5:7d:7e:f9:f3:a0:5b:42:38:1b:cb:3d:b8:f8:ef:d6:d0:c6:38:a2:cd:d4:6e:fb:0b:8f:12:74:e9:86:72:f6:44:b2:27:59:47:e6:26:b0:2e:51:66:f8:6c:2d:d4:a6:7b:81:e2:13:f8:c0:64:92:7a:39:68:15:db:58:9f:10:e5:e5:21:ff:ed:b1:3f:8e:db:e2:de:01:c6:fc:8b:f0:e1:2c:82:21:2e:49:77:94:aa:04:5e:9b:6f:cc:a8:3b:4c:ad:0a:3b:5e:6c:a2:d1:fe:af:88:87:b4:d6:4f:22:98:93:96:ec:fa:8f:7f:18:35:ee:d4:22:58:05:05:10:9f:ed:36:79:7b:dc:10:a9:16:8d:51:48:da:ef:6a:87:10:c3:df:1d:63:66:c9:76:3a:b4:eb:d3:59:d8:6a:8e:a1:48:19:25:2f:b5:2b:a4:23:42:2d:1f:60:b0:17:93:16:b3:72:9e:47:9b:a0:7e:88:cb:88:69:38:c8:da:ae:65:d4:70:dd:e9:1e:53:36:e0:fc:42:21:a7:2c:c4:90:57:d8:78:aa:59:24:87:5d:09:74:83:e9:4b:c4:4a:4e:a9:3a:ee:87:80:e5:6c:50:a4:05:93:28:41:f5:0d:a1:56:e1:f9:05:59:a7:c4:f7:69:99:44:2f:b4:33:a2:6f:c7:03:de:a6:56:bb:e0:37:90:ac:3c:9c:53:18:ff:5f:81:d8:7d:48:35:24:bb:fe:7f:f1:67",
          "unmaskedvalue": null,
          "hide": null,
          "fields": null,
          "protos": null
        }
      ]
    }
  ]
};
