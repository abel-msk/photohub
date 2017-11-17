/**
 * Created by abel on 31.10.15.
 */

define( ["jquery"], function($) {

    var API_DEBUG = true;

    var JobQueue = function(){

        var queue = [],
            bisy = false;


        function debug_(msg) {
            if (typeof console !== "undefined") {
                console.log(msg);
            }
        }

        return {

            push: function (job) {
                API_DEBUG && debug_("[JobQueue] push request");
                queue.push(job);
                if (!bisy) {
                    this.next();
                }
            },

            next: function () {
                //var job = queue.pop();
                var job = queue.shift();
                bisy = false;
                if (typeof job == 'function') {
                    API_DEBUG && debug_("[JobQueue] execute request");
                    job();
                    bisy = true;
                }
            }
        }
    };

    var API = (function(){

        var API_URL, url_actions, defaultAjaxOptions,
            CALL_TIMEOUT_MSEC,
            callTimeouts = {},
            ajaxQueue = new JobQueue();

        API_URL = 'http://localhost:8081/api';
        //var TEST_SERVER_URL = 'http://localhost:8081/api';
        //var BASE_SERVER_URL = 'api';

        $.getJSON("property.json", function(data) {
            API_DEBUG && debug_("load property = " + data.apiUrl);
            if (data.apiUrl && (data.apiUrl.substring(0,1)  != '$' )) {
                API_URL =  data.apiUrl;
            }
        });


        url_actions = {
            "thumbUrl":["GET","","/thumb"],
            "imageUrl":["GET","","/image"],
            "ping": ["GET","","/ping"],
            "login": ["POST","","/login/login"],
            "logout": ["GET","","/logout"],
            "checkLogin": ["GET","","/login/check"],
            "siteTypes": ["GET","","/site/types"],
            "listSites": ["GET","","/site/"],
            "addSite": ["POST","json","/site/add"],
            "delSite": ["DELETE","","/site/"],
            "updateSite":["PUT","json","/site/"],
            "connectSite":["GET","","/site/"],
            "authSite":["PUT","json","/site/"],
            "disconnectSite":["GET","","/site/"],
            //"getLastScan":["GET","","/site/"],
            "listTasks":["GET","","/site/"],
            "startTask":["POST","","/site/"],
            "getTask":["GET","","/site/"],
            "logTask":["GET","","/site/"],
            // "cleanSite":["GET","","/site/"],
            // "listTasks":["GET","","/site/"],

            "listSched":["GET","","/site/"],
            "getShed":["GET","","/site/"],
            "setSched":["PUT","json","/site/"],
            "logSched":["GET","","/site/"],

            "photoList":["GET","","/list"],
            "getPhoto":["GET","","/photo/"],
            "siteUpload":["POST","json","/site/"]
        };

        defaultAjaxOptions = {
            async: true,
            dataType: 'jsonp',
            crossDomain: false,  /// Required for use other methods like POST UPDATE DELEE etc
            'xhrFields':{withCredentials: true}
        };

        //_lastResultCode = null;
        //_lastResultMessage = null;

        function debug_(msg) {
            if (typeof console !== "undefined") {
                console.log(msg);
                //var e = new Error();
                //console.log(e.stack);
            }
        }

        function getActionUri(actionName) {
            if (url_actions[actionName]) {
                return url_actions[actionName][2];
            } else {
                return "";
            }
        }
        function getActionUrl(actionName) {
            return API_URL + getActionUri(actionName);
        }
        function getActionMethod(actionName) {
            return url_actions[actionName][0];
        }
        function getActionContentType(actionName) {
            return url_actions[actionName][1];
            //    return "application/json; charset=utf-8";
            //}
            //return "application/x-www-form-urlencoded; charset=UTF-8";
        }
        function getParametersObject(actionName, parameters) {
            if ( getActionContentType(actionName)=== 'json') {
                return JSON.stringify(parameters);
            }
            return parameters;
        }


        CALL_TIMEOUT_MSEC = 5000;

        function onCallTimeout(onError) {
            if (typeof window.globalVidimaxAjaxCompleteHandler === "function") {
                window.globalVidimaxAjaxCompleteHandler();
            }
            API_DEBUG && debug_("onCallTimeout");
            if (typeof onError === "function") {
                onError({'rc':"600",'message':"Response timeout expired."});
            }
        }

        function setCallTimeout(onError, actionName) {
            try {
                if (callTimeouts[actionName] !== null) {
                    API_DEBUG && debug_("CallTimeout already set");
                    clearTimeout(callTimeouts[actionName]);
                }
                API_DEBUG && debug_("setCallTimeout");
                callTimeouts[actionName] = setTimeout(function (){
                    try {
                        onCallTimeout(onError);
                    } catch (e) {
                        debug_("[onError exception] " + actionName +". "+ e.stack);
                    }
                    nextCall();
                }, CALL_TIMEOUT_MSEC);
            } catch (e) {
                debug_('setCallTimeout ', e.stack);
            }
        }

        function clearCallTimeout(actionName) {
            try {
                if (callTimeouts[actionName] !== null) {
                    API_DEBUG && debug_("clearCallTimeout");
                    clearTimeout(callTimeouts[actionName]);
                } else {
                    API_DEBUG && debug_("CallTimeout already clean");
                }
            } catch (e) {
                debug_ ('clearCallTimeout', e.stack);
            }
        }

        function nextCall() {
            setTimeout(function (){ajaxQueue.next();}, 100);
        }

        //------------------------------------------------------------------------------
        //
        //      Prepare ajax options and push in queue for call
        //
        //------------------------------------------------------------------------------

        function call(actionName, callOptions, data, onSuccess, onError) {
            var actionNameUnique = actionName + new Date().getTime();

            //
            //   Prepare url
            //
            if (  ! callOptions.url ) {
                callOptions.url = getActionUrl(actionName);
            }

            //
            //   Prepare contentType
            //
            if (( getActionContentType(actionName) == 'json') && (! callOptions.contentType )) {
                callOptions.contentType =  "application/json; charset=utf-8";
            }

            var options = $.extend({}, defaultAjaxOptions, {
                actionNameUnique: actionNameUnique,
                method: getActionMethod(actionName),
                data: getParametersObject(actionName,data),

                success: function (response) {

                    clearCallTimeout(actionNameUnique);
                    try {
                        debug_("[" + actionName + "] Success.");
                        if ((typeof onSuccess === "function") && (response.rc === 0)) {
                            onSuccess(response);
                        }
                        else if (response.rc !== 0) {
                            debug_("[" + actionName + "] Bad response.");
                            var fireGlobalEvent = false;
                            if (typeof onError === "function") {
                                fireGlobalEvent = onError(response.rc,response.message,this);
                            }
                        }
                    } catch (e) {
                        debug_("[onSuccess exception] " + actionNameUnique +". "+ e.stack);
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    clearCallTimeout(actionNameUnique);

                    var respStatus = jqXHR.status;
                    var respTextObject = null;
                    if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
                        respTextObject = jqXHR.responseJSON.message;
                    }
                    else if ( ! respTextObject )  {
                        try {
                            respTextObject = JSON.parse(jqXHR.responseText);
                        } catch (e) {
                            respTextObject = errorThrown;
                        }
                    }
                    if (! respTextObject) {
                        respTextObject = jqXHR.state();
                    }

                    if ( respStatus == 0 ) {
                        respStatus = '600';
                    }

                    try {
                        debug_("[" + actionName + "] Error rc="+respStatus+", msg="+respTextObject);
                        var fireGlobalEvent = false;
                        if (typeof onError === "function") {
                            fireGlobalEvent = onError({'rc':respStatus, 'message':respTextObject,'object':null}, this);
                        }
                        if (fireGlobalEvent && (respStatus == 403 )) {
                            $(document).trigger('notauthorized', {'rc':respStatus, 'message':respTextObject.message, 'object':null, 'ajaxObject':this});
                        }
                    } catch (e) {
                        debug_("[onError exception] " + actionNameUnique +". "+ e.stack);
                    }
                },
                complete: function(xhr, textStatus) {
                    nextCall();
                }
            }, callOptions);

            ajaxQueue.push(function () {
                $.ajax(options);
                if (options.dataType === 'jsonp') {

                    //   в случае jsonp запускаем таймаут на случай неответа сервера Api
                    setCallTimeout(onError, actionNameUnique);
                }
            });
        }

        //------------------------------------------------------------------------------
        //
        //      Prepare ajax options and perform call
        //
        //------------------------------------------------------------------------------
        function callAndWait(actionName, callOptions, data, onSuccess, onError) {

            var contentType = callOptions.contentType;

            if (( getActionContentType(actionName) == 'json') && (! contentType )) {
                contentType =  "application/json; charset=utf-8";
            }

            var options = $.extend({}, defaultAjaxOptions, {
                //async: false,
                'dataType': 'json',
                'contentType': contentType,
                'method': getActionMethod(actionName),
                'url':  (callOptions&&callOptions.url?callOptions.url:getActionUrl(actionName)),
                'data': getParametersObject(actionName, data),

                'success': function (response) {
                    try {
                        debug_("[" + actionName + "] Success.");
                        if ((typeof onSuccess === "function") && (response.rc === 0)) {
                            onSuccess(response);
                        }
                        else if (response.rc !== 0) {
                            debug_("[" + actionName + "] Bad response.");
                            var fireGlobalEvent = false;
                            if (typeof onError === "function") {
                                fireGlobalEvent = onError(response.rc,response.message,this);
                            }
                        }
                    } catch (e) {
                        debug_("[onSuccess exception] " + actionName +". "+ e.stack);
                    }
                },

                'error': function (jqXHR, textStatus, errorThrown) {
                    var respStatus = jqXHR.status==0?"600":jqXHR.status;
                    var respTextObject = null;
                    try {

                        if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
                            respTextObject = jqXHR.responseJSON.message;
                        }
                        else if ( ! respTextObject )  {
                            try {
                                respTextObject = JSON.parse(jqXHR.responseText);
                            } catch (e) {
                                respTextObject = errorThrown;
                            }
                        }
                        respTextObject = (!respTextObject?jqXHR.state():respTextObject);

                        debug_("[api.callAndWait] do action=" + actionName + " has error rc="+respStatus+", msg="+respTextObject);
                        var fireGlobalEvent = false;
                        if (typeof onError === "function") {
                            //fireGlobalEvent = onError({'rc':respStatus, 'message':respTextObject,'object':null}, this);
                            fireGlobalEvent = onError(respStatus, respTextObject, this);
                        }
                        if (fireGlobalEvent && (respStatus == 403 )) {
                            $(document).trigger('notauthorized', {'rc':respStatus, 'message':respTextObject.message, 'object':null, 'ajaxObject':this});
                        }
                    } catch (e) {
                        debug_("[onError exception] " + actionName +". "+ e.stack);
                    }
                }
            });

            $.ajax(options);

        }




        return {

            init: function (apiUrl) {
                if (apiUrl && apiUrl !== "") {
                    API_URL = apiUrl;
                }
            },


            //   Ставит в очередь на исполнение полностью сформированный запрос
            recall: function(options,onError) {

                //  Отрезаем  callback параметер от прошлого запроса
                options.url =  options.url.replace(/&?callback=[^&]*&?/,'').replace(/_=[^&]*&?/,'').replace(/\?$/,'');
                ajaxQueue.push(function () {
                    $.ajax(options);
                    if (options.dataType === 'jsonp') {
                        // в случае jsonp запускаем таймаут на случай неответа сервера Api
                        setCallTimeout(onError, options.actionNameUnique);
                    }
                });
            },

            getActionUrl: function (actionName) {
                return API_URL + getActionUri(actionName);
            },

            login: function (username, password, rememberMe, callbackResult, callbackError) {
                API_DEBUG && debug_("[login] start login as " + username);
                call("login", {}, {"username": username, "password": password, "rememberMe": !!rememberMe},
                    callbackResult, callbackError);
            },

            checkLogin: function (callbackResult, callbackError) {
                debug_("[checkLogin] start ");
                callAndWait("checkLogin",{}, {}, callbackResult, callbackError);
            },

            logout: function (callbackResult, callbackError) {
                API_DEBUG && debug_("[logout] start ");
                call("logout", {}, {}, callbackResult, callbackError);
            },

            ping: function (callbackResult, callbackError) {
                API_DEBUG && debug_("[ping] start ");
                call("ping", {}, {}, callbackResult, callbackError);
            },

            siteTypes: function (callbackResult, callbackError) {
                API_DEBUG && debug_("[siteTypes] start ");
                call("siteTypes", {}, {}, callbackResult, callbackError);
            },

            listSites: function (callbackResult, callbackError) {
                API_DEBUG && debug_("[listSites] start ");
                call("listSites",{}, {}, callbackResult, callbackError);
            },

            addSite: function (siteObject, callbackResult, callbackError) {
                API_DEBUG && debug_("[addSite] start ");
                call("addSite", {}, siteObject, callbackResult, callbackError);
            },
            delSite: function (siteId, callbackResult, callbackError) {
                API_DEBUG && debug_("[addSite] start ");
                call("delSite", { 'url':getActionUrl('delSite')+siteId },{}, callbackResult, callbackError);
            },
            updateSite: function(siteObject, callbackResult, callbackError) {
                API_DEBUG && debug_("[aupdateSite] start ");
                call("updateSite", { 'url':getActionUrl('updateSite')+siteObject.id },siteObject, callbackResult, callbackError);
            },
            connectSite: function(siteId, url, callbackResult, callbackError) {
                API_DEBUG && debug_("[connectSite] start ");
                call("connectSite", { 'url':getActionUrl('updateSite')+siteId+"/connect" }, {'caller':url}, callbackResult, callbackError);
            },
            authSite: function(siteId, credObj, callbackResult, callbackError) {
                API_DEBUG && debug_("[authSite] start ");
                call("authSite", { 'url':getActionUrl('authSite')+siteId+"/auth" }, credObj, callbackResult, callbackError);
            },
            disconnectSite: function(siteId, callbackResult, callbackError) {
                API_DEBUG && debug_("[disconnectSite] start ");
                call("disconnectSite", { 'url':getActionUrl('disconnectSite')+siteId+"/disconnect" },{}, callbackResult, callbackError);
            },


            //getLastScan: function(siteId, callbackResult, callbackError) {
            //    API_DEBUG && debug_("[getLastScan] start ");
            //    call("getLastScan", { 'url':getActionUrl('getLastScan')+siteId+"/scan/last" },{}, callbackResult, callbackError);
            //},


            //  GET site/{id}/tasks
            listTasks: function(siteId, callbackResult, callbackError) {
                API_DEBUG && debug_("[listTasks] start ");
                call("listTasks", { 'url':getActionUrl('listTasks')+siteId+"/tasks" },{}, callbackResult, callbackError);
            },

            //  PUT site/{id}/task
            startTask: function(siteId, taskName, callbackResult, callbackError) {
                API_DEBUG && debug_("[startTask] start ");
                call("startTask", { 'url':getActionUrl('startTask')+siteId+"/task" },{'taskname':taskName }, callbackResult, callbackError);
            },

            //  GET site/{id}/task/{id}
            getTask: function(siteId, taskId, callbackResult, callbackError) {
                API_DEBUG && debug_("[getTask] start ");
                call("getTask", { 'url':getActionUrl('getTask')+siteId+"/task/"+taskId },{}, callbackResult, callbackError);
            },


            //  GET site/{id}/tasklog/{name}
            logTask: function(siteId, taskname, callbackResult, callbackError) {
                API_DEBUG && debug_("[logTask] start ");
                call("logTask", { 'url':getActionUrl('logTask')+siteId+"/tasklog/"+taskname },{}, callbackResult, callbackError);
            },

            // scanSite: function(siteId, options,callbackResult, callbackError) {
            //     API_DEBUG && debug_("[scanSite] start ");
            //     call("scanSite", { 'url':getActionUrl('scanSite')+siteId+"/scan" },options, callbackResult, callbackError);
            // },
            // cleanSite: function(siteId, options, callbackResult, callbackError) {
            //     API_DEBUG && debug_("[cleanSite] start ");
            //     call("cleanSite", { 'url':getActionUrl('cleanSite')+siteId+"/clean" },options, callbackResult, callbackError);
            // },

            listSched: function(siteId, callbackResult, callbackError){
                API_DEBUG && debug_("[listSched] start ");
                call("listSched", { 'url':getActionUrl('listSched')+siteId+"/schedules"},{}, callbackResult, callbackError);
            },

            getSched: function(siteId, taskName, callbackResult, callbackError){
                API_DEBUG && debug_("[getSched] start ");
                call("getSched", { 'url':getActionUrl('getSched')+siteId+"/schedule/"+ taskName },{}, callbackResult, callbackError);
            },

            setSched: function(siteId, object, callbackResult, callbackError) {
                API_DEBUG && debug_("[setSched] start ");
                call("setSched", { 'url':getActionUrl('setSched')+siteId+"/schedule/"+ object.taskName}, object, callbackResult, callbackError);
            },

            logSched: function(siteId, taskName, callbackResult, callbackError) {
                API_DEBUG && debug_("[logSched] start ");
                call("logSched", { 'url':getActionUrl('logSched')+siteId+"/schedule/"+ taskName + "/log"}, {}, callbackResult, callbackError);
            },




            photoList: function(options, callbackResult, callbackError) {
                API_DEBUG && debug_("[photoList] start ");
                call("photoList", {}, options,callbackResult, callbackError);
            },

            getPhoto: function (photoId, callbackResult, callbackError) {
                API_DEBUG && debug_("[addSite] start ");
                call("getPhoto", { 'url':getActionUrl('getPhoto')+photoId },{}, callbackResult, callbackError);
            }

        };

    }());

return API;

});
