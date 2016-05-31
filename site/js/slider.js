jQuery(document).ready(function () {
    /* We slider*/
    init();
    pageNav();
    navigation();
    next();
    prev();
    swipeTouch();
    /* Multi section slider*/
    mInit();
    mSlideNav();
    mCheckNav();
    mSlideNext();
    mSlidePrev();
    mSwipeTouch();
});
jQuery(window).resize(function () {
    /* We slider*/
    init();
    /* Multi section slider*/
    mInit();
    var window_width = jQuery(window).width();
    if (jQuery(".multi-section-slider").length) {
        if (window_width < 640) {
            jQuery("#webcast-slider .multi-section-slider").attr("data-items", "1");
        } else {
            jQuery("#webcast-slider .multi-section-slider").attr("data-items", "2");
        }
        if (window_width > 1200) {
            jQuery(".partner-block .multi-section-slider").attr("data-items", "5");
        } else if (window_width < 1200 && window_width > 1023) {
            jQuery(".partner-block .multi-section-slider").attr("data-items", "4");
        } else if (window_width < 1024 && window_width > 767) {
            jQuery(".partner-block .multi-section-slider").attr("data-items", "3");
        } else if (window_width < 768 && window_width > 480) {
            jQuery(".partner-block .multi-section-slider").attr("data-items", "2");
        } else if (window_width < 481) {
            jQuery(".partner-block .multi-section-slider").attr("data-items", "1");
        }
        if (window_width > 767) {
            jQuery(".video-carousel .multi-section-slider").attr("data-items", "3");
        } else if (window_width < 768 && window_width > 595) {
            jQuery(".video-carousel .multi-section-slider").attr("data-items", "1");
        } else if (window_width < 596) {
            jQuery(".video-carousel .multi-section-slider").attr("data-items", "1");
        }
    }
});
jQuery(window).on('load', function () {
    jQuery(window).on('resize', function () {
        /* To imitate background image dimensions on real img */
        if (jQuery('.we-slider .item').length) {
            jQuery('.we-slider .bg-img, .we-slider .slide-bg').each(function () {
                var parent_height = jQuery(this).height(),
                        parent_width = jQuery(this).width(),
                        image = jQuery(this).children("img"),
                        img_width = image.get(0).naturalWidth,
                        img_height = image.get(0).naturalHeight,
                        ratio = img_width / img_height;
                image.css({"height": "auto", "width": "100%", "max-width": "100%", "left": "0", "top": "0"});
                if (parent_height > image.height()) {
                    var new_width = parent_height * ratio,
                            left = new_width - parent_width,
                            new_left = left / 2;
                    image.css({"height": "100%", "width": new_width + "px", "max-width": new_width + "px", "left": "-" + new_left + "px", "top": "0"});
                } else if (parent_width > image.width() && parent_height < image.height()) {
                    var new_height = parent_width / ratio,
                            top = new_height - parent_height,
                            new_top = top / 2;
                    image.css({"height": new_height + "px", "width": "100%", "max-width": "100%", "left": "0", "top": "-" + new_top + "px"});
                }
            });
        }
        if (jQuery('.multi-section-slider .item').length) {
            jQuery('.multi-section-slider .bg-img').each(function () {
                var parent_height = jQuery(this).height(),
                        parent_width = jQuery(this).width(),
                        image = jQuery(this).children("img"),
                        img_width = image.get(0).naturalWidth,
                        img_height = image.get(0).naturalHeight,
                        ratio = img_width / img_height;
                image.css({"height": "auto", "width": "100%", "max-width": "100%", "left": "0", "top": "0"});
                if (parent_height > image.height()) {
                    var new_width = parent_height * ratio,
                            left = new_width - parent_width,
                            new_left = left / 2;
                    image.css({"height": "100%", "width": new_width + "px", "max-width": new_width + "px", "left": "-" + new_left + "px", "top": "0"});
                } else if (parent_width > image.width() && parent_height < image.height()) {
                    var new_height = parent_width / ratio,
                            top = new_height - parent_height,
                            new_top = top / 2;
                    image.css({"height": new_height + "px", "width": "100%", "max-width": "100%", "left": "0", "top": "-" + new_top + "px"});
                }
            });
        }

    }).resize();
});
/* Global Variable*/
var windowWidth,
        parentWidth,
        nextbtn,
        prevbtn,
        slider,
        item,
        itemLength,
        activeItem,
        activeIndex,
        leftValue,
        initItem;
