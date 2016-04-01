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
function (angular,_) {
  'use strict';

  angular
    .module('kibana.directives')
    .directive('configModal', function($modal,$q) {
      return {
        restrict: 'A',
        link: function(scope, elem, attrs) {
          var
            model = attrs.kbnModel,
            partial = attrs.configModal;


          // create a new modal. Can't reuse one modal unforunately as the directive will not
          // re-render on show.
          elem.bind('click',function(){

            // Create a temp scope so we can discard changes to it if needed
            var tmpScope = scope.$new();
            tmpScope[model] = angular.copy(scope[model]);

            tmpScope.editSave = function(panel) {
              // Correctly set the top level properties of the panel object
              _.each(panel,function(v,k) {
                scope[model][k] = panel[k];
              });
            };

            var panelModal = $modal({
              //template: './app/partials/paneleditor.html',
              template: partial,
              persist: true,
              show: false,
              scope: tmpScope,
              keyboard: false
            });

            // and show it
            $q.when(panelModal).then(function(modalEl) {
              modalEl.modal('show');
            });
            scope.$apply();
          });
        }
      };
    });
});
