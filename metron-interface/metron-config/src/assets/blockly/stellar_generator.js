Blockly.JavaScript['available_fields'] = function(block) {
    var field_name = block.getFieldValue('FIELD_NAME');
    return [field_name, Blockly.JavaScript.ORDER_ADDITION];
};
Blockly.JavaScript['stellar_and'] = function(block) {
    var arguments = [];
    for (var i = 1; i < block.inputList.length - 1; i++) {
        arguments[i - 1] = Blockly.JavaScript.valueToCode(block, block.inputList[i].name, Blockly.JavaScript.ORDER_ADDITION);
    }
    if (block.getFieldValue('OP') == 'OR') {
        return [arguments.join(' || '), Blockly.JavaScript.ORDER_LOGICAL_OR];
    } else {
        return [arguments.join(' && '), Blockly.JavaScript.ORDER_LOGICAL_AND];
    }
};
Blockly.JavaScript['stellar_arithmetic'] = function(block) {
    // Basic arithmetic operators, and power.
    var OPERATORS = {
        'ADD': [' + ', Blockly.JavaScript.ORDER_ADDITION],
        'MINUS': [' - ', Blockly.JavaScript.ORDER_SUBTRACTION],
        'MULTIPLY': [' * ', Blockly.JavaScript.ORDER_MULTIPLICATION],
        'DIVIDE': [' / ', Blockly.JavaScript.ORDER_DIVISION]
    };
    var tuple = OPERATORS[block.getFieldValue('OP')];
    var operator = tuple[0];
    var order = tuple[1];
    var argument0 = Blockly.JavaScript.valueToCode(block, 'A', order) || '0';
    var argument1 = Blockly.JavaScript.valueToCode(block, 'B', order) || '0';
    var code = argument0 + operator + argument1;
    return [code, order];
};
Blockly.JavaScript['stellar_in'] = function(block) {
    var value_input = Blockly.JavaScript.valueToCode(block, 'INPUT', Blockly.JavaScript.ORDER_ADDITION);
    var value_list = Blockly.JavaScript.valueToCode(block, 'LIST', Blockly.JavaScript.ORDER_ADDITION);
    var field_op = block.getFieldValue('OP');
    var code = value_input + ' ' + field_op + ' ' + value_list;
    return [code, Blockly.JavaScript.ORDER_ADDITION];
};
Blockly.JavaScript['stellar_map_create'] = function(block) {
    // Create a list with any number of elements of any type.
    var elements = new Array(block.itemCount_);
    for (var i = 0; i < block.itemCount_; i++) {
        elements[i] = Blockly.JavaScript.valueToCode(block, 'ADD' + i,
                Blockly.JavaScript.ORDER_COMMA) || 'null';
    }
    var code = '{' + elements.join(', ') + '}';
    return [code, Blockly.JavaScript.ORDER_ATOMIC];
};
Blockly.JavaScript['stellar_key_value'] = function(block) {
    var value_key = Blockly.JavaScript.valueToCode(block, 'KEY', Blockly.JavaScript.ORDER_ADDITION);
    var value_value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_ADDITION);
    var code = value_key + " : " + value_value;
    return [code, Blockly.JavaScript.ORDER_ADDITION];
};
Blockly.JavaScript['stellar_negate'] = function(block) {
    // Negation.
    var order = Blockly.JavaScript.ORDER_LOGICAL_NOT;
    var argument0 = Blockly.JavaScript.valueToCode(block, 'BOOL', order) ||
        'true';
    var code = 'not' + argument0;
    return [code, order];
};
Blockly.JavaScript['stellar_EXISTS'] = function(block) {
    var value_input = Blockly.JavaScript.valueToCode(block, 'INPUT', Blockly.JavaScript.ORDER_ADDITION);
    var code = 'EXISTS(' + value_input + ')';
    return [code, Blockly.JavaScript.ORDER_ADDITION];
};