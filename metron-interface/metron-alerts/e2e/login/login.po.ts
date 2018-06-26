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
import { browser, element, by } from 'protractor';
import {waitForElementVisibility, waitForURL} from '../utils/e2e_util';

export class LoginPage {
    navigateToLogin() {
        return browser.get('/');
    }

    login() {
        return this.navigateToLogin()
        .then(() => this.setUserNameAndPassword('admin', 'password'))
        .then(() => this.submitLoginForm())
        .then(() => browser.waitForAngularEnabled(false))
        .then(() => waitForElementVisibility(element(by.css('.logout-link'))))
        .then(() => browser.executeScript("document.body.className += ' notransition';"));
    }

    logout() {
        return browser.waitForAngularEnabled(false)
        .then(() => waitForElementVisibility(element(by.css('.logout-link'))))
        .then(() => element(by.css('.logout-link')).click())
        .then(() => waitForURL('http://localhost:4200/login'));
    }

    setUserNameAndPassword(userName: string, password: string) {
        return waitForElementVisibility(element(by.css('[name=user]')))
        .then(() => waitForElementVisibility(element(by.css('[name=password]'))))
        .then(() => element(by.css('[name=user]')).clear())
        .then(() => element(by.css('[name=user]')).sendKeys(userName))
        .then(() => element(by.css('[name=password]')).clear())
        .then(() => element(by.css('[name=password]')).sendKeys(password));
    }

    submitLoginForm() {
        return element(by.buttonText('LOG IN')).click();
    }

    getErrorMessage() {
        let errElement = element(by.css('.login-failed-msg'));
        return browser.waitForAngularEnabled(false)
                .then(() => waitForElementVisibility(errElement))
                .then(() => {
                        browser.sleep(1000);
                    return errElement.getText().then((message) => {
                        return message.replace(/\n/, '').replace(/LOG\ IN$/, '');
            });
        });
    }

    getLocation() {
        return browser.getCurrentUrl();
    }
}
