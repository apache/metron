jQuery(document).ready(function () {

    /*
     * Mobile menu to open and close
     */
    jQuery('.f-main-menu:has(.f-sub-menu)').addClass('f-arrow-menu');

    function checkbrowserwidth() {
        if (jQuery("nav > .toggle").css("display") === "block") {

            jQuery('.f-menu-holder').addClass('f-mobile-menu');


        } else {
            jQuery('.f-menu-holder').removeClass('f-mobile-menu');
        }
    }
    checkbrowserwidth();


    jQuery('.f-mobile-menu .f-arrow-menu > li > a').addClass('f-mobile');

    jQuery(document).on('click', '.f-arrow-menu > li > a.f-mobile', function () {
        jQuery(this).toggleClass('arrow_change');
        jQuery('.f-mobile').not(this).removeClass('arrow_change');
        jQuery('.f-mobile').not(this).parent().find('.f-sub-menu').slideUp();
        jQuery(this).parent().children('.f-sub-menu').stop(true, true).slideToggle();
        return false;
    });

});
jQuery(window).resize(function () {

    function checkbrowserwidth() {
        if (jQuery("nav > .toggle").css("display") === "block") {

            jQuery('.f-menu-holder').addClass('f-mobile-menu');
            jQuery('.f-mobile-menu .f-arrow-menu > li > a').addClass('f-mobile');
        } else {

            jQuery('.f-mobile-menu .f-arrow-menu > li > a').removeClass('f-mobile');
            jQuery('.f-menu-holder').removeClass('f-mobile-menu');
            jQuery(".f-sub-menu").removeAttr('style');
            jQuery('.f-arrow-menu > li >').children('a').removeClass('arrow_change');
            jQuery("body").css("overflow-y", "visible");
        }
    }
    checkbrowserwidth();
});