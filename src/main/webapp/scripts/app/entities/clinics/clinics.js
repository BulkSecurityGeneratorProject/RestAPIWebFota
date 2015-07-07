'use strict';

angular.module('hillromvestApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('clinic', {
                parent: 'entity',
                url: '/clinic',
                data: {
                    roles: [],
                    pageTitle: 'clinic.title'
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/clinics/clinics.html',
                        controller: 'PatientsController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        // $translatePartialLoader.addPart('dashboard');
                        return $translate.refresh();
                    }]
                }
            });
    });
