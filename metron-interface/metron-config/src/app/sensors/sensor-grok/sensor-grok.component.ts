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

import { Component, OnInit, Input, OnChanges, SimpleChanges, ViewChild, EventEmitter, Output} from '@angular/core';
import {SensorParserConfig} from '../../model/sensor-parser-config';
import {ParseMessageRequest} from '../../model/parse-message-request';
import {SensorParserConfigService} from '../../service/sensor-parser-config.service';
import {AutocompleteOption} from '../../model/autocomplete-option';
import {GrokValidationService} from '../../service/grok-validation.service';
import {SampleDataComponent} from '../../shared/sample-data/sample-data.component';
import {MetronAlerts} from '../../shared/metron-alerts';

@Component({
  selector: 'metron-config-sensor-grok',
  templateUrl: './sensor-grok.component.html',
  styleUrls: ['./sensor-grok.component.scss']
})
export class SensorGrokComponent implements OnInit, OnChanges {

  @Input() showGrok; boolean;
  @Input() sensorParserConfig: SensorParserConfig;
  @Input() grokStatement: string;
  @Input() patternLabel: string;

  @Output() hideGrok = new EventEmitter<void>();
  @Output() onSaveGrokStatement = new EventEmitter<string>();
  @Output() onSavePatternLabel = new EventEmitter<string>();

  @ViewChild(SampleDataComponent) sampleData: SampleDataComponent;

  newGrokStatement = '';
  newPatternLabel = '';
  availablePatternLabels = [];
  parsedMessage: any = {};
  parsedMessageKeys: string[] = [];
  grokFunctionList: AutocompleteOption[] = [];
  parseMessageRequest: ParseMessageRequest = new ParseMessageRequest();

  constructor(private sensorParserConfigService: SensorParserConfigService, private grokValidationService: GrokValidationService,
              private metronAlerts: MetronAlerts) {
    this.parseMessageRequest.sampleData = '';
  }

  ngOnInit() {
    this.getGrokFunctions();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['showGrok'] && changes['showGrok'].currentValue) {
      this.newPatternLabel = this.patternLabel;
      if (this.grokStatement) {
        this.newGrokStatement = this.grokStatement;
      } else {
        this.newGrokStatement = this.newPatternLabel + ' ';
      }
      this.getAvailablePatternLabels();
      this.sampleData.getNextSample();
    }
  }

  onSampleDataChanged(sampleData: string) {
    if (sampleData) {
      this.parseMessageRequest.sampleData = sampleData;
      this.onTestGrokStatement();
    }
  }

  onTestGrokStatement() {
    this.parsedMessage = {};

    if (this.newGrokStatement.indexOf('%{') === -1) {
      return;
    }

    this.parseMessageRequest.sensorParserConfig = JSON.parse(JSON.stringify(this.sensorParserConfig));
    this.parseMessageRequest.grokStatement = this.newGrokStatement;
    this.parseMessageRequest.sensorParserConfig.parserConfig['patternLabel'] = this.newPatternLabel;
    this.parseMessageRequest.sensorParserConfig.parserConfig['grokPath'] = './' + this.parseMessageRequest.sensorParserConfig.sensorTopic;

    this.sensorParserConfigService.parseMessage(this.parseMessageRequest).subscribe(
        result => {
          this.parsedMessage = result;
          this.setParsedMessageKeys();
        }, error => {
          this.metronAlerts.showErrorMessage(error.message);
          this.setParsedMessageKeys();
        });
  }

  private getGrokFunctions() {
    this.grokValidationService.list().subscribe(result => {
      Object.keys(result).forEach(name => {
        let autocompleteOption: AutocompleteOption = new AutocompleteOption();
        autocompleteOption.name = name;
        this.grokFunctionList.push(autocompleteOption);
      });
    });
  }

  private setParsedMessageKeys() {
    try {
      this.parsedMessageKeys = Object.keys(this.parsedMessage).sort();
    } catch (e) {
      this.parsedMessageKeys = [];
    }
  }

  onSaveGrok(): void {
    this.onSaveGrokStatement.emit(this.newGrokStatement);
    this.onSavePatternLabel.emit(this.newPatternLabel);
    this.hideGrok.emit();
  }

  onCancelGrok(): void {
    this.hideGrok.emit();
  }

  getAvailablePatternLabels() {
    this.availablePatternLabels = [];
    let statements = this.newGrokStatement.split('\n');
    for (let statement of statements) {
      if (statement) {
        let patternLabel = statement.split(' ')[0];
        this.availablePatternLabels.push(patternLabel);
      }
    }
  }

  isTestDisabled() {
    return this.parseMessageRequest.sampleData.length === 0 || this.newGrokStatement.length === 0;
  }

  isSaveDisabled() {
    return this.newGrokStatement.length === 0 || this.availablePatternLabels.indexOf(this.newPatternLabel) === -1;
  }


}