initItem = 0;
activeIndex = 0;

/* To initialize or reset Function on document ready */

function init() {
    jQuery(".we-slider").each(function () {
        windowWidth = jQuery(window).width();
        parentWidth = jQuery(this).width();
        prevbtn = jQuery(this).find(".slider-nav .prev");
        nextbtn = jQuery(this).find(".slider-nav .next");
        slider = jQuery(this).find(".slider");
        itemLength = jQuery(this).find(".slider .item").size();
        /* set width of slide item */
        jQuery(this).find(".slider .item").css("width", parentWidth + "px");
        jQuery(this).find(".slider-nav .we-navs").empty();
        var i = 0;
        for (i; i < itemLength; i++) {
            jQuery(this).find(".slider-nav .we-navs").append("<span class=''></span>");
        }
        slider.css("margin-left", "0px");
        jQuery(this).find(".slider-nav .we-navs span").not(".slider-nav .we-navs .span:eq(0)").removeClass("active");
        jQuery(this).find(".slider-nav .we-navs span:eq(0)").addClass("active");
        prevbtn.css({"display": "none", "opacity": "0"});
        nextbtn.css("display", "block");
    });
}

/* To operate slider with we-nav(dots) */

function pageNav() {
    jQuery(document).on("click", ".we-slider .slider-nav span", function () {
        parentWidth = jQuery(this).parents(".we-slider").width();
        slider = jQuery(this).parents(".we-slider").find(".slider");
        jQuery(this).addClass("active");
        jQuery(this).parents(".we-slider").find(".slider-nav ").find("span").not(jQuery(this)).removeClass("active");
        activeIndex = jQuery(this).index();
        leftValue = parentWidth * activeIndex;
        slider.stop(true, true).animate({"margin-left": "-" + leftValue + "px"}, 600);
    });
}

/* To operate slider with next button */

function next() {
    jQuery(".we-slider .slider-nav").find(".next").click(function () {
        activeIndex = jQuery(this).parents(".we-slider").find(".slider-nav").find("span.active").index();
        parentWidth = jQuery(this).parents(".we-slider").width();
        slider = jQuery(this).parents(".we-slider").find(".slider");
        initItem = activeIndex;
        initItem++;
        leftValue = parentWidth * initItem;
        slider.stop(true, true).animate({"margin-left": "-" + leftValue + "px"}, 600);
        jQuery(this).parents(".we-slider").find(".slider-nav .we-navs span").not(".slider-nav .we-navs span:eq(" + initItem + ")").removeClass("active");
        jQuery(this).parents(".we-slider").find(".slider-nav .we-navs span:eq(" + initItem + ")").addClass("active");
    });
}

/* To operate slider with prev button */

function prev() {
    jQuery(".we-slider .slider-nav").find(".prev").click(function () {
        activeIndex = jQuery(this).parents(".we-slider").find(".slider-nav").find("span.active").index();
        parentWidth = jQuery(this).parents(".we-slider").width();
        slider = jQuery(this).parents(".we-slider").find(".slider");
        initItem = activeIndex;
        initItem--;
        leftValue = parentWidth * initItem;
        slider.stop(true, true).animate({"margin-left": "-" + leftValue + "px"}, 600);
        jQuery(this).parents(".we-slider").find(".slider-nav .we-navs span:eq(" + initItem + ")").addClass("active");
        jQuery(this).parents(".we-slider").find(".slider-nav .we-navs span").not(".slider-nav .we-navs span:eq(" + initItem + ")").removeClass("active");
    });
}

/* to check and hide/show prevbtn and nextbtn*/

