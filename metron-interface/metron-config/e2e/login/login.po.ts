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
import { browser, element, by } from 'protractor/globals';

export class LoginPage {
    navigateToLogin() {
        return browser.get('/');
    }

    login() {
        browser.wait(function() {return element(by.css('input.form-control')).isPresent();});
        this.setUserNameAndPassword('admin', 'password');
        this.submitLoginForm();
        browser.wait(function() {return element(by.css('.logout')).isPresent();});
    }

    logout() {
        browser.ignoreSynchronization = true;
        element.all(by.css('.alert .close')).click();
        element.all(by.css('.logout-link')).click();
        browser.sleep(2000);
    }

    setUserNameAndPassword(userName: string, password: string) {
        element.all(by.css('input.form-control')).get(0).sendKeys(userName);
        element.all(by.css('input.form-control')).get(1).sendKeys(password);
    }

    submitLoginForm() {
        return element.all(by.buttonText('LOG IN')).click();
    }

    getErrorMessage() {
        let errorMessage = element(by.css('div[style="color:#a94442"]')).getText();
        return errorMessage.then(message => {
            return message.replace(/\n/,'').replace(/LOG\ IN$/,'');
        })
    }

    getLocation() {
        return browser.getCurrentUrl().then(url => {
            return url;
        });
    }
}
