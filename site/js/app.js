var app = angular.module('hortonWorksApp', ['ngAnimate']);
app.directive('onFinishRender', function ($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            if (scope.$last === true) {
                $timeout(function () {
                    scope.$emit('ngRepeatFinished');
                });
            }
        }
    }
});

app.directive('proonFinishRender', function ($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attr) {
            if (scope.$last === true) {
                $timeout(function () {
                    scope.$emit('prongRepeatFinished');
                });
            }
        }
    }
});

app.directive('loadall', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                var post_type = [];
                jQuery('.filter-btn li a').removeAttr('style');
                jQuery(this).css({'pointer-events': 'none', 'cursor': 'default'});
                var class_name = '';
                var post_type = '';
                var offset = '0';
                var check = 'true';
                scope.loadAll(post_type, offset, class_name, check);
            });
        }
    }
});

app.directive('lpress', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                jQuery('.filter-btn li a').removeAttr('style');
                jQuery(this).css({'pointer-events': 'none', 'cursor': 'default'});
                var post_type = 'hw_news';
                var class_name = 'press';
                var check = 'false';
                var offset = jQuery('.isotope .press').length;
                scope.loadAll(post_type, offset, class_name, check);
            });
        }
    }
});

app.directive('lexecutive', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                jQuery('.filter-btn li a').removeAttr('style');
                jQuery(this).css({'pointer-events': 'none', 'cursor': 'default'});
                var post_type = 'executive';
                var class_name = 'executive';
                var check = 'false';
                var offset = jQuery('.isotope .execpost').length;
                scope.loadAll(post_type, offset, class_name, check);
            });
        }
    }
});

app.directive('lnews', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                jQuery('.filter-btn li a').removeAttr('style');
                jQuery(this).css({'pointer-events': 'none', 'cursor': 'default'});
                var post_type = 'article';
                var class_name = 'news';
                var check = 'false';
                var offset = jQuery('.isotope .news').length;
                scope.loadAll(post_type, offset, class_name, check);
            });
        }
    }
});

app.directive('lblog', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                jQuery('.filter-btn li a').removeAttr('style');
                jQuery(this).css({'pointer-events': 'none', 'cursor': 'default'});
                var post_type = 'post';
                var class_name = 'blog';
                var check = 'false';
                var offset = jQuery('.isotope .blog').length;
                scope.loadAll(post_type, offset, class_name, check);
            });
        }
    }
});

app.directive('loadentries', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                element.addClass('loader');
                element.prop('disabled', true);
                var offset = jQuery('.filter-elements .item').length;
                var class_name = jQuery('.tabsets .active a').attr('class');
                var str = class_name.split(" ");
                var post_type = str[1];
                if (post_type == 'press') {
                    post_type = 'hw_news';
                } else if (post_type == 'news') {
                    post_type = 'article';
                } else if (post_type == 'blog') {
                    post_type = 'post';
                } else if (post_type == 'executive') {
                    post_type = '';
                }
                else if (post_type == 'all') {
                    post_type = '';
                }
                scope.loadMore(post_type, offset, element);
            });
        }
    }
});

app.directive('loadtrigger', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {

            jQuery(element).on('change', function ($event) {
                var offset = 0;
                var class_name = jQuery('.tabsets .active a').attr('class');
                var str = class_name.split(" ");
                var post_type = str[1];
                if (post_type == 'press') {
                    post_type = 'hw_news';
                } else if (post_type == 'news') {
                    post_type = 'article';
                } else if (post_type == 'blog') {
                    post_type = 'post';
                } else if (post_type == 'executive') {
                    post_type = '';
                }
                else if (post_type == 'all') {
                    post_type = '';
                }
                scope.loadAll(post_type, offset);
            });
        }
    }
});

app.directive('loadcustomer', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                element.addClass('loader');
                element.prop('disabled', true);
                var customer_name = scope.customer;
                var offset = jQuery('.grid-customers .icon-tab li').length;
                scope.loadCustomer(customer_name, offset, element);
            })
        }
    }
});

app.directive('loadevents', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                element.addClass('loader');
                element.prop('disabled', true);
                var offset = jQuery('.events-all .event-list').length - 1;

                scope.loadMoreEvents(offset, element);
            });
        }
    }
});

app.directive('loadwebinar', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {
                jQuery('.webcast-all .event-list').attr('ng-class', scope.otherclass);
                element.addClass('loader');
                element.prop('disabled', true);
                var offset = jQuery('.webcast-all .event-list').length - 1;
                scope.loadMoreWebinars(offset, element);
            });
        }
    }
});

app.directive('loadondemand', function ($compile) {
    return{
        scope: true,
        restrict: 'C',
        replace: false,
        link: function (scope, element, attr) {
            element.bind('click', function ($event) {

                element.addClass('loader');
                element.prop('disabled', true);
                var offset = jQuery('.ondemand-all-wrapper .event-list').length - 2;
                scope.loadMoreOndemand(offset, element);
            });
        }
    }
});