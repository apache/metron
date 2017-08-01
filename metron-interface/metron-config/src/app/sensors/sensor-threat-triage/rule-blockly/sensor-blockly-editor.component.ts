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
import {Component, OnInit, Input, EventEmitter, Output, AfterViewInit, ViewChild, ElementRef} from '@angular/core';
import {BlocklyService} from "../../../service/blockly.service";
import {RiskLevelRule} from "../../../model/risk-level-rule";
import {SensorParserConfigService} from "../../../service/sensor-parser-config.service";
import {SampleDataComponent} from "../../../shared/sample-data/sample-data.component";
import {ParseMessageRequest} from "../../../model/parse-message-request";
import {BlocklyEditorComponent} from "../../../shared/blockly-editor/blockly-editor.component";

declare var Blockly: any;

@Component({
  selector: 'metron-config-sensor-rule-blockly',
  templateUrl: './sensor-blockly-editor.component.html',
  styleUrls: ['./sensor-blockly-editor.component.scss']
})

export class SensorBlocklyEditorComponent implements OnInit, AfterViewInit {

  @Input() riskLevelRule: RiskLevelRule;
  @Input() sensorParserConfig: string;

  @ViewChild(SampleDataComponent) sampleData: SampleDataComponent;
  @ViewChild(BlocklyEditorComponent) blocklyEditor: BlocklyEditorComponent;
  @Output() onCancelBlocklyEditor: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() onSubmitBlocklyEditor: EventEmitter<RiskLevelRule> = new EventEmitter<RiskLevelRule>();
  private newRiskLevelRule = new RiskLevelRule();

  constructor(private sensorParserConfigService: SensorParserConfigService) { }

  ngOnInit() {
    Object.assign(this.newRiskLevelRule, this.riskLevelRule);
  }

  ngAfterViewInit(): void {
    this.sampleData.getNextSample();
  }

  onStatementChange(statement: string): void {
    this.newRiskLevelRule.rule = statement;
  }

  onSave(): void {
    this.onSubmitBlocklyEditor.emit(this.newRiskLevelRule);
  }

  onCancel(): void {
    this.onCancelBlocklyEditor.emit(true);
  }

  onSampleDataChanged(sampleData: string) {
    let parseMessageRequest = new ParseMessageRequest();
    parseMessageRequest.sensorParserConfig = JSON.parse(JSON.stringify(this.sensorParserConfig));
    parseMessageRequest.sampleData = sampleData;
    this.sensorParserConfigService.parseMessage(parseMessageRequest).subscribe(
        parserResult => {
          let availableFields = Object.keys(parserResult);
          this.blocklyEditor.updateAvailableFieldsBlock(availableFields);
        },
        error => {
          this.blocklyEditor.updateAvailableFieldsBlock([]);
        });
  }

}
