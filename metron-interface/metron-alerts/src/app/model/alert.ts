export class Alert {
  _index: string;
  _type: string;
  _id: string;
  _score: number;
  _timestamp: number;
  _source: {
    msg: string;
    sig_rev: number;
    ip_dst_port: number;
    ethsrc: string;
    tcpseq: number;
    dgmlen: number;
    tcpwindow: number;
    tcpack: number;
    protocol: string;
    'source:type': string;
    ip_dst_addr: number;
    original_string: string;
    tos: number;
    id: number;
    ip_src_addr: string;
    timestamp: number;
    ethdst: string;
    is_alert: boolean;
    ttl: number;
    ethlen: number;
    iplen: number;
    ip_src_port: number;
    tcpflags: string;
    guid: string;
    sig_id: number;
    sig_generator: number;
    'threat:triage:score': number;
    'threatinteljoinbolt:joiner:ts': number;
    'enrichmentsplitterbolt:splitter:begin:ts': number;
    'enrichmentjoinbolt:joiner:ts': number;
    'threatintelsplitterbolt:splitter:end:ts': number;
    'enrichmentsplitterbolt:splitter:end:ts': number;
    'threatintelsplitterbolt:splitter:begin:ts': number;
  };

  status: string;
}
