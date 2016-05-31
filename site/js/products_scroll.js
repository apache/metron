jQuery(document).ready(function() {
    var position;

    if (jQuery('.scroll-slider').length > 0) {
        jQuery(window).scroll(function() {
            var section_offset = jQuery('.scroll-slider').offset().top;
            var window_offset = (jQuery(window).scrollTop()) + 25;
            position = section_offset - window_offset;

        });
        jQuery('html').on('mousewheel', function(e) {
            scrollSlider();
        });
    }

    function scrollSlider() {
        var section_offset = jQuery('.scroll-slider').offset().top;
        var window_offset = (jQuery(window).scrollTop()) + 25;
        if ((section_offset < window_offset) && (jQuery('.scroll-slider').hasClass('screen-scroll')) && (position > -100)) {
            jQuery(window).scrollTop(((jQuery('.mask').offset().top) - 25).toFixed());
            jQuery('body').css({'overflow': 'hidden'});
            jQuery('.mask').addClass('fixed-mode');
            jQuery('.mask').delay(700).animate({scrollTop: '640px'}, {easing: 'swing', duration: 1000, complete:
                        function() {
                            jQuery('.mask').removeClass('fixed-mode');
                            jQuery('body').css({'overflow': 'scroll'});
                            jQuery('.scroll-slider').removeClass('screen-scroll').addClass('fixed');
                        }
            });
            if (jQuery('.scroll-slider').hasClass('fixed')) {
                return false;
            }
        } else if ((section_offset > window_offset) && (jQuery('.scroll-slider').hasClass('fixed'))) {
            jQuery(window).scrollTop(((jQuery('.mask').offset().top) - 25).toFixed(0));
            jQuery('body').css({'overflow': 'hidden'});
            jQuery('.mask').addClass('fixed-mode');
            jQuery('.mask').delay(700).animate({scrollTop: '-640px'}, {easing: 'swing', duration: 1000, complete:
                        function() {
                            jQuery('.mask').removeClass('fixed-mode');
                            jQuery('body').css({'overflow': 'scroll'});
                            jQuery('.scroll-slider').removeClass('fixed').addClass('screen-scroll');
                        }
            });
        }
    }
});

