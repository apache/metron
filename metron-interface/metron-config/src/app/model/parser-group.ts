import { ParserConfigListItem } from 'app/sensors/sensor-aggregate/parser-config-item';

export class ParserGroupModel implements ParserConfigListItem {
  name: string;
  description: string;
  history: any = { latency: '', throughput: '', modifiedByDate: '', modifiedBy: '' };
  status = 'Stopped';

  constructor(raw: object) {
    this.setConfig(raw);
  }

  getName(): string {
    return this.name;
  }
  setName(value: string) {
    this.name = value;
  }

  getConfig(): any {
    return { name: this.name, description: this.description };
  }
  setConfig(raw: object) {
    if (raw['name']) {
      this.name = raw['name'];
    } else {
      throw new Error('Json response not contains name');
    }
    this.description = raw['description'] || '';
  }

  getStatus(): any {
    return this.status;
  }
  setStatus(value: string): void {
    this.status = value;
  }

  getHistory(): { latency: string, throughput: string, modifiedByDate: string, modifiedBy: string } {
    return this.history;
  }
  setHistory( value: { latency: string, throughput: string, modifiedByDate: string, modifiedBy: string } ) {
    this.history = value;
  }

  toJson(): string {
    return JSON.stringify(this.getConfig());
  }

  clone(): ParserGroupModel {
    const clone: ParserGroupModel = new ParserGroupModel(
      { name: this.name, description: this.description }
    );

    clone.setStatus(this.getStatus());

    return clone;
  }
}
