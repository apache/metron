jQuery(document).ready(function(){
    
     /*set the top and left value of video btn solution adversting and developer page on resize*/
    jQuery(window).on('load',function(){
        var imgHeight= jQuery('.media-video .video-link img').height(),
         imgWidth= jQuery('.media-video .video-link img').width(),
         imgBtnHeight= jQuery('.media-video .video-link .video-btn').height(),
         imgBtnWidth= jQuery('.media-video .video-link .video-btn').width();
        var  totalHeight=(imgHeight - imgBtnHeight)/2;
         var totalWidth=(imgWidth - imgBtnWidth)/2;
        jQuery('.media-inner .media-video .video-link .video-btn ').css({'top': totalHeight,'left': totalWidth});
    });
    
    //set blog form value on load//
    setTimeout (function(){
      if(jQuery('.blog-slider .comment-list input').val()){
          jQuery('.blog-slider .comment-list input').closest('#comments p').children('label').addClass('used');
      }
      else{
           jQuery('.blog-slider .comment-list input').closest('#comments p').children('label').removeClass('used');
      }
      },100);
      
          setTimeout (function(){
      if(jQuery('.legacy input,.legacy textarea').val()){
          jQuery('.legacy input,.legacy textarea').closest('#comments p').children('label').addClass('used');
      }
      else{
           jQuery('.legacy input,.legacy textarea').closest('#comments p').children('label').removeClass('used');
      }
      },100);
      
    jQuery(window).resize(function () {
    /* We slider*/
    init();
    /* Multi section slider*/
    mInit();
    var window_width = jQuery(window).width();
    if (jQuery(".multi-section-slider").length) {
        if (window_width < 767) {
            jQuery("#horton-webcast-slider .multi-section-slider").attr("data-items", "1");
        }
        else {
            jQuery("#horton-webcast-slider .multi-section-slider").attr("data-items", "2");
        }
    }
});
//webinar/apache-hive form//
jQuery('.legacy p').on('click',function(e){
    jQuery('.legacy .labels-on-top label').css({'top':'22px'});
    jQuery(this).children('label').css({'top':'5px'});
     e.stopPropagation();
});
jQuery('.legacy').on('click',function(){
     jQuery('.legacy .labels-on-top label').css({'top':'22px'});
    
});
    $(document).on('blur', '.legacy input,.legacy textarea', function () {
        if (jQuery(this).val()){
            jQuery(this).closest('#comments p').children('label').addClass('used');
        }
        else
        {
             jQuery(this).closest('#comments p').children('label').removeClass('used');
        }
    });
    
    //BLOG COMMENTS FORM//
    jQuery('.blog-slider .comment-list p').on('click',function(e){
    jQuery('.blog-slider .comment-list .labels-on-top label').css({'top':'22px'});
    jQuery(this).children('label').css({'top':'5px'});
     e.stopPropagation();
    });
    jQuery('.blog-slider').on('click',function(){
     jQuery('.blog-slider .comment-list .labels-on-top label').css({'top':'22px'});
    
});
    jQuery(document).on('blur', '.blog-slider .comment-list input,.blog-slider .comment-list textarea', function () {
        if (jQuery(this).val()){
            jQuery(this).closest('#comments p').children('label').addClass('used');
        }
        else
        {
             jQuery(this).closest('#comments p').children('label').removeClass('used');
        }
    });
// makes a loader
    jQuery('.loadcustomer').click(function(){
     jQuery.ajax({
     // your ajax code
      beforeSend: function(){
       jQuery('.loadcustomer').addClass('loader');
     },
      complete: function(){
      jQuery('.loadcustomer').removeClass('loader');
     }
        });
    });
      jQuery('.loadentries').click(function(){
       jQuery.ajax({
  // your ajax code
  beforeSend: function(){
       jQuery('.loadentries').addClass('loader');
   },
  complete: function(){
      jQuery('.loadentries').removeClass('loader');
  }
       });
   });
   
         jQuery('.loadevents').click(function(){
       jQuery.ajax({
  // your ajax code
  beforeSend: function(){
       jQuery('.loadevents').addClass('loader');
   },
  complete: function(){
      jQuery('.loadevents').removeClass('loader');
  }
       });
   });
   
    jQuery('.loadwebinar').click(function(){
       jQuery.ajax({
  // your ajax code
  beforeSend: function(){
       jQuery('.loadwebinar').addClass('loader');
   },
  complete: function(){
      jQuery('.loadwebinar').removeClass('loader');
  }
       });
   });
   
       jQuery('.loadondemand').click(function(){
       jQuery.ajax({
  // your ajax code
  beforeSend: function(){
       jQuery('.loadwebinar').addClass('loader');
   },
  complete: function(){
      jQuery('.loadwebinar').removeClass('loader');
  }
       });
   });
        //fixed the position of footer-nav
    jQuery(window).on('scroll', function(){
        if (jQuery(window).width()>=768){
        if(jQuery('.footer-wht').length > 0){
        var stickyFooter=jQuery('.footer-wht').offset().top;
        var windowHeight=jQuery(window).height();
        var totalHeight=stickyFooter - windowHeight;
        var scrollHeight =jQuery(window).scrollTop();
        
        if(scrollHeight > totalHeight)
        {
            jQuery('.footer-nav').addClass('fixed');
            jQuery('.footer-wht').css('margin-top','0px');
        }
        else
        {
            jQuery('.footer-nav').removeClass('fixed');
            jQuery('.footer-wht').css('margin-top','34px');
        }
    }
        }
        else{
            jQuery('.footer-nav').addClass('fixed');
            jQuery('.footer-wht').css('margin-top','0px');
        }
    });
    jQuery(window).trigger('scroll');
            //works for tabs on section customer-landing
            
         jQuery('.customer-landing li a').click(function(e){
         e.preventDefault();
         jQuery("a.active").removeClass("active");
         jQuery(this).addClass('active');
         var tab = jQuery(this).attr('href');
         jQuery(".information-center .four-col-row ").not(tab).css('display','none');
         jQuery(tab).fadeIn();
       
         });
        jQuery('.share_popup').click(function(event) {
        var width = 575,
                height = 400,
                left = (jQuery(window).width() - width) / 2,
                top = (jQuery(window).height() - height) / 2,
                url = this.href,
                opts = 'status=1' +
                ',width=' + width +
                ',height=' + height +
                ',top=' + top +
                ',left=' + left;

        window.open(url, 'share_popup', opts);
        return false;
    });
    jQuery(window).on("resize", function () {
        jQuery('.grid li .logo-container').each(function () {
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
        jQuery(".grid > ul > li .logo-container").each(function () {
            jQuery(this).parents("li").find('.tabs-content').stop(true, true).slideUp(300);
            jQuery(this).parents("li").stop(true, true).animate({"margin-bottom": "0px"}, 300);
            jQuery(".grid > ul > li").removeClass("current");

        });
    });
    /*Grid tabs toggle on click*/

    jQuery(document).on("click", ".grid > ul > li .logo-container, .customer-search", function (event) {
        event.stopPropagation();

        /* Select filter for customer featured post */
        jQuery('.grid > ul > li .logo-container').each(function () {
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

         });
         /*remove fixed class from footer below 767px*/
jQuery(window).resize(function(){
   if (jQuery(window).width()<=767){
        jQuery('.footer-nav').addClass('fixed');
        jQuery('.footer-wht').css('margin-top','0px');
    }
    /*set the top and left value of video btn solution adversting and developer page on resize*/
        var imgHeight= jQuery('.media-video .video-link img').height(),
         imgWidth= jQuery('.media-video .video-link img').width(),
         imgBtnHeight= jQuery('.media-video .video-link .video-btn').height(),
         imgBtnWidth= jQuery('.media-video .video-link .video-btn').width();
        var  totalHeight=(imgHeight - imgBtnHeight)/2;
         var totalWidth=(imgWidth - imgBtnWidth)/2;
        jQuery('.media-inner .media-video .video-link .video-btn ').css({'top': totalHeight,'left': totalWidth});
});

  jQuery(document).on({
    ready: function() { jQuery('#video iframe').addClass("loader");    },
        load: function() { jQuery('#video iframe').removeClass("loader"); }    
});