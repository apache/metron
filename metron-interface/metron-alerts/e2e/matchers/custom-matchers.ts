let customMatchers:jasmine.CustomMatcherFactories = {
  toEqualBcoz: function (util:jasmine.MatchersUtil, customEqualityTesters:Array<jasmine.CustomEqualityTester>):jasmine.CustomMatcher {
    return {
      compare: function (actual:any, expected:any, message = ''):jasmine.CustomMatcherResult {
        if (expected === undefined) {
          expected = '';
        }
        var result = {pass: false, message: ''};
        result.pass = util.equals(actual, expected, customEqualityTesters);
        if (!result.pass) {
          result.message = "Expected '" + actual + "' to equal '" + expected + "' " + message;
        }
        return result;
      }
    }
  }
};

export {customMatchers};