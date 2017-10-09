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

import {browser, element, by, protractor} from 'protractor';

export class MetronAlertsPage {
  navigateTo() {
    browser.waitForAngularEnabled(false);
    return browser.get('/alerts-list');
  }

  clearLocalStorage() {
    browser.executeScript('window.localStorage.clear();');
  }

  isMetronLogoPresent() {
    return element(by.css('img[src="../assets/images/logo.png"]')).isPresent();
  }

  isSavedSearchButtonPresent() {
    return element(by.buttonText('Searches')).isPresent();
  }

  isClearSearchPresent() {
    return element(by.css('.btn-search-clear')).isPresent();
  }

  isSearchButtonPresent() {
    return element(by.css('.btn-search-clear')).isPresent();
  }

  isSaveSearchButtonPresent() {
    return element(by.css('.save-button')).isPresent();
  }

  isTableSettingsButtonPresent() {
    return element(by.css('.btn.settings')).isPresent();
  }

  isPausePlayRefreshButtonPresent() {
    return element(by.css('.btn.pause-play')).isPresent();
  }

  isActionsButtonPresent() {
    return element.all(by.buttonText('ACTIONS')).isPresent();
  }

  isConfigureTableColumnsPresent() {
    return element(by.css('.fa.fa-cog.configure-table-icon')).isPresent();
  }

  getAlertTableTitle() {
    return element(by.css('.col-form-label-lg')).getText();
  }

  clickActionDropdown() {
    return element(by.buttonText('ACTIONS')).click();
  }

  clickActionDropdownOption(option: string) {
    this.clickActionDropdown().then(() => {
      element(by.cssContainingText('.dropdown-menu span', option)).click();
    });
  }

  getActionDropdownItems() {
    return this.clickActionDropdown().then(() => element.all(by.css('.dropdown-menu .dropdown-item.disabled')).getText());
  }

  getTableColumnNames() {
    return element.all(by.css('app-alerts-list .table th')).getText();
  }

  getPaginationText() {
    return element(by.css('metron-table-pagination span')).getText();
  }

  isChevronLeftEnabled() {
    return element(by.css('metron-table-pagination .fa.fa-chevron-left')).getAttribute('class').then((classes) => {
      return classes.split(' ').indexOf('disabled') === -1;
    });
  }

  isChevronRightEnabled() {
    return element(by.css('metron-table-pagination .fa.fa-chevron-right')).getAttribute('class').then((classes) => {
      return classes.split(' ').indexOf('disabled') === -1;
    });
  }

  clickChevronRight(times = 1) {
    for (let i = 0; i < times; i++) {
      element(by.css('metron-table-pagination .fa.fa-chevron-right')).click();
    }
  }

  clickChevronLeft(times = 1) {
    for (let i = 0; i < times; i++) {
      element(by.css('metron-table-pagination .fa.fa-chevron-left')).click();
    }
  }

  clickSettings() {
    return element(by.css('.btn.settings')).click();
  }

  getSettingsLabels() {
    return element.all(by.css('form label:not(.switch)')).getText();
  }

  getRefreshRateOptions() {
    return element.all(by.css('.preset-row.refresh-interval .preset-cell')).getText();
  }

  getRefreshRateSelectedOption() {
    return element.all(by.css('.preset-row.refresh-interval .preset-cell.is-active')).getText();
  }

  getPageSizeOptions() {
    return element.all(by.css('.preset-row.page-size .preset-cell')).getText();
  }

  getPageSizeSelectedOption() {
    return element.all(by.css('.preset-row.page-size .preset-cell.is-active')).getText();
  }

  clickRefreshInterval(intervalText: string) {
    return element(by.cssContainingText('.refresh-interval .preset-cell', intervalText)).click();
  }

  clickPageSize(pageSizeText: string) {
    return element.all(by.cssContainingText('.page-size .preset-cell', pageSizeText)).first().click();
  }

  clickConfigureTable() {
    element(by.css('app-alerts-list .fa.fa-cog.configure-table-icon')).click();
    browser.sleep(1000);
  }

  clickCloseSavedSearch() {
    element(by.css('app-saved-searches .close-button')).click();
  }

