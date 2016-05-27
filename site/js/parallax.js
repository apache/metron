$(document).ready(function() {
    // Cache the Window object
    $window = $(window);

    // var windowWidth = jQuery(window).width();
    if ($('.ua-mobile').length == 0) {
        $('.parallax').each(function() {
            var $bgobj = $(this); // assigning the object

            $(window).on('scroll resize', function() {
                // Get top, height, windowHeight, and pos
                var top = $bgobj.offset().top;
                var height = $bgobj.outerHeight();
                var windowHeight = $window.height();
                var pos = $window.scrollTop();
                // Do nothing if image not in viewport
//                if (top + height < pos || top > pos + windowHeight) {
//                    return;
//                }
                var win_wid = jQuery(window).width();
                var win_hei = jQuery(window).height();
                // Find new position
                if (win_wid >= 1401)
                {
                    if (win_hei > 1300) {
                        var toppos = (($(window).height()) - ($('.win-height').height()));
                        var yPos = ($bgobj.offset().top - pos) / $bgobj.data('speed');
                        $bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                    }
                    else {
                        var toppos = (($(window).height()) - ($('.win-height').height()));
                        var yPos = ($bgobj.offset().top - pos) / $bgobj.data('speed');
                        //$bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                        //console.log($bgobj.offset().top);
                        //console.log(pos);
                        //console.log(yPos);
                        $bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                    }
                }
                else if ((win_wid >= 1200) && (win_wid <= 1400)) {
                    var toppos = (($(window).height()) - ($('.win-height').height()));
                    var yPos = ($bgobj.offset().top - (pos + (toppos + 50))) / $bgobj.data('speed');
                    $bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                }
                else if ((win_wid >= 992) && (win_wid <= 1199)) {
                    var toppos = (($(window).height()) - ($('.win-height').height()));
                    var yPos = ($bgobj.offset().top - (pos + 50)) / $bgobj.data('speed');
                    $bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                }
                else if ((win_wid >= 768) && (win_wid <= 991)) {
                    var toppos = (($(window).height()) - ($('.win-height').height()));
                    var yPos = ($bgobj.offset().top - (pos + 50)) / $bgobj.data('speed');
                    $bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                }
                else if ((win_wid >= 596) && (win_wid <= 767)) {
                    var toppos = (($(window).height()) - ($('.win-height').height()));
                    var yPos = ($bgobj.offset().top - (pos + 50)) / $bgobj.data('speed');
                    $bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                }
                else if ((win_wid >= 300) && (win_wid <= 595)) {
                    var toppos = (($(window).height()) - ($('.win-height').height()));
                    var yPos = ($bgobj.offset().top - pos) / $bgobj.data('speed');
                    $bgobj.css({backgroundPosition: '50% ' + yPos + 'px'});
                }
                // Move the background

            }); // window scroll Ends
        });
    }
    $window.trigger('resize');
});
/* 
 * Create HTML5 elements for IE's sake
 */

document.createElement("article");
document.createElement("section");