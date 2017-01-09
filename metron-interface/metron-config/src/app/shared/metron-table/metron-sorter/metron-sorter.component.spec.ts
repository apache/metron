/* tslint:disable:no-unused-variable */
// directiveSelectorNameRule

import {MetronSorterComponent} from './metron-sorter.component';
import {MetronTableDirective} from '../metron-table.directive';

describe('Component: MetronSorter', () => {

  it('should create an instance', () => {
    let metronTable = new MetronTableDirective();
    let component = new MetronSorterComponent(metronTable);
    expect(component).toBeTruthy();
  });

  it('should set the variables according to sorter', () => {
    let metronTable = new MetronTableDirective();
    let sorter1 = new MetronSorterComponent(metronTable);
    let sorter2 = new MetronSorterComponent(metronTable);
    let sorter3 = new MetronSorterComponent(metronTable);

    sorter1.sortBy = 'col1';
    sorter2.sortBy = 'col2';
    sorter3.sortBy = 'col3';

    sorter1.sort();
    expect(sorter1.sortAsc).toEqual(true);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter1.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(true);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter2.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(true);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter2.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(true);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(false);

    sorter3.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(true);
    expect(sorter3.sortDesc).toEqual(false);

    sorter3.sort();
    expect(sorter1.sortAsc).toEqual(false);
    expect(sorter1.sortDesc).toEqual(false);
    expect(sorter2.sortAsc).toEqual(false);
    expect(sorter2.sortDesc).toEqual(false);
    expect(sorter3.sortAsc).toEqual(false);
    expect(sorter3.sortDesc).toEqual(true);

  });

});
