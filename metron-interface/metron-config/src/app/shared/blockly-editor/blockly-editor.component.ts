/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {Component, OnInit, OnChanges, Input, EventEmitter, Output, AfterViewInit, ViewChild, ElementRef, SimpleChanges} from '@angular/core';
import {BlocklyService} from "../../service/blockly.service";
import {StellarService} from "../../service/stellar.service";
import {StellarFunctionDescription} from "../../model/stellar-function-description";

declare var Blockly: any;

@Component({
  selector: 'metron-config-blockly-editor',
  templateUrl: './blockly-editor.component.html',
  styleUrls: ['./blockly-editor.component.scss']
})

export class BlocklyEditorComponent implements OnInit, AfterViewInit {

  @Input() value: string;
  @Input() availableFields: string[] = [];

  @Output() onStatementChange: EventEmitter<string> = new EventEmitter<string>();

  xml: string;

  @ViewChild('xmltemplate') xmltemplate: ElementRef;
  statement: string;
  workspace;

  constructor(private blocklyService: BlocklyService, private stellarService: StellarService) { }

  ngOnInit() {
    // B'coz of https://github.com/google/blockly/issues/299
    Blockly.WorkspaceSvg.prototype.preloadAudio_ = function() {};
  }

  ngAfterViewInit() {
    this.stellarService.listFunctionsByCategory().subscribe(stellarFunctionMap => {
      this.generateBlocks(stellarFunctionMap);
      this.generateToolbox(stellarFunctionMap);
      this.injectBlockly();
      this.updateAvailableFieldsBlock(this.availableFields);
      this.updateStatement(this.value);
    });
  }

  generateBlocks(stellarFunctionMap: {string: StellarFunctionDescription[]}) {
    for(let category of Object.keys(stellarFunctionMap)) {
      for (let stellarFunctionDescription of stellarFunctionMap[category]) {
        Blockly.Blocks['stellar_' + stellarFunctionDescription.name] = {
          init: function() {
            this.appendDummyInput()
                .appendField(stellarFunctionDescription.name);
            for(let param of stellarFunctionDescription.params) {
              let formattedParam = param.split('-')[0].trim();
              this.appendValueInput(formattedParam.toUpperCase())
                  .setCheck(null)
                  .appendField(formattedParam);
            }
            this.setOutput(true, null);
            this.setColour(160);
            this.setTooltip(stellarFunctionDescription.description);
            this.setHelpUrl('http://www.example.com/');
          }
        };
        Blockly.JavaScript['stellar_' + stellarFunctionDescription.name] = function(block) {
          let values = [];
          for(let param of stellarFunctionDescription.params) {
            let formattedParam = param.split('-')[0].trim();
            values.push(Blockly.JavaScript.valueToCode(block, formattedParam.toUpperCase(), Blockly.JavaScript.ORDER_ADDITION));
          }
          var code = stellarFunctionDescription.name + '(' + values.join(',') + ')';
          return [code, Blockly.JavaScript.ORDER_ADDITION];
        };
      }
    }
  }

  generateToolbox(stellarFunctionMap: {string: StellarFunctionDescription[]}) {
    let xml = '<xml xmlns="http://www.w3.org/1999/xhtml" id="toolbox" style="display: none;">';

    //Stellar functions
    xml += '<category name="Stellar">';
    for(let category of Object.keys(stellarFunctionMap).sort()) {
      xml += '<category name="' + category + '">';
      for (let stellarFunctionDescription of stellarFunctionMap[category]) {
        xml += '<block type="stellar_' + stellarFunctionDescription.name + '"></block>';
      }
      xml += '</category>';
    }
    xml += '</category>';

    //Collection functions
    xml += '<category name="Collection">';
    xml += '<block type="stellar_in"></block>';
    xml += '<block type="lists_create_with"></block>';
    xml += '<block type="stellar_map_create"></block>';
    xml += '<block type="stellar_key_value"><value name="KEY"><block type="text"></value></block>';
    xml += '</category>';

    //Boolean functions
    xml += '<category name="Boolean">';
    xml += '<block type="logic_compare"></block>';
    xml += '<block type="logic_operation"></block>';
    xml += '<block type="stellar_negate"></block>';
    xml += '<block type="logic_ternary"></block>';
    xml += '<block type="stellar_EXISTS"></block>';
    xml += '</category>';

    //Math functions
    xml += '<category name="Math">';
    xml += '<block type="stellar_arithmetic"></block>';
    xml += '</category>';

    //Fields
    xml += '<category name="Fields">';
    xml += '<block type="available_fields"></block>';
    xml += '</category>';

    //Constants
    xml += '<category name="Constants">';
    xml += '<block type="text"><field name="TEXT"></field></block>';
    xml += '<block type="logic_boolean"><field name="BOOL">TRUE</field></block>';
    xml += '<block type="math_number"></block>';
    xml += '<block type="logic_null"></block>';
    xml += '</category>';

    xml += '</xml>';
    this.xml = xml;
  }

  injectBlockly() {
    this.xmltemplate.nativeElement.outerHTML = this.xml;
    this.workspace = Blockly.inject('blocklyDiv',
        {media: 'assets/blockly/media/',
          css: false,
          grid:
          {spacing: 15,
            length: 15,
            colour: '#4d4d4d',
            snap: true},
          toolbox: document.getElementById('toolbox')});

    this.workspace.addChangeListener(event => {
      if (event.type != 'ui') {
        this.statement = Blockly.JavaScript.workspaceToCode(this.workspace).replace(/[;\n]/g, '');
        this.onStatementChange.emit(this.statement);
      }
    });
  }

  loadCurrentStatement() {
    if (this.statement && this.workspace) {
      this.blocklyService.statementToXml(this.statement).subscribe(xml => {
        this.workspace.clear();
        let dom = Blockly.Xml.textToDom(xml);
        Blockly.Xml.domToWorkspace(dom, this.workspace);
      });
    }
  }

  updateStatement(statement) {
    this.statement = this.value;
    this.loadCurrentStatement();
  }

  updateAvailableFieldsBlock(fields) {
    if (!fields || fields.length == 0) {
      fields = ['ip_src_addr', 'ip_src_port', 'ip_dst_addr', 'ip_dst_port', 'protocol', 'timestamp', 'includes_reverse_traffic'];
    }
    fields.sort();
    let fieldArray = [];
    for(let field of fields) {
      fieldArray.push([field, field]);
    }
    Blockly.Blocks['available_fields'] = {
      init: function() {
        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(fieldArray), "FIELD_NAME");
        this.setOutput(true, "String");
        this.setTooltip('These are the available fields');
        this.setHelpUrl('http://www.example.com/');
        this.setColour(270);
      }
    };
    this.loadCurrentStatement();
  }

}
