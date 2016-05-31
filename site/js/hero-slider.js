/*
 * Hero-slider
 */
(function ($) {
    function ScrollAbsoluteGallery(options) {
        this.options = $.extend({
            activeClass: 'active',
            mask: 'div.slides-mask',
            slider: '>ul',
            slides: '>li',
            btnPrev: '.btn-prev',
            btnNext: '.btn-next',
            pagerLinks: 'ul.pager > li',
            generatePagination: false,
            pagerList: '<ul>',
            pagerListItem: '<li><a href="#"></a></li>',
            pagerListItemText: 'a',
            galleryReadyClass: 'gallery-js-ready',
            currentNumber: 'span.current-num',
            totalNumber: 'span.total-num',
            maskAutoSize: false,
            autoRotation: false,
            pauseOnHover: false,
            stretchSlideToMask: false,
            switchTime: 3000,
            animSpeed: 500,
            handleTouch: true,
            swipeThreshold: 15,
            vertical: false
        }, options);
        this.init();
    }
    ScrollAbsoluteGallery.prototype = {
        init: function () {
            if (this.options.holder) {
                this.findElements();
                this.attachEvents();
                this.makeCallback('onInit', this);
            }
        },
        findElements: function () {
            // find structure elements
            this.holder = $(this.options.holder).addClass(this.options.galleryReadyClass);
            this.mask = this.holder.find(this.options.mask);
            this.slider = this.mask.find(this.options.slider);
            this.slides = this.slider.find(this.options.slides);
            this.btnPrev = this.holder.find(this.options.btnPrev);
            this.btnNext = this.holder.find(this.options.btnNext);

            // slide count display
            this.currentNumber = this.holder.find(this.options.currentNumber);
            this.totalNumber = this.holder.find(this.options.totalNumber);

            // create gallery pagination
            if (typeof this.options.generatePagination === 'string') {
                this.pagerLinks = this.buildPagination();
            } else {
                this.pagerLinks = this.holder.find(this.options.pagerLinks);
            }

            // define index variables
            this.sizeProperty = this.options.vertical ? 'height' : 'width';
            this.positionProperty = this.options.vertical ? 'top' : 'left';
            this.animProperty = this.options.vertical ? 'marginTop' : 'marginLeft';

            this.slideSize = this.slides[this.sizeProperty]();
            this.currentIndex = 0;
            this.prevIndex = 0;

            // reposition elements
            this.options.maskAutoSize = this.options.vertical ? false : this.options.maskAutoSize;
            if (this.options.vertical) {
                this.mask.css({
                    height: this.slides.innerHeight()
                });
            }
            if (this.options.maskAutoSize) {
                this.mask.css({
                    height: this.slider.height()
                });
            }
            this.slider.css({
                position: 'relative',
                height: this.options.vertical ? this.slideSize * this.slides.length : '100%'
            });
            this.slides.css({
                position: 'absolute'
            }).css(this.positionProperty, -9999).eq(this.currentIndex).css(this.positionProperty, 0);
            this.refreshState();
        },
        buildPagination: function () {
            var pagerLinks = $();
            if (!this.pagerHolder) {
                this.pagerHolder = this.holder.find(this.options.generatePagination);
            }
            if (this.pagerHolder.length) {
                this.pagerHolder.empty();
                this.pagerList = $(this.options.pagerList).appendTo(this.pagerHolder);
                for (var i = 0; i < this.slides.length; i++) {
                    $(this.options.pagerListItem).appendTo(this.pagerList).find(this.options.pagerListItemText).text(i + 1);
                }
                pagerLinks = this.pagerList.children();
            }
            return pagerLinks;
        },
        attachEvents: function () {
            // attach handlers
            var self = this;
            if (this.btnPrev.length) {
                this.btnPrevHandler = function (e) {
                    e.preventDefault();
                    self.prevSlide();
                };
                this.btnPrev.click(this.btnPrevHandler);
            }
            if (this.btnNext.length) {
                this.btnNextHandler = function (e) {
                    e.preventDefault();
                    self.nextSlide();
                };
                this.btnNext.click(this.btnNextHandler);
            }
            if (this.pagerLinks.length) {
                this.pagerLinksHandler = function (e) {
                    e.preventDefault();
                    self.numSlide(self.pagerLinks.index(e.currentTarget));
                };
                this.pagerLinks.click(this.pagerLinksHandler);
            }

            // handle autorotation pause on hover
            if (this.options.pauseOnHover) {
                this.hoverHandler = function () {
                    clearTimeout(self.timer);
                };
                this.leaveHandler = function () {
                    self.autoRotate();
                };
                this.holder.bind({mouseenter: this.hoverHandler, mouseleave: this.leaveHandler});
            }

            // handle holder and slides dimensions
            this.resizeHandler = function () {
                if (!self.animating) {
                    if (self.options.stretchSlideToMask) {
                        self.resizeSlides();
                    }
                    self.resizeHolder();
                    self.setSlidesPosition(self.currentIndex);
                }
            };
            $(window).bind('load resize orientationchange', this.resizeHandler);
            if (self.options.stretchSlideToMask) {
                self.resizeSlides();
            }

            // handle swipe on mobile devices
            if (this.options.handleTouch && window.Hammer && this.mask.length && this.slides.length > 1 && isTouchDevice) {
                this.swipeHandler = new Hammer.Manager(this.mask[0]);
                this.swipeHandler.add(new Hammer.Pan({
                    direction: self.options.vertical ? Hammer.DIRECTION_VERTICAL : Hammer.DIRECTION_HORIZONTAL,
                    threshold: self.options.swipeThreshold
                }));

                this.swipeHandler.on('panstart', function () {
                    if (self.animating) {
                        self.swipeHandler.stop();
                    } else {
                        clearTimeout(self.timer);
                    }
                }).on('panmove', function (e) {
                    self.swipeOffset = -self.slideSize + e[self.options.vertical ? 'deltaY' : 'deltaX'];
                    self.slider.css(self.animProperty, self.swipeOffset);
                    clearTimeout(self.timer);
                }).on('panend', function (e) {
                    if (e.distance > self.options.swipeThreshold) {
                        if (e.offsetDirection === Hammer.DIRECTION_RIGHT || e.offsetDirection === Hammer.DIRECTION_DOWN) {
                            self.nextSlide();
                        } else {
                            self.prevSlide();
                        }
                    } else {
                        var tmpObj = {};
                        tmpObj[self.animProperty] = -self.slideSize;
                        self.slider.animate(tmpObj, {duration: self.options.animSpeed});
                        self.autoRotate();
                    }
                    self.swipeOffset = 0;
                });
            }

            // start autorotation
            this.autoRotate();
            this.resizeHolder();
            this.setSlidesPosition(this.currentIndex);
        },
        resizeSlides: function () {
            this.slideSize = this.mask[this.options.vertical ? 'height' : 'width']();
            this.slides.css(this.sizeProperty, this.slideSize);
        },
        resizeHolder: function () {
            if (this.options.maskAutoSize) {
                this.mask.css({
                    height: this.slides.eq(this.currentIndex).outerHeight(true)
                });
            }
        },
        prevSlide: function () {
            if (!this.animating && this.slides.length > 1) {
                this.direction = -1;
                this.prevIndex = this.currentIndex;
                if (this.currentIndex > 0)
                    this.currentIndex--;
                else
                    this.currentIndex = this.slides.length - 1;
                this.switchSlide();
            }
        },
        nextSlide: function (fromAutoRotation) {
            if (!this.animating && this.slides.length > 1) {
                this.direction = 1;
                this.prevIndex = this.currentIndex;
                if (this.currentIndex < this.slides.length - 1)
                    this.currentIndex++;
                else
                    this.currentIndex = 0;
                this.switchSlide();
            }
        },
        numSlide: function (c) {
            if (!this.animating && this.currentIndex !== c && this.slides.length > 1) {
                this.direction = c > this.currentIndex ? 1 : -1;
                this.prevIndex = this.currentIndex;
                this.currentIndex = c;
                this.switchSlide();
            }
        },
        preparePosition: function () {
            // prepare slides position before animation
            this.setSlidesPosition(this.prevIndex, this.direction < 0 ? this.currentIndex : null, this.direction > 0 ? this.currentIndex : null, this.direction);
        },
        setSlidesPosition: function (index, slideLeft, slideRight, direction) {
            // reposition holder and nearest slides
            if (this.slides.length > 1) {
                var prevIndex = (typeof slideLeft === 'number' ? slideLeft : index > 0 ? index - 1 : this.slides.length - 1);
                var nextIndex = (typeof slideRight === 'number' ? slideRight : index < this.slides.length - 1 ? index + 1 : 0);

                this.slider.css(this.animProperty, this.swipeOffset ? this.swipeOffset : -this.slideSize);
                this.slides.css(this.positionProperty, -9999).eq(index).css(this.positionProperty, this.slideSize);
                if (prevIndex === nextIndex && typeof direction === 'number') {
                    var calcOffset = direction > 0 ? this.slideSize * 2 : 0;
                    this.slides.eq(nextIndex).css(this.positionProperty, calcOffset);
                } else {
                    this.slides.eq(prevIndex).css(this.positionProperty, 0);
                    this.slides.eq(nextIndex).css(this.positionProperty, this.slideSize * 2);
                }
            }
        },
        switchSlide: function () {
            // prepare positions and calculate offset
            var self = this;
            var oldSlide = this.slides.eq(this.prevIndex);
            var newSlide = this.slides.eq(this.currentIndex);
            this.animating = true;

            // resize mask to fit slide
            if (this.options.maskAutoSize) {
                this.mask.animate({
                    height: newSlide.outerHeight(true)
                }, {
                    duration: this.options.animSpeed
                });
            }

            // start animation
            var animProps = {};
            animProps[this.animProperty] = this.direction > 0 ? -this.slideSize * 2 : 0;
            this.preparePosition();
            this.slider.animate(animProps, {duration: this.options.animSpeed, complete: function () {
                    self.setSlidesPosition(self.currentIndex);

                    // start autorotation
                    self.animating = false;
                    self.autoRotate();

                    // onchange callback
                    self.makeCallback('onChange', self);
                }});

            // refresh classes
            this.refreshState();

            // onchange callback
            this.makeCallback('onBeforeChange', this);
        },
        refreshState: function (initial) {
            // slide change function
            this.slides.removeClass(this.options.activeClass).eq(this.currentIndex).addClass(this.options.activeClass);
            this.pagerLinks.removeClass(this.options.activeClass).eq(this.currentIndex).addClass(this.options.activeClass);

            // display current slide number
            this.currentNumber.html(this.currentIndex + 1);
            this.totalNumber.html(this.slides.length);

            // add class if not enough slides
            this.holder.toggleClass('not-enough-slides', this.slides.length === 1);
        },
        autoRotate: function () {
            var self = this;
            clearTimeout(this.timer);
            if (this.options.autoRotation) {
                this.timer = setTimeout(function () {
                    self.nextSlide();
                }, this.options.switchTime);
            }
        },
        makeCallback: function (name) {
            if (typeof this.options[name] === 'function') {
                var args = Array.prototype.slice.call(arguments);
                args.shift();
                this.options[name].apply(this, args);
            }
        },
        destroy: function () {
            // destroy handler
            this.btnPrev.unbind('click', this.btnPrevHandler);
            this.btnNext.unbind('click', this.btnNextHandler);
            this.pagerLinks.unbind('click', this.pagerLinksHandler);
            this.holder.unbind('mouseenter', this.hoverHandler);
            this.holder.unbind('mouseleave', this.leaveHandler);
            $(window).unbind('load resize orientationchange', this.resizeHandler);
            clearTimeout(this.timer);

            // destroy swipe handler
            if (this.swipeHandler) {
                this.swipeHandler.destroy();
            }

            // remove inline styles, classes and pagination
            this.holder.removeClass(this.options.galleryReadyClass);
            this.slider.add(this.slides).removeAttr('style');
            if (typeof this.options.generatePagination === 'string') {
                this.pagerHolder.empty();
            }
        }
    };

    // detect device type
    var isTouchDevice = /Windows Phone/.test(navigator.userAgent) || ('ontouchstart' in window) || window.DocumentTouch && document instanceof DocumentTouch;

    // jquery plugin
    $.fn.scrollAbsoluteGallery = function (opt) {
        return this.each(function () {
            $(this).data('ScrollAbsoluteGallery', new ScrollAbsoluteGallery($.extend(opt, {holder: this})));
        });
    };
}(jQuery));