  clickSavedSearch() {
    element(by.buttonText('Searches')).click();
    browser.sleep(1000);
  }

  clickPlayPause() {
    element(by.css('.btn.pause-play')).click();
  }

  clickTableText(name: string) {
    element.all(by.linkText(name)).get(0).click();
  }

  clickClearSearch() {
    element(by.css('.btn-search-clear')).click();
  }

  getSavedSearchTitle() {
    return element(by.css('app-saved-searches .form-title')).getText();
  }

  getPlayPauseState() {
    return element(by.css('.btn.pause-play i')).getAttribute('class');
  }

  getSearchText() {
    return element(by.css('.ace_line')).getText();
  }

  getRecentSearchOptions() {
    browser.sleep(1000);
    let map = {};
    let recentSearches = element.all(by.css('app-saved-searches metron-collapse')).get(0);
    return recentSearches.all(by.css('a')).getText().then(title => {
       return recentSearches.all(by.css('.collapse.show')).getText().then(values => {
         map[title] = values;
        return map;
      });
    });
  }

  getSavedSearchOptions() {
    browser.sleep(1000);
    let map = {};
    let recentSearches = element.all(by.css('app-saved-searches metron-collapse')).get(1);
    return recentSearches.all(by.css('a')).getText().then(title => {
      return recentSearches.all(by.css('.collapse.show')).getText().then(values => {
        map[title] = values;
        return map;
      });
    });
  }

  getSelectedColumnNames() {
    return element.all(by.css('app-configure-table input[type="checkbox"]:checked')).map(ele => {
      return ele.getAttribute('id').then(id => id.replace(/select-deselect-/, ''));
    });
  }

  toggleSelectCol(name: string, scrollTo = '') {
    scrollTo = scrollTo === '' ? name : scrollTo;
    let ele = element(by.css('app-configure-table label[for="select-deselect-' + name + '"]'));
    let scrollToEle = element(by.css('app-configure-table label[for="select-deselect-' + scrollTo + '"]'));
    browser.actions().mouseMove(scrollToEle).perform().then(() => ele.click());
  }

  saveSearch(name: string) {
     return element(by.css('.save-button')).click().then(() => element(by.css('app-save-search #name')).sendKeys(name))
      .then(() => element(by.css('app-save-search button[type="submit"]')).click());
  }

  saveConfigureColumns() {
    element(by.css('app-configure-table')).element(by.buttonText('SAVE')).click();
  }

  clickRemoveSearchChip() {
    let aceLine = element.all(by.css('.ace_keyword')).get(0);
    browser.actions().mouseMove(aceLine).perform().then(() => {
      this.waitForElementPresence(element(by.css('.ace_value i'))).then(() => {
        element.all(by.css('.ace_value i')).get(0).click();
      });
    });
  }

  setSearchText(search: string) {
    this.clickClearSearch();
    element(by.css('app-alerts-list .ace_text-input')).sendKeys(protractor.Key.BACK_SPACE);
    element(by.css('app-alerts-list .ace_text-input')).sendKeys(search);
    element(by.css('app-alerts-list .ace_text-input')).sendKeys(protractor.Key.ENTER);
    browser.sleep(2000);
  }

  waitForElementPresence (element ) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.presenceOf(element));
  }

  waitForTextChange(element, previousText) {
    let EC = protractor.ExpectedConditions;
    return browser.wait(EC.not(EC.textToBePresentInElement(element, previousText)));
  }

  toggleAlertInList(index: number) {
    let selector = by.css('app-alerts-list tbody tr label');
    let checkbox = element.all(selector).get(index);
    this.waitForElementPresence(checkbox).then(() => {
      browser.actions().mouseMove(checkbox).perform().then(() => {
        checkbox.click();
      });
    });
  }

  getAlertStatus(rowIndex: number, previousText) {
    let row = element.all(by.css('app-alerts-list tbody tr')).get(rowIndex);
    let column = row.all(by.css('td a')).get(8);
    return this.waitForTextChange(column, previousText).then(() => {
      return column.getText();
    });
  }
}
