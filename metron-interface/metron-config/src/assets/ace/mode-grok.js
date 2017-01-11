ace.define('ace/mode/grok_highlight_rules', function(require, exports, module) {

    "use strict";

    var oop = require("../lib/oop");
    var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

    var GrokHighlightRules = function() {

        var escapeRe = /\\u[0-9a-fA-F]{4}|\\/;

        this.$rules = {
            "start" : [
                {
                    token : "entity.name.function",
                    regex: "\\%{",
                    next  : "value"
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
            "value" : [
                // {
                //     token : "comment",
                //     regex : ":"
                // },
                {
                    token : "entity.name.function",
                    regex : "\\}\\s*",
                    next:   "start"
                },
                {
                    token: "support.function",
                    regex: "[a-zA-Z0-9]+:"
                },{
                    token: "variable.parameter",
                    regex: "\\s*[a-zA-Z0-9-_]+"
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
        // Extra logic goes here. (see below)
    }).call(Mode.prototype);

    exports.Mode = Mode;
});