/* 
 * Init hero-slider 
 */
var $ = jQuery;
function initCycleCarousel() {
    $('.home-page-slider').scrollAbsoluteGallery({
        mask: 'div.hero-slider',
        slider: 'div.item-wrapper',
        slides: 'div.slide-item',
        btnPrev: 'div.prev',
        btnNext: 'div.next',
        generatePagination: '.hero-nav .pagination',
        stretchSlideToMask: true,
        maskAutoSize: true,
        pauseOnHover: true,
        autoRotation: true,
        switchTime: 6000,
        animSpeed: 500,
        onInit: function (self) {
            self.holder.find('input:text').each(function () {
                var input = $(this);
                input.on('focus', function () {
                    self.options.autoRotation = false;
                    clearTimeout(self.timer);
                }).on('blur', function () {
                    self.options.autoRotation = true;
                    self.autoRotate();
                });
            });
        }
    });
}




/* 
 * Call hero-slider On document ready 
 */
$(document).ready(function () {
    initCycleCarousel();

    $('.home-page-slider .slide-item').on('swipeleft', function () {
        $(this).parents('.hero-slider').find('div.next').trigger('click');
    });
    $('.home-page-slider .slide-item').on('swiperight', function () {
        $(this).parents('.hero-slider').find('div.prev').trigger('click');
    });

    //osFixer();
    heightFix();

});

