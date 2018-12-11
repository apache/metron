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
import * as fromReducers from './';
import * as fromActionts from '../actions';
import { ParserConfigModel } from '../models/parser-config.model';
import { ParserGroupModel } from '../models/parser-group.model';
import { TopologyStatus } from '../../model/topology-status';

describe('sensors: parsers configs reducer', () => {

  it('should return with the initial state by default', () => {
    expect(
      fromReducers.parserConfigsReducer(undefined, { type: '' })
    ).toBe(fromReducers.initialParserState);
  });

  it('should return with the previous state', () => {
    const previousState = { items: [] };
    expect(
      fromReducers.parserConfigsReducer(previousState, { type: '' })
    ).toBe(previousState);
  });

  it('should set items on LoadSuccess', () => {
    const parsers = [];
    const previousState = {
      items: []
    };
    const action = new fromActionts.LoadSuccess({
      parsers
    });

    expect(
      fromReducers.parserConfigsReducer(previousState, action).items
    ).toBe(parsers);
  });

  it('should aggregate parsers on AggregateParsers', () => {
    const groupName = 'Foo group';
    const parserIds = [
      'Parser Config ID 02',
    ]
    const previousState: fromReducers.ParserState = {
      items: [{
        config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'Kafka/Sensor Topic ID 1'})
      }, {
        config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'Kafka/Sensor Topic ID 2'})
      }]
    };
    const action = new fromActionts.AggregateParsers({
      groupName,
      parserIds,
    });

    const newState = fromReducers.parserConfigsReducer(previousState, action);
    expect(newState.items[0]).toBe(previousState.items[0]);
    expect(newState.items[1]).not.toBe(previousState.items[1]);
    expect(newState.items[1].isDirty).toBe(true);
    expect(newState.items[1].config.getName()).toBe('Parser Config ID 02');
    expect(newState.items[1].config.group).toEqual(groupName);
  });

  it('should set group on AddToGroup', () => {
    const groupName = 'Foo group';
    const parserIds = [
      'Parser Config ID 02',
    ]
    const previousState: fromReducers.ParserState = {
      items: [{
        config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'Kafka/Sensor Topic ID 1'})
      }, {
        config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'Kafka/Sensor Topic ID 2'})
      }]
    };
    const action = new fromActionts.AddToGroup({
      groupName,
      parserIds,
    });

    const newState = fromReducers.parserConfigsReducer(previousState, action);
    expect(newState.items[0]).toBe(previousState.items[0]);
    expect(newState.items[1]).not.toBe(previousState.items[1]);
    expect(newState.items[1].isDirty).toBe(true);
    expect(newState.items[1].config.getName()).toBe('Parser Config ID 02');
    expect(newState.items[1].config.group).toEqual(groupName);
  });

  it('should mark items as deleted on MarkAsDeleted', () => {
    const parserIds = ['Parser Config ID 02'];
    const previousState: fromReducers.ParserState = {
      items: [{
        config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'Kafka/Sensor Topic ID 1'})
      }, {
        config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'Kafka/Sensor Topic ID 2'})
      }]
    };
    const action = new fromActionts.MarkAsDeleted({ parserIds });
    const newState = fromReducers.parserConfigsReducer(previousState, action);
    expect(newState.items[0]).toBe(previousState.items[0]);
    expect(newState.items[1]).not.toBe(previousState.items[1]);
    expect(newState.items[1].isDeleted).toBe(true);
  });

  it('should remove group property of items which belong to a group marked as deleted on MarkAsDeleted', () => {
    const groupName = 'Foo Group';
    const parserIds = [groupName];
    const previousState: fromReducers.ParserState = {
      items: [{
        config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'Kafka/Sensor Topic ID 1'})
      }, {
        config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'Kafka/Sensor Topic ID 2', group: groupName})
      }]
    };
    const action = new fromActionts.MarkAsDeleted({ parserIds });
    const newState = fromReducers.parserConfigsReducer(previousState, action);
    expect(newState.items[0]).toBe(previousState.items[0]);
    expect(newState.items[1]).not.toBe(previousState.items[1]);
    expect(newState.items[1].isDeleted).toBeFalsy();
    expect(newState.items[1].config.group).toBe('');
  });
});

