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
import {LoginPage} from '../login/login.po';
import {SensorDetailsPage} from './sensor-config-readonly.po';

describe('Sensor Details View', function () {
    let page = new SensorDetailsPage();
    let loginPage = new LoginPage();

    beforeAll(() => {
        loginPage.login();
    });

    afterAll(() => {
        loginPage.logout();
    });

    it('should have squid attributes defined', (done) => {
        let parserNotRunnigExpected = ['',
            'PARSERS\nGrok',
            'LAST UPDATED\n-',
            'LAST EDITOR\n-',
            'STATE\n-',
            'ORIGINATOR\n-',
            'CREATION DATE\n-',
            ' ',
            'STORM\nStopped',
            'LATENCY\n-',
            'THROUGHPUT\n-',
            'EMITTED(10 MIN)\n-',
            'ACKED(10 MIN)\n-',
            ' ',
            'KAFKA\nNo Kafka Topic',
            'PARTITONS\n-',
            'REPLICATION FACTOR\n-',
            ''];
        let parserRunningExpected = ['',
            'PARSERS\nGrok',
            'LAST UPDATED\n-',
            'LAST EDITOR\n-',
            'STATE\nEnabled',
            'ORIGINATOR\n-',
            'CREATION DATE\n-',
            ' ',
            'STORM\nRunning',
            'LATENCY\n-',
            'THROUGHPUT\n-',
            'EMITTED(10 MIN)\n-',
            'ACKED(10 MIN)\n-',
            ' ',
            'KAFKA\nNo Kafka Topic',
            'PARTITONS\n-',
            'REPLICATION FACTOR\n-',
            ''];
        let parserDisabledExpected = ['',
            'PARSERS\nGrok',
            'LAST UPDATED\n-',
            'LAST EDITOR\n-',
            'STATE\nDisabled',
            'ORIGINATOR\n-',
            'CREATION DATE\n-',
            ' ',
            'STORM\nDisabled',
            'LATENCY\n-',
            'THROUGHPUT\n-',
            'EMITTED(10 MIN)\n-',
            'ACKED(10 MIN)\n-',
            ' ',
            'KAFKA\nNo Kafka Topic',
            'PARTITONS\n-',
            'REPLICATION FACTOR\n-',
            ''];
        expect(page.navigateTo('squid')).toEqual('http://localhost:4200/sensors(dialog:sensors-readonly/squid)');
        expect(page.getTitle()).toEqual("squid");
        expect(page.getParserConfig()).toEqual(parserNotRunnigExpected);
        expect(page.getButtons()).toEqual([ 'EDIT', 'START', 'Delete' ]);

        expect(page.startParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'STOP', 'DISABLE',  'Delete' ]);
        expect(page.getParserConfig()).toEqual(parserRunningExpected);

        expect(page.disableParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'STOP', 'ENABLE',  'Delete' ]);
        expect(page.getParserConfig()).toEqual(parserDisabledExpected);

        expect(page.enableParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'STOP', 'DISABLE',  'Delete' ]);
        expect(page.getParserConfig()).toEqual(parserRunningExpected);

        expect(page.stopParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'START', 'Delete' ]);

        page.closePane('squid');

        done();

    }, 300000);
});
