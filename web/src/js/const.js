
define(["jquery"],function ($) {
    "use strict";
    var Obj = {};
    var properties = {
        'apiUrl': 'http://localhost:8081/api'
    };


    $.getJSON("property.json", function(data) {
        if (data.apiUrl && (data.apiUrl.substring(0,1)  != '$' )) {
            properties = $.extend(true, properties, data || {});
        }
    });


    Obj.MEDIA_THUMB = 11;
    Obj.MEDIA_IMAGE = 12;
    Obj.MEDIA_VIDEO = 13;
    Obj.API_DEBUG = false;


    Obj.getProperties = function(name) {
        return  properties[name];
    };

    Obj.getAPIPath = function() {
        return properties['apiUrl'];
    };

    Obj.getImageURL = function(){
        return properties['apiUrl']+ "/image";
    };

    Obj.getVideoURL = function(){
        return properties['apiUrl']+ "/video";
    };

    return  Obj;
});