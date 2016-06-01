jQuery(document).ready(function () {


    /*
     * Filter Tabs convert to select box on smaller width
     */
    jQuery('.tab-filter .toggle-tab').click(function () {
        jQuery(this).parents('.filter-tab').find('.tabsets').stop(true, true).toggle();
    });
    var window_width = jQuery(window).width();
    if (window_width < 768)
    {
        jQuery('.tab-filter .filter-btn li a').click(function () {
            var active_txt = jQuery(this).html();
            jQuery(this).parents('.filter-tab').find('.copy-text').html(active_txt);
            jQuery(this).parents('.filter-tab').find('.tabsets').stop(true, true).hide();
        });
    }
    else if (window_width > 767)
    {
        jQuery('.tabsets').removeAttr('style');
        jQuery('.tab-filter .filter-btn li a').click(function () {
            jQuery(this).parents('.filter-tab').find('.tabsets').stop(true, true).show();
        });
    }

    /*
     * Accordion on Downloads Page
     */

    jQuery('.downloads .accordion .opener').click(function () {
        jQuery(this).parents('.accordion').find('.accordion-content').stop(true, true).slideToggle(200);
        jQuery(this).parents('.accordion').find('.display').toggleClass('close');
    });
    /*
     * Tabs - Products Page
     */

    var tab_button = jQuery(".tab-list"),
            tab_container = jQuery(".tab-content");
    jQuery(".tab-list li:first-child > a").addClass('active');
    jQuery(".tab-content > div:first-child").css("display", "block");
    tab_button.find("a").click(function (e) {
        e.preventDefault();
        var button_index = jQuery(this).parent("li").index(".tab-list li");
        jQuery(this).addClass("active");
        tab_button.find("a").not(this).removeClass("active");
        tab_container.find(".tab:eq(" + button_index + ")").css("display", "block");
        tab_container.find(".tab").not(".tab:eq(" + button_index + ")").css("display", "none");
    });

    /*Background Image zoom effect on  button hover*/

    jQuery('.cta .button-default').hover(function () {
        jQuery(this).parents('.v-middle-wrapper').parent().find('.bg-img img').toggleClass('img-zoom');
    });
    /*indutries solutions tabs toggle on click*/
    jQuery(window).on("resize", function () {
        jQuery('.grid li .tab-logo').each(function () {
            var parentOffset = jQuery('.grid').offset().left;
            var tabOffset = jQuery(this).parent().offset().left;
            var leftMargin = tabOffset - parentOffset;
            var parent_width = jQuery(this).parents('.grid').width();
            jQuery(this).parents("li").find('.tabs-content').css("width", parent_width + "px"); /*Assign width to tab*/
            jQuery(this).parents("li").find('.tabs-content').css("margin-left", "-" + (leftMargin + 2) + "px"); /* Set tab position to start*/
            var tab_height = jQuery(this).parents("li").find(".tabs-content").outerHeight();
            jQuery(this).parents("li").find('.tabs-content').attr("data-height", tab_height);
            jQuery(this).parents("li").find('.tabs-content').css("display", "none");
            jQuery('.grid > ul > li').css("margin-bottom", "0px");
            jQuery('.grid li').removeClass("current");
        });
    }).resize();
    /*closes tabs on click anywhere on document*/
    jQuery(document).click(function () {
        jQuery(".grid > ul > li .tab-logo").each(function () {
            jQuery(this).parents("li").find('.tabs-content').stop(true, true).slideUp(300);
            jQuery(this).parents("li").stop(true, true).animate({"margin-bottom": "0px"}, 300);
            jQuery(".grid > ul > li").removeClass("current");

        });
    });
    /*Grid tabs toggle on click*/

    jQuery(document).on("click", ".grid > ul > li .tab-logo, .customer-search", function (event) {
        event.stopPropagation();

        /* Select filter for customer featured post */
        jQuery('.grid > ul > li .tab-logo').each(function () {
            var parentOffset = jQuery('.grid').offset().left;
            var tabOffset = jQuery(this).parent().offset().left;
            var leftMargin = tabOffset - parentOffset;
            var parent_width = jQuery(this).parents('.grid').width();
            jQuery(this).parents("li").find('.tabs-content').css("width", parent_width + "px"); /*Assign width to tab*/
            jQuery(this).parents("li").find('.tabs-content').css("margin-left", "-" + (leftMargin + 2) + "px"); /* Set tab position to start*/
            var tab_height = jQuery(this).parents("li").find(".tabs-content").outerHeight();
            jQuery(this).parents("li").find('.tabs-content').attr("data-height", tab_height);
        });

        var prevIndex = jQuery(".grid > ul > li.current").index();
        var currentIndex = jQuery(this).parents("li").index();
        jQuery(".grid > ul > li").not(jQuery(this).parents("li")).removeClass("current");
        var tabHeight = parseInt(jQuery(this).parents("li").find(".tabs-content").attr("data-height"));
        if (prevIndex === currentIndex) {
            jQuery('.grid > ul > li .tab-logo').parents("li").find('.tabs-content').stop(true, true).slideUp(300);
            jQuery('.grid > ul > li .tab-logo').parents("li").stop(true, true).animate({"margin-bottom": "0px"}, 300);
            jQuery(".grid > ul > li:eq(" + currentIndex + ")").removeClass("current");
        }
        else if (prevIndex >= 0) {
            jQuery(".grid > ul > li:eq(" + prevIndex + ")").stop(true, true).animate({"margin-bottom": "0px"}, 300, function () {
                jQuery(".grid > ul > li:eq(" + currentIndex + ")").not('.no-hover').addClass("current");
                jQuery(".grid > ul > li:eq(" + currentIndex + ")").find('.tabs-content').stop(true, true).slideToggle(300);
                jQuery(".grid > ul > li:eq(" + currentIndex + ")").stop(true, true).animate({"margin-bottom": (tabHeight + 40) + "px"});
            });
            jQuery(".grid > ul > li:eq(" + prevIndex + ")").find(".tabs-content").stop(true, true).slideUp(300);
        } else if (prevIndex < 0) {
            jQuery(this).parents("li").not('.no-hover').addClass("current");
            jQuery(this).parents("li").find('.tabs-content').stop(true, true).slideToggle(300);
            jQuery(this).parents("li").stop(true, true).animate({"margin-bottom": (tabHeight + 40) + "px"});
        }
    });
    /*cta banner and second level banner img responsive */

    jQuery(window).on("resize", function () {
        var banner_image = jQuery('.bg-img').find('img');
        var window_width = jQuery(window).width();
        if (window_width >= 1400) {
            banner_image.css({"width": "100%", "left": "0px"});
        }
        if (window_width < 1400) {
            var left_margin = (1400 - window_width) / 2;
            banner_image.css({"width": "1400px", "left": "-" + left_margin + "px", "max-width": "inherit"});
        }

        /*feature box img responsive */
        jQuery('.feature-bg').each(function () {
            var feat_img = jQuery(this).find('img');
            var feat_box_wid = jQuery(this).width();
            var feat_img_wid = jQuery(this).find('img').width();
            var feat_left = (feat_img_wid - feat_box_wid) / 2;
            if (window_width >= 1400)
            {
                feat_img.css({"width": "100%", "left": "0px"});
            }
            if (window_width < 1400 & window_width >= 768)
            {
                feat_img.css({'width': 'auto', 'height': 'auto', 'left': '-' + feat_left + 'px', 'max-width': 'inherit'});
            }
            if (window_width < 768)
            {
                feat_img.css({'width': '100%', 'height': 'auto', 'left': '-' + feat_left + 'px', 'max-width': 'inherit'});
            }
        });

    }).resize();


});
/*assign equal height */