function osFixer() {
//    if($('.hero-slider').height() == 0) {
//        console.log('zero');
    var heroHeight = $('.home-page-slider').height();
    $('.hero-slider').css({'height': heroHeight + 'px'});
    console.log(heroHeight);
//    }
}



function heightFix() {
    var sliderNav = $('.hero-slider').find('.hero-nav');
    var navWidth = $('.hero-slider').find('.hero-nav').width();
    $(window).on('resize', function () {
        var winHeight = $(window).height(),
        winWidth = $(window).width(),
        calcHeight = winHeight - 237,
        calcLeft = winWidth - (winWidth/2 + navWidth/2);
        $('.home-page-slider').css({'height': calcHeight});
        sliderNav.css({'left': calcLeft});
    }).resize();

}

///* Plugin for Cycle2; Copyright (c) 2012 M. Alsup; v20141007 */
//!function(a){"use strict";a.event.special.swipe=a.event.special.swipe||{scrollSupressionThreshold:10,durationThreshold:1e3,horizontalDistanceThreshold:30,verticalDistanceThreshold:75,setup:function(){var b=a(this);b.bind("touchstart",function(c){function d(b){if(g){var c=b.originalEvent.touches?b.originalEvent.touches[0]:b;e={time:(new Date).getTime(),coords:[c.pageX,c.pageY]},Math.abs(g.coords[0]-e.coords[0])>a.event.special.swipe.scrollSupressionThreshold&&b.preventDefault()}}var e,f=c.originalEvent.touches?c.originalEvent.touches[0]:c,g={time:(new Date).getTime(),coords:[f.pageX,f.pageY],origin:a(c.target)};b.bind("touchmove",d).one("touchend",function(){b.unbind("touchmove",d),g&&e&&e.time-g.time<a.event.special.swipe.durationThreshold&&Math.abs(g.coords[0]-e.coords[0])>a.event.special.swipe.horizontalDistanceThreshold&&Math.abs(g.coords[1]-e.coords[1])<a.event.special.swipe.verticalDistanceThreshold&&g.origin.trigger("swipe").trigger(g.coords[0]>e.coords[0]?"swipeleft":"swiperight"),g=e=void 0})})}},a.event.special.swipeleft=a.event.special.swipeleft||{setup:function(){a(this).bind("swipe",a.noop)}},a.event.special.swiperight=a.event.special.swiperight||a.event.special.swipeleft}(jQuery);
//
//


