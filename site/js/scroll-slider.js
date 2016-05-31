////jQuery(document).ready(function () {
////    var window_width = jQuery(window).width();
////    if (jQuery('.mask').length) {
////
////        var waypoints = jQuery('.animated-screen').waypoint(function (direction) {
////            jQuery('.scroll-slider').toggleClass('screen-scroll');
////            
////        });
////        var waypoints2 = jQuery('.animated-screen:last-of-type').waypoint(function (direction) {
////            jQuery('.scroll-slider').toggleClass('screen-scroll');
////        //}, {offset: '400px'
////        });
////
////    }
////});
//
//
//
//
//
//
//// page init
//jQuery(function(){
//	initParallax();
//});
//
//// init parallax
//function initParallax(){
//	var animSpeed = 1200;
//	var fixedClass = 'fixed-mode';
//	var win = jQuery(window);
//	var isTouchDevice = /Windows Phone/.test(navigator.userAgent) || ('ontouchstart' in window) || window.DocumentTouch && document instanceof DocumentTouch;
//	jQuery('.mask').each(function(){
//		var hold = jQuery(this);
//		var busy = false;
//
//		if (!hold.length || isTouchDevice) return;
//
//		function move(offset){
//			hold.addClass(fixedClass);
//			hold.delay(1000).animate({scrollTop: offset}, animSpeed, function(){
//				busy = false;
//				hold.removeClass(fixedClass);
//			})
//			busy = true;
//		}
//		function scrollHandler(){
//			if (busy) {
//				win.scrollTop((hold.offset().top ).toFixed(0));
//				return;
//			}
//			if (hold.offset().top  < win.scrollTop()  && hold.scrollTop() == 0  && !busy) {
//				move(hold.outerHeight())
//			}
//			if (hold.offset().top  > win.scrollTop()  && hold.scrollTop() == hold.outerHeight()  && !busy) {
//				move(0)
//			}
//		}
//
//		ResponsiveHelper.addRange({
//			'1024..': {
//				on: function() {
//					scrollHandler();
//					win.on('load scroll resize orientationchange', scrollHandler);
//				},
//				off: function() {
//					hold.stop().removeAttr('style');
//					win.off('scroll resize orientationchange', scrollHandler);
//				}
//			}
//		});
//	})
//}
//
//
///*
// * Responsive Layout helper
// */
//ResponsiveHelper = (function($){
//  // init variables
//  var handlers = [],
//    prevWinWidth,
//    win = $(window),
//    nativeMatchMedia = false;
//
//  // detect match media support
//  if(window.matchMedia) {
//    if(window.Window && window.matchMedia === Window.prototype.matchMedia) {
//      nativeMatchMedia = true;
//    } else if(window.matchMedia.toString().indexOf('native') > -1) {
//      nativeMatchMedia = true;
//    }
//  }
//
//  // prepare resize handler
//  function resizeHandler() {
//    var winWidth = win.width();
//    if(winWidth !== prevWinWidth) {
//      prevWinWidth = winWidth;
//
//      // loop through range groups
//      $.each(handlers, function(index, rangeObject){
//        // disable current active area if needed
//        $.each(rangeObject.data, function(property, item) {
//          if(item.currentActive && !matchRange(item.range[0], item.range[1])) {
//            item.currentActive = false;
//            if(typeof item.disableCallback === 'function') {
//              item.disableCallback();
//            }
//          }
//        });
//
//        // enable areas that match current width
//        $.each(rangeObject.data, function(property, item) {
//          if(!item.currentActive && matchRange(item.range[0], item.range[1])) {
//            // make callback
//            item.currentActive = true;
//            if(typeof item.enableCallback === 'function') {
//              item.enableCallback();
//            }
//          }
//        });
//      });
//    }
//  }
//  win.bind('load resize orientationchange', resizeHandler);
//
//  // test range
//  function matchRange(r1, r2) {
//    var mediaQueryString = '';
//    if(r1 > 0) {
//      mediaQueryString += '(min-width: ' + r1 + 'px)';
//    }
//    if(r2 < Infinity) {
//      mediaQueryString += (mediaQueryString ? ' and ' : '') + '(max-width: ' + r2 + 'px)';
//    }
//    return matchQuery(mediaQueryString, r1, r2);
//  }
//
//  // media query function
//  function matchQuery(query, r1, r2) {
//    if(window.matchMedia && nativeMatchMedia) {
//      return matchMedia(query).matches;
//    } else if(window.styleMedia) {
//      return styleMedia.matchMedium(query);
//    } else if(window.media) {
//      return media.matchMedium(query);
//    } else {
//      return prevWinWidth >= r1 && prevWinWidth <= r2;
//    }
//  }
//
//  // range parser
//  function parseRange(rangeStr) {
//    var rangeData = rangeStr.split('..');
//    var x1 = parseInt(rangeData[0], 10) || -Infinity;
//    var x2 = parseInt(rangeData[1], 10) || Infinity;
//    return [x1, x2].sort(function(a, b){
//      return a - b;
//    });
//  }
//
//  // export public functions
//  return {
//    addRange: function(ranges) {
//      // parse data and add items to collection
//      var result = {data:{}};
//      $.each(ranges, function(property, data){
//        result.data[property] = {
//          range: parseRange(property),
//          enableCallback: data.on,
//          disableCallback: data.off
//        };
//      });
//      handlers.push(result);
//
//      // call resizeHandler to recalculate all events
//      prevWinWidth = null;
//      resizeHandler();
//    }
//  };
//}(jQuery));
