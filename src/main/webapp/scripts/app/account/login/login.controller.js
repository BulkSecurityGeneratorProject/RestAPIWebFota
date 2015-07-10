'use strict';

angular.module('hillromvestApp')
    .controller('LoginController', function ($rootScope, $scope, $state, $timeout, Auth, vcRecaptchaService) {
        $scope.showLogin = true;
        $scope.isEmailExist = true;
        $scope.isFirstLogin = false;
        $scope.showCaptcha = false;
        $scope.response = null;
        $scope.widgetId = null;
        $scope.user = {};
        $scope.errors = {};
        $scope.siteKey = '6LcXjQkTAAAAAMZ7kb5v9YZ8vrYKFJmDcg2oE-SH';

        $scope.setResponse = function (response) {
            $scope.response = response;
        };

        $scope.setWidgetId = function (widgetId) {
            $scope.widgetId = widgetId;
        };

        $scope.authenticate = function(){
          Auth.login({
            username: $scope.username,
            password: $scope.password,
            captcha: $scope.user.captcha
          }).then(function (data) {
            $scope.authenticationError = false;
            if(data.status === 200){
              localStorage.removeItem('loginCount');
              $state.go('patient');
              localStorage.setItem('token', data.data.token);
            }
          }).catch(function (data) {
            if (data.status === 401) {
              if(!data.data.APP_CODE){
                $scope.authenticationError = true;
                var loginCount = parseInt(localStorage.getItem('loginCount')) || 0;
                localStorage.setItem('loginCount', loginCount + 1);
                if (loginCount > 2) {
                  $scope.showCaptcha = true;
                }
              }else if (data.data.APP_CODE === 'EMAIL_PASSWORD_RESET') {
                localStorage.setItem('token', data.data.token);
                $scope.isFirstLogin = true;
                $scope.isEmailExist = false;
                $scope.showLogin = false;
              } else if (data.data.APP_CODE === 'PASSWORD_RESET') {
                localStorage.setItem('token', data.data.token);
                $scope.isFirstLogin = true;
                $scope.showLogin = false;
              }
            }
          });
        };

        $scope.rememberMe = true;
        $timeout(function (){angular.element('[ng-model="username"]').focus();});

        $scope.login = function (event) {
            event.preventDefault();
            if($scope.showCaptcha){
                  Auth.captcha($scope.response).then(function (data) {
                    $scope.showCaptcha = false;
                    $scope.captchaError = false;
                    $scope.authenticate();
                  }).catch(function (err) {
                    $scope.captchaError = true;
                    $scope.response = null;
                    vcRecaptchaService.reload($scope.widgetId);
                  });
            } else {
              $scope.authenticate();
            }
        };

        $scope.submitPassword = function (event){
          event.preventDefault();
          Auth.submitPassword({
                email: $scope.user.email,
                password: $scope.user.password
            }).then(function (data) {
              Auth.logout();
              $state.go('home');
            }).catch(function (err) {
              Auth.logout();
              console.log('Error...!');
            });
        };
    });