/*
 * Filter Tabs convert to select box on smaller width
 */
jQuery(window).on("resize", function () {
    var window_width = jQuery(window).width();
    var window_height = jQuery(window).height();
    if (window_width < 768)
    {
        jQuery('.tab-filter .filter-btn li a').click(function () {
            var active_txt = jQuery(this).html();
            jQuery(this).parents('.filter-tab').find('.copy-text').html(active_txt);
            jQuery(this).parents('.filter-tab').find('.tabsets').stop(true, true).hide();
        });
    }
    else if (window_width > 767)
    {
        jQuery('.tabsets').removeAttr('style');
        jQuery('.tab-filter .filter-btn li a').click(function () {
            jQuery(this).parents('.filter-tab').find('.tabsets').stop(true, true).show();
        });
    }


}).resize();

jQuery(window).load(function () {
    jQuery.fn.equalHeight = function () {
        var maxHeight = 0;
        return this.each(function (index, box) {
            var boxHeight = jQuery(box).height();
            maxHeight = Math.max(maxHeight, boxHeight);
        }).height(maxHeight);
    };
    jQuery.fn.equalOuterHeight = function () {
        var maxHeight = 0;
        return this.each(function (index, box) {
            var boxHeight = jQuery(box).height();
            maxHeight = Math.max(maxHeight, boxHeight);
        }).outerHeight(maxHeight);
    };

    jQuery(window).on('resize', function () {
        jQuery('.tabular-box .row-data .col-four,.tabular-box .row-data .col-three,.grid-section .col-three,.grid-text .col-inner .col-three,.col-inner .learning-menu, .col-inner .same-height, .comming-soon, .hdp-hadoop .item-content, .hdf-hadoop .item-content').css('height', 'auto');
        var window_width = jQuery(window).width();
        jQuery('.hdp-hadoop .item-content, .hdf-hadoop .item-content').equalHeight();
        if (window_width > 767) {
            jQuery('.grid-section .col-three').equalHeight();
            jQuery('.col-inner .learning-menu').equalHeight();
            jQuery('.comming-soon').equalHeight();
            //jQuery('.sub-tab-content .col-two').equalHeight();
            //jQuery('.blue-boxes .col-two').equalHeight();
            jQuery('.col-inner .same-height').equalHeight();
            jQuery('.filter-press-release .filter-container > div').equalHeight();
        } else if (window_width < 768) {
            jQuery('.grid-section .col-three').css("height", "auto");
            jQuery('.col-inner .learning-menu').css("height", "auto");
            jQuery('.comming-soon').css("height", "auto");
            //jQuery('.sub-tab-content .col-two').css("height", "auto");
            //jQuery('.blue-boxes .col-two').css("height", "auto");
            jQuery('.col-inner .same-height').css("height", "auto");
        }
        if (window_width > 666) {
            jQuery('.tabular-box .row-data').each(function () {
                jQuery(this).find('.col-four').equalHeight();
                jQuery(this).find('.col-three').equalHeight();
            });
        }
        else {
            jQuery('.tabular-box .row-data').each(function () {
                jQuery(this).find('.col-four').css("height", "auto");
                jQuery(this).find('.col-three').css("height", "auto");
            });
        }
        jQuery('.grid-text .col-inner').each(function () {
            if (window_width > 767) {
                jQuery(this).find('.col-three').equalHeight();
            } else if (window_width < 768) {
                jQuery(this).find('.col-three').css("height", "auto");
            }
        });
    }).resize();


    /**
     *  Add isotop filter on video and press release section
     */
    var activeClass = 'active';
    jQuery('.isotope-filter').each(function () {
        var holder = jQuery(this),
                filterLinks = holder.find('.filter-btn a'),
                filterButtons = holder.find('.filter-btn li'),
                container = holder.find('.filter-elements');
//                    items = container.children(),
//                    btn = holder.find('.load-more'),
//                    ajaxbusy;

        container.isotope({
            itemSelector: '.item',
            layoutMode: 'fitRows'
        });

        filterLinks.click(function (e) {
            var link = jQuery(this),
                    linkButton = jQuery(this).parents("li"),
                    filter = link.data('filter');
            e.preventDefault();
            refreshActiveClass(link);
            refreshActiveClass(linkButton);
            container.isotope({
                filter: filter,
                layoutMode: 'fitRows'
            });

            // Hide or show no results message
            if (container.data('isotope').filteredItems.length == 0) {
                holder.find('.isotope-no-results').show();
            } else {
                holder.find('.isotope-no-results').hide();
            }
        });

        var refreshActiveClass = function (link) {
            filterLinks.removeClass(activeClass);
            filterButtons.removeClass(activeClass);
            link.addClass(activeClass);
        };


    });

    var holder = jQuery('.filter-drop'),
            filterLinks = holder.find('input'),
            container = jQuery('.filter-rows'),
            items = container.children('.item');

    container.isotope({
        itemSelector: items,
        layoutMode: 'fitRows'
    });

    filterLinks.click(function (e) {
        var link = jQuery(this),
                filter = link.data('filter');
        container.isotope({
            filter: filter,
            layoutMode: 'fitRows'
        });
    });

});



