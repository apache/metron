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
import { browser, element, by, protractor } from 'protractor/globals';
import {changeURL, waitForElementVisibility, waitForElementInVisibility, waitForElementPresence} from '../utils/e2e_util';

export class SensorConfigPage {

  private getEnrichmentsMultipleInput() {
    return element.all(by.css('.config.container')).all(by.css('metron-config-multiple-input')).get(1).all(by.css('select')).last();
  }

  private getThreatIntelMultipleInput() {
    return element.all(by.css('.config.container')).all(by.css('metron-config-multiple-input')).last().all(by.css('select')).last();
  }

  private getTransformationMultipleInput() {
    return element.all(by.css('.config.container')).all(by.css('metron-config-multiple-input')).first().all(by.css('select')).last();
  }

  clickAddButton() {
    changeURL(browser.baseUrl + '/sensors');
    let addButton = element(by.css('.metron-add-button.hexa-button'));
    return waitForElementPresence(addButton).then(() => {
      addButton.click();
    });
  }

  clickAddThreatTriageRule() {
    let addThreatTriageButton = element(by.css('metron-config-sensor-threat-triage .add-button'));
    return waitForElementVisibility(addThreatTriageButton).then(() => {
      return addThreatTriageButton.click();
    });
  }

  clickGrokStatement() {
    let grokInput = element(by.css('input[formcontrolname="grokStatement"] + span'));
    return waitForElementVisibility(grokInput).then(() => {
      return grokInput.click();
    });
  }

  clickSchema() {
    let schemaInput = element(by.css('div[name="fieldSchema"] button'));
    return waitForElementVisibility(schemaInput).then(() => {
      return schemaInput.click();
    });
  }

  clickThreatTriage() {
    let threatTriageInput = element(by.css('div[name="threatTriage"] button'));
    return waitForElementVisibility(threatTriageInput).then(() => {
      return threatTriageInput.click();
    });
  }

  closeMainPane() {
    return element(by.css('.btn.save-button + button')).click();
  }

  closeSchemaPane() {
    return element.all(by.css('metron-config-sensor-field-schema .form-title + i')).click();
  }

  closeThreatTriagePane() {
    return element.all(by.css('metron-config-sensor-threat-triage .form-title + i')).click();
  }

  getGrokStatementFromMainPane() {
    browser.waitForAngular;
    return element.all(by.css('input[formcontrolname="grokStatement"]')). getAttribute('value');
  }

  getFieldSchemaSummary() {
    return element.all(by.css('[name="fieldSchema"] table tr')).getText();
  }

  getFieldSchemaValues() {
    return waitForElementPresence(element(by.css('metron-config-sensor-field-schema .field-schema-row'))).then(() => {
      return element.all(by.css('metron-config-sensor-field-schema .field-schema-row')).getText();
    });
  }

  getGrokResponse() {
    let tableRows = element(by.css('metron-config-sensor-grok table tr'));
    return waitForElementPresence(tableRows).then(() => {
      return element.all(by.css('metron-config-sensor-grok table tr')).getText();
    });
  }

  getFieldSchemaEditButton(name: string) {
    let fieldRowClassName = 'metron-config-sensor-field-schema .field-schema-row';
    return element(by.cssContainingText(fieldRowClassName, name)).element(by.xpath("..")).element(by.css('.fa-pencil'));
  }

  getFormData() {
    let mainPanel = element.all(by.css('.metron-slider-pane-edit')).last();
    return protractor.promise.all([
      mainPanel.element(by.css('.form-title')).getText(),
      mainPanel.element(by.css('input[name="sensorTopic"]')).getAttribute('value'),
      mainPanel.element(by.css('select[formcontrolname="parserClassName"]')).getAttribute('value'),
      mainPanel.element(by.css('input[formcontrolname="grokStatement"]')).getAttribute('value'),
      mainPanel.all(by.css('div[name="fieldSchema"] table tr')).getText(),
      mainPanel.all(by.css('div[name="threatTriage"] table tr')).getText(),
      mainPanel.element(by.css('input[formcontrolname="index"]')).getAttribute('value'),
      mainPanel.element(by.css('metron-config-number-spinner[name="batchSize"] input')).getAttribute('value'),
      mainPanel.all(by.css('metron-config-advanced-form input')).getAttribute('value')
    ]).then(args => {
      return  {
        title: args[0],
        parserName: args[1],
        parserType: args[2],
        grokStatement: args[3],
        fieldSchemaSummary: args[4],
        threatTriageSummary: args[5],
        indexName: args[6],
        batchSize: args[7],
        advancedConfig: args[8]
      }
    });
  }

