export class Alert {
  score: number;
  description: string;
  alertId: string;
  age: string;
  source_type: string;
  ip_src_addr: string;
  sourceLocation: string;
  ip_dst_addr: string;
  designatedHost: string;
  status: string;
  _id: string;
  _index: string;
  _type: string;
  _source: {};

  constructor(score: number, description: string, alertId: string, age: string, alertSource: string, sourceIp: string,
              sourceLocation: string, destinationIP: string, designatedHost: string, status: string, index: string,
              type: string, _source: {}) {
    this.score = score;
    this.description = description;
    this.alertId = alertId;
    this._id = alertId;
    this.age = age;
    this.source_type = alertSource;
    this.ip_src_addr = sourceIp;
    this.sourceLocation = sourceLocation;
    this.ip_dst_addr = destinationIP;
    this.designatedHost = designatedHost;
    this.status = status ? status : 'NEW';
    this._index = index;
    this._type = type;

    this._source = _source;
  }
}
