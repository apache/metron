import { ParserModel } from './parser.model';

export class ParserGroupModel implements ParserModel {
  name: string;
  description: string;

  constructor(rawJson: object) {
    if (rawJson['name']) {
      this.name = rawJson['name'];
    } else {
      throw new Error('Json response not contains name');
    }
    this.description = rawJson['description'] || '';
  }

  getName(): string {
    return this.name;
  }

  setName(value: string) {
    this.name = value;
  }
}