  getKafkaStatusText() {
    return element(by.css('input[name="sensorTopic"] + label')).getText();
  }

  getThreatTrigaeRule() {
    return element.all(by.css('.threat-triage-rule-row.threat-triage-rule-str')).getText();
  }

  getThreatTriageSummary() {
    return element.all(by.css('div[name="threatTriage"] table tr')).getText();
  }

  getTransformText() {
    return element.all(by.css('.config.container')).all(by.css('.edit-pane-readonly')).getText();
  }

  navigateTo(parserName: string) {
    let url = browser.baseUrl + '/sensors(dialog:sensors-readonly/'+ parserName + ')';
    return changeURL(url);
  }

  setAdvancedConfig(key: string, value: string) {
    return element.all(by.css('.advanced-link')).click().then(() => {
      element(by.css('input[formcontrolname="newConfigKey"]')).sendKeys(key);
      element(by.css('input[ng-reflect-name="newConfigValue"]')).sendKeys(value);
    });
  }

  saveFieldSchemaConfig() {
    return element.all(by.css('.config.container')).all(by.cssContainingText('.btn', 'SAVE')).click();
  }

  saveGrokStatement() {
    let saveButton = element(by.cssContainingText('metron-config-sensor-grok button', 'SAVE'));
    return waitForElementVisibility(saveButton).then(() => {
      return element(by.cssContainingText('metron-config-sensor-grok button', 'SAVE')).click();
    })
  }

  saveThreatTriageRule() {
    return element(by.cssContainingText('metron-config-sensor-rule-editor .btn', 'SAVE')).click();
  }

  setGrokStatement(statement: string) {
    return element(by.css('metron-config-sensor-grok .ace_text-input')).sendKeys(statement);
  }

  saveParser() {
    return element(by.css('.btn.save-button')).click();
  }

  setParserName(name: string) {
    return element(by.css('input[name="sensorTopic"]')).sendKeys(name);
  }

  setParserType(parserName: string) {
    return element(by.css('select[formcontrolname="parserClassName"]')).click().then(() => {
      return element.all(by.cssContainingText('select[formcontrolname="parserClassName"] option', parserName)).get(0).click();
    })
  }

  setSampleMessage(paneName: string, message: string) {
    let sampleMsgTextArea = element(by.css('metron-config-' + paneName + ' .form-control.sample-input'));
    return waitForElementVisibility(sampleMsgTextArea).then(() => {
      return sampleMsgTextArea.sendKeys(message);
    })
  }

  setSchemaConfig(fieldName: string, transformValues: string[], enrichmentValues: string[], threatIntelValues: string[]) {
    this.getFieldSchemaEditButton(fieldName).click();
    
    transformValues.forEach(transform => {
      let transformSelect = this.getTransformationMultipleInput();
      transformSelect.click().then(() => {
        transformSelect.all(by.cssContainingText('option', transform)).last().click();
      });
    });
    
    enrichmentValues.forEach(enrichment => {
      let enrichmentSelect = this.getEnrichmentsMultipleInput();
      enrichmentSelect.click().then(() => {
        enrichmentSelect.all(by.cssContainingText('option', enrichment)).last().click();
      });
    })

    threatIntelValues.forEach(threatIntel => {
      let threatIntelSelect = this.getThreatIntelMultipleInput();
      threatIntelSelect.click().then(() => {
        threatIntelSelect.all(by.cssContainingText('option', threatIntel)).last().click();
      });
    })
  }

  setThreatTriageRule(rule: string) {
    let threatTriageTextArea = element(by.css('metron-config-sensor-rule-editor textarea'));
    return waitForElementVisibility(threatTriageTextArea).then(() => {
      return threatTriageTextArea.sendKeys(rule);
    })
  }

  testGrokStatement() {
    let saveButton = element(by.cssContainingText('metron-config-sensor-grok button', 'TEST'));
    return waitForElementVisibility(saveButton).then(() => {
      return element(by.cssContainingText('metron-config-sensor-grok button', 'TEST')).click();
    })
  }
}
