app.controller("pressReleaseController", ['$http', '$scope', function ($http, $scope) {
        $scope.isotopefilter = function () {
            var activeClass = 'active';
            jQuery('.isotope-filter').each(function () {
                var holder = jQuery(this),
                        filterLinks = holder.find('.filter-btn a'),
                        container = holder.find('.filter-elements'),
                        items = container.children(),
                        btn = holder.find('.load-more'),
                        ajaxbusy;

                container.isotope({
                    itemSelector: '.item',
                    layoutMode: 'fitRows'
                });

                filterLinks.click(function (e) {
                    var link = jQuery(this),
                            filter = link.data('filter');
                    e.preventDefault();
                    refreshActiveClass(link);
                    container.isotope({
                        filter: filter,
                        layoutMode: 'fitRows'
                    });
                });

                var refreshActiveClass = function (link) {
                    filterLinks.removeClass(activeClass);
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

        }

        /**
         * Load post on click
         */

        $scope.loadAll = function (post_type, offset, class_name, check) {
            $scope.page_number = '0';
            $scope.post_type = post_type;
            var options = {page_number: $scope.page_number, post_type: $scope.post_type, sort: jQuery('#sort').val(), topic: jQuery('#topic').val()};
            $http.get('/wp-content/themes/hortonworks/template_part/views/press-release-loop.php', {params: options}).success(function (response) {
                $scope.myposts = response;
                $scope.loadPost(response, class_name, check);
                jQuery('.filter-press-release .load_more').css({'display': 'block'});
                jQuery('.filter-press-release .no_more').css({'display': 'none'});
                if ($scope.myposts.length == 0) {
                    jQuery('.filter-press-release .load_more').css({'display': 'none'});
                    jQuery('.filter-press-release .no_more').css({'display': 'block'});
                }
            });
        }
        $scope.loadPost = function (myposts, class_name, check) {
            console.log(myposts);
            var post_length = $scope.myposts.length;
            var $allItems = [];
            var $removeItems = [];
            var $post_type;
            var $grid = jQuery('.filter-elements');
            for (i = 0; i < post_length; i++) {
                if (myposts[i].post_type == 'post') {
                    $post_type = 'blog';
                } else if (myposts[i].post_type == 'hw_news') {
                    $post_type = 'press';
                }
                else if (myposts[i].post_type == 'article') {
                    $post_type = 'news';
                }
                var post_date = myposts[i].post_date;
                if (post_date != null && post_date != '') {
                    /*
                     var p_date = post_date.substr(0, post_date.indexOf(' '));
                     p_date = p_date.split('-');
                     var s_date = p_date[2];
                     var s_year = (p_date[0] % 100);
                     var s_month = p_date[1];
                     var date = s_year + '.' + s_date + '.' + s_month;
                     */
                    var date = post_date;
                }
                var title = myposts[i].post_title;
                var newtab = '';
                if(myposts[i].post_type == 'article') newtab = ' target="_blank"';
                if(myposts[i].featured_image != null){
                    var col_bg = 'back-bg';
                    var image = '<img src="'+myposts[i].featured_image[0]+'"/>'
                }
                else{
                    var col_bg = '';
                    var image = '';
                }
                $allItems.push(jQuery(
                
                '<div class="item col-three ' + $post_type +' '+col_bg+'">   <div class="feature-bg">'+image+'</div>\n\
                <div class="v-middle-wrapper"><div class="v-middle-inner"><div class="item-content v-middle">\n\
                <div class="item-caption"><h6>' + $post_type + '</h6></div>\n\
                <div class="text-container">\n\
                <span class="news-date">' + date + '</span>\n\
                <h5>' + title + '</h5>\n\
                </div>\n\
                <a class="learn-more" href="' + myposts[i].guid + '"' + newtab + '>learn more</a>\n\
                </div> </div> </div></div>'
                        ));
            }
//            if (check == 'true') {
            for (i = 0; i < jQuery('.item').length; i++) {
                $removeItems.push(jQuery('.item').eq(i));
            }
//            } else {
//                for (i = 0; i < jQuery('.item:not(.' + class_name + ')').length; i++) {
//                    $removeItems.push(jQuery('.item:not(.' + class_name + ')').eq(i));
//                }
//            }
            for (i = 0; i < $allItems.length; i++) {
                $grid.isotope('insert', $allItems[i]);
            }

            for (i = 0; i < $removeItems.length; i++) {
                $grid.isotope('remove', $removeItems[i]);
            }
            $grid.isotope('layout');
            window.hwxFixGridTitles();
        }

        /**
         * Load more post on click
         */

        $scope.loadMore = function (post_type, offset, element) {
            $scope.page_number = offset;
            $scope.post_type = post_type;
            $http
                    .get('/wp-content/themes/hortonworks/template_part/views/press-release-loop.php', {params: {page_number: $scope.page_number, post_type: $scope.post_type, sort: jQuery('#sort').val(), topic: jQuery('#topic').val()}}).success(function (response) {
                $scope.myposts = response;
                $scope.loadMorePost(response);
                jQuery('.filter-press-release .load_more').css({'display': 'block'});
                jQuery('.filter-press-release .no_more').css({'display': 'none'});
                if ($scope.myposts.length == 0) {
                    jQuery('.filter-press-release .load_more').css({'display': 'none'});
                    jQuery('.filter-press-release .no_more').css({'display': 'block'});
                }
                if ($scope.myposts.length == 0) {
                    jQuery('.filter-press-release .no_more').css({'display': 'block'});
                    jQuery('.filter-press-release .load_more').css({'display': 'none'});
                }
                element.removeClass('loader');
                element.prop('disabled', false);
            });
        }
        $scope.loadMorePost = function (myposts) {
            var post_length = $scope.myposts.length;
            var $allItems = [];
            var $post_type;
            var $grid = jQuery('.filter-elements');
            for (i = 0; i < post_length; i++) {
                if (myposts[i].post_type == 'post') {
                    $post_type = 'blog';
                } else if (myposts[i].post_type == 'hw_news') {
                    $post_type = 'press';
                }
                else if (myposts[i].post_type == 'article') {
                    $post_type = 'news';
                }
                var post_date = myposts[i].post_date;
                if (post_date != null && post_date != '') {
                    /*
                     var p_date = post_date.substr(0, post_date.indexOf(' '));
                     p_date = p_date.split('-');
                     var s_date = p_date[2];
                     var s_year = (p_date[0] % 100);
                     var s_month = p_date[1];
                     var date = s_year + '.' + s_date + '.' + s_month;
                     */
                    var date = post_date;
                }
                var title = myposts[i].post_title;
                var newtab = '';
                if(myposts[i].post_type == 'article') newtab = ' target="_blank"';
                if(myposts[i].featured_image != null){
                    var col_bg = 'back-bg';
                    var image = '<img src="'+myposts[i].featured_image[0]+'"/>'
                }
                else{
                    var col_bg = '';
                    var image = '';
                }
                $allItems.push(jQuery('<div class="item col-three ' + $post_type +' '+col_bg+'">   <div class="feature-bg">'+ image+'</div>\n\
                <div class="v-middle-wrapper"><div class="v-middle-inner"><div class="item-content v-middle">\n\
                <div class="item-caption"><h6>' + $post_type + '</h6></div>\n\
                <div class="text-container">\n\
                <span class="news-date">' + date + '</span>\n\
                <h5>' + title + '</h5>\n\
                </div>\n\
                <a class="learn-more" href="' + myposts[i].guid + '"' + newtab + '>learn more</a>\n\
                </div> </div> </div></div>'
                        ));
            }

            for (i = 0; i < $allItems.length; i++) {
                $grid.isotope('insert', $allItems[i]);
            }
            $grid.isotope('layout');
            window.hwxFixGridTitles();
        }
    }]);


