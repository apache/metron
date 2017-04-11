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
 * See the License for the specific language governing permissions andx
 * limitations under the License.
 */

import { browser, element, by, protractor } from 'protractor/globals';
import { waitForElementPresence, waitForStalenessOf } from '../utils/e2e_util';
var Promise = require('bluebird');

export class SensorListPage {

    clickOnActionsAndWait(parserNames: string[], clickOnClassName: string, waitOnClassName: string) {
        let queueForClick = Promise.resolve();
        parserNames.forEach((name) => {
            queueForClick = queueForClick.then((result) => {
                return this.getIconButton(name, clickOnClassName).click();
            });
        });

        return queueForClick.then(() => {
            let promiseArray = [];
            parserNames.map(name => {
                promiseArray.push(this.waitForElement(this.getIconButton(name, waitOnClassName)));
            });

            return protractor.promise.all(promiseArray).then(args => {
                return args;
            });
        });
    }

    clickOnDropdownAndWait(parserNames: string[], dropDownLinkName: string, waitOnClassName: string) {
        return protractor.promise.all([this.toggleSelectAll(), this.toggleDropdown()]).then(() => {

            return element(by.css('span[data-action=\"'+ dropDownLinkName +'\"]')).click().then(() => {
                let promiseArray = [];
                parserNames.map(name => {
                    promiseArray.push(this.waitForElement(this.getIconButton(name, waitOnClassName)));
                });

                return protractor.promise.all(promiseArray).then(args => {
                    return this.toggleSelectAll().then(() => {
                        return args;
                    })
                });
            });
        });
    }

    closePane(className: string ='.close-button') {
        return this.waitForElement(element(by.css(className))).then(() => {
            return element(by.css(className)).click();
        });
    }

    disableParsers(names: string[]) {
        return this.clickOnActionsAndWait(names, 'i.fa-ban', 'i.fa-check-circle-o');
    }

    disableParsersFromDropdown(names: string[]) {
        return this.clickOnDropdownAndWait(names, 'Disable', 'i.fa-check-circle-o');
    }

    deleteParser(name: string) {
        return this.getIconButton(name, '.fa-trash-o').click().then(() => {
            browser.sleep(1000);
            return element(by.css('.metron-dialog .btn-primary')).click().then(() => {
                browser.sleep(1000);
                return element(by.css('.alert .close')).click().then(() => {
                    return waitForStalenessOf(element(by.cssContainingText('td', name))).then(() =>{
                        return true;
                    });
                })
            });
        });
    }

    enableParsers(names: string[]) {
        return this.clickOnActionsAndWait(names, 'i.fa-check-circle-o', 'i.fa-ban');
    }

    enableParsersFromDropdown(names: string[]) {
        return this.clickOnDropdownAndWait(names, 'Enable', 'i.fa-ban');
    }

    getActions(name: string) {
        return element.all(by.css('table>tbody>tr')).filter(row => {
            return row.all(by.tagName('td')).get(0).getText().then(pName => {
                return pName === name;
            })
        }).get(0).all(by.tagName('i')).map(icon => {
            return icon.getAttribute('class').then(classNames => {
                let className = classNames.replace('fa ', '').replace('fa-lg', '').replace('fa-spin  fa-fw', '').trim();
                return {classNames: className, displayed: icon.isDisplayed()};
            });
        });
    }

    getAddButton() {
        return element(by.css('.metron-add-button.hexa-button .fa-plus')).isPresent();
    }

    getColumnValues(colId: number) {
        return element.all(by.css('table tbody tr')).map(function(elm) {
            return elm.all(by.css('td')).get(colId).getText();
        });
    }

    getDropdownActionState() {
        return protractor.promise.all([
            element.all(by.css('.dropdown.open .dropdown-menu span:not(.disabled)')).count(),
            element.all(by.css('.dropdown.open .dropdown-menu span.disabled')).count(),
            element.all(by.css('.dropdown-menu')).isDisplayed()
        ]).then(args => {
            return  {
                enabled: args[0],
                disabled: args[1],
                displayed: args[2][0],
            }
        });
    }

    getIconButton(name: string, className: string) {
        return element.all(by.css('table>tbody>tr')).filter(row => {
            return row.all(by.tagName('td')).get(0).getText().then(pName => {
                return pName === name;
            })
        }).get(0).element(by.css(className));
    }

    getParserCount() {
        browser.waitForAngular();
        return element.all(by.css('table>tbody>tr')).count();
    }

    getRow(name: string) {
        return element.all(by.css('table>tbody>tr')).filter(row => {
            return row.all(by.tagName('td')).get(0).getText().then(pName => {
                return pName === name;
            })
        }).get(0);
    }

    getSelectedRowCount() {
        return element.all(by.css('tr.active')).count();
    }

    getSortOrder(name: string) {
        return element(by.linkText(name)).element(by.tagName('i')).getAttribute('class');
    }

    getTableColumnNames() {
        return element.all(by.css('table th a')).map(function(elm) {
            return elm.getText();
        });
    }

    getTitle() {
        return element(by.css('.metron-title')).getText();
    }

    load() {
        return browser.get('/sensors');
    }

    openDetailsPane(name: string) {
        return this.getRow(name).click().then(() =>{
            return browser.getCurrentUrl();
        });
    }

    openEditPane(name: string) {
        let row = element(by.cssContainingText('td', name));
        return waitForElementPresence(row).then(() => {
            return this.getIconButton(name, '.fa-pencil').click().then(() =>{
                return browser.getCurrentUrl();
            });
        })
    }

    openEditPaneAndClose(name: string) {
        return this.getIconButton(name, '.fa-pencil').click().then(() =>{
            let url = browser.getCurrentUrl();
            browser.sleep(500);
            return this.closePane('.main.close-button').then(() => {
                return url;
            });
        });
    }

    startParsers(names: string[]) {
        return this.clickOnActionsAndWait(names, 'i.fa-play', 'i.fa-stop');
    }

    startParsersFromDropdown(names: string[]) {
        return this.clickOnDropdownAndWait(names, 'Start', 'i.fa-stop');
    }

    stopParsers(names: string[]) {
        return this.clickOnActionsAndWait(names, 'i.fa-stop', 'i.fa-play');
    }

    stopParsersFromDropdown(names: string[]) {
        return this.clickOnDropdownAndWait(names, 'Stop', 'i.fa-play');
    }

    toggleDropdown() {
        return element.all(by.id('dropdownMenu1')).click();
    }

    toggleRowSelect(name: string) {
        element.all(by.css('label[for=\"'+name+'\"]')).click();
    }

    toggleSelectAll() {
        return element.all(by.css('label[for="select-deselect-all"]')).click();
    }

    toggleSort(name: string) {
        element.all(by.linkText(name)).click();
    }

    waitForElement ( _element ) {
        var EC = protractor.ExpectedConditions;
        return browser.wait(EC.visibilityOf(_element));
    };
}