jQuery(document).ready(function () {
    /* Remove class "overlay" from footer on window load */
    jQuery(window).load(function () {
        jQuery('.footer-nav').removeClass('overlay');
    });

    /**
     *  footer expanded overlay close on click footer bar
     */
    jQuery(document).on('click', '.footer-nav', function () {
        //jQuery('.footer-overlay').fadeOut();
        jQuery('.footer-menu.open').removeClass('open');
        jQuery('.footer-nav, .newsletter').removeClass('overlay');
        //jQuery("body").css("overflow-y", "auto");
        jQuery('body').removeClass('overflow-fix  hw-footer-overlay');
    });

    /**
     *  footer expanded overlay close on click close button
     */
    jQuery(document).on('click', '.f-close-btn', function () {
        //jQuery('.footer-overlay, .newsletter-overlay').fadeOut();
        jQuery(this).parents().find('.footer-menu.open, .newsletter.open').removeClass('open');
        jQuery(this).parents().find('.footer-nav').removeClass('overlay');
        jQuery(this).parents().find('.footer-nav').removeClass('news-fix');
        jQuery('.marketo-form-embed').validationEngine('hideAll');
//        jQuery("body").css("overflow-y", "auto");
        jQuery('body').removeClass('overflow-fix hw-search-overlay hw-footer-overlay hw-newsletter-overlay');
    });

    jQuery('.newsletter').click(function (a) {
        a.stopPropagation();
    });

    /*
     * Common code for resize of overlays
     */
    jQuery(window).on("resize", function () {
        var overlay_hei = jQuery(window).height() - (32);
        var menu_hei = overlay_hei - 90;
        var window_width = jQuery(window).width();
        jQuery('.footer-expand-menu').height(menu_hei);
        /* To remove footer from overlay & expand overlay to 100vh */
        if (window_width > 767) {

            if (jQuery('body').hasClass('overflow-fix')) {
                jQuery('.footer-nav').addClass('overlay');
//                jQuery('.footer-nav').css({'z-index': '11'});
            }

            jQuery('.search-box').height(overlay_hei);

            jQuery('.footer-overlay').height(overlay_hei);
            //jQuery('.footer-expand-menu').height(menu_hei);

            jQuery('.nws-box').height(overlay_hei);
        } else if (window_width < 768) {

            if (jQuery('body').hasClass('overflow-fix')) {
//                jQuery('.footer-nav').css({'z-index': '9'});
            }

            jQuery('.footer-nav').removeClass('overlay');

            jQuery('.search-box').height(jQuery(window).height());

            jQuery('.footer-overlay').height(jQuery(window).height());

            jQuery('.nws-box').height(jQuery(window).height());
        }

//        jQuery('.footer-nav').removeClass('overlay');  
//        if (jQuery('.footer-overlay').css('display')  == "none") {
//            jQuery('.footer-nav').removeClass('overlay');
//        }
    }).resize();

    /*
     *  top search  overlay 
     */
    jQuery('.top-menu .search, .fix-search').on('click', function () {
        //jQuery('.search-overlay').fadeIn();
        jQuery(this).parents().find('.footer-nav').addClass('overlay');
        jQuery('body').addClass('overflow-fix hw-search-overlay');

        //To focus on input field
        setTimeout(function () {
            jQuery('.top-search-input').focus();
        }, 100);
    });

    /*
     *  footer expanded overlay
     */
    jQuery('.footer-menu').on('click', function (event) {
        event.stopPropagation();
        jQuery(this).toggleClass('open');
        jQuery(this).parents().find('.footer-nav').toggleClass('overlay');
        jQuery('.footer-overlay').toggle();
//        jQuery("body").css("overflow-y", "hidden");
        if (jQuery(this).hasClass('open')) {
            jQuery('body').addClass('overflow-fix hw-footer-overlay');
        }
        else {
            jQuery('body').removeClass('overflow-fix  hw-footer-overlay');
        }
    });

    /*
     *  footer newsletter overlay 
     */
    jQuery('.newsletter a').click(function (ev) {
        ev.preventDefault();
    });
    jQuery('.newsletter').on('click', function () {
        jQuery(this).addClass('open');
        jQuery(this).parents().find('.footer-nav').addClass('overlay');
        //jQuery('.newsletter-overlay').fadeIn();
        jQuery('body').addClass('overflow-fix hw-newsletter-overlay');

        //To focus on input field
        setTimeout(function () {
            jQuery('.top-search-input').focus();
            jQuery('.top-search-input').focus(function () {
                jQuery(this).attr('placeholder', jQuery(this).data('placeholder'));
            });
        }, 100);

    });
});


