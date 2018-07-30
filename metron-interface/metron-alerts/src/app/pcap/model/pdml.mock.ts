import { PdmlPacket, PdmlProto, PdmlField } from './pdml';

export const fakePacket = {
  "name": '',
  "expanded": false,
  "protos": [
    {
      "name": "geninfo",
      "showname": "",
      "fields": [
        { "name": "timestamp", "pos": "0", "showname": "Captured Time", "size": "722", "value": "1458240269.373968000", "show": "Mar 17, 2016 18:44:29.373968000 UTC", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField
      ]
    } as PdmlProto,
    {
      "name": "ip",
      "showname": "",
      "fields": [
        { "name": "ip.proto", "pos": "23", "showname": "Protocol: TCP (6)", "size": "1", "value": "06", "show": "6", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField,
        { "name": "ip.src", "pos": "26", "showname": "Source: 192.168.66.121 (192.168.66.121)", "size": "4", "value": "c0a84279", "show": "192.168.66.121", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField,
        { "name": "ip.dst", "pos": "30", "showname": "Destination: 192.168.66.1 (192.168.66.1)", "size": "4", "value": "c0a84201", "show": "192.168.66.1", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField
      ]
    } as PdmlProto,
    {
      "name": "tcp",
      "showname": "",
      "fields": [
        { "name": "tcp.srcport", "pos": "34", "showname": "Source port: ssh (22)", "size": "2", "value": "0016", "show": "22", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField,
        { "name": "tcp.dstport", "pos": "36", "showname": "Destination port: 55791 (55791)", "size": "2", "value": "d9ef", "show": "55791", "unmaskedvalue": null, "hide": null, "fields": null, "protos": null } as PdmlField
      ],
    } as PdmlProto
  ]
} as PdmlPacket;