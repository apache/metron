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
import {changeURL, waitForElement} from '../utils/e2e_util';

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

    disableParser() {
        return waitForElement(this.disableButton).then(() => {
            return this.disableButton.click().then(() => {
                return waitForElement(this.enableButton).then(() => {
                    return true;
                })
            });
        });
    }

    enableParser() {
        return waitForElement(this.enableButton).then(() => {
            return this.enableButton.click().then(() => {
                return waitForElement(this.disableButton).then(() => {
                    return true;
                })
            });
        });
    }

    startParser() {
        return waitForElement(this.startButton).then(() => {
            return this.startButton.click().then(() => {
                return waitForElement(this.stopButton).then(() => {
                    return true;
                })
            });
        });
    }

    stopParser() {
        return waitForElement(this.stopButton).then(() => {
            return this.stopButton.click().then(() => {
                return waitForElement(this.startButton).then(() => {
                    return true;
                })
            });
        });
    }

    getButtons() {
        return element.all(by.css('metron-config-sensor-parser-readonly button:not([hidden=""])')).getText();
    }

    getParserConfig() {
        return element.all(by.css('metron-config-sensor-parser-readonly .row')).getText().then(data => {
            return data.slice(1, 19);
        });
    }

    getTitle() {
        return element(by.css('metron-config-sensor-parser-readonly .form-title')).getText();
    }

    navigateTo(parserName: string) {
        let url = browser.baseUrl + '/sensors(dialog:sensors-readonly/'+ parserName + ')';
        return changeURL(url);
    }
}
