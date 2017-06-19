/// <reference path="../../matchers/custom-matchers.d.ts"/>
import { customMatchers } from  '../../matchers/custom-matchers';
import {MetronAlertsPage} from '../alerts-list.po';

describe('metron-alerts Search', function() {
  let page: MetronAlertsPage;

  beforeEach(() => {
    page = new MetronAlertsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should display all the default values for saved searches', () => {
    page.clearLocalStorage();
    page.navigateTo();

    page.clickSavedSearch();
    expect(page.getSavedSearchTitle()).toEqualBcoz('Searches', 'for saved searches title');
    expect(page.getRecentSearchOptions()).toEqual({ 'Recent Searches': [ 'No Recent Searches yet' ] }, 'for recent search options');
    expect(page.getSavedSearchOptions()).toEqual({ 'Saved Searches': [ 'No Saved Searches yet' ] }, 'for saved search options');
    page.clickCloseSavedSearch();

  });

  it('should have all save search controls and they save search should be working', () => {
    page.saveSearch('e2e-1');
    page.clickSavedSearch();
    expect(page.getSavedSearchOptions()).toEqual({ 'Saved Searches': [ 'e2e-1' ] }, 'for saved search options e2e-1');
    page.clickCloseSavedSearch();
  });

  it('should populate search items when selected on table', () => {
    page.clickTableText('US');
    expect(page.getSearchText()).toEqual('enrichments:geo:ip_dst_addr:country:US', 'for search text ip_dst_addr_country US');
    page.clickClearSearch();
    expect(page.getSearchText()).toEqual('*', 'for clear search');
  });

  it('should delete search items from search box', () => {
    page.clickTableText('US');
    expect(page.getSearchText()).toEqual('enrichments:geo:ip_dst_addr:country:US', 'for search text ip_dst_addr_country US');
    page.clickRemoveSearchChip();
    expect(page.getSearchText()).toEqual('*', 'for search chip remove');
  });

  it('manually entering search queries to search box and pressing enter key should search', () => {
    page.setSearchText('enrichments:geo:ip_dst_addr:country:US');
    expect(page.getPaginationText()).toEqualBcoz('1 - 22 of 22', 'for pagination text with search text enrichments:geo:ip_dst_addr:country:US');
    page.setSearchText('enrichments:geo:ip_dst_addr:country:RU');
    expect(page.getPaginationText()).toEqualBcoz('1 - 25 of 44', 'for pagination text with search text enrichments:geo:ip_dst_addr:country:RU as text');
  });

});
