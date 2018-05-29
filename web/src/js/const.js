
define(["jquery","json!../../property.json!bust"],function ($, properties) {
    "use strict";
    var Obj = {};
    //var testUrl = 'http://192.168.1.200:8081/api';
    var testUrl = 'http://localhost:8081/api';

    // properties = $.extend(true, {}, properties, Data || {});
    // console.log("[Const.init ] Load properties : ") ;


    // console.log("[Const.init ] START Load properties") ;
    // require(["json!../../property.json!bust"], function(data){
    //     //if (data.apiUrl && (data.apiUrl.substring(0,1)  != '@' )) {
    //     properties = $.extend(true, {}, properties, data || {});
    //     //}
    //
    //     console.log("[Const.init ] Load properties : ") ;
    //     console.log(data);
    // });


    //console.log( require(["json!../../property.json!bust" )] );

    // $.getJSON("property.json", function(data) {
    //     //if (data.apiUrl && (data.apiUrl.substring(0,1)  != '@' )) {
    //         properties = $.extend(true, {}, properties, data || {});
    //     //}
    //
    //     console.log("[Const.init ] Load properties : ") ;
    //     console.log(properties) ;
    // });


    Obj.MEDIA_THUMB = 11;
    Obj.MEDIA_IMAGE = 12;
    Obj.MEDIA_VIDEO = 13;
    Obj.API_DEBUG = false;


    Obj.getProperties = function(name) {
        return  properties[name];
    };

    Obj.getAPIPath = function() {
        if (properties.apiUrl && (properties.apiUrl.substring(0,1)  == '@' )) {
            properties.apiUrl = testUrl;
            return testUrl;
        }
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