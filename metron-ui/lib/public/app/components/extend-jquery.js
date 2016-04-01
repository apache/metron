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

define(['jquery'],
function ($) {
  'use strict';

  /**
   * jQuery extensions
   */
  var $win = $(window);

  $.fn.place_tt = (function () {
    var defaults = {
      offset: 5,
      css: {
        position : 'absolute',
        top : -1000,
        left : 0,
        color : "#c8c8c8",
        padding : '10px',
        'font-size': '11pt',
        'font-weight' : 200,
        'background-color': '#1f1f1f',
        'border-radius': '5px',
        'z-index': 9999
      }
    };

    return function (x, y, opts) {
      opts = $.extend(true, {}, defaults, opts);
      return this.each(function () {
        var $tooltip = $(this), width, height;

        $tooltip.css(opts.css);
        if (!$.contains(document.body, $tooltip[0])) {
          $tooltip.appendTo(document.body);
        }

        width = $tooltip.outerWidth(true);
        height = $tooltip.outerHeight(true);

        $tooltip.css('left', x + opts.offset + width > $win.width() ? x - opts.offset - width : x + opts.offset);
        $tooltip.css('top', y + opts.offset + height > $win.height() ? y - opts.offset - height : y + opts.offset);
      });
    };
  })();

  return $;
});
