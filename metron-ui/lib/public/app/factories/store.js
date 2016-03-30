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

define([
  'angular',
  'lodash'
],
function (angular, _) {
  'use strict';

  var module = angular.module('kibana.factories');
  module.factory('storeFactory', function() {

    return function storeFactory($scope, name, defaults) {
      if (!_.isFunction($scope.$watch)) {
        throw new TypeError('Invalid scope.');
      }
      if (!_.isString(name)) {
        throw new TypeError('Invalid name, expected a string that the is unique to this store.');
      }
      if (defaults && !_.isPlainObject(defaults)) {
        throw new TypeError('Invalid defaults, expected a simple object or nothing');
      }

      defaults = defaults || {};

      // get the current value, parse if it exists
      var current = localStorage.getItem(name);
      if (current != null) {
        try {
          current = JSON.parse(current);
        } catch (e) {
          current = null;
        }
      }

      if (current == null) {
        current = _.clone(defaults);
      } else if (_.isPlainObject(current)) {
        _.defaults(current, defaults);
      } else {
        throw new TypeError('Invalid store value' + current);
      }

      $scope[name] = current;

      // listen for changes and store them in localStorage.
      // delete the value to reset to the defaults, ie. `delete $scope[name]` -> digest cycle -> `$scope[name] == defaults`
      $scope.$watch(name, function (val) {
        if (val === void 0) {
          localStorage.removeItem(name);
          $scope[name] = _.clone(defaults);
        } else {
          localStorage.setItem(name, JSON.stringify(val));
        }
      }, true);

      return current;
    };
  });

});