jQuery(document).ready(function () {
    /**
     * 
     * Video overlay
     */

    jQuery("body").append("<div class='outer-box'>" +
            "<div class='overlay-form'>" +
            "<div id='video'>" +
            "<span id='close-video' type='button' class='close-video'>" + "close" +
            "<img src='/wp-content/themes/hortonworks/images/overlay-close.png' alt='Close button'/>" +
            "</span>" +
            "<iframe src=''></iframe>" +
            "</div>" +
            "</div>" +
            "<div class='overlay-elephant-logo'>" +
            "<img src='/wp-content/themes/hortonworks/images/logo1.png' alt='Close button'/>" +
            "</div>" +
            "</div>");
    jQuery('.video-link').on('click', function (e) {
        var url = jQuery(this).attr('data');
        jQuery('#video').find('iframe').attr('src', url).addClass('test');
        //jQuery('.outer-box').fadeIn();
        jQuery('body').css({'overflow': 'hidden'}).addClass('hw-video-overlay');
        e.preventDefault();
    });
    jQuery('#close-video').click(function () {
        jQuery('#video').find('iframe').attr('src', '');
        jQuery('body').css({'overflow': 'scroll'}).removeClass('hw-video-overlay');
    });
    jQuery('.outer-box').click(function () {
        jQuery('#video').find('iframe').attr('src', '');
        jQuery('body').css({'overflow': 'scroll'}).removeClass('hw-video-overlay');
    });

});

