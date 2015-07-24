'use strict';

angular.module('hillromvestApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('patient', {
                parent: 'entity',
                url: '/patient',
                data: {
                    roles: [],
                    pageTitle: 'patient.title'
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/patients/patients.html',
                        controller: 'PatientsController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('patient');
                        return $translate.refresh();
                    }]
                }
            });
    });