describe('sensors: group configs reducer', () => {

  it('should return with the initial state by default', () => {
    expect(
      fromReducers.groupConfigsReducer(undefined, { type: '' })
    ).toBe(fromReducers.initialGroupState);
  });

  it('should return with the previous state', () => {
    const previousState = { items: [] };
    expect(
      fromReducers.groupConfigsReducer(previousState, { type: '' })
    ).toBe(previousState);
  });

  it('should set items on LoadSuccess', () => {
    const groups = [];
    const previousState = {
      items: []
    };
    const action = new fromActionts.LoadSuccess({
      groups
    });

    expect(
      fromReducers.groupConfigsReducer(previousState, action).items
    ).toBe(groups);
  });

  it('should add a new group on CreateGroup', () => {
    const previousState: fromReducers.GroupState = {
      items: [{ config: new ParserGroupModel({ name: 'Existing group' }) }]
    };
    const action = new fromActionts.CreateGroup({name: 'New group', description: 'New description'});
    const newState = fromReducers.groupConfigsReducer(previousState, action);

    expect(newState.items.length).toBe(previousState.items.length + 1);
    expect(newState.items[0]).toBe(previousState.items[0]);
    expect(newState.items[1].config.getName()).toBe('New group');
    expect(newState.items[1].isGroup).toBe(true);
    expect(newState.items[1].isPhantom).toBe(true);
  });

  it('should edit an existing group description on UpdateGroupDescription', () => {
    const previousState: fromReducers.GroupState = {
      items: [{ config: new ParserGroupModel({ name: 'Existing group', description: 'Existing description' }) }]
    };
    const newConfig = {name: 'Existing group', description: 'New description'};
    const action = new fromActionts.UpdateGroupDescription(newConfig);
    const newState = fromReducers.groupConfigsReducer(previousState, action);

    expect(newState.items.length).toBe(previousState.items.length);
    expect(newState.items[0].config.getName()).toBe(newConfig.name);
    expect(newState.items[0].config.getDescription()).toBe(newConfig.description);
    expect(newState.items[0].isDirty).toBe(true);
  });

  it('should mark groups as deleted on MarkAsDeleted', () => {
    const groupName = 'Existing group';
    const previousState: fromReducers.GroupState = {
      items: [{ config: new ParserGroupModel({ name: groupName }) }]
    };
    const action = new fromActionts.MarkAsDeleted({
      parserIds: [groupName]
    });
    const newState = fromReducers.groupConfigsReducer(previousState, action);

    expect(newState.items[0].isDeleted).toBe(true);
  });
});

describe('sensors: parser statuses reducer', () => {

  it('should return with the initial state by default', () => {
    expect(
      fromReducers.parserStatusReducer(undefined, { type: '' })
    ).toBe(fromReducers.initialStatusState);
  });

  it('should return with the previous state', () => {
    const previousState = { items: [] };
    expect(
      fromReducers.parserStatusReducer(previousState, { type: '' })
    ).toBe(previousState);
  });

  it('should set items on LoadSuccess', () => {
    const statuses = [];
    const previousState = {
      items: []
    };
    const action = new fromActionts.LoadSuccess({
      statuses
    });

    expect(
      fromReducers.parserStatusReducer(previousState, action).items
    ).toBe(statuses);
  });

  it('should set items on PollStatusSuccess', () => {
    const statuses = [];
    const previousState = {
      items: []
    };
    const action = new fromActionts.PollStatusSuccess({
      statuses
    });

    expect(
      fromReducers.parserStatusReducer(previousState, action).items
    ).toBe(statuses);
  });
});

