jQuery(document).ready(function () {

    jQuery('.fixed-links li a[href^="#"]').on('click', function (e) {
        e.preventDefault();
        var target = this.hash;
        $target = jQuery(target);
        jQuery('html, body').stop().animate({
            'scrollTop': ($target.offset().top - 50)
        }, 800, 'swing', function () {
        });
    });



    var window_width = jQuery(window).width();
    /*
     *  Set main navigation active based on path.
     */
    var page = window.location.pathname;

    jQuery('.main-navigation .main-menu li').each(function () {
        var section_url = jQuery('a', this).attr('href');
        if (page == section_url)
            jQuery(this).children("a").addClass('active');
    });
    jQuery('.main-navigation .main-menu li:has(.drop-menu)').addClass('arrow-menu');


    /*local main-menu on scroll */

    var scroll = jQuery(document).scrollTop();
    var headerHeight = jQuery('header').outerHeight();
    var sticky = jQuery('.fixed-links');
    var topVal;
    jQuery(window).on("resize", function () {
        window_width = jQuery(window).width();
        if (window_width > 767) {
            topVal = 63;
        } else if (window_width < 768) {
            topVal = 49;
        }
    }).resize();
    
    jQuery(window).scroll(function () {
        var bodyScroll = jQuery('body').css('overflow');
        var scrolled = jQuery(document).scrollTop();
        if (scrolled > headerHeight) {
            jQuery('.main-nav').addClass('off-canvas');
            jQuery('body').addClass('no-menu');

        } else if (scrolled <= 50) {
            jQuery('.main-nav').removeClass('off-canvas');
            jQuery('body').removeClass('no-menu');
            jQuery('.search-overlay, .newsletter-overlay').removeAttr('style');

        }

        if ((scrolled > scroll) && (bodyScroll !== "hidden")) {
            jQuery('.main-nav').removeClass('fixed');
            //jQuery('body').removeClass('fx-menu');
            sticky.css({'top': '0', 'transition': 'top .5s ease'});
        } else if ((scrolled < scroll) && (bodyScroll !== "hidden")) {
            jQuery('.main-nav').addClass('fixed');
            //jQuery('body').addClass('fx-menu');
            //jQuery('.search-overlay, .newsletter-overlay').css('top', '64px');
            sticky.css('top', topVal);

        }
        scroll = jQuery(document).scrollTop();
        jQuery('.drop-menu').removeAttr('style');
    });
    /**
     * stick anchor-links
     */
//    jQuery('.fixed-anchor').each(function () {
//        var anch_hei = jQuery('.fixed-anchor').outerHeight();
//        jQuery(this).css('height', anch_hei);
//        jQuery(window).load(function () {
//            jQuery(window).on("resize", function () {
//
//                if (jQuery('.fixed-links').length > 0) {
//                    var stickyOffset = '';
//                    if (jQuery('.fixed-links')) {
//                        sticky.removeClass('stick');
//                        var stickyOffset = jQuery('.fixed-links').offset().top;
//                    }
//                    jQuery(window).scroll(function () {
//                        var anch_hei = jQuery('.fixed-anchor').outerHeight();
//                        jQuery(this).css('height', anch_hei);
//                        scroll = jQuery(window).scrollTop();
//                        if (scroll >= stickyOffset) {
//                            sticky.addClass('stick');
//                        }
//                        else {
//                            sticky.removeClass('stick');
//                        }
//                    });
//                }
//            }).resize();
//        });
//    });

});
jQuery(window).load(function () {
    jQuery(window).on("resize", function () {
        var window_height = jQuery(window).height();
        var window_width = jQuery(window).width();

        /*
         *  Set menu height to avoid body jump on scroll
         */

        if (window_width > 1023) {
            jQuery('.main-navigation').css('height', "85px");
        } else
        if (window_width > 767 && window_width < 1024) {
            jQuery('.main-navigation').css('height', "85px");
        } else {
            jQuery('.main-navigation').css('height', "71px");
        }

        /*
         *  Set menu wrapper height full screen
         */

        var wrapper_height = window_height - 49;
        if (window_width < 768) {
            jQuery('.main-navigation .menu-wrapper').css("height", wrapper_height + "px");
        } else {
            jQuery('.main-navigation .menu-wrapper').css("height", "auto");
        }
        if (window_width > 767) {
            jQuery('.main-navigation .menu-wrapper').css("display", "block");

        } else {
            jQuery('.main-navigation .toggle').removeClass("active");
            jQuery('.main-navigation .menu-wrapper').css("display", "none");

        }
    }).resize();

    /*
     *  Second level navigation
     */
    jQuery(".fixed-links li:eq(0)").addClass("active");
    jQuery(window).on('scroll', function () {
        /*
         * Active links on page scroll
         */
        if (jQuery(".fixed-anchor").length) {
            var nav_height = jQuery('.fixed-anchor').outerHeight();
            var scrollPos = jQuery(document).scrollTop() + nav_height;
            if (jQuery('.fixed-anchor').length) {
                jQuery('.fixed-links li a').each(function () {
                    var currLink = jQuery(this);
                    var refElement = jQuery(currLink.attr("href"));
                    if (refElement.length) {
                        if (refElement.position().top <= scrollPos && refElement.position().top + refElement.outerHeight() > scrollPos) {
                            jQuery('.fixed-links li').removeClass("active");
                            currLink.parent().addClass("active");
                        }
                    }
//                    else {
//                        currLink.parent().removeClass("active");
//                    }
                    if (!jQuery(".fixed-links li").hasClass("active")) {
                        var thisElement = jQuery(".fixed-links li:eq(0)").children();
                        var thisId = jQuery(thisElement.attr("href"));
                        var topPosition = thisId.position().top - jQuery(window).scrollTop();
                        if (topPosition > nav_height) {
                            jQuery('.fixed-links li:eq(0)').addClass("active");
                        }
                    }
                });

            }
        }
    });

});