function navigation() {
    jQuery(document).on("click", ".we-slider span, .we-slider .prev, .we-slider .next", function () {
        activeIndex = jQuery(this).parents(".we-slider").find(".slider-nav").find("span.active").index();
        prevbtn = jQuery(this).parents(".we-slider").find(".slider-nav").find(".prev");
        nextbtn = jQuery(this).parents(".we-slider").find(".slider-nav").find(".next");
        itemLength = jQuery(this).parents(".we-slider").find(".slider .item").size();
        if (activeIndex === 0) {
            prevbtn.animate({"opacity": "0"}, 150);
            nextbtn.animate({"opacity": "1"}, 100);
            nextbtn.css({"display": "block"});
            setTimeout(function () {
                prevbtn.css({"display": "none"});
            }, 150);
        } else if (activeIndex >= (itemLength - 1)) {
            prevbtn.animate({"opacity": "1"}, 100);
            nextbtn.animate({"opacity": "0"}, 150);
            prevbtn.css({"display": "block"});
            setTimeout(function () {
                nextbtn.css({"display": "none"});
            }, 150);
        } else if (activeIndex === 0 && itemLength == 1) {
            prevbtn.css({"display": "none"});
            nextbtn.css({"display": "none"});
        } else {
            prevbtn.animate({"opacity": "1"}, 100);
            nextbtn.animate({"opacity": "1"}, 100);
            nextbtn.css({"display": "block"});
            prevbtn.css({"display": "block"});

        }
    });
}

/*
 * multi-section-slider 
 */

/* Global Variables */
var mParentWidth,
        mNextbtn,
        mPrevbtn,
        mSlider,
        mItem,
        mItemLength,
        mActiveItem,
        mActiveIndex,
        mLeftValue,
        mInitItem,
        mViewItem,
        mItemsWidth,
        mItemWidth,
        mTotalNav;
mInitItem = 0;
mActiveIndex = 0;

/* To initialize or reset Function on document ready */

function mInit() {
    jQuery(".multi-section-slider").each(function () {
        mViewItem = jQuery(this).attr("data-items");
        mParentWidth = jQuery(this).width();
        mItem = jQuery(this).find(".item");
        mItemLength = jQuery(this).find(".item").size();
        mItem.css("width", (mParentWidth / mViewItem) + "px");
        mSlider = jQuery(this).find(".slider");
        mNextbtn = jQuery(this).find(".slider-nav .next");
        mPrevbtn = jQuery(this).find(".slider-nav .prev");
        mTotalNav = Math.ceil(mItemLength / mViewItem);
        jQuery(this).find(".slider-nav .we-navs").empty();
        for (i = 0; i < mTotalNav; i++) {
            jQuery(this).find(".slider-nav .we-navs").append("<span class=''></span>");
        }
        mSlider.css("margin-left", "0px");
        jQuery(this).find(".slider-nav .we-navs span").not(".slider-nav .we-navs .span:eq(0)").removeClass("active");
        jQuery(this).find(".slider-nav .we-navs span:eq(0)").addClass("active");
        mPrevbtn.css({"display": "none", "opacity": "0"});
        mNextbtn.css("display", "block");
    });
}

/* To operate slider with nav(dots) */

function mSlideNav() {

    jQuery(document).on("click", ".multi-section-slider .slider-nav span", function () {
        mViewItem = jQuery(this).parents(".multi-section-slider").attr("data-items");
        mActiveIndex = jQuery(this).index();
        mParentWidth = jQuery(this).parents(".multi-section-slider").width();
        mItemLength = jQuery(this).parents(".multi-section-slider").find(".item").size();
        mItemWidth = mParentWidth / mViewItem;
        mItemsWidth = mItemWidth * mItemLength;
        mLeftValue = mActiveIndex * mParentWidth;
        if ((mLeftValue + mParentWidth) > mItemsWidth) {
            mLeftValue = mItemsWidth - mParentWidth;
        }
        jQuery(this).parents(".multi-section-slider").find(".slider").stop(true, true).animate({"margin-left": "-" + mLeftValue + "px"}, 600);
        jQuery(this).addClass("active");
        jQuery(this).parents(".multi-section-slider").find(".slider-nav").find("span").not(jQuery(this)).removeClass("active");
    });
}

/* To operate slider with next button */

function mSlideNext() {
    jQuery(".multi-section-slider .slider-nav").find(".next").click(function () {
        mViewItem = jQuery(this).parents(".multi-section-slider").attr("data-items");
        mActiveIndex = jQuery(this).parents(".multi-section-slider").find(".slider-nav").find("span.active").index();
        mParentWidth = jQuery(this).parents(".multi-section-slider").width();
        mItemLength = jQuery(this).parents(".multi-section-slider").find(".item").size();
        mItemWidth = mParentWidth / mViewItem;
        mItemsWidth = mItemWidth * mItemLength;
        mInitItem = mActiveIndex;
        mInitItem++;
        mLeftValue = mInitItem * mParentWidth;
        if ((mLeftValue + mParentWidth) > mItemsWidth) {
            mLeftValue = mItemsWidth - mParentWidth;
        }
        jQuery(this).parents(".multi-section-slider").find(".slider").stop(true, true).animate({"margin-left": "-" + mLeftValue + "px"}, 600);
        jQuery(this).parents(".multi-section-slider").find(".slider-nav .we-navs span").not(".slider-nav .we-navs span:eq(" + mInitItem + ")").removeClass("active");
        jQuery(this).parents(".multi-section-slider").find(".slider-nav .we-navs span:eq(" + mInitItem + ")").addClass("active");
    });
}

