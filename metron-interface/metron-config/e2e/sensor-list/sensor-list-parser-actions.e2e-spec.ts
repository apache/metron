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
import { SensorListPage } from './sensor-list.po';
import {LoginPage} from '../login/login.po';

describe('Sensor List Long Running Cases', function() {
    let page: SensorListPage = new SensorListPage();
    let loginPage = new LoginPage();
    let ASCENDING_CSS = 'fa fa-sort-asc';
    let DESCENDING_CSS = 'fa fa-sort-desc';
    let defaultActionState = [
        { classNames: 'fa-circle-o-notch', displayed: false },
        { classNames: 'fa-stop', displayed: false },
        { classNames: 'fa-ban', displayed: false },
        { classNames: 'fa-play', displayed: true },
        { classNames: 'fa-check-circle-o', displayed: false },
        { classNames: 'fa-pencil', displayed: true },
        { classNames: 'fa-trash-o', displayed: true }
    ];
    let runningActionstate = [
        { classNames: 'fa-circle-o-notch', displayed: false},
        { classNames: 'fa-stop', displayed: true},
        { classNames: 'fa-ban', displayed: true},
        { classNames: 'fa-play', displayed: false},
        { classNames: 'fa-check-circle-o', displayed: false},
        { classNames: 'fa-pencil', displayed: true},
        { classNames: 'fa-trash-o', displayed: true}
    ];
    let disabledActionState = [
        { classNames: 'fa-circle-o-notch', displayed: false},
        { classNames: 'fa-stop', displayed: true},
        { classNames: 'fa-ban', displayed: false},
        { classNames: 'fa-play', displayed: false},
        { classNames: 'fa-check-circle-o', displayed: true},
        { classNames: 'fa-pencil', displayed: true},
        { classNames: 'fa-trash-o', displayed: true}
    ];
    
    beforeAll(() => {
        loginPage.login();
    });

    afterAll(() => {
        loginPage.logout();
    });

    it('should start parsers from actions', (done) => {
        expect(page.getAddButton()).toEqual(true);
        page.startParsers(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'bro', 'yaf']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true])
            done();
        })

    }, 300000)

    it('should have correct values when parser is running', () => {
        expect(page.getColumnValues(2)).toEqual(['Running', 'Running', 'Running', 'Running', 'Running', 'Running', 'Running']);
        expect(page.getColumnValues(3)).toEqual(['0s', '0s', '0s', '0s', '0s', '0s', '0s']);
        expect(page.getColumnValues(4)).toEqual(['0kb/s', '0kb/s', '0kb/s', '0kb/s', '0kb/s', '0kb/s', '0kb/s']);
        expect(page.getColumnValues(5)).toEqual(['', '', '', '', '', '', '']);
        expect(page.getColumnValues(6)).toEqual(['', '', '', '', '', '', '']);
    });

    it('should disable all parsers from actions', (done) => {
        page.disableParsers(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'bro', 'yaf']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true])
            done();
        })

    }, 300000)

    it('should have correct values when parser is disabled', () => {
        expect(page.getColumnValues(2)).toEqual(['Disabled', 'Disabled', 'Disabled', 'Disabled', 'Disabled', 'Disabled', 'Disabled']);
    });

    it('should enable all parsers from actions', (done) => {
        page.enableParsers(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'bro', 'yaf']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true])
            done();
        })
    }, 300000)

    it('should stop parsers from actions', (done) => {
        page.stopParsers(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'yaf', 'bro']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true]);
            done();
        })
    }, 300000)

    it('should start parsers from dropdown', (done) => {
        page.startParsersFromDropdown(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'bro', 'yaf']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true]);
            done();
        })
    }, 300000)

    it('should disable parsers from dropdown', (done) => {
        page.disableParsersFromDropdown(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'bro', 'yaf']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true]);
            done();
        })
    }, 300000)

    it('should enable parsers from dropdown', (done) => {
        page.enableParsersFromDropdown(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'bro', 'yaf']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true]);
            done();
        })
    }, 300000)

    it('should stop parsers from dropdown', (done) => {
        page.stopParsersFromDropdown(['websphere', 'jsonMap', 'squid', 'asa', 'snort', 'bro', 'yaf']).then((args) => {
            expect(args).toEqual([true, true, true, true, true, true, true]);
            done();
        })
    }, 300000)
});
