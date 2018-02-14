/**
 * Created by abel on 12.01.17.
 */
requirejs.config({
    baseUrl: "js/app",

    paths: {
        'moment':'../lib/moment',
        'locale/ru':'../lib/locale/ru',
        'jquery': '../lib/jquery',
        'backbone': '../lib/backbone',
        'underscore': '../lib/underscore',
        'bootstrap': '../lib/bootstrap',
        'dropzone': '../lib/dropzone-amd-module',
        'pikaday': '../lib/pikaday',
        'const': '../const'
    },


    shim: {
        'locale/ru': {
            deps:['moment']
        },
        'bootstrap' : {
            deps:['jquery']
        },
        'underscore': {
            exports: '_'
        },
        'backbone': {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },
        'pikaday': {
            deps: ['jquery',"moment"]
        },
        'const': {
            deps: ['jquery']
        }
    }

});

