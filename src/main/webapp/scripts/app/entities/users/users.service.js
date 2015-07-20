'use strict';

angular.module('hillromvestApp')
  .factory('User', function ($http,localStorageService) {
    var token = localStorage.getItem('token');
    return {
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
      deleteUser : function(id){
        id=16;
        return $http.delete('api/user/'+id, {
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      }
    };
  });