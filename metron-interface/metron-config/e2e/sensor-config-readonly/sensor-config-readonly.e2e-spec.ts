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

    it('should navigate to snort parser', () => {
        page.navigateTo('snort')
        expect(page.getCurrentUrl()).toEqual('http://localhost:4200/sensors(dialog:sensors-readonly/snort)');
        expect(page.getTitle()).toEqual("snort");
    });


    it('should disable snort parser', () => {
        expect(page.disableParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'STOP', 'ENABLE',  'Delete' ]);
        expect(page.getStormStatus()).toEqual('Disabled');
        expect(page.getParserState()).toEqual('Disabled');
        expect(page.getKafkaState()).toEqual('Emitting');
    });

    it('should enable snort parser', () => {
        expect(page.enableParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'STOP', 'DISABLE',  'Delete' ]);
        expect(page.getStormStatus()).toEqual('Running');
        expect(page.getParserState()).toEqual('Enabled');
        expect(page.getKafkaState()).toEqual('Emitting');
    });

    it('should stop parser', (done) => {
        expect(page.stopParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'START', 'Delete' ]);
        expect(page.getStormStatus()).toEqual('Stopped');
        expect(page.getParserState()).toEqual('-');
        expect(page.getKafkaState()).toEqual('Emitting');

        done();
    }, 300000);

    it('should start parser', (done) => {
        expect(page.startParser()).toEqual(true);
        expect(page.getButtons()).toEqual([ 'EDIT', 'STOP', 'DISABLE',  'Delete' ]);
        expect(page.getStormStatus()).toEqual('Running');
        expect(page.getParserState()).toEqual('Enabled');
        expect(page.getKafkaState()).toEqual('Emitting');

        page.closePane('squid');
        done();
    }, 300000);
});
