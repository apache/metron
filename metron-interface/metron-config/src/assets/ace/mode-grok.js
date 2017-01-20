ace.define('ace/mode/grok_highlight_rules', function(require, exports, module) {

    "use strict";

    var oop = require("../lib/oop");
    var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

    var GrokHighlightRules = function() {

        var escapeRe = /\\u[0-9a-fA-F]{4}|\\/;

        this.$rules = {
            "start" : [
                {
                    token : "paren.lparen",
                    regex: "\\%{",
                    next  : "key"
                },{
                    token : "comment",
                    regex: "\\s*[-/]\\s*"
                },{
                    token : "comment",
                    regex: "\\s*\\\\s*"
                },{
                    defaultToken: "invalid"
                }
            ],
            "key" : [
                {
                    token: "variable",
                    regex: "[a-zA-Z0-9]*",
                    next  : "seperator"
                },{
                    defaultToken: "invalid"
                }
            ],"seperator" : [
                {
                    token: "seperator",
                    regex: "\\s*:{1}",
                    next  : "value"
                },{
                    defaultToken: "invalid"
                }
            ],"value" : [
                {
                    token: "string",
                    regex: "\\s*[a-zA-Z0-9-_]*",
                    next  : "end"
                },{
                    defaultToken: "invalid"
                }
            ],"end" : [
                {
                    token : "paren.rparen",
                    regex : "\\}\\s*",
                    next:   "start"
                },{
                    defaultToken: "invalid"
                }
            ]
        };

    };

    oop.inherits(GrokHighlightRules, TextHighlightRules);

    exports.GrokHighlightRules = GrokHighlightRules;

});

ace.define('ace/mode/grok', function(require, exports, module) {

    var oop = require("ace/lib/oop");
    var TextMode = require("ace/mode/text").Mode;
    var GrokHighlightRules = require("ace/mode/grok_highlight_rules").GrokHighlightRules;

    var Mode = function() {
        this.HighlightRules = GrokHighlightRules;
    };

    oop.inherits(Mode, TextMode);

    (function() {
    }).call(Mode.prototype);

    exports.Mode = Mode;
});


