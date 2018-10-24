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

import {browser, element, by, protractor, ElementArrayFinder} from 'protractor';
import * as moment from 'moment/moment';
import {
  waitForElementVisibility, waitForElementPresence, waitForElementInVisibility,
  waitForText, waitForCssClass, waitForCssClassNotToBePresent, waitForTextChange,
  waitForStalenessOf, reduce_for_get_all, waitForElementCountGreaterThan, scrollIntoView,
  waitForNonEmptyText, catchNoSuchElementError
} from '../utils/e2e_util';

export class MetronAlertsPage {
  private EC = protractor.ExpectedConditions;

  navigateTo() {
    return browser.waitForAngularEnabled(false)
    .then(() => browser.get('/alerts-list'));
  }

  clearLocalStorage() {
    return browser.executeScript('window.localStorage.clear();');
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
    let actionsDropDown = element(by.buttonText('ACTIONS'));
    return scrollIntoView(actionsDropDown, true)
    .then(() => actionsDropDown.click());
  }

  clickActionDropdownOption(option: string) {
    return this.clickActionDropdown()
    .then(() => element(by.cssContainingText('.dropdown-menu span', option)).click())
    .then(() => waitForCssClassNotToBePresent(element(by.css('#table-actions')), 'show'));
  }

  getActionDropdownItems() {
    return this.clickActionDropdown()
    .then(() => element.all(by.css('.dropdown-menu .dropdown-item.disabled')).reduce(reduce_for_get_all(), []));
  }

  getTableColumnNames() {
    return element.all(by.css('app-alerts-list .table th')).reduce(reduce_for_get_all(), []);
  }

