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
import { QueryBuilder, FilteringMode } from './query-builder';
import { Filter } from 'app/model/filter';
import { TIMESTAMP_FIELD_NAME } from '../../utils/constants';
import { Utils } from 'app/utils/utils';


describe('QueryBuilder', () => {
  let queryBuilder: QueryBuilder;

  beforeEach(() => {
    queryBuilder = new QueryBuilder();
  });

  it('should be able to handle multiple filters', () => {
    queryBuilder.setSearch('alert_status:RESOLVE AND ip_src_addr:0.0.0.0');

    expect(queryBuilder.searchRequest.query).toBe(
      '(alert_status:RESOLVE OR metron_alert.alert_status:RESOLVE) AND (ip_src_addr:0.0.0.0 OR metron_alert.ip_src_addr:0.0.0.0)'
      );
  });

  it('should be able to handle multiple EXCLUDING filters for the same field', () => {
    queryBuilder.setSearch('-alert_status:RESOLVE AND -alert_status:DISMISS');

    expect(queryBuilder.searchRequest.query).toBe(
      '-(alert_status:RESOLVE OR metron_alert.alert_status:RESOLVE) AND -(alert_status:DISMISS OR metron_alert.alert_status:DISMISS)'
      );
  });

  it('should be able to handle group multiple clauses to a single field, aka. field grouping', () => {
    queryBuilder.setSearch('alert_status:(RESOLVE OR DISMISS)');

    expect(queryBuilder.searchRequest.query).toBe(
      '(alert_status:(RESOLVE OR DISMISS) OR metron_alert.alert_status:(RESOLVE OR DISMISS))'
      );
  });

  it('should trim whitespace', () => {
    queryBuilder.setSearch(' alert_status:(RESOLVE OR DISMISS) ');

    expect(queryBuilder.searchRequest.query).toBe(
      '(alert_status:(RESOLVE OR DISMISS) OR metron_alert.alert_status:(RESOLVE OR DISMISS))'
      );
  });

  it('should remove wildcard', () => {
    queryBuilder.setSearch('* alert_status:(RESOLVE OR DISMISS)');

    expect(queryBuilder.searchRequest.query).toBe(
      '(alert_status:(RESOLVE OR DISMISS) OR metron_alert.alert_status:(RESOLVE OR DISMISS))'
      );
  });

  it('should properly parse excluding filters event with wildcard and whitespaces', () => {
    queryBuilder.setSearch('* -alert_status:(RESOLVE OR DISMISS)');

    expect(queryBuilder.searchRequest.query).toBe(
      '-(alert_status:(RESOLVE OR DISMISS) OR metron_alert.alert_status:(RESOLVE OR DISMISS))'
      );
  });

  it('should remove wildcard from an excluding filter', () => {
    queryBuilder.setSearch('* -alert_status:(RESOLVE OR DISMISS)');

    expect(queryBuilder.searchRequest.query).toBe(
      '-(alert_status:(RESOLVE OR DISMISS) OR metron_alert.alert_status:(RESOLVE OR DISMISS))'
      );
  });

  it('should allow only one timerange filter', () => {
    queryBuilder.addOrUpdateFilter(new Filter(TIMESTAMP_FIELD_NAME, '[1552863600000 TO 1552950000000]'));
    queryBuilder.addOrUpdateFilter(new Filter(TIMESTAMP_FIELD_NAME, '[1552863700000 TO 1552960000000]'));

    expect(queryBuilder.query).toBe('(timestamp:[1552863700000 TO 1552960000000] OR ' +
      'metron_alert.timestamp:[1552863700000 TO 1552960000000])');
  });

  it('should escape : chars in ElasticSearch field names', () => {
    queryBuilder.setSearch('source:type:bro');

    expect(queryBuilder.searchRequest.query).toBe('(source\\:type:bro OR metron_alert.source\\:type:bro)');
  });

  it('should escape ALL : chars in ElasticSearch field names', () => {
    queryBuilder.setSearch('enrichments:geo:ip_dst_addr:country:US');

    expect(queryBuilder.searchRequest.query).toBe('(enrichments\\:geo\\:ip_dst_addr\\:country:US ' +
      'OR metron_alert.enrichments\\:geo\\:ip_dst_addr\\:country:US)');
  });

  it('should not multiply escaping in field name', () => {
    queryBuilder.setSearch('source:type:bro');
    queryBuilder.setSearch('source:type:bro');
    queryBuilder.setSearch('source:type:bro');

    expect(queryBuilder.searchRequest.query).toBe('(source\\:type:bro OR metron_alert.source\\:type:bro)');
  });

  it('removeFilter should remove filter by reference', () => {
    const filter1 = new Filter(TIMESTAMP_FIELD_NAME, '[1552863600000 TO 1552950000000]');
    const filter2 = new Filter('fieldName', 'value');

    queryBuilder.addOrUpdateFilter(filter1);
    queryBuilder.addOrUpdateFilter(filter2);

    queryBuilder.removeFilter(filter1);

    expect(queryBuilder.filters.length).toBe(1);
    expect(queryBuilder.filters[0]).toBe(filter2);
  });

  it('removeFilterByField should remove filter having the passed field name', () => {
    const filter1 = new Filter('fruit', 'banana');
    const filter2 = new Filter('fruit', 'orange');
    const filter3 = new Filter('animal', 'horse');

    queryBuilder.addOrUpdateFilter(filter1);
    queryBuilder.addOrUpdateFilter(filter2);
    queryBuilder.addOrUpdateFilter(filter3);

    queryBuilder.removeFilterByField('fruit');

    expect(queryBuilder.filters.length).toBe(1);
    expect(queryBuilder.filters[0]).toBe(filter3);
  });

  describe('filter query builder modes', () => {
    it('should have a getter for filtering mode', () => {
      expect(typeof queryBuilder.getFilteringMode).toBe('function');
    });

    it('should have a setter for filtering mode', () => {
      expect(typeof queryBuilder.setFilteringMode).toBe('function');

      expect(queryBuilder.getFilteringMode()).toBe(FilteringMode.BUILDER);

      queryBuilder.setFilteringMode(FilteringMode.MANUAL);
      expect(queryBuilder.getFilteringMode()).toBe(FilteringMode.MANUAL);

      queryBuilder.setFilteringMode(FilteringMode.BUILDER);
      expect(queryBuilder.getFilteringMode()).toBe(FilteringMode.BUILDER);
    });

    it('filtering mode should be builder by default', () => {
      expect(queryBuilder.getFilteringMode()).toBe(FilteringMode.BUILDER);
    });

    it('should have a getter for manual query', () => {
      expect(typeof queryBuilder.getManualQuery).toBe('function');
    });

    it('should have a setter for manual query string', () => {
      expect(typeof queryBuilder.setManualQuery).toBe('function');

      queryBuilder.setManualQuery('test manual query');
      expect(queryBuilder.getManualQuery()).toBe('test manual query');

      queryBuilder.setManualQuery('another test manual query');
      expect(queryBuilder.getManualQuery()).toBe('another test manual query');
    });

    it('getManualQuery should return the built query string first', () => {
      const expected = '(timestamp:[1552863600000 TO 1552950000000] OR metron_alert.timestamp:[1552863600000 ' +
        'TO 1552950000000]) AND (animal:horse OR metron_alert.animal:horse)';

      queryBuilder.clearSearch();

      queryBuilder.addOrUpdateFilter(new Filter(TIMESTAMP_FIELD_NAME, '[1552863600000 TO 1552950000000]'));
      queryBuilder.addOrUpdateFilter(new Filter('animal', 'horse'));

      expect(queryBuilder.getManualQuery()).toBe(expected);

      queryBuilder.setManualQuery('test:query');

      expect(queryBuilder.getManualQuery()).toBe('test:query');
    });

    it('should use manual query string value in manual mode', () => {
      queryBuilder.addOrUpdateFilter(new Filter(TIMESTAMP_FIELD_NAME, '[1552863600000 TO 1552950000000]'));
      queryBuilder.addOrUpdateFilter(new Filter('animal', 'horse'));

      queryBuilder.setFilteringMode(FilteringMode.MANUAL);
      queryBuilder.setManualQuery('test:query');

      expect(queryBuilder.searchRequest.query).toBe('test:query');
    });

    it('should use built query string value in builder mode', () => {
      queryBuilder.addOrUpdateFilter(new Filter(TIMESTAMP_FIELD_NAME, '[1552863600000 TO 1552950000000]'));
      queryBuilder.addOrUpdateFilter(new Filter('animal', 'horse'));

      queryBuilder.setFilteringMode(FilteringMode.BUILDER);
      queryBuilder.setManualQuery('test:query');

      expect(queryBuilder.searchRequest.query).toBe(
        '(timestamp:[1552863600000 TO 1552950000000] OR metron_alert.timestamp:[1552863600000 ' +
        'TO 1552950000000]) AND (animal:horse OR metron_alert.animal:horse)'
        );
    });

    it('clearSearch should clear manual query value', () => {
      queryBuilder.setFilteringMode(FilteringMode.MANUAL);
      queryBuilder.setManualQuery('manual:test:query');

      expect(queryBuilder.getManualQuery()).toBe('manual:test:query');

      queryBuilder.clearSearch();

      expect(queryBuilder.getManualQuery()).toBe('*');
    });

  });

});
