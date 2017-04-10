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
    let parsersToTest = ['snort', 'bro'];

    beforeAll(() => {
        loginPage.login();
    });

    afterAll(() => {
        loginPage.logout();
    });

    it('should disable all parsers from actions', (done) => {
        expect(page.getAddButton()).toEqual(true);
        page.disableParsers(parsersToTest).then((args) => {
            expect(args).toEqual([true, true])
            done();
        })

    }, 300000)

    it('should have correct values when parser is disabled', () => {
        page.toggleSort('Status');
        expect(page.getSortOrder('Status')).toEqual(ASCENDING_CSS);
        let latencyValues = page.getColumnValues(2);
        latencyValues.then(values => {
            expect(values.slice(0, 2)).toEqual(['Disabled', 'Disabled']);
            expect(values.slice(2)).toEqual(['Stopped', 'Stopped', 'Stopped', 'Stopped', 'Stopped']);
        });

        page.toggleSort('Status');
        expect(page.getSortOrder('Status')).toEqual(DESCENDING_CSS);
        latencyValues = page.getColumnValues(2);
        latencyValues.then(values => {
            expect(values.slice(0, 5)).toEqual(['Stopped', 'Stopped', 'Stopped', 'Stopped', 'Stopped']);
            expect(values.slice(5)).toEqual(['Disabled', 'Disabled']);
        });
    });

    it('should enable all parsers from actions', (done) => {
        page.enableParsers(parsersToTest).then((args) => {
            expect(args).toEqual([true, true])
            done();
        })
    }, 300000)

    it('should stop parsers from actions', (done) => {
        page.stopParsers(parsersToTest).then((args) => {
            expect(args).toEqual([true, true]);
            done();
        })
    }, 300000)

    it('should start parsers from dropdown', (done) => {
        page.startParsersFromDropdown(parsersToTest).then((args) => {
            expect(args).toEqual([true, true]);
            done();
        })
    }, 300000)

    it('should disable parsers from dropdown', (done) => {
        page.disableParsersFromDropdown(parsersToTest).then((args) => {
            expect(args).toEqual([true, true]);
            done();
        })
    }, 300000)

    it('should enable parsers from dropdown', (done) => {
        page.enableParsersFromDropdown(parsersToTest).then((args) => {
            expect(args).toEqual([true, true]);
            done();
        })
    }, 300000)

    it('should stop parsers from dropdown', (done) => {
        page.stopParsersFromDropdown(parsersToTest).then((args) => {
            expect(args).toEqual([true, true]);
            done();
        })
    }, 300000)

    it('should start parsers from actions', (done) => {
        page.startParsers(parsersToTest).then((args) => {
            expect(args).toEqual([true, true])
            done();
        })

    }, 300000)
});
