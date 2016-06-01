var $ = jQuery;

$(document).ready(function () {
    /*
     * Trigger top-form from "Contact Form/Contact Us"
     */
//    var selector = $('.get-started .v-middle:eq(2)');
//    selector.children('p').find('a').addClass('trigger-top-form');
//    function triggerTopForm() {
//        var selector = $('.get-started .v-middle a');
//        if (selector > -1) {
//            for (i = 0; i < 3; i++) {
//                console.log(22);
//                var innerText = selector[i].innerText;
//                var matchText_0 = "CONTACT FORM";
//                var matchText_1 = "INQUIRIES";
//                if ((innerText.indexOf(matchText_0) > -1) || (innerText.indexOf(matchText_1) > -1)) {
//                    var triggerForm = selector[i];
//                    $(triggerForm).addClass('trigger-top-form');
//                }
//            }
//        }
//    }
//    triggerTopForm();

    /*
     * Top-form UI/UX effects
     */
    $('.trigger-top-form, .top-form-container .close-btn').on('click', function () {
        if ($('body').hasClass('form-active')) {
            $('body').removeClass('form-active');
            $('.marketo-form-embed').validationEngine('hideAll');
        }
        else {
            $('body').addClass('form-active');
        }
    });


    /*
     *  Top form Material design UI/UX effects
     */
    $(document).on('change', '.top-form select, .marketo-form-embed select', function () {
        $(this).addClass('used').siblings('.placeHolder').addClass('placeholderfix');
    });

    $(document).on('blur', 'input, textarea', function () {
        if ($(this).val())
            $(this).addClass('used');
        else
            $(this).removeClass('used');
    });

    /*
     * SelectBoxIt Initialization for Select Lists
     */
    $('.top-form select, .marketo-form-embed select, .grid-customers select').selectBoxIt();

    /*
     * Listen to change/create events to make sure our validation on select elements works with SelectBoxIt
     */
    $('.marketo-form-embed select').on('change create', function (ev, obj) {
        obj.dropdown.attr('value', $(this).val());
    });


    /*
     * Test code for Second-level-nav
     */
    jQuery(window).load(function () {
        jQuery(window).on("ready, resize", function () {
            if (jQuery('.fixed-links').length > 0) {
                var stickyOffset = '';
                if (jQuery('.fixed-links')) {
                    var sticky = jQuery('.fixed-links');

                    /* Sticky height fix on resonsive */
                    sticky.parent().css({'min-height': 'initial'});
                    //console.log(sticky.parent());
                    //console.log(sticky.parent().outerHeight());
                    var stickyHeight = sticky.parent().outerHeight();
                    sticky.parent().css({'min-height': stickyHeight});

                    sticky.removeClass('stick');
                    stickyOffset = jQuery('.fixed-links').offset().top;


                    var scroll = jQuery(window).scrollTop();
                    if (scroll >= stickyOffset) {
                        sticky.addClass('stick');
                    }
                    else {
                        sticky.removeClass('stick');
                    }
                }

                /* Remove sticky on responsive() & apply it back on scroll */
                jQuery(window).scroll(function () {
                    var scroll = jQuery(window).scrollTop();
                    if (scroll >= stickyOffset) {
                        sticky.addClass('stick');
                    }
                    else {
                        sticky.removeClass('stick');
                    }
                });
            }



            /*
             * Active links on page ready
             */
            jQuery(".fixed-links li").siblings('li').find('li.active').removeClass("active");
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
                   else {
                       currLink.parent().removeClass("active");
                   }
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
        }).resize();
    });
});