/*
 * Mobile menu to open and close
 */


jQuery(document).ready(function () {

    function checkbrowserwidth() {
        if (jQuery("nav > .toggle").css("display") === "block") {

            jQuery('.main-menu').addClass('mobile-menu');

        } else {
            jQuery('.main-menu').removeClass('mobile-menu');
            jQuery("body").css("overflow-y", "visible");
            jQuery('.toggle').removeClass("active");
        }
    }
    checkbrowserwidth();
    jQuery('.mobile-menu li.arrow-menu > a').addClass('mobile');

    jQuery(document).on('click', '.mobile-menu > li.arrow-menu > .mobile', function () {
        jQuery('.mobile').not(this).parent().find('.drop-menu').slideUp();
        jQuery(this).parent().find('.drop-menu').stop(true, true).slideToggle();
        return false;
    });


    jQuery(".toggle").click(function () {
        jQuery(this).toggleClass("active");
        jQuery('.drop-menu').removeAttr('style');
        jQuery(".menu-wrapper").toggle();
        if (jQuery(this).hasClass("active")) {
            jQuery("body").css("overflow-y", "hidden");
        } else {
            jQuery("body").css("overflow-y", "visible");
        }
    });

    /*drop-menu show on hover*/

//    jQuery('.main-menu > li.arrow-menu').hover(function () {
//        jQuery(this).find(".drop-menu").stop(true, true).delay(200).fadeIn(200);
//    }, function () {
//        jQuery(this).find(".drop-menu").stop(true, true).fadeOut(200);
//    });
    
    // Make the main menu touch friendly for mobile devices, like iPads
    if (jQuery('.ua-mobile').length > 0) {
	    // Track what was last clicked
	    window.hwxLastClicked = null;
	    jQuery(document).click(function(e){
	      window.hwxLastClicked = e.target;
	    });
	    
	    // Prevent top level links from being followed unless they were the last element clicked
	    jQuery('.main-menu > li > a').click(function(e){
	      if(window.hwxLastClicked !== this && !jQuery('.main-menu').hasClass('mobile-menu')) {
	        e.preventDefault();
	      }
	    });
	  }
});

jQuery(window).resize(function () {

    function checkbrowserwidth() {
        if (jQuery("nav > .toggle").css("display") === "block") {

            jQuery('.main-menu').addClass('mobile-menu');
            jQuery('.mobile-menu li.arrow-menu > a').addClass('mobile');

        } else {

            jQuery('.mobile-menu li.arrow-menu > a').removeClass('mobile');
            jQuery('.main-menu').removeClass('mobile-menu');
            jQuery(".drop-menu").removeAttr('style');
            jQuery("body").css("overflow-y", "visible");
            jQuery('.toggle').removeClass("active");

        }
    }
    checkbrowserwidth();

});


/*drop-menu show on hover*/


var $= jQuery;

$(document).ready(function () {
    $(window).resize(function () {

        if (jQuery("nav > .toggle").css("display") === "none") {
            $('.main-menu > li.arrow-menu').on('mouseenter', function () {
                $(this).find('.drop-menu').stop(true, true).delay(200).fadeIn(200);
            }).on('mouseleave', function () {
                $(this).find('.drop-menu').stop(true, true).fadeOut(200);
            });
        } else {
            $('.main-menu > li.arrow-menu').off('mouseenter mouseleave');
        }
    }).resize(); //to initialize the value
});


/**
 * CssUserAgent (cssua.js) v2.1.31
 * http://cssuseragent.org
 * 
 * Copyright (c)2006-2015 Stephen M. McKamey.
 * Licensed under The MIT License.
 */