/*
 *  Adjust Img dimensions On resize 
 */
$(window).on('load', function () {
    $(window).on('resize', function () {
        /* To imitate background image dimensions on real img */
        if ($('.hero-slider .slide-item').length) {
            $('.hero-slider .slide-img').each(function () {
                var parent_height = $(this).height(),
                        parent_width = $(this).width(),
                        image = $(this).children("img"),
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
//        $('.hero-slider').css({'height': 'Calc(100vh - 237px)'});
//    $('.hero-slider .slide-item .slide-img img').css({'height': 'Calc(100vh - 237px)', 'width': 'auto'});
    }).resize();
});


















//jQuery(document).ready(function () {
//    slideItem = jQuery('.hero-slider .slide-item');
//    slideLength = slideItem.length;
//    slideWidth = jQuery(".hero-slider").width();
//
//    /* Global variables values on load and before init() */
//    nextSlideIndex = activeSlideIndex + 1;
//    activeSlideIndex = 0;
//    autoIndex = 1;
//    time = 6000;
//
//    /* hero-slider init call and click event */
//    heroSliderInit();
//    jQuery('.hero-nav > .pagination').find('span').on('click', function () {
//        currentActiveIndex = jQuery('.hero-slider').find('span.active').index();
//        activeSlideIndex = jQuery(this).index();
//        if (currentActiveIndex !== activeSlideIndex) {
////            clearTimeout(autoTimeout);
//            heroSliderNav(currentActiveIndex, activeSlideIndex);
//        }
//    });
//    jQuery('.hero-slider .hero-nav .prev').on('click', function () {
//        currentActiveIndex = jQuery('.hero-slider').find('span.active').index();
//        clearTimeout(autoTimeout);
//        slidePrev(currentActiveIndex);
//    });
//    jQuery('.hero-slider .hero-nav .next').on('click', function () {
//        currentActiveIndex = jQuery('.hero-slider').find('span.active').index();
//        clearTimeout(autoTimeout);
//        slideNext(currentActiveIndex);
//    });
//});
//
///* Global variables */
//var slideItem,
//        activeSlideIndex,
//        currentActiveIndex,
//        nextSlideIndex,
//        prevSlideIndex,
//        slideWidth,
//        slideLength,
//        initSlideItem,
//        autoInterval,
//        autoTimeout,
//        time,
//        autoIndex;
//
///* Initiate slider for the 1st time */
//function heroSliderInit() {
//    jQuery('.hero-slider .hero-nav').append('<div class="pagination"></div>');
//    for (i = 0; i < slideLength; i++) {
//        jQuery('.hero-slider .hero-nav .pagination').append('<span>' + i + '</span>');
//    }
//    /* Assign width to slide-item */
//    jQuery('.hero-slider').find('.slide-item').css({'width': slideWidth});
//    /* pagination active class */
//    jQuery('.hero-slider').find('.hero-nav .pagination span:eq(0)').addClass('active');
//    jQuery('.hero-slider').find('.hero-nav .pagination span').not('.hero-nav .pagination span:eq(0)').removeClass('active');
//    /* slide active class */
//    jQuery('.hero-slider .slide-item').not('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').removeClass('active').css({'left': slideWidth + 'px', 'z-index': '-2'});
//    jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').addClass('active').css({'left': 0 + 'px', 'z-index': '2'});
//    jQuery('.hero-slider .slide-item .slide-content').css({"opacity": "0"});
//    jQuery('.hero-slider .slide-item.active .slide-content').css({"opacity": "1"});
//    /* next, prev slide */
//    jQuery('.hero-slider .slide-item:eq(' + nextSlideIndex + ')').css({'left': slideWidth + 'px'});
//    jQuery('.hero-slider .slide-item:eq(' + prevSlideIndex + ')').css({'left': -slideWidth + 'px'});
//    clearInterval(autoInterval);
//    clearTimeout(autoTimeout);
//    heroSlideAuto();
//}
//
///* Auto hero-slider */
//function heroSlideAuto() {
//    autoInterval = setInterval(function () {
////        if (autoIndex === 1) {
////            jQuery('.hero-slider .slide-item').not('.hero-slider .slide-item:eq(' + 0 + ')').css({'left': slideWidth + 'px', 'z-index': '-2'});
////            console.log("auto index = " + autoIndex);
////        }
//        jQuery('.hero-slider').find('.hero-nav .pagination span:eq(' + (autoIndex - 1) + ')').removeClass('active');
//        jQuery('.hero-slider').find('.hero-nav .pagination span:eq(' + (autoIndex) + ')').addClass('active');
//
//        jQuery('.hero-slider .slide-item:eq(' + (autoIndex + 1) + ')').css({'left': slideWidth + 'px', 'z-index': '-2'});
//        jQuery('.hero-slider .slide-item:eq(' + (autoIndex - 1) + ')').removeClass('active').animate({'left': -slideWidth + 'px', 'z-index': '2'}, 500);
//        jQuery('.hero-slider .slide-item:eq(' + autoIndex + ')').addClass('active').animate({'left': 0 + 'px', 'z-index': '2'}, 500);
//        setTimeout(function () {
//            jQuery('.hero-slider .slide-item.active .slide-content').animate({'opacity': '1'}, 500);
//        }, 300);
//        autoTimeout = setTimeout(function () {
//            jQuery('.hero-slider .slide-item.active .slide-content').css({'opacity': '0'});
//        }, time);
//        autoIndex++;
////        console.log(autoIndex);
//        if (autoIndex === slideLength) {
//            jQuery('.hero-slider .slide-item').not('.hero-slider .slide-item:eq(' + (autoIndex - 1) + ')').not('.hero-slider .slide-item:eq(' + (autoIndex - 2) + ')').css({'left': slideWidth + 'px', 'z-index': '-2'});
//            autoIndex = 0;
//        }
//    }, time);
//}
//
///* Compare current slide to  previously active slide */
//function navComparator(currentActiveIndex, activeSlideIndex) {
//    if (currentActiveIndex !== activeSlideIndex) {
//        clearTimeout(autoTimeout);
//        setTimeout(function () {
//            jQuery('.hero-slider .slide-item .slide-content').css({'opacity': '0'});
//        }, 300);
//        if (currentActiveIndex < activeSlideIndex) {
//            jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').css({'left': slideWidth + 'px', 'z-index': '-2'});
//            jQuery('.hero-slider .slide-item:eq(' + currentActiveIndex + ')').removeClass('active').stop(true, true).animate({'left': -slideWidth + 'px'}, 500);
//            jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').addClass('active').stop(true, true).animate({'left': 0 + 'px', 'z-index': '2'}, 500);
//            setTimeout(function () {
//                jQuery('.hero-slider .slide-item.active .slide-content').animate({'opacity': '1'}, 500);
//            }, 300);
//            for (i = 0; i < activeSlideIndex; i++) {
//                jQuery('.hero-slider .slide-item:eq(' + i + ')').animate({'left': -slideWidth + 'px', 'z-index': '-2'}, 10);
//            }
//        }
//        else if (currentActiveIndex > activeSlideIndex) {
//            jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').css({'left': -slideWidth + 'px', 'z-index': '-2'});
//            jQuery('.hero-slider .slide-item:eq(' + currentActiveIndex + ')').stop(true, true).removeClass('active').animate({'left': slideWidth + 'px'}, 500);
//            jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').stop(true, true).addClass('active').animate({'left': 0 + 'px', 'z-index': '2'}, 500);
//            setTimeout(function () {
//                jQuery('.hero-slider .slide-item.active .slide-content').animate({'opacity': '1'}, 500);
//            }, 300);
//            for (i = slideLength; i > activeSlideIndex; i--) {
//                jQuery('.hero-slider .slide-item:eq(' + i + ')').animate({'left': slideWidth + 'px', 'z-index': '-2'}, 10);
//            }
//        }
//    }
//}
//
///* Navigate slider with pagination */
//function heroSliderNav(currentActiveIndex, activeSlideIndex) {
//    jQuery('.hero-nav > .pagination').find('span:eq(' + currentActiveIndex + ')').removeClass('active');
//    jQuery('.hero-nav > .pagination').find('span:eq(' + activeSlideIndex + ')').addClass('active');
//    autoIndex = activeSlideIndex;
//    navComparator(currentActiveIndex, activeSlideIndex);
//}
//
///* Navigate with prev button */
//function slidePrev(currentActiveIndex) {
//    activeSlideIndex = currentActiveIndex;
//    activeSlideIndex--;
//    setTimeout(function () {
//        jQuery('.hero-slider .slide-item .slide-content').css({'opacity': '0'});
//    }, 350);
//
//    if (currentActiveIndex == 0) {
//        activeSlideIndex = slideLength - 1;
////        console.log(activeSlideIndex);
//        jQuery('.hero-slider .slide-item:eq(' + (activeSlideIndex) + ')').css({'left': -slideWidth + 'px', 'z-index': '-2'});
//    }
////        if (currentActiveIndex == (slideLength - 1)) {
////            jQuery('.hero-slider .slide-item:eq(' + (0) + ')').css({'left': -slideWidth + 'px', 'z-index': '-2'});
////        }
////        jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').css({'left': -slideWidth + 'px', 'z-index': '-2'});
//    jQuery('.hero-nav > .pagination').find('span:eq(' + currentActiveIndex + ')').removeClass('active');
//    jQuery('.hero-nav > .pagination').find('span:eq(' + activeSlideIndex + ')').addClass('active');
//    jQuery('.hero-slider .slide-item:eq(' + currentActiveIndex + ')').removeClass('active').stop(true, true).animate({'left': slideWidth + 'px', 'z-index': '-2'}, 500);
//    jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').addClass('active').stop(true, true).animate({'left': 0 + 'px', 'z-index': '2'}, 500);
//    jQuery('.hero-slider .slide-item').not('.hero-slider .slide-item:eq(' + currentActiveIndex + ')').not('.hero-slider .slide-item:eq(' + (currentActiveIndex - 1) + ')').css({'left': -slideWidth + 'px', 'z-index': '-2'});
//
//    setTimeout(function () {
//        jQuery('.hero-slider .slide-item.active .slide-content').animate({'opacity': '1'}, 500);
//    }, 300);
//    autoIndex = activeSlideIndex;
//}
//
///* Navigate with next button */
//function slideNext(currentActiveIndex) {
//    activeSlideIndex = currentActiveIndex;
//    activeSlideIndex++;
//    setTimeout(function () {
//        jQuery('.hero-slider .slide-item .slide-content').css({'opacity': '0'});
//    }, 350);
//
//    if (activeSlideIndex == (slideLength)) {
//        activeSlideIndex = 0;
//    }
//    if (currentActiveIndex == (slideLength - 1)) {
//        jQuery('.hero-slider .slide-item:eq(' + (0) + ')').css({'left': slideWidth + 'px', 'z-index': '-2'});
//    }
//    jQuery('.hero-nav > .pagination').find('span:eq(' + currentActiveIndex + ')').removeClass('active');
//    jQuery('.hero-nav > .pagination').find('span:eq(' + activeSlideIndex + ')').addClass('active');
//    jQuery('.hero-slider .slide-item:eq(' + currentActiveIndex + ')').removeClass('active').stop(true, true).animate({'left': -slideWidth + 'px', 'z-index': '-2'}, 500);
//    jQuery('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').addClass('active').stop(true, true).animate({'left': 0 + 'px', 'z-index': '2'}, 500);
//    jQuery('.hero-slider .slide-item').not('.hero-slider .slide-item:eq(' + activeSlideIndex + ')').not('.hero-slider .slide-item:eq(' + (activeSlideIndex - 1) + ')').stop(true, true).css({'left': slideWidth + 'px', 'z-index': '-2'});
//
//    setTimeout(function () {
//        jQuery('.hero-slider .slide-item.active .slide-content').animate({'opacity': '1'}, 500);
//    }, 300);
//    autoIndex = activeSlideIndex;
//}

///* On hero-slider resize() */
//jQuery(window).resize(function(){
//     var winWidth=jQuery(window).width(); 
//     if(winWidth > 320) {
//         jQuery(".hero-slider").css({'width': winWidth + 'px'});
//         slideWidth = jQuery(".hero-slider").width();
//        console.log('slideWidth' + slideWidth);
//     }
//     else {
//         jQuery(".hero-slider").css({'width': '320px'});
//         slideWidth = jQuery(".hero-slider").width();
//         console.log('slideWidth' + slideWidth);
//     }
//});