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
  'app',
  'lodash'
],
function (angular, app, _) {
  'use strict';

  var module = angular.module('kibana.controllers');

  module.controller('PulldownCtrl', function($scope, $rootScope, $timeout,ejsResource, querySrv) {
      var _d = {
        collapse: false,
        notice: false,
        enable: true
      };

      _.defaults($scope.pulldown,_d);

      $scope.init = function() {
        $scope.querySrv = querySrv;

        // Provide a combined skeleton for panels that must interact with panel and row.
        // This might create name spacing issues.
        $scope.panel = $scope.pulldown;
        $scope.row = $scope.pulldown;
      };

      $scope.toggle_pulldown = function(pulldown) {
        pulldown.collapse = pulldown.collapse ? false : true;
        if (!pulldown.collapse) {
          $timeout(function() {
            $scope.$broadcast('render');
          });
        } else {
          $scope.row.notice = false;
        }
      };

      $scope.init();

    }
  );

});