describe('sensors: layout reducer', () => {
  it('should return with the initial state by default', () => {
    expect(
      fromReducers.layoutReducer(undefined, { type: '' })
    ).toBe(fromReducers.initialLayoutState);
  });

  it('should return with the previous state', () => {
    const previousState = { order: [], dnd: {} };
    expect(
      fromReducers.layoutReducer(previousState, { type: '' })
    ).toBe(previousState);
  });

  it('should set the order on LoadSuccess', () => {
    const previousState = { order: [], dnd: {} };
    const action = new fromActionts.LoadSuccess({
      parsers: [
        { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1', group: 'group 2' }) },
        { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'sensor topic 2', group: 'group 2' }) },
        { config: new ParserConfigModel('Parser Config ID 03', { sensorTopic: 'sensor topic 3' }) },
        { config: new ParserConfigModel('Parser Config ID 04', { sensorTopic: 'sensor topic 4', group: 'group 1' }) },
      ],
      groups: [
        { config: new ParserGroupModel({ name: 'group 1' }) },
        { config: new ParserGroupModel({ name: 'group 2' }) },
      ]
    });
    const newState = fromReducers.layoutReducer(previousState, action);
    expect(newState.order).not.toBe(previousState.order);
    expect(newState.order).toEqual([
      'group 1',
      'Parser Config ID 04',
      'group 2',
      'Parser Config ID 01',
      'Parser Config ID 02',
      'Parser Config ID 03',
    ]);
  });

  it('should set the draggedId on SetDragged', () => {
    const previousState = { order: [], dnd: {} };
    const action = new fromActionts.SetDragged('Foo');
    const newState = fromReducers.layoutReducer(previousState, action);

    expect(newState.dnd.draggedId).toBe('Foo');
  });

  it('should set the dropTargetId on SetDropTarget', () => {
    const previousState = { order: [], dnd: {} };
    const action = new fromActionts.SetDropTarget('Bar');
    const newState = fromReducers.layoutReducer(previousState, action);

    expect(newState.dnd.dropTargetId).toBe('Bar');
  });

  it('should set the targetGroup on SetTargetGroup', () => {
    const previousState = { order: [], dnd: {} };
    const action = new fromActionts.SetTargetGroup('Lorem');
    const newState = fromReducers.layoutReducer(previousState, action);

    expect(newState.dnd.targetGroup).toBe('Lorem');
  });

  it('should append the group name to the order on CreateGroup', () => {
    const previousState = { order: [], dnd: {} };
    const action = new fromActionts.CreateGroup({name: 'Group name', description: 'Group description'});
    const newState = fromReducers.layoutReducer(previousState, action);

    expect(newState.order[newState.order.length - 1]).toBe('Group name');
  });

  it('should recalculate the order on AggregateParsers', () => {
    const previousState = {
      order: [
        'group 1',
        'sensor topic 3',
        'group 2',
        'sensor topic 1',
        'sensor topic 2',
        'group 4',
        'sensor topic 4',
      ],
      dnd: {}
    };
    const action = new fromActionts.AggregateParsers({
      groupName: 'group 4',
      parserIds: ['sensor topic 2', 'sensor topic 1']
    });
    const newState = fromReducers.layoutReducer(previousState, action);

    expect(newState.order).not.toBe(previousState.order);
    expect(newState.order).toEqual([
      'group 1',
      'sensor topic 3',
      'group 2',
      'group 4',
      'sensor topic 1',
      'sensor topic 2',
      'sensor topic 4',
    ]);
  });

  it('should inject the order item after the reference item on InjectAfter', () => {
    const previousState = {
      order: [
        'group 1',
        'group 2',
        'sensor topic 1',
        'sensor topic 2',
        'sensor topic 3',
        'sensor topic 4',
      ],
      dnd: {}
    };
    const action = new fromActionts.InjectAfter({
      parserId: 'sensor topic 1',
      reference: 'sensor topic 3'
    });
    const newState = fromReducers.layoutReducer(previousState, action);

    expect(newState.order).not.toBe(previousState.order);
    expect(newState.order).toEqual([
      'group 1',
      'group 2',
      'sensor topic 2',
      'sensor topic 3',
      'sensor topic 1',
      'sensor topic 4',
    ]);
  });

  it('should inject the order item before the reference item on InjectBefore', () => {
    const previousState = {
      order: [
        'group 1',
        'group 2',
        'sensor topic 1',
        'sensor topic 2',
        'sensor topic 3',
        'sensor topic 4',
      ],
      dnd: {}
    };
    const action = new fromActionts.InjectBefore({
      parserId: 'sensor topic 4',
      reference: 'sensor topic 2'
    });
    const newState = fromReducers.layoutReducer(previousState, action);

    expect(newState.order).not.toBe(previousState.order);
    expect(newState.order).toEqual([
      'group 1',
      'group 2',
      'sensor topic 1',
      'sensor topic 4',
      'sensor topic 2',
      'sensor topic 3',
    ]);
  });
});

describe('sensors: selectors', () => {

  it('should return with the sensors substate from the store', () => {
    const sensors = {
      parsers: { items: [] },
      groups: { items: [] },
      statuses: { items: [] },
      layout: { order: [], dnd: {} }
    };
    const state = { sensors };

    expect(fromReducers.getSensorsState(state))
      .toBe(sensors);
  });

  it('should return with the parsers substate from the store', () => {
    const sensors = {
      parsers: { items: [] },
      groups: { items: [] },
      statuses: { items: [] },
      layout: { order: [], dnd: {} }
    };
    const state = { sensors };

    expect(fromReducers.getParsers(state))
      .toBe(sensors.parsers.items);
  });

  it('should return with the groups substate from the store', () => {
    const sensors = {
      parsers: { items: [] },
      groups: { items: [] },
      statuses: { items: [] },
      layout: { order: [], dnd: {} }
    };
    const state = { sensors };

    expect(fromReducers.getGroups(state))
      .toBe(sensors.groups.items);
  });

  it('should return with the statuses substate from the store', () => {
    const sensors = {
      parsers: { items: [] },
      groups: { items: [] },
      statuses: { items: [] },
      layout: { order: [], dnd: {} }
    };
    const state = { sensors };

    expect(fromReducers.getStatuses(state))
      .toBe(sensors.statuses.items);
  });

  it('should return with the order substate from the store', () => {
    const sensors = {
      parsers: { items: [] },
      groups: { items: [] },
      statuses: { items: [] },
      layout: { order: [], dnd: {} }
    };
    const state = { sensors };

    expect(fromReducers.getLayoutOrder(state))
      .toBe(sensors.layout.order);
  });

  it('should return with a merged version of groups, parsers and statuses ordered by the order state', () => {
    const state = {
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'Kafka/Sensor Topic ID 1' }) },
            { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'Kafka/Sensor Topic ID 2', group: 'group 1' }) },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }) },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: {
          items: [
            new TopologyStatus({ name: 'Parser Config ID 02' }),
            new TopologyStatus({ name: 'Parser Config ID 01' }),
            new TopologyStatus({ name: 'group 2' }),
          ]
        },
        layout: {
          order: [
            'Parser Config ID 02',
            'Parser Config ID 01',
            'group 2',
            'group 1'
          ],
          dnd: {}
        }
      }
    };

    const merged = fromReducers.getMergedConfigs(state);

    expect(merged.length).toBe(state.sensors.parsers.items.length + state.sensors.groups.items.length);

    // the reference changes !!
    expect(merged[0]).not.toBe(state.sensors.parsers.items[1]);
    expect(merged[1]).not.toBe(state.sensors.parsers.items[0]);
    expect(merged[2]).not.toBe(state.sensors.groups.items[1]);
    expect(merged[3]).not.toBe(state.sensors.groups.items[0]);

    // should be ordered by the order state
    expect(merged[0].config.getName()).toBe(state.sensors.layout.order[0]);
    expect(merged[1].config.getName()).toBe(state.sensors.layout.order[1]);
    expect(merged[2].config.getName()).toBe(state.sensors.layout.order[2]);
    expect(merged[3].config.getName()).toBe(state.sensors.layout.order[3]);

    // make sure they got the status
    expect(merged[0].status).toEqual(state.sensors.statuses.items[0]);
    expect(merged[1].status).toEqual(state.sensors.statuses.items[1]);
    expect(merged[2].status).toEqual(state.sensors.statuses.items[2]);

    // no status belongs to it but got a status instance witn no name
    expect(merged[3].status).toBeTruthy();
    expect(merged[3].status.name).toBeFalsy();
  });

  it('should tell if any of the groups or parser configs are dirty', () => {
    expect(fromReducers.isDirty({
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1' }) },
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 2' }) },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }) },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: { items: [] },
        layout: { order: [], dnd: {} }
      }
    })).toBe(false);

    expect(fromReducers.isDirty({
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1' }) },
            { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'sensor topic 2' }) },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }), isDeleted: true },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: { items: [] },
        layout: { order: [], dnd: {} }
      }
    })).toBe(true);

    expect(fromReducers.isDirty({
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1' }) },
            { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'sensor topic 2' }), isDeleted: true },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }) },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: { items: [] },
        layout: { order: [], dnd: {} }
      }
    })).toBe(true);

    expect(fromReducers.isDirty({
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1' }) },
            { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'sensor topic 2' }) },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }), isDirty: true },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: { items: [] },
        layout: { order: [], dnd: {} }
      }
    })).toBe(true);

    expect(fromReducers.isDirty({
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1' }), isDirty: true },
            { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'sensor topic 2' }) },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }) },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: { items: [] },
        layout: { order: [], dnd: {} }
      }
    })).toBe(true);

    expect(fromReducers.isDirty({
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1' }) },
            { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'sensor topic 2' }), isPhantom: true },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }) },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: { items: [] },
        layout: { order: [], dnd: {} }
      }
    })).toBe(true);

    expect(fromReducers.isDirty({
      sensors: {
        parsers: {
          items: [
            { config: new ParserConfigModel('Parser Config ID 01', { sensorTopic: 'sensor topic 1' }) },
            { config: new ParserConfigModel('Parser Config ID 02', { sensorTopic: 'sensor topic 2' }) },
          ]
        },
        groups: {
          items: [
            { config: new ParserGroupModel({ name: 'group 1' }), isPhantom: true },
            { config: new ParserGroupModel({ name: 'group 2' }) },
          ]
        },
        statuses: { items: [] },
        layout: { order: [], dnd: {} }
      }
    })).toBe(true);
  });

  it('should update the parser config in state', () => {
    const previousState: fromReducers.ParserState = {
      items: [
        { config: new ParserConfigModel('bar', { sensorTopic: 'bar' }) },
        { config: new ParserConfigModel('foo', { sensorTopic: 'foo' }) },
      ]
    };
    const action = new fromActionts.UpdateParserConfig(
      new ParserConfigModel('foo', { sensorTopic: 'foo updated' })
    );
    const newState = fromReducers.parserConfigsReducer(previousState, action);
    const updated = newState.items.find(item => item.config.getName() === 'foo');
    expect((updated.config as ParserConfigModel).sensorTopic).toBe('foo updated');
  });
});