/* To operate slider with prev button */

function mSlidePrev() {
    jQuery(".multi-section-slider .slider-nav").find(".prev").click(function () {
        mViewItem = jQuery(this).parents(".multi-section-slider").attr("data-items");
        mActiveIndex = jQuery(this).parents(".multi-section-slider").find(".slider-nav").find("span.active").index();
        mParentWidth = jQuery(this).parents(".multi-section-slider").width();
        mItemLength = jQuery(this).parents(".multi-section-slider").find(".item").size();
        mItemWidth = mParentWidth / mViewItem;
        mItemsWidth = mItemWidth * mItemLength;
        mInitItem = mActiveIndex;
        mInitItem--;
        mLeftValue = mInitItem * mParentWidth;
        jQuery(this).parents(".multi-section-slider").find(".slider").stop(true, true).animate({"margin-left": "-" + mLeftValue + "px"}, 600);
        jQuery(this).parents(".multi-section-slider").find(".slider-nav .we-navs span").not(".slider-nav .we-navs span:eq(" + mInitItem + ")").removeClass("active");
        jQuery(this).parents(".multi-section-slider").find(".slider-nav .we-navs span:eq(" + mInitItem + ")").addClass("active");
    });
}

/* to check and hide/show prevbtn and nextbtn*/

function mCheckNav() {
    jQuery(document).on("click", ".multi-section-slider span, .multi-section-slider .prev, .multi-section-slider .next", function () {
        mViewItem = jQuery(this).parents(".multi-section-slider").attr("data-items");
        mActiveIndex = jQuery(this).parents(".multi-section-slider").find(".slider-nav").find("span.active").index();
        mPrevbtn = jQuery(this).parents(".multi-section-slider").find(".slider-nav").find(".prev");
        mNextbtn = jQuery(this).parents(".multi-section-slider").find(".slider-nav").find(".next");
        mItemLength = jQuery(this).parents(".multi-section-slider").find(".item").size();
        mTotalNav = Math.ceil(mItemLength / mViewItem);
        mItemLength = mTotalNav;
        if (mActiveIndex === 0) {
            mPrevbtn.animate({"opacity": "0"}, 150);
            mNextbtn.animate({"opacity": "1"}, 100);
            mNextbtn.css({"display": "block"});
            setTimeout(function () {
                mPrevbtn.css({"display": "none"});
            }, 150);
        } else if (mActiveIndex >= (mItemLength - 1)) {
            mPrevbtn.animate({"opacity": "1"}, 100);
            mNextbtn.animate({"opacity": "0"}, 150);
            mPrevbtn.css({"display": "block"});
            setTimeout(function () {
                mNextbtn.css({"display": "none"});
            }, 150);
        } else {
            mPrevbtn.animate({"opacity": "1"}, 100);
            mNextbtn.animate({"opacity": "1"}, 100);
            mNextbtn.css({"display": "block"});
            mPrevbtn.css({"display": "block"});

        }
    });
}

function mSwipeTouch() {
    jQuery('.multi-section-slider').each(function () {
        jQuery(this).on('swipeleft', function () {
            if (mActiveIndex !== (mItemLength - 1)) {
                jQuery(this).children('.slider-nav').find('.next').trigger('click');
            }
        });
        jQuery(this).on('swiperight', function () {
            if (mActiveIndex !== 0) {
                jQuery(this).children('.slider-nav').find('.prev').trigger('click');
            }
        });
    });
}

function swipeTouch() {
    jQuery('.we-slider').each(function () {
        jQuery(this).on('swipeleft', function () {
            if (activeIndex !== (itemLength - 1)) {
                jQuery(this).children('.slider-nav').find('.next').trigger('click');
            }
        });
        jQuery(this).on('swiperight', function () {
            if (activeIndex !== 0) {
                jQuery(this).children('.slider-nav').find('.prev').trigger('click');
            }
        });
    });
}