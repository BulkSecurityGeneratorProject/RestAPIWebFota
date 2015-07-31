'use strict';
angular.module('hillromvestApp')
  .factory('ClinicService', function ($http,localStorageService) {
	var token = localStorage.getItem('token');
    return {
      createClinic: function (data) {
        return $http.post('api/clinics', data, {
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      },

      updateClinic: function (data) {
        return $http.put('api/clinics/' + data.id, data, {
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      },

      deleteClinic : function(id){

        return $http.delete('api/clinics/'+id, {
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      },

      getClinics : function (searchString,sortOption, pageNo, offset) {
        if (searchString === undefined) { searchString = '';}
        return $http.get('api/clinics/search?searchString=' + searchString + '&page=' + pageNo + '&per_page=' + offset + '&sort_by=' + sortOption + '&asc=' + true,{
          headers: {
            'Content-Type' : 'application/json',
            'Accept' : 'application/json',
            'x-auth-token' : token
          }
        }).success(function (response) {
          return response;
        });
      },

      getAllClinics : function () {
        return $http.get('/api/clinics', {
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