jQuery(window).resize(function () {
    var window_width = jQuery(window).width();
    var window_height = jQuery(window).height();

    var video_width = window_width / 1.5;
    var video_height = video_width / 1.77;
    var video_top_margin = (window_height - video_height) / 2 + (10);
    var video_left_margin = (window_width - video_width) / 2;

    jQuery('#video > iframe').attr('width', video_width + 'px');
    jQuery('#video > iframe').attr('height', video_height + 'px');
    jQuery('#video').css({'margin-top': video_top_margin + 'px', 'padding': '0px'});


    /**
     * 
     * placeholder text show after focus in ie
     */
//
//    var dataPlaceholders = document.querySelectorAll("input[placeholder]"),
//            l = dataPlaceholders.length,
//            // Set caret at the beginning of the input
//            setCaret = function (evt) {
//                if (this.value === this.getAttribute("data-placeholder")) {
//                    this.setSelectionRange(0, 0);
//                    evt.preventDefault();
//                    evt.stopPropagation();
//                    return false;
//                }
//            },
//            // Clear placeholder value at user input
//            clearPlaceholder = function (evt) {
//                if (!(evt.shiftKey && evt.keyCode === 16) && evt.keyCode !== 9) {
//                    if (this.value === this.getAttribute("data-placeholder")) {
//                        this.value = "";
//                        this.className = "top-search-input";
//                        if (this.getAttribute("data-type") === "password") {
//                            this.type = "password";
//                        }
//                    }
//                }
//            },
//            restorePlaceHolder = function () {
//                if (this.value.length === 0) {
//                    this.value = this.getAttribute("data-placeholder");
//                    setCaret.apply(this, arguments);
//                    this.className = "top-search-input";
//                    if (this.type === "password") {
//                        this.type = "text";
//                    }
//                }
//            },
//            clearPlaceholderAtSubmit = function (evt) {
//                for (var i = 0, placeholder; i < l; i++) {
//                    placeholder = dataPlaceholders[i];
//                    if (placeholder.value === placeholder.getAttribute("data-placeholder")) {
//                        placeholder.value = "";
//                    }
//                }
//            };
//
//    for (var i = 0, placeholder, placeholderVal; i < l; i++) {
//        placeholder = dataPlaceholders[i];
//        placeholderVal = placeholder.getAttribute("placeholder");
//        placeholder.setAttribute("data-placeholder", placeholderVal);
//        placeholder.removeAttribute("placeholder");
//
//        if (placeholder.value.length === 0) {
//            placeholder.value = placeholderVal;
//            if (placeholder.type === "password") {
//                placeholder.type = "text";
//            }
//        }
//        else {
//            placeholder.className = "top-search-input";
//        }
//
//        // Apply events for placeholder handling         
//        placeholder.addEventListener("focus", setCaret, false);
//        placeholder.addEventListener("drop", setCaret, false);
//        placeholder.addEventListener("click", setCaret, false);
//        placeholder.addEventListener("keydown", clearPlaceholder, false);
//        placeholder.addEventListener("keyup", restorePlaceHolder, false);
//        placeholder.addEventListener("blur", restorePlaceHolder, false);
//
//        // Clear all default placeholder values from the form at submit
//        placeholder.form.addEventListener("submit", clearPlaceholderAtSubmit, false);
//    }
});


/**
 * 
 * service page tables responsive
 */

$(document).ready(function () {
    var switched = false;
    var updateTables = function () {
        if (($(window).width() < 767) && !switched) {
            switched = true;
            $(".service-table").each(function (i, element) {
                splitTable($(element));
            });
            return true;
        }
        else if (switched && ($(window).width() > 767)) {
            switched = false;
            $(".service-table").each(function (i, element) {
                unsplitTable($(element));
            });
        }
    };
    $(window).load(updateTables);
    $(window).bind("resize", updateTables);
    function splitTable(original)
    {
        original.wrap("<div class='table-wrapper' />");
        var copy = original.clone();
        copy.find(".row-head .cell-four:not(:first-of-type), .row-data .cell-four:not(:first-of-type),.row-head .cell-three:not(:first-of-type), .row-data .cell-three:not(:first-of-type)").css("display", "none");
        copy.removeClass("responsive");
        original.closest(".table-wrapper").append(copy);
        copy.wrap("<div class='pinned' />");
        original.wrap("<div class='scrollable' />");
    }
    function unsplitTable(original) {
        original.closest(".table-wrapper").find(".pinned").remove();
        original.unwrap();
        original.unwrap();
    }}
);
