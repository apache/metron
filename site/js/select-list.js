jQuery(document).ready(function () {
    /* Selects each select list*/
    
    jQuery(".select-list").each(function () {
        var default_select = jQuery(this).find("select"),
                option_length = default_select.children("option").size(),
                first_option = default_select.children("option:eq(0)").text(),
                i = 0;

        /* Append html in each select-list */
        jQuery(this).append('<div class="selected">' +
                ' <div class="v-middle-wrapper">' +
                '<div class="v-middle-inner">' +
                '<div class="v-middle text-left">' +
                '<p>' + first_option + '</p>' +
                '</div>' +
                '</div>' +
                '</div>' +
                '</div>' +
                '<ul class="options text-left grey-bg">' +
                '</ul>');
        /* Append select option value to select-list options */
        for (i; i < option_length; i++) {
            var option_values = default_select.children("option:eq(" + i + ")").text();
            jQuery(this).find(".options").append("<li><p>" + option_values + "</p></li>");
        }
    });

    /*closes select list on click anywhere on document*/
    jQuery(document).click(function () {
        jQuery(".select-list").each(function () {
            if (jQuery(this).find(".options").css("display") === "block") {
                jQuery(this).find(".options").css("display", "none");
            }
        });
    });
    /* opens & close options list on click of selected box*/
    jQuery(document).on("click", ".select-list .selected", function (event) {
        event.stopPropagation();
        if (jQuery(this).siblings(".options").css("display") === "block") {
            jQuery(this).siblings(".options").css("display", "none");
        } else {
            jQuery(this).siblings(".options").css("display", "block");
        }
        jQuery(".select-list .options").not(jQuery(this).siblings(".options")).css("display", "none");
    });

    /* selects option from both custom and default option list */
    jQuery(document).on("click", ".select-list .options li", function () {
        var selected = jQuery(this).text(),
                this_index = jQuery(this).index();
        jQuery(this).parents(".select-list").find(".selected p").text(selected);
        jQuery(this).parents(".select-list").find(".options").css("display", "none");
        jQuery(this).parents(".select-list").find("select option:eq(" + this_index + ")").prop("selected", true);
        jQuery(this).parents(".select-list").find("select option").not("select option:eq(" + this_index + ")").prop("selected", false);
        jQuery(this).parents(".select-list").find("select").trigger('change');
    });
});
