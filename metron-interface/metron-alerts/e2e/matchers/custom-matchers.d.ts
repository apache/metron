declare module jasmine {
  interface Matchers {
    toEqualBcoz(expected: any, message: string): boolean;
  }
}
