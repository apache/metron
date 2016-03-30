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
  'kbn'
],
function (angular) {
  'use strict';

  var module = angular.module('kibana.directives');

  module.directive('confirmClick', function() {
    return {
      restrict: 'A',
      link: function(scope, elem, attrs) {
        elem.bind('click', function() {
          var message = attrs.confirmation || "Are you sure you want to do that?";
          if (window.confirm(message)) {
            var action = attrs.confirmClick;
            if (action) {
              scope.$apply(scope.$eval(action));
            }
          }
        });
      },
    };
  });
});
