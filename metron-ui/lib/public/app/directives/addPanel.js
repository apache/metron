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

  angular
    .module('kibana.directives')
    .directive('addPanel', function($compile) {
      return {
        restrict: 'A',
        link: function($scope, elem) {

          $scope.$on("$destroy",function() {
            elem.remove();
          });

          $scope.$watch('panel.type', function() {
            var _type = $scope.panel.type;
            $scope.reset_panel(_type);
            if(!_.isUndefined($scope.panel.type)) {
              $scope.panel.loadingEditor = true;
              $scope.require(['panels/'+$scope.panel.type.replace(".","/") +'/module'], function () {
                var template = '<div ng-controller="'+$scope.panel.type+'" ng-include="\'app/partials/paneladd.html\'"></div>';
                elem.html($compile(angular.element(template))($scope));
                $scope.panel.loadingEditor = false;
              });
            }
          });
        }
      };
    });
});
