/**
 * Created by abel on 12.12.15.
 */


define(["jquery","logger","moment"],function ($,logger,moment) {
    "use strict";

    var MEDIA_PHOTO = "12";
    var MEDIA_THUMB = "11";

    var utilsObj = {};
    var monthStr =  ["Янв.","Февр.","Март.","Апр.", "Майя", "Июня", "Июля", "Авг.", "Сент.", "Окт.", "Ноя.", "Дек."];

    // var moment = null;
    // try {
    //     moment = require('moment');
    // }
    // catch (e){
    //     logger.debug("[utils], Module 'moment' not found.");
    // }


    var dd = new Date();
    var st = "dfasfasf"
    logger.debug("Type of date - "+ typeof st);


    /**
     * Парсинг параметров запроса
     * @param params
     * @param isHash
     * @return {Object}
     */
    utilsObj.paramsToMap = function(params, isHash) {

        var options = {};

        if (!isHash) {
            params = params.replace(/.*\?/, "?");
        }

        if (!isHash) {
            params = params.replace(/#.*/, '');
        }
        // ".*(#|\\?)(([\\w\\d]*)=?([^&#]*)?&?)+$" - universal
        if (params.match((isHash ? "#" : "\\?") + "(([\\w\\d]*)=?([^&]*)?&?)+$")) {
            var paramvals = params.replace(isHash ? "#" : "?", "").split("&");
            for (var i = 0; paramvals && i < paramvals.length; i++) {
                if (paramvals[i] != null && paramvals[i] != "") {
                    if (paramvals[i].indexOf("=") > -1) {
                        var pv = paramvals[i].split("=");
                        if (pv && pv.length == 2 && typeof pv[0] != "undefined" && typeof pv[1] != "undefined") {
                            options[pv[0]] = pv[1];
                        }
                    } else {
                        options[paramvals[i]] = paramvals[i];
                    }
                }
            }
        }
        return options;
    };

    utilsObj.hashParamsToMap =  function (params) {
        return this.paramsToMap(params, true);
    };


    utilsObj.toDateTimeString = function(timeStamp) {
        var createDate = new Date(timeStamp);
        var monthDigit = parseInt(createDate.getMonth())+1;

        return createDate.getDate()+' '+
                //(monthDigit < 10?'0'+monthDigit:monthDigit)+' '+
            monthStr[createDate.getMonth()]+' '+
            createDate.getFullYear()+" "+
            createDate.getHours()+":"+
            (createDate.getMinutes() < 10?("0"+createDate.getMinutes()):createDate.getMinutes()) + ":" +
            (createDate.getSeconds() < 10?("0"+createDate.getSeconds()):createDate.getSeconds());
    };

    //  Receive date string in format like: 2015-03-25T12:00:00 or  date object
    //  Return 25.03.2015
    utilsObj.toDateString = function(timeStamp) {
        if ((typeof timeStamp == "string") && (moment)) {
            return moment(timeStamp).format("L");
        }
        var createDate = new Date(timeStamp);
        var monthDigit = parseInt(createDate.getMonth()) + 1;
        return createDate.getDate() + '.' + (monthDigit < 10 ? '0' + monthDigit : monthDigit) + '.' + createDate.getFullYear();

    };

    //  Receive date string in format like: 2015-03-25T12:00:00   or  2013-02-04T22:44:30.652Z
    //  Return 20150325
    utilsObj.getDateStamp = function(dateStr) {
        var createDate = new Date(dateStr);
        var monthDigit = parseInt(createDate.getMonth())+1;
        return createDate.getFullYear() +  (monthDigit < 10?'0'+monthDigit:monthDigit)  + createDate.getDate();
    };

    utilsObj.dateStr2obj = function(dateStr,short) {
        if (moment) {
            if (/^[\d\.\/]+$/.test(dateStr)) {
                return new Date(moment(dateStr,"l").format());
            }
            return new Date(moment(dateStr).format());
        }
        return new Date(dateStr);
    };


    utilsObj.isArray = function(input) {
        return input instanceof Array || Object.prototype.toString.call(input) === '[object Array]';
    };

    utilsObj.hasClass = function(el, cn) {
        return (' ' + el.className + ' ').indexOf(' ' + cn + ' ') !== -1;
    };

    utilsObj.isDate = function(obj) {
        return (/Date/).test(Object.prototype.toString.call(obj)) && !isNaN(obj.getTime());
    };

    utilsObj.getMediaObject = function(object) {
        var mediaList = object.mediaObjects;
        for (var i = 0; i < mediaList.length; i++) {
            if  (mediaList[i].type == MEDIA_PHOTO) {
                return mediaList[i];
            }
        }
        return null;
    };


    //------------------------------------------------------------------------------
    //
    //    Сканирует дерево DOM объекта от листа  помеченного элементом 'element'
    //    к корню до момента когда найдется тег с классом  'className'
    //
    //    Возвращает найденный элемент либо   null  если сканирование поднялось до тега "body"
    //
    utilsObj.getParent = function(element,className) {
        var res = element;
        while((! res.classList.contains(className)) && (res.tagName != "body")) {
            res = res.parentElement;
        }
        return res;
    };


    return  utilsObj;
});