  getChangedPaginationText(previousText: string) {
    let paginationElement = element(by.css('metron-table-pagination span'));
    return waitForTextChange(paginationElement, previousText)
    .then(() => paginationElement.getText());
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

  clickChevronRight() {
    let paginationEle = element(by.css('metron-table-pagination .fa.fa-chevron-right'));
    return waitForElementVisibility(paginationEle)
    .then(() => browser.actions().mouseMove(paginationEle).perform())
    .then(() => paginationEle.click())
  }

  clickChevronLeft(times = 1) {
    let paginationEle = element(by.css('metron-table-pagination .fa.fa-chevron-left'));
    return waitForElementVisibility(paginationEle)
    .then(() => browser.actions().mouseMove(paginationEle).perform())
    .then(() => paginationEle.click());
  }

  clickSettings() {
    let settingsIcon = element(by.css('.btn.settings'));
    return waitForElementVisibility(settingsIcon).then(() => settingsIcon.click());
  }

  getSettingsLabels() {
    return element.all(by.css('app-configure-rows  form label:not(.switch)')).reduce(reduce_for_get_all() ,[]);
  }

  getRefreshRateOptions() {
    return element.all(by.css('.preset-row.refresh-interval .preset-cell')).reduce(reduce_for_get_all() ,[]);
  }

  getRefreshRateSelectedOption() {
    return element.all(by.css('.preset-row.refresh-interval .preset-cell.is-active')).reduce(reduce_for_get_all() ,[]);
  }

  getPageSizeOptions() {
    return element.all(by.css('.preset-row.page-size .preset-cell')).reduce(reduce_for_get_all() ,[]);
  }

  getPageSizeSelectedOption() {
    return element.all(by.css('.preset-row.page-size .preset-cell.is-active')).reduce(reduce_for_get_all() ,[]);
  }

  clickRefreshInterval(intervalText: string) {
    return element(by.cssContainingText('.refresh-interval .preset-cell', intervalText)).click();
  }

  clickPageSize(pageSizeText: string) {
    return element.all(by.cssContainingText('.page-size .preset-cell', pageSizeText)).first().click();
  }

  clickConfigureTable() {
    let gearIcon = element(by.css('app-alerts-list .fa.fa-cog.configure-table-icon'));
    return waitForElementVisibility(gearIcon)
    .then(() => scrollIntoView(gearIcon, true))
    .then(() => gearIcon.click())
    .then(() => waitForElementCountGreaterThan('app-configure-table tr', 282));
  }

  clickCloseSavedSearch() {
    return element(by.css('app-saved-searches .close-button')).click()
    .then(() => waitForStalenessOf(element(by.css('app-saved-searches'))));
  }

  clickSavedSearch() {
    return element(by.buttonText('Searches')).click()
    .then(() => waitForElementVisibility(element(by.css('app-saved-searches'))))
    .then(() => browser.sleep(1000));
  }

  clickPlayPause(waitForPreviousClass: string) {
    let playPauseButton = element(by.css('.btn.pause-play'));
    return browser.actions().mouseMove(playPauseButton).perform()
          .then(() => playPauseButton.click())
          .then(() => waitForCssClass(element(by.css('.btn.pause-play i')), waitForPreviousClass));
  }

  clickTableTextAndGetSearchText(name: string, textToWaitFor: string) {
    browser.sleep(500);
    return waitForElementVisibility(element.all(by.cssContainingText('table tr td a', name)).get(0))
          .then(() => element.all(by.cssContainingText('table tr td a', name)).get(0).click())
          .then(() => waitForText('.ace_line', textToWaitFor))
          .then(() => element(by.css('.ace_line')).getText());
  }

  private clickTableText(name: string) {
    waitForElementVisibility(element.all(by.linkText(name))).then(() => element.all(by.linkText(name)).get(0).click());
  }

  clickClearSearch(alertCount = '169') {
    return element(by.css('.btn-search-clear')).click()
    .then(() => waitForText('.ace_line', '*'))
    .then(() => waitForText('.col-form-label-lg', `Alerts (${alertCount})`));
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

  isCommentIconPresentInTable() {
    return element.all(by.css('app-table-view .fa.fa-comments-o')).count();
  }

  getRecentSearchOptions() {
    return element.all(by.css('[data-name="recent-searches"] li')).getText();
  }

  getDefaultRecentSearchValue() {
    return element.all(by.css('[data-name="recent-searches"] i')).getText();
  }

  getSavedSearchOptions() {
    return element.all(by.css('[data-name="saved-searches"] li')).getText();
  }

  getDefaultSavedSearchValue() {
    return element.all(by.css('[data-name="saved-searches"] i')).getText();
  }

  getSelectedColumnNames() {
    return element.all(by.css('app-configure-table input[type="checkbox"]:checked')).reduce((acc, ele) => {
      return ele.getAttribute('id').then(id => {
        acc.push(id.replace(/select-deselect-/, ''));
        return acc;
      })
    }, []);
  }

  toggleSelectCol(name: string) {
    const selector = `app-configure-table label[for=\"select-deselect-${name}\"]`;
    const ele = element(by.css(selector));
    return waitForElementVisibility(ele)
          .then(() => scrollIntoView(ele, true))
          .then(() => (element(by.css(selector)).click()))
          .catch(err => console.log(err));
  }

  saveSearch(name: string) {
     return element(by.css('.save-button')).click().then(() => element(by.css('app-save-search #name')).sendKeys(name))
      .then(() => element(by.css('app-save-search button[type="submit"]')).click());
  }

  saveConfigureColumns() {
    return element(by.css('app-configure-table')).element(by.buttonText('SAVE')).click();
  }

  clickRemoveSearchChipAndGetSearchText(expectedSearchText: string) {
    return this.clickRemoveSearchChip()
    .then(() => waitForText('.ace_line', expectedSearchText))
    .then(() => element(by.css('.ace_line')).getText())
  }

  private clickRemoveSearchChip(): any {
    let aceLine = element.all(by.css('.ace_keyword')).get(0);
    /* - Focus on the search text box by sending a empty string
       - move the mouse to the text in search bos so that delete buttons become visible
       - wait for delete buttons become visible
       - click on delete button
    */
    return element(by.css('app-alerts-list .ace_text-input')).sendKeys('')
    .then(() => browser.actions().mouseMove(aceLine).perform())
    .then(() => this.waitForElementPresence(element(by.css('.ace_value i'))))
    .then(() => element.all(by.css('.ace_value i')).get(0).click());
  }

  setSearchText(search: string,  alertCount = '169') {
    // It is required to click on a visible element of Ace editor in order to
    // bring focus to the input: https://stackoverflow.com/questions/37809915/element-not-visible-error-not-able-to-click-an-element
    let EC = protractor.ExpectedConditions;
    let aceInput = element(by.css('app-alerts-list .ace_text-input'));
    let aceScroller = element(by.css('app-alerts-list .ace_scroller'));
    return this.clickClearSearch(alertCount)
    .then(() => browser.wait(EC.presenceOf(aceScroller)))
    .then(() => aceScroller.click())
    .then(() => aceInput.sendKeys(protractor.Key.BACK_SPACE))
    .then(() => aceInput.sendKeys(search))
    .then(() => aceInput.sendKeys(protractor.Key.ENTER))
    .then(() => browser.sleep(10000));
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
    return this.waitForElementPresence(checkbox)
    .then(() => scrollIntoView(checkbox, true))
    .then(() => checkbox.click());
  }

  getAlertStatus(rowIndex: number, previousText: string, colIndex = 8) {
    let row = element.all(by.css('app-alerts-list tbody tr')).get(rowIndex);
    let column = row.all(by.css('td a')).get(colIndex);
    return this.waitForTextChange(column, previousText).then(() => column.getText());
  }

  waitForMetaAlert(expectedCount) {
    let title = element(by.css('.col-form-label-lg'));
    function waitForMetaAlert$()
    {
      return function () {
        return browser.sleep(2000)
        .then(() => element(by.css('button[data-name="search"]')).click())
        .then(() => waitForTextChange(title, `Alerts (169)`))
        .then(() => title.getText())
        .then((text) => text === `Alerts (${expectedCount})`)
        .catch(catchNoSuchElementError())
      }
    }
    return browser.wait(waitForMetaAlert$()).catch(catchNoSuchElementError());
  }

  isDateSeettingDisabled() {
    return element.all(by.css('app-time-range button.btn.btn-search[disabled=""]')).count().then((count) => { return (count === 1); });
  }

  clickDateSettings() {
    return scrollIntoView(element(by.css('app-time-range button.btn-search')), true)
    .then(() => element(by.css('app-time-range button.btn-search')).click())
    .then(() => waitForCssClass(element(by.css('app-time-range #time-range')), 'show'));
  }

  hideDateSettings() {
    return element(by.css('app-time-range button.btn-search')).click()
    .then(() => waitForCssClassNotToBePresent(element(by.css('app-time-range #time-range')), 'show'))
    .then(() => waitForElementInVisibility(element(by.css('app-time-range #time-range'))));
  }

  getTimeRangeTitles() {
    return element.all(by.css('app-time-range .title')).reduce(reduce_for_get_all(), []);
  }

  getQuickTimeRanges() {
    return element.all(by.css('app-time-range .quick-ranges span')).reduce(reduce_for_get_all(), []);
  }

  getValueForManualTimeRange() {
    return element.all(by.css('app-time-range input.form-control')).reduce((acc, ele) => {
      return ele.getAttribute('value').then(value => {
        acc.push(value);
        return acc;
      });
    }, []);
  }

  isManulaTimeRangeApplyButtonPresent() {
    return element.all(by.css('app-time-range')).all(by.buttonText('APPLY')).count().then(count => count === 1);
  }

  waitForTextAndSubTextInTimeRange(currentTimeRangeVal) {
    return waitForTextChange(element(by.css('app-time-range .time-range-value')), currentTimeRangeVal[1])
    .then(() => waitForTextChange(element(by.css('app-time-range .time-range-text')), currentTimeRangeVal[0]))
  }

  async selectQuickTimeRangeAndGetTimeRangeAndTimeText(quickRange: string) {
    let currentTimeRangeVal: any = [];
    let timeRangeVal = '', timeRangeText = '';
    await element(by.css('app-time-range .time-range-value')).getText().then(text => timeRangeVal = text.trim());
    await element(by.css('app-time-range .time-range-text')).getText().then(text => timeRangeText = text.trim());

    await this.selectQuickTimeRange(quickRange);
    await waitForCssClassNotToBePresent(element(by.css('app-time-range #time-range')), 'show');
    await waitForTextChange(element(by.css('app-time-range .time-range-value')), timeRangeVal);
    await waitForTextChange(element(by.css('app-time-range .time-range-text')), timeRangeText);

    return this.getTimeRangeButtonAndSubText();
  }

  selectQuickTimeRangeAndGetTimeRangeText(quickRange: string) {
    return this.selectQuickTimeRange(quickRange)
    .then(() => waitForElementInVisibility(element(by.css('#time-range'))))
    .then(() => browser.wait(this.EC.textToBePresentInElement(element(by.css('app-time-range .time-range-text')), quickRange)))
    .then(() => element.all(by.css('app-time-range button span')).get(0).getText());
  }

  selectQuickTimeRange(quickRange: string) {
    return element(by.id(quickRange.toLowerCase().replace(/ /g,'-'))).click();
  }

  getTimeRangeButtonText() {
    return element(by.css('app-time-range .time-range-text')).getText();
  }

  setDate(index: number, year: string, month: string, day: string, hour: string, min: string, sec: string) {
    return element.all(by.css('app-time-range .calendar')).get(index).click()
    .then(() => element.all(by.css('.pika-select.pika-select-hour')).get(index).click())
    .then(() => element.all(by.css('.pika-select.pika-select-hour')).get(index).element(by.cssContainingText('option', hour)).click())
    .then(() => element.all(by.css('.pika-select.pika-select-minute')).get(index).click())
    .then(() => element.all(by.css('.pika-select.pika-select-minute')).get(index).element(by.cssContainingText('option', min)).click())
    .then(() => element.all(by.css('.pika-select.pika-select-second')).get(index).click())
    .then(() => element.all(by.css('.pika-select.pika-select-second')).get(index).element(by.cssContainingText('option', sec)).click())
    .then(() => element.all(by.css('.pika-select.pika-select-year')).get(index).click())
    .then(() => element.all(by.css('.pika-select.pika-select-year')).get(index).element(by.cssContainingText('option', year)).click())
    .then(() => element.all(by.css('.pika-select.pika-select-month')).get(index).click())
    .then(() => element.all(by.css('.pika-select.pika-select-month')).get(index).element(by.cssContainingText('option', month)).click())
    .then(() => element.all(by.css('.pika-table')).get(index).element(by.buttonText(day)).click())
    .then(() => waitForElementInVisibility(element.all(by.css('.pika-single')).get(index)));
  }

  selectTimeRangeApplyButton() {
    return element(by.css('app-time-range')).element(by.buttonText('APPLY')).click();
  }

  getChangesAlertTableTitle(previousText: string) {
    let title = element(by.css('.col-form-label-lg'));
    return waitForTextChange(title, previousText)
    .then(() => title.getText());
  }

  getAlertStatusById(id: string) {
    return element(by.css('a[title="' + id + '"]'))
          .element(by.xpath('../..')).all(by.css('td a')).get(8).getText();
  }

  getCellValue(rowIndex: number, colIndex: number, previousText: string) {
    let cellElement = element.all(by.css('table tbody tr')).get(rowIndex).all(by.css('td')).get(colIndex);
    return this.waitForTextChange(cellElement, previousText).then(() => cellElement.getText());
  }

  expandMetaAlert(rowIndex: number) {
    return element.all(by.css('table tbody tr')).get(rowIndex).element(by.css('.icon-cell.dropdown-cell')).click();
  }

  getHiddenRowCount() {
    return element.all(by.css('table tbody tr.d-none')).count();
  }

  getNonHiddenRowCount() {
    return element.all(by.css('table tbody tr:not(.d-none)')).count();
  }

  getAllRowsCount() {
    return element.all(by.css('table tbody tr')).count();
  }

  clickOnMetaAlertRow(rowIndex: number) {
    return element.all(by.css('table tbody tr')).get(rowIndex).all(by.css('td')).get(5).click()
    .then(() => browser.sleep(2000));
  }

  removeAlert(rowIndex: number) {
    return element.all(by.css('app-table-view .fa-chain-broken')).get(rowIndex).click();
  }

  loadSavedSearch(name: string) {
    return element.all(by.css('app-saved-searches metron-collapse')).get(1).element(by.css('li[title="'+ name +'"]')).click();
  }

  loadRecentSearch(name: string) {
    return element.all(by.css('app-saved-searches metron-collapse')).get(0).all(by.css('li')).get(2).click();
  }

  getTimeRangeButtonTextForNow() {
    return element.all(by.css('app-time-range button span')).reduce(reduce_for_get_all(), []);
  }

  async getTimeRangeButtonAndSubText() {
    let timeRangetext = '', timeRangeValue = '';

    await element(by.css('app-time-range .time-range-text')).getText().then(text => timeRangetext = text);
    await element(by.css('app-time-range .time-range-value')).getText().then(text => timeRangeValue = text);

    let retArr = [timeRangetext];
    let dateStr = timeRangeValue.split(' to ');
    let fromTime = moment.utc(dateStr[0], 'YYYY-MM-DD HH:mm:ss Z').unix() * 1000;
    let toTime = moment.utc(dateStr[1], 'YYYY-MM-DD HH:mm:ss Z').unix() * 1000;
    retArr.push((toTime - fromTime) + '');
    return retArr;
  }

  renameColumn(name: string, value: string) {
    return element(by.cssContainingText('app-configure-table span', name))
    .element(by.xpath('../..'))
    .element(by.css('.input')).sendKeys(value);
  }

  getTableCellValues(cellIndex: number, startRowIndex: number, endRowIndex: number): any {
    return element.all(by.css('table tbody tr td:nth-child(' + cellIndex + ')')).reduce(reduce_for_get_all(), [])
  }
}
