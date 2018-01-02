/// <reference path="../../matchers/custom-matchers.d.ts"/>
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

import {customMatchers} from '../../matchers/custom-matchers';
import {LoginPage} from '../../login/login.po';
import {AlertFacetsPage} from './alert-filters.po';
import {loadTestData, deleteTestData} from '../../utils/e2e_util';

describe('metron-alerts facets', function() {
  let page: AlertFacetsPage;
  let loginPage: LoginPage;

  beforeAll(() => {
    loadTestData();
    loginPage = new LoginPage();
    loginPage.login();
  });

  afterAll(() => {
    loginPage.logout();
    deleteTestData();
  });

  beforeEach(() => {
    page = new AlertFacetsPage();
    jasmine.addMatchers(customMatchers);
  });

  it('should display facets data', () => {
    let facetValues = [ 'enrichm...:country 3', 'host 9', 'ip_dst_addr 8', 'ip_src_addr 2', 'source:type 1' ];

    page.navgateToAlertList();
    expect(page.getFacetsTitle()).toEqualBcoz(['Filters'], 'for Title as Filters');
    expect(page.getFacetsValues()).toEqual(facetValues, 'for Facet values');
  });

  it('should expand all facets', () => {
    expect(page.getFacetState(0)).toEqualBcoz('collapse', 'for first facet');
    expect(page.getFacetState(1)).toEqualBcoz('collapse', 'for second facet');
    expect(page.getFacetState(2)).toEqualBcoz('collapse', 'for third facet');
    expect(page.getFacetState(3)).toEqualBcoz('collapse', 'for fourth facet');
    expect(page.getFacetState(4)).toEqualBcoz('collapse', 'for fifth facet');

    page.toggleFacetState(0);
    page.toggleFacetState(1);
    page.toggleFacetState(2);
    page.toggleFacetState(3);
    page.toggleFacetState(4);

    expect(page.getFacetState(0)).toEqualBcoz('collapse show', 'for first facet');
    expect(page.getFacetState(1)).toEqualBcoz('collapse show', 'for second facet');
    expect(page.getFacetState(2)).toEqualBcoz('collapse show', 'for third facet');
    expect(page.getFacetState(3)).toEqualBcoz('collapse show', 'for fourth facet');
    expect(page.getFacetState(4)).toEqualBcoz('collapse show', 'for fifth facet');
  });

  it('should have all facet  values', () => {
    let hostMap = {
        'comarks...rity.com': '9' ,
        '7oqnsnz...ysun.com': '44' ,
        'node1': '36' ,
        '62.75.195.236': '18' ,
        'runlove.us': '13' ,
        'ip-addr.es': '2' ,
        'ubb67.3...grams.in': '1' ,
        'r03afd2...grams.in': '3' ,
        'va872g....grams.in': '1'
    };

    expect(page.getFacetValues(0)).toEqualBcoz({ US: '22', RU: '44', FR: '25' }, 'for enrichment facet');
    expect(page.getFacetValues(1)).toEqual(hostMap, 'for host facet');
    expect(page.getFacetValues(4)).toEqual({ alerts_ui_e2e: '169' }, 'for source:type facet');
  });

  it('should collapse all facets', () => {
    expect(page.getFacetState(0)).toEqualBcoz('collapse show', 'for first facet');
    expect(page.getFacetState(1)).toEqualBcoz('collapse show', 'for second facet');
    expect(page.getFacetState(2)).toEqualBcoz('collapse show', 'for third facet');
    expect(page.getFacetState(3)).toEqualBcoz('collapse show', 'for fourth facet');
    expect(page.getFacetState(4)).toEqualBcoz('collapse show', 'for fifth facet');

    page.toggleFacetState(0);
    page.toggleFacetState(1);
    page.toggleFacetState(2);
    page.toggleFacetState(3);
    page.toggleFacetState(4);

    expect(page.getFacetState(0)).toEqualBcoz('collapse', 'for first facet');
    expect(page.getFacetState(1)).toEqualBcoz('collapse', 'for second facet');
    expect(page.getFacetState(2)).toEqualBcoz('collapse', 'for third facet');
    expect(page.getFacetState(3)).toEqualBcoz('collapse', 'for fourth facet');
    expect(page.getFacetState(4)).toEqualBcoz('collapse', 'for fifth facet');
  });
});

