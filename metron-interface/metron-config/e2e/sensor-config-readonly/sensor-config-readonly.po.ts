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
import {changeURL, waitForElementVisibility} from '../utils/e2e_util';

export class SensorDetailsPage {

    private enableButton;
    private disableButton;
    private startButton;
    private stopButton;

    constructor() {
        this.enableButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'ENABLE'));
        this.disableButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'DISABLE'));
        this.startButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'START'));
        this.stopButton = element(by.cssContainingText('metron-config-sensor-parser-readonly .btn', 'STOP'));
    }

    clickToggleShowMoreLess(text: string, index: number) {
        let ele = element.all(by.linkText(text)).get(index);
        browser.driver.executeScript('arguments[0].scrollIntoView()', ele.getWebElement());

        return element.all(by.linkText(text)).get(index).click().then(() => {
            browser.sleep(1000);
            return true;
        });
    }

    closePane(name: string) {
        element.all(by.css('.alert .close')).click();
        return element(by.css('metron-config-sensor-parser-readonly .fa-times')).click().then(() => {
            return true;
        });
    }

    disableParser() {
        return waitForElementVisibility(this.disableButton).then(this.disableButton.click())
                .then(protractor.promise.controlFlow().execute(() =>{waitForElementVisibility(this.enableButton)}))
                .then(() => true);
    }

    enableParser() {
        return waitForElementVisibility(this.enableButton).then(this.enableButton.click())
                .then(protractor.promise.controlFlow().execute(() =>{ waitForElementVisibility(this.disableButton)}))
                .then(() => true);
    }

    startParser() {
        return waitForElementVisibility(this.startButton).then(this.startButton.click())
                .then(protractor.promise.controlFlow().execute(() => { waitForElementVisibility(this.stopButton)}))
                .then(() => true);
    }

    stopParser() {
        return waitForElementVisibility(this.stopButton).then(this.stopButton.click())
                .then(protractor.promise.controlFlow().execute(() => {waitForElementVisibility(this.startButton)}))
                .then(() => true);
    }

    getButtons() {
        return element.all(by.css('metron-config-sensor-parser-readonly button:not([hidden=""])')).getText();
    }

    getCurrentUrl() {
        return browser.getCurrentUrl();
    }

    getGrokStatement() {
        return element(by.css('.form-value.grok')).getText();
    }

    getKafkaState() {
        return element(by.cssContainingText('metron-config-sensor-parser-readonly .form-label', 'KAFKA')).all(by.xpath('..//div')).getText().then(data => data[1]);
    }

    getParserConfig() {
        return element.all(by.css('metron-config-sensor-parser-readonly .row')).getText().then(data => {
            return data.slice(1, 19).map(val => val.replace('\n', ':'));
        });
    }

    getParserState() {
        return element(by.cssContainingText('metron-config-sensor-parser-readonly .form-label', 'STATE')).all(by.xpath('..//div')).getText().then(data => data[1]);
    }

    getSchemaSummary() {
        return element.all(by.css('.transforms .form-label')).getText();
    }

    getSchemaSummaryTitle() {
        return element.all(by.css('.transforms .form-sub-sub-title')).getText();
    }

    getSchemaFullSummary() {
        return protractor.promise.all([
            element.all(by.css('.collapse.in .form-label')).getText(),
            element.all(by.css('.collapse.in .form-value')).getText()
        ]).then(args => {
            let labels = args[0];
            let values = args[1];
            return labels.reduce((acc, val, ind) => { acc[val] = values[ind]; return acc;}, {})
        });
    }

    getStormStatus() {
        return element(by.cssContainingText('metron-config-sensor-parser-readonly .form-label', 'STORM')).all(by.xpath('..//div')).getText().then(data => data[1]);
    }

    getThreatTriageSummary() {
        return element(by.css('.threat-triage-rules')).all(by.xpath("./div")).getText();
    }

    getThreatTriageTableHeaders() {
        return element.all(by.css('#collapseThreatTriage .form-sub-sub-title')).getText();
    }

    getThreatTriageTableValues() {
        return protractor.promise.all([
            element.all(by.css('#collapseThreatTriage .form-label')).getText(),
            element.all(by.css('#collapseThreatTriage .form-value')).getText()
        ]).then(args => {
            let labels = args[0];
            let values = args[1];
            return labels.reduce((acc, val, ind) => { acc[val] = values[ind]; return acc;}, {})
        });
    }

    getTitle() {
        let title = element(by.css('metron-config-sensor-parser-readonly .form-title'));
        return waitForElementVisibility(title).then(() => {
            return title.getText();
        });
    }

    navigateTo(parserName: string) {
        return element(by.cssContainingText('metron-config-sensor-parser-list td', parserName)).element(by.xpath('..')).click();
    }
}