var cssua=function(n,l,p){var q=/\s*([\-\w ]+)[\s\/\:]([\d_]+\b(?:[\-\._\/]\w+)*)/,r=/([\w\-\.]+[\s\/][v]?[\d_]+\b(?:[\-\._\/]\w+)*)/g,s=/\b(?:(blackberry\w*|bb10)|(rim tablet os))(?:\/(\d+\.\d+(?:\.\w+)*))?/,t=/\bsilk-accelerated=true\b/,u=/\bfluidapp\b/,v=/(\bwindows\b|\bmacintosh\b|\blinux\b|\bunix\b)/,w=/(\bandroid\b|\bipad\b|\bipod\b|\bwindows phone\b|\bwpdesktop\b|\bxblwp7\b|\bzunewp7\b|\bwindows ce\b|\bblackberry\w*|\bbb10\b|\brim tablet os\b|\bmeego|\bwebos\b|\bpalm|\bsymbian|\bj2me\b|\bdocomo\b|\bpda\b|\bchtml\b|\bmidp\b|\bcldc\b|\w*?mobile\w*?|\w*?phone\w*?)/,
x=/(\bxbox\b|\bplaystation\b|\bnintendo\s+\w+)/,k={parse:function(b,d){var a={};d&&(a.standalone=d);b=(""+b).toLowerCase();if(!b)return a;for(var c,e,g=b.split(/[()]/),f=0,k=g.length;f<k;f++)if(f%2){var m=g[f].split(";");c=0;for(e=m.length;c<e;c++)if(q.exec(m[c])){var h=RegExp.$1.split(" ").join("_"),l=RegExp.$2;if(!a[h]||parseFloat(a[h])<parseFloat(l))a[h]=l}}else if(m=g[f].match(r))for(c=0,e=m.length;c<e;c++)h=m[c].split(/[\/\s]+/),h.length&&"mozilla"!==h[0]&&(a[h[0].split(" ").join("_")]=h.slice(1).join("-"));
w.exec(b)?(a.mobile=RegExp.$1,s.exec(b)&&(delete a[a.mobile],a.blackberry=a.version||RegExp.$3||RegExp.$2||RegExp.$1,RegExp.$1?a.mobile="blackberry":"0.0.1"===a.version&&(a.blackberry="7.1.0.0"))):x.exec(b)?(a.game=RegExp.$1,c=a.game.split(" ").join("_"),a.version&&!a[c]&&(a[c]=a.version)):v.exec(b)&&(a.desktop=RegExp.$1);a.intel_mac_os_x?(a.mac_os_x=a.intel_mac_os_x.split("_").join("."),delete a.intel_mac_os_x):a.cpu_iphone_os?(a.ios=a.cpu_iphone_os.split("_").join("."),delete a.cpu_iphone_os):a.cpu_os?
(a.ios=a.cpu_os.split("_").join("."),delete a.cpu_os):"iphone"!==a.mobile||a.ios||(a.ios="1");a.opera&&a.version?(a.opera=a.version,delete a.blackberry):t.exec(b)?a.silk_accelerated=!0:u.exec(b)&&(a.fluidapp=a.version);a.edge&&(delete a.applewebkit,delete a.safari,delete a.chrome,delete a.android);if(a.applewebkit)a.webkit=a.applewebkit,delete a.applewebkit,a.opr&&(a.opera=a.opr,delete a.opr,delete a.chrome),a.safari&&(a.chrome||a.crios||a.fxios||a.opera||a.silk||a.fluidapp||a.phantomjs||a.mobile&&
!a.ios?(delete a.safari,a.vivaldi&&delete a.chrome):a.safari=a.version&&!a.rim_tablet_os?a.version:{419:"2.0.4",417:"2.0.3",416:"2.0.2",412:"2.0",312:"1.3",125:"1.2",85:"1.0"}[parseInt(a.safari,10)]||a.safari);else if(a.msie||a.trident)if(a.opera||(a.ie=a.msie||a.rv),delete a.msie,delete a.android,a.windows_phone_os)a.windows_phone=a.windows_phone_os,delete a.windows_phone_os;else{if("wpdesktop"===a.mobile||"xblwp7"===a.mobile||"zunewp7"===a.mobile)a.mobile="windows desktop",a.windows_phone=9>+a.ie?
"7.0":10>+a.ie?"7.5":"8.0",delete a.windows_nt}else if(a.gecko||a.firefox)a.gecko=a.rv;a.rv&&delete a.rv;a.version&&delete a.version;return a},format:function(b){var d="",a;for(a in b)if(a&&b.hasOwnProperty(a)){var c=a,e=b[a],c=c.split(".").join("-"),g=" ua-"+c;if("string"===typeof e){for(var e=e.split(" ").join("_").split(".").join("-"),f=e.indexOf("-");0<f;)g+=" ua-"+c+"-"+e.substring(0,f),f=e.indexOf("-",f+1);g+=" ua-"+c+"-"+e}d+=g}return d},encode:function(b){var d="",a;for(a in b)a&&b.hasOwnProperty(a)&&
(d&&(d+="\x26"),d+=encodeURIComponent(a)+"\x3d"+encodeURIComponent(b[a]));return d}};k.userAgent=k.ua=k.parse(l,p);l=k.format(k.ua)+" js";n.className=n.className?n.className.replace(/\bno-js\b/g,"")+l:l.substr(1);return k}(document.documentElement,navigator.userAgent,navigator.standalone);
