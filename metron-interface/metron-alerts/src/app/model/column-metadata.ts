export class ColumnMetadata {
  name: string;
  type: string;

  static fromJSON(objs: ColumnMetadata[]): ColumnMetadata[] {
    let columns = [];
    for (let obj of objs) {
      columns.push(new ColumnMetadata(obj.name, obj.type));
    }
    return columns;
  }

  constructor(name: string, type: string) {
    this.name = name;
    this.type = type;
  }
}
