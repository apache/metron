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
        this.navigateToLogin();
        this.setUserNameAndPassword('admin', 'password');
        this.submitLoginForm();
        browser.waitForAngularEnabled(false);
        browser.wait(function() {return element(by.css('.logout')).isPresent(); });
    }

    logout() {
        browser.waitForAngularEnabled(false);
        element.all(by.css('.alert .close')).click();
        element.all(by.css('.logout-link')).click();
        waitForURL('http://localhost:4200/login');
    }

    setUserNameAndPassword(userName: string, password: string) {
        element.all(by.css('input.form-control')).get(0).sendKeys(userName);
        element.all(by.css('input.form-control')).get(1).sendKeys(password);
    }

    submitLoginForm() {
        return element.all(by.buttonText('LOG IN')).click();
    }

    getErrorMessage() {
        browser.waitForAngularEnabled(false);
        let errElement = element(by.css('div[style="color:#a94442"]'));
        return waitForElementVisibility(errElement).then(() => {
            browser.sleep(1000);
            return errElement.getText().then((message) => {
                return message.replace(/\n/, '').replace(/LOG\ IN$/, '');
            });
        });
    }

    getLocation() {
        return browser.getCurrentUrl().then(url => {
            return url;
        });
    }
}
