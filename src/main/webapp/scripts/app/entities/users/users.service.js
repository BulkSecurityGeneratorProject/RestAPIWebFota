'use strict';
/**
 * @ngdoc service
 * @name UserService
 * @description
 *
 */
angular.module('hillromvestApp')
  .factory('UserService', function ($http, localStorageService) {
    var token = localStorage.getItem('token');
    return {

      /**
      * @ngdoc method
      * @name createUser
      * @description
      *
      */
      createUser: function (data) {
        return $http.post('api/user', data, {
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      },

      /**
      * @ngdoc method
      * @name deleteUser
      * @description
      *
      */
      deleteUser : function (id) {
        return $http.delete('api/user/' + id, {
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      },

      /**
      * @ngdoc method
      * @name editUser
      * @description
      *
      */
      editUser : function (data) {
        return $http.put('api/user/' + data.id, data, {
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      },

      getState : function () {
         return $http.get('scripts/components/entities/patients/new/state.json')
         .success(function (response) {
          return response;
        });
      },

      getUser : function (userId) {
        return $http.get('api/user/' + userId, {headers: { 'Content-Type' : 'application/json', 'Accept' : 'application/json', 'x-auth-token' : token } })
        .success(function (response) {
          return response;
        });
      }
    };
  });
