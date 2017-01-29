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
import { SensorListPage } from './page-objects/sensor-list.po';
import {LoginPage} from './page-objects/login.po';

describe('Sensor List', function() {
    let page: SensorListPage = new SensorListPage();
    let loginPage = new LoginPage();

    beforeAll(() => {
        loginPage.login();
    });

    afterAll(() => {
        loginPage.logout();
    });

    it('should have all the default parsers', () => {
      expect(page.getParserCount()).toEqual(7);
    });

    it('should have all the table headers', () => {
        expect(page.getTableColumnNames()).toEqual([ 'Name', 'Parser', 'Status', 'Latency', 'Throughput', 'Last Updated', 'Last Editor' ]);
    });
});
