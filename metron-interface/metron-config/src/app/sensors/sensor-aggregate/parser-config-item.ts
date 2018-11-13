export interface ParserConfigListItem {

  getName(): string;
  setName(value: string): void;

  getConfig(): any;
  setConfig(value: any): void;

  getStatus(): any;
  setStatus(value: any): void;

  getHistory(): { latency: string, throughput: string, modifiedByDate: string, modifiedBy: string }
  setHistory(value: { latency: string, throughput: string, modifiedByDate: string, modifiedBy: string }): void

  toJson(): string;
  clone(): any;
}
