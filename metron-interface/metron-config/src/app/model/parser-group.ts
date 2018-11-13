export class ParserGroupModel {
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
}
