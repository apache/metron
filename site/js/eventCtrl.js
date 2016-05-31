
app.controller("eventCtrl", ['$http', '$scope', '$compile', function ($http, $scope, $compile) {
        /*
         * Load more events for Events-Webcast
         */
        $scope.loadMoreEvents = function (offset, element) {
            $scope.eventshow = '';
            jQuery('.event-all .loadnew.event-list').css({'padding-top': '93px'});
            $scope.data = {
                'offset': offset
            }
            $http({
                method: 'POST',
                url: '/wp-content/themes/hortonworks/template_part/loop/view-events-load-more.php',
                data: $.param($scope.data), // pass in data as strings
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}  // set the headers so angular passing info as form data (not request payload)
            })
                    .success(function (response) {
                        $scope.response_content = $compile(response)($scope);
                        if (response.length < 3267) {
                            jQuery('.events-all .event-all-wrapper').append($scope.response_content);
                            element.removeClass('loader');
                            element.prop('disabled', false);
                            element.hide();
                        }
                        else {
                            jQuery('.events-all .event-all-wrapper').append($scope.response_content);
                            element.removeClass('loader');
                            element.prop('disabled', false);
                        }
                        $scope.eventshow = 'css-class';
                    });
        }

        /*
         * Load more webinars for Events-Webcast
         */
        $scope.loadMoreWebinars = function (offset, element) {
            $scope.eventshow = '';
            jQuery('.webcast-all .loadnew.event-list').css({'padding-top': '93px'});
            $scope.webinaroffset = offset;
            $scope.data = {
                'offset': offset
            }
            $http({
                method: 'POST',
                url: '/wp-content/themes/hortonworks/template_part/loop/view-events-webcast-load-more.php',
                data: $.param($scope.data), // pass in data as strings
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}  // set the headers so angular passing info as form data (not request payload)
            })
                    .success(function (response) {
                        $scope.response_content = $compile(response)($scope);
                        if (response.length < 3267) {
                            jQuery('.webcast-all .webcast-wrapper').append($scope.response_content);
                            element.removeClass('loader');
                            element.prop('disabled', false);
                            element.hide();
                        }
                        else {
                            jQuery('.webcast-all .webcast-wrapper').append($scope.response_content);
                            element.removeClass('loader');
                            element.prop('disabled', false);
                        }
                        $scope.eventshow = 'css-class';
                    });
        }

        /*
         * Load more ondemand for Events-Webcast
         */
        $scope.loadMoreOndemand = function (offset, element) {
            $scope.eventshow = '';
            jQuery('.ondemand-all-wrapper .loadnew.event-list').css({'padding-top': '93px'});
            $scope.ondemandoffset = offset;
            $scope.data = {
                'offset': offset
            }
            $http({
                method: 'POST',
                url: '/wp-content/themes/hortonworks/template_part/loop/ondemand-webcast-load-more.php',
                data: $.param($scope.data), // pass in data as strings
                headers: {'Content-Type': 'application/x-www-form-urlencoded'}  // set the headers so angular passing info as form data (not request payload)
            })
                    .success(function (response) {
                        $scope.response_content = $compile(response)($scope);
                        if (response.length < 3267) {
                            jQuery('.ondemand-all-wrapper').append($scope.response_content);
                            element.removeClass('loader');
                            element.prop('disabled', false);
                            element.hide();
                        }
                        else {
                            jQuery('.ondemand-all-wrapper').append($scope.response_content);
                            element.removeClass('loader');
                            element.prop('disabled', false);
                        }
                        $scope.eventshow = 'css-class';
                    });
        }
    }]);