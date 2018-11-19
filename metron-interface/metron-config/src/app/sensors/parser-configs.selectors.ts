import { createSelector } from '@ngrx/store';
import { MetaParserConfigItem } from './sensor-aggregate/meta-parser-config-item';
import { SensorParserConfigHistory } from '../model/sensor-parser-config-history';

export const getGroups = (state) => {

  // TODO: it's hardcoded for now, see below.
  //return state.parserGroups;


  // TODO: it's temporary.
  // Remove this and uncomment the lines above if groups are available from the state.
  return [{
    name: 'TheMostHappiestGroup',
    description: 'This is a nice group indeed'
  }, {
    name: 'ANiceGroup',
    description: 'This is not a political party'
  }];
};

export const getParsers = (state) => {
  return state.parserConfigs;
};

export const getMergedConfigs = createSelector(
  getGroups,
  getParsers,
  (groups, parsers) => {
    let result = [];

    groups.forEach((group, i) => {
      const metaGroupItem = new MetaParserConfigItem(new SensorParserConfigHistory());
      metaGroupItem.setName(group.name);
      metaGroupItem.setIsGroup(true);
      result = result.concat(metaGroupItem);

      const configsForGroup = parsers
        .filter(parser => parser.config && parser.config.group === group.name)
        .map(parser => new MetaParserConfigItem(parser));

      result = result.concat(configsForGroup);
    });

    result = result.concat(
      parsers
        .filter(parser => !parser.config || !parser.config.group)
        .map(parser => new MetaParserConfigItem(parser))
      );

    return result;
  }
);
