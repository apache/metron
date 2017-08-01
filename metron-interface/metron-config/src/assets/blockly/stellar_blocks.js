'use strict';

goog.provide('Blockly.Blocks.colour');

goog.require('Blockly.Blocks');

Blockly.Blocks['stellar_and'] = {
    init: function() {
        var OPERATORS =
            [[Blockly.Msg.LOGIC_OPERATION_AND, 'AND'],
                [Blockly.Msg.LOGIC_OPERATION_OR, 'OR']];
        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(OPERATORS), 'OP');
        this.appendValueInput("ARG1")
            .setCheck('Boolean');
        this.setOutput(true, "Boolean");
        this.setTooltip('');
        this.setHelpUrl('http://www.example.com/');
        this.setColour(50);
    },
    onchange: function(changeEvent) {
        if (changeEvent.newParentId == this.id) {
            var numFields = this.inputList.length - 1;
            this.appendValueInput('ARG' + (numFields + 1))
                .setCheck('Boolean');
        }
        if (changeEvent.oldParentId == this.id) {
            this.removeInput(this.inputList[this.inputList.length - 1].name);
            this.moveInputBefore(changeEvent.oldInputName, null);
            for(var j = 1; j < this.inputList.length; j++) {
                this.inputList[j].name = 'ARG' + j;
            }
        }
    },
    domToMutation: function(xmlElement) {
        console.log(xmlElement);
    }
};
Blockly.Blocks['stellar_arithmetic'] = {
    /**
     * Block for basic arithmetic operator.
     * @this Blockly.Block
     */
    init: function() {
        this.jsonInit({
            "message0": "%1 %2 %3",
            "args0": [
                {
                    "type": "input_value",
                    "name": "A"
                    // "check": "Number"
                },
                {
                    "type": "field_dropdown",
                    "name": "OP",
                    "options":
                        [[Blockly.Msg.MATH_ADDITION_SYMBOL, 'ADD'],
                            [Blockly.Msg.MATH_SUBTRACTION_SYMBOL, 'MINUS'],
                            [Blockly.Msg.MATH_MULTIPLICATION_SYMBOL, 'MULTIPLY'],
                            [Blockly.Msg.MATH_DIVISION_SYMBOL, 'DIVIDE']]
                },
                {
                    "type": "input_value",
                    "name": "B"
                    //  "check": "Number"
                }
            ],
            "inputsInline": true,
            "output": "Number",
            "colour": Blockly.Blocks.math.HUE,
            "helpUrl": Blockly.Msg.MATH_ARITHMETIC_HELPURL
        });
        // Assign 'this' to a variable for use in the tooltip closure below.
        var thisBlock = this;
        this.setTooltip(function() {
            var mode = thisBlock.getFieldValue('OP');
            var TOOLTIPS = {
                'ADD': Blockly.Msg.MATH_ARITHMETIC_TOOLTIP_ADD,
                'MINUS': Blockly.Msg.MATH_ARITHMETIC_TOOLTIP_MINUS,
                'MULTIPLY': Blockly.Msg.MATH_ARITHMETIC_TOOLTIP_MULTIPLY,
                'DIVIDE': Blockly.Msg.MATH_ARITHMETIC_TOOLTIP_DIVIDE
            };
            return TOOLTIPS[mode];
        });
    }
};
Blockly.Blocks['stellar_in'] = {
    /**
     * Block for finding an item in the list.
     * @this Blockly.Block
     */
    init: function() {
        this.setHelpUrl(Blockly.Msg.LISTS_INDEX_OF_HELPURL);
        this.setColour(Blockly.Blocks.lists.HUE);
        this.setOutput(true, 'Boolean');
        this.appendValueInput('INPUT');
        this.appendValueInput('LIST')
            .appendField(new Blockly.FieldDropdown([["in", "in"],["not in", "not in"]]), 'OP')
            .setCheck('Array');
        this.setInputsInline(true);
        // Assign 'this' to a variable for use in the tooltip closure below.
        var thisBlock = this;
        this.setTooltip(function() {
            return Blockly.Msg.LISTS_INDEX_OF_TOOLTIP.replace('%1',
                this.workspace.options.oneBasedIndex ? '0' : '-1');
        });
    }
};
Blockly.Blocks['stellar_map_create'] = {
    /**
     * Block for creating a list with any number of elements of any type.
     * @this Blockly.Block
     */
    init: function() {
        this.setHelpUrl(Blockly.Msg.LISTS_CREATE_WITH_HELPURL);
        this.setColour(Blockly.Blocks.lists.HUE);
        this.itemCount_ = 3;
        this.updateShape_();
        this.setOutput(true, 'Map');
        this.setMutator(new Blockly.Mutator(['lists_create_with_item']));
        this.setTooltip(Blockly.Msg.LISTS_CREATE_WITH_TOOLTIP);
    },
    /**
     * Create XML to represent list inputs.
     * @return {!Element} XML storage element.
     * @this Blockly.Block
     */
    mutationToDom: function() {
        var container = document.createElement('mutation');
        container.setAttribute('items', this.itemCount_);
        return container;
    },
    /**
     * Parse XML to restore the list inputs.
     * @param {!Element} xmlElement XML storage element.
     * @this Blockly.Block
     */
    domToMutation: function(xmlElement) {
        this.itemCount_ = parseInt(xmlElement.getAttribute('items'), 10);
        this.updateShape_();
    },
    /**
     * Populate the mutator's dialog with this block's components.
     * @param {!Blockly.Workspace} workspace Mutator's workspace.
     * @return {!Blockly.Block} Root block in mutator.
     * @this Blockly.Block
     */
    decompose: function(workspace) {
        var containerBlock = workspace.newBlock('lists_create_with_container');
        containerBlock.initSvg();
        var connection = containerBlock.getInput('STACK').connection;
        for (var i = 0; i < this.itemCount_; i++) {
            var itemBlock = workspace.newBlock('lists_create_with_item');
            itemBlock.initSvg();
            connection.connect(itemBlock.previousConnection);
            connection = itemBlock.nextConnection;
        }
        return containerBlock;
    },
    /**
     * Reconfigure this block based on the mutator dialog's components.
     * @param {!Blockly.Block} containerBlock Root block in mutator.
     * @this Blockly.Block
     */
    compose: function(containerBlock) {
        var itemBlock = containerBlock.getInputTargetBlock('STACK');
        // Count number of inputs.
        var connections = [];
        while (itemBlock) {
            connections.push(itemBlock.valueConnection_);
            itemBlock = itemBlock.nextConnection &&
                itemBlock.nextConnection.targetBlock();
        }
        // Disconnect any children that don't belong.
        for (var i = 0; i < this.itemCount_; i++) {
            var connection = this.getInput('ADD' + i).connection.targetConnection;
            if (connection && connections.indexOf(connection) == -1) {
                connection.disconnect();
            }
        }
        this.itemCount_ = connections.length;
        this.updateShape_();
        // Reconnect any child blocks.
        for (var i = 0; i < this.itemCount_; i++) {
            Blockly.Mutator.reconnect(connections[i], this, 'ADD' + i);
        }
    },
    /**
     * Store pointers to any connected child blocks.
     * @param {!Blockly.Block} containerBlock Root block in mutator.
     * @this Blockly.Block
     */
    saveConnections: function(containerBlock) {
        var itemBlock = containerBlock.getInputTargetBlock('STACK');
        var i = 0;
        while (itemBlock) {
            var input = this.getInput('ADD' + i);
            itemBlock.valueConnection_ = input && input.connection.targetConnection;
            i++;
            itemBlock = itemBlock.nextConnection &&
                itemBlock.nextConnection.targetBlock();
        }
    },
    /**
     * Modify this block to have the correct number of inputs.
     * @private
     * @this Blockly.Block
     */
    updateShape_: function() {
        if (this.itemCount_ && this.getInput('EMPTY')) {
            this.removeInput('EMPTY');
        } else if (!this.itemCount_ && !this.getInput('EMPTY')) {
            this.appendDummyInput('EMPTY')
                .appendField('create empty map');
        }
        // Add new inputs.
        for (var i = 0; i < this.itemCount_; i++) {
            if (!this.getInput('ADD' + i)) {
                var input = this.appendValueInput('ADD' + i).setCheck('KeyValue');
                if (i == 0) {
                    input.appendField("create map with");
                }
            }
        }
        // Remove deleted inputs.
        while (this.getInput('ADD' + i)) {
            this.removeInput('ADD' + i);
            i++;
        }
    }
};
Blockly.Blocks['stellar_key_value'] = {
    /**
     * Block for finding an item in the list.
     * @this Blockly.Block
     */
    init: function() {
        this.setHelpUrl(Blockly.Msg.LISTS_INDEX_OF_HELPURL);
        this.setColour(Blockly.Blocks.lists.HUE);
        this.setOutput(true, 'KeyValue');
        this.appendValueInput('KEY');
        this.appendValueInput('VALUE')
            .appendField(" : ");
        this.setInputsInline(true);
        // Assign 'this' to a variable for use in the tooltip closure below.
        var thisBlock = this;
        this.setTooltip(function() {
            return Blockly.Msg.LISTS_INDEX_OF_TOOLTIP.replace('%1',
                this.workspace.options.oneBasedIndex ? '0' : '-1');
        });
    }
};
Blockly.Blocks['stellar_negate'] = {
    /**
     * Block for negation.
     * @this Blockly.Block
     */
    init: function() {
        this.jsonInit({
            "message0": Blockly.Msg.LOGIC_NEGATE_TITLE,
            "args0": [
                {
                    "type": "input_value",
                    "name": "BOOL",
                    "check": "Boolean"
                }
            ],
            "output": "Boolean",
            "colour": Blockly.Blocks.logic.HUE,
            "tooltip": Blockly.Msg.LOGIC_NEGATE_TOOLTIP,
            "helpUrl": Blockly.Msg.LOGIC_NEGATE_HELPURL
        });
    }
};
Blockly.Blocks['stellar_EXISTS'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("EXISTS");
        this.appendValueInput("INPUT")
            .setCheck(null)
            .appendField("input");
        this.setOutput(true, null);
        this.setColour(160);
        this.setTooltip('');
        this.setHelpUrl('http://www.example.com/');
    }
};