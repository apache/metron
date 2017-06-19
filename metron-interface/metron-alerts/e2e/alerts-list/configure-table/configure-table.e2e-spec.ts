/// <reference path="../../matchers/custom-matchers.d.ts"/>
import { MetronAlertsPage } from '../alerts-list.po';
import {customMatchers} from '../../matchers/custom-matchers';

describe('metron-alerts configure table', function() {
  let page: MetronAlertsPage;
  let colNamesColumnConfig = [ 'score', '_id', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
    'ip_dst_addr', 'host', 'alert_status' ];

  beforeEach(() => {
    page = new MetronAlertsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should select columns from table configuration', () => {
    let newColNamesColumnConfig = [ 'score', 'timestamp', 'source:type', 'ip_src_addr', 'enrichments:geo:ip_dst_addr:country',
      'ip_dst_addr', 'host', 'alert_status', 'guid' ];

    page.clearLocalStorage();
    page.navigateTo();

    page.clickConfigureTable();
    expect(page.getSelectedColumnNames()).toEqual(colNamesColumnConfig, 'for default selected column names');
    page.toggleSelectCol('_id');
    page.toggleSelectCol('guid', 'method');
    expect(page.getSelectedColumnNames()).toEqual(newColNamesColumnConfig, 'for guid added to selected column names');
    page.saveConfigureColumns();

  });

});
