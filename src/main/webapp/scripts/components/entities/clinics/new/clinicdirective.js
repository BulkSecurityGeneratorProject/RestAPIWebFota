'use strict';

angular.module('hillromvestApp')
  .directive('clinic', function(ClinicService, UserService) {
    return {
      templateUrl: 'scripts/components/entities/clinics/new/create.html',
      restrict: 'E',
      scope: {
        clinic: '=clinicData',
        onSuccess: '&',
        clinicStatus: '=clinicStatus'
      },
      link: function(scope) {
      scope.$on('setClinicType', function () {
        scope.clinic.type = 'parent'
      })
    },


      controller: function($scope, noty, $filter) {

         $scope.open = function () {
          $scope.showModal = true;
        };

        $scope.close = function () {
          $scope.showModal = false;
        };

        $scope.init = function() {
          $scope.clinic = {};
          $scope.clinic.type = 'parent';
          UserService.getState().then(function(response) {
            $scope.states = response.data.states;
          }).catch(function(response) {

          });
          $scope.getParentClinic();
        };



        $scope.newChildClinic = function() {
          $scope.clinic.childClinics.push({
            name: ''
          });
        };

        $scope.removeChildClinic = function(idx) {
          $scope.clinic.childClinics.splice(idx, 1);
        };

        $scope.submitted = false;
          $scope.formSubmit = function() {
          $scope.submitted = true;
        };

        $scope.states = [];



        $scope.createClinic = function() {
          if ($scope.form.$invalid) {
            return false;
          }
          if ($scope.clinic.type === 'parent') {
            $scope.clinic.parent = true;
          } else {
            $scope.clinic.parent = false;
          }
          if ($scope.clinicStatus.editMode) {
            // edit Clinic section
            var data = $scope.clinic;
            if (data.parentClinic) {
              var id = data.parentClinic.id;
              var name = data.parentClinic.name;
              delete data.parentClinic;
              data.parentClinic ={};
              data.parentClinic.id = id;
              //$scope.clinic.parentClinic.name = name;
            }
            ClinicService.updateClinic(data).then(function(data) {
              $scope.clinicStatus.isMessage = true;
              $scope.clinicStatus.message = "Clinic updated successfully" + " for ID " + data.data.Clinic.id;
              noty.showNoty({
                text: $scope.clinicStatus.message,
                ttl: 5000,
                type: "success"
              });
              $scope.init();
              $scope.reset();
              
            }).catch(function(response) {
              if (response.data.message !== undefined) {
                $scope.clinicStatus.message = response.data.message;
              } else if(response.data.ERROR !== undefined){
                $scope.clinicStatus.message = data.data.ERROR;
              } else {
                $scope.clinicStatus.message = 'Error occurred! Please try again';
              }
              $scope.clinicStatus.isMessage = true;
              noty.showNoty({
                text: $scope.clinicStatus.message,
                ttl: 5000,
                type: "warning"
              });
              
            });
          } else {
            if ($scope.clinic.type === 'parent' && $scope.clinic.parentClinic) {
              delete $scope.clinic.parentClinic;
            } else {
            }
            // create clinic section
            var data = $scope.clinic;
             if (data.parentClinic) {
               delete data.parentClinic.name;
               //$scope.clinic.parentClinic.name = data.parentClinic.name;
            }
            ClinicService.createClinic(data).then(function(data) {
              $scope.clinicStatus.isMessage = true;
              $scope.clinicStatus.message = "Clinic created successfully";
              noty.showNoty({
                text: $scope.clinicStatus.message,
                ttl: 5000,
                type: "success"
              });
              $scope.reset();
            }).catch(function(response) {
              if (response.data.message !== undefined) {
                $scope.clinicStatus.message = response.data.message;
              }else if(response.data.ERROR !== undefined){
              $scope.clinicStatus.message = data.data.ERROR;
              }else {
                $scope.clinicStatus.message = 'Error occured! Please try again';
              }
              $scope.clinicStatus.isMessage = true;
              noty.showNoty({
                text: $scope.clinicStatus.message,
                ttl: 5000,
                type: "warning"
              });
            });
          }
        };

        $scope.deleteClinic = function() {
          ClinicService.deleteClinic($scope.clinic.id).then(function(data) {
            $scope.showModal = false;
            $scope.clinicStatus.isMessage = true;
            $scope.clinicStatus.message = data.data.message;
            noty.showNoty({
              text: $scope.clinicStatus.message,
              ttl: 5000,
              type: "success"
            });
            $scope.reset();
          }).catch(function(response) {
             $scope.showModal = false;
            if (response.data.message !== undefined) {
              $scope.clinicStatus.message = data.data.message;
            } else if(response.data.ERROR !== undefined){
              $scope.clinicStatus.message = data.data.ERROR;
            } else {
              $scope.clinicStatus.message = 'Error occured! Please try again';
            }
            $scope.clinicStatus.isMessage = true;
            noty.showNoty({
              text: $scope.clinicStatus.message,
              ttl: 5000,
              type: "warning"
            });
          });
        };

        $scope.cancel = function(){
          $scope.reset();
        };

        $scope.reset = function(){
          $scope.clinicStatus.editMode = false;
          $scope.clinicStatus.isCreate = false;
          $scope.submitted = false;
          $scope.clinic = {};
          $scope.form.$setPristine();
          $scope.onSuccess();
        };



        $scope.selectClinic = function(clinic) {
          $scope.clinic.parentClinic.name = clinic.name;
          $scope.clinic.parentClinic.id = clinic.id;
          $scope.clinics = [];
        };

        $scope.removeParent = function() {
          $scope.clinic.parentClinic = null;
        };

        $scope.getParentClinic = function() {
          ClinicService.getAllClinics().then(function (response) {
            $scope.clinics = response.data;
          }).catch(function (response) {

          });
        };

        $scope.init();
      }
    };
  });
