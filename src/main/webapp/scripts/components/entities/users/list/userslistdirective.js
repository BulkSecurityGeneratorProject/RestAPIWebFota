'use strict';

angular.module('hillromvestApp')
  .directive('userList', function() {
    return {
      templateUrl: 'scripts/components/entities/users/list/list.html',
      restrict: 'E',
      scope: {
        onSelect: '&'
      },
      link: function(scope, element, attrs) {
        var user = scope.user;
      },
      controller: function($scope) {
        $scope.users = [];

        $scope.selectUser = function(user) {
          $scope.user = user;
          $scope.onSelect({'user': user});
        };

        $scope.sortList = function () {
          console.log('Todo Sort Functionality...!');
        };
        $scope.searchUsers = function() {
            $scope.users = [{
              'title':'Mr.',
              'firstName': 'John',
              'lastName': 'Smith',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'James',
              'lastName': 'Williams',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'ACCT_SERVICES', 'value': 'Account Service'}
            }, {
              'title':'Mr.',
              'firstName': 'David',
              'lastName': 'Jones',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'ASSOCIATES', 'value': 'Associates'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'CLINIC_ADMIN', 'value': 'Clinic Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'Joseph',
              'lastName': 'Taylor',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'ACCT_SERVICES', 'value': 'Acct Services'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }, {
              'title':'Mr.',
              'firstName': 'William',
              'lastName': 'Davis',
              'middleName':'MiddleName',
              'email':'email',
              'role':{'key': 'SUPER_ADMIN', 'value': 'Super Admin'}
            }];
        };
      }
    };
  });
