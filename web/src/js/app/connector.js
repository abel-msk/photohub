/**
 * Created by abel on 29.11.15.
 */



define(["jquery","logger","modalDialog","api"],function ($,logger,Dialog,Api) {
    "use strict";


    var conectorObj = (function () {
        var defaultOptions = {};

        var CONNECTOR_STATE_ID = "#site_connect_state";

        var CONNECT_BTN_ID = "#connect_btn";
        var CONNECTOR_STATE_BTN_ATTR = "data-state";
        var CONNECTO_BTN_SITE_ID_ATTR = "site";



        function renderDialog(cred) {
            var element = $('#ext-login-dialog');
            //
            //    Dialogs elements does not exist on a page
            if (!element.length) {
                var html = '<!-- CONNECT DIALOG -->' +
                    '<div id="ext-login-dialog" class="modal fade col-sm-12 col-md-offset-1 col-md-10" role="dialog" tabindex="-1" style="padding-top: 110px;">' +
                    '    <div class="modal-dialog">' +
                    '        <div class="modal-content">' +
                    '            <div class="modal-header">' +
                    '                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
                    '                <h4 class="modal-title">Connect to </h4>' +
                    '            </div>' +
                    '            <div class="modal-body">' +
                    '                <div id="contentBody"></div>' +
                    '                 <div id="user-message" class="row-block">'+cred.userMessage+'</div>'+
                    '                <div id="auth-url" class="row-block" style="overflow-x: scroll"><a href="'+cred.userLoginFormUrl+'" target="_blank" >'+cred.userLoginFormUrl+'</a></div> '+
                    '		         <div class="form-group row-block">' +
                    '                    <label for="auth_code">Use аuthorization code you receive from site by url above</label>' +
                    '                    <input type="text" class="form-control" id="auth_code" placeholder="Enter authorization code here"> ' +
                    '                </div>' +
                    '                <!--<iframe src="' + cred.userLoginFormUrl + '" class="ext-login-frame" width="100%" height="100%"></iframe>-->' +
                    '            </div>' +
                    '            <div class="modal-footer">' +
                    '                <button id="get_code_cmd" type="button" class="btn btn-primary" data-dismiss="modal">OK</button>' +
                    '                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>' +
                    '            </div>' +
                    '        </div>' +
                    '    </div>' +
                    '</div>';

                return $(html).clone().appendTo("body");
            }
            //
            //    Dialogs elements does not exist on a page
            else {
                return element;
            }
        }


        //------------------------------------------------------------------------
        //   Generate event when connector state changed.
        //   Usually after doConnect and doDisconnect
        //------------------------------------------------------------------------
        function changeState(state,siteId) {

            var btnLabel = "Connect";
            var statusClass;

            switch (state) {
                case 'DISCONNECT':
                    btnLabel = "Connect";
                    statusClass = "status-err";
                    break;
                case 'AUTH_WAIT':
                    btnLabel = "Reconnect";
                    statusClass = "status-warn";
                    break;
                case 'CONNECT':
                    btnLabel = "Disconnect";
                    statusClass = "status-succ";
                    break;
            }

            if (CONNECT_BTN_ID) {
                $(CONNECT_BTN_ID).attr(CONNECTOR_STATE_BTN_ATTR, state).text(btnLabel);
                $(CONNECTOR_STATE_ID).html('<span class="' + statusClass + '">' + state + '</span>')
            }
            $("body").trigger("connector.changeState",{ 'state' : state, 'siteId': siteId});
        }


        //------------------------------------------------------------------------
        //
        //   returned Cred object :
        //      userLoginFormUrl
        //      accessToken
        //      userMessage
        //      properties[]
        //      state
        //      authReceiveType    - AUTH_TYPE_NET / AUTH_TYPE_DIRECT
        //
        //------------------------------------------------------------------------
        function doConnect(siteId) {

            //hash: "#siteId=41"
            //host: "localhost:63342"
            //hostname: "localhost"
            //href: "http://localhost:63342/photohub2-client2/src/sites.html#siteId=41"
            //origin: "http://localhost:63342"
            //pathname: "/photohub2-client2/src/sites.html"
            //port: "63342"
            //protocol: "http:"

            var backUrl = window.location.protocol +"//"+window.location.host + window.location.pathname + "#siteId="+siteId;


            Api.connectSite(siteId,backUrl,

                //  OnSuccess
                function(dirtyResponse){
                    var respState = dirtyResponse.object.state;
                    var response = dirtyResponse.object;

                    logger.debug("[connector.doConnect] success. State="+respState+", authReceiveType=" + response.authReceiveType);


                    if (respState== "AUTH_WAIT") {
                        if ( response.authReceiveType == "AUTH_TYPE_NET"  ) {
                            window.location.href = response.userLoginFormUrl;
                        }
                        else {
                            try {
                                var $dialog = renderDialog(response);
                                var $dialogSelector = '#' + $dialog.attr('id');

                                $($dialogSelector + ' #get_code_cmd').one("click", function (event) {
                                    var code = $($dialogSelector + ' #auth_code').val();
                                    response.accessToken = code;
                                    doAuth(siteId, response);
                                    $($dialogSelector).modal('hide');
                                });
                                $($dialogSelector).modal('show');
                            } catch (e) {
                                logger.trace("[connector.doConnect.success] Error ",e);

                            }
                        }
                    }
                    changeState(respState,siteId);
                },

                //   OnError
                function(response){
                    Dialog.open({
                        'error': true,
                        'title': "Server error",
                        'text': response.message,
                        'buttons': {OK: function(){}}
                    });
                }
            );
        }

        //------------------------------------------------------------------------
        //
        //   AUTH   pass auth code for completing auth flow
        //
        //------------------------------------------------------------------------
        function doAuth(siteId,credential) {
            //credential

            Api.authSite(siteId,credential,

                //  on Success
                function(response){
                    // set connection state to connect
                    logger.debug("[connector.doAuth] success. State="+response.object.state );
                    changeState(response.object.state,siteId);
                },
                //  on Error
                function(response){
                    Dialog.open({
                        'error': true,
                        'title': "Server error",
                        'text': response.message,
                        'buttons': {OK: function(){}}
                    });
                }
            );
        }

        //------------------------------------------------------------------------
        //
        //   returned site object :
        //
        //------------------------------------------------------------------------
        function doDisconnect(siteId) {
            //var caller = this;
            Api.disconnectSite(siteId,

                //  OnSuccess
                function(response){
                    var respState = response.object.state;
                    logger.debug("[connector.doConnect] success. State="+respState );

                    changeState(respState);
                },

                //   OnError
                function(response){
                    Dialog.open({
                        'error': true,
                        'title': "Server error",
                        'text': response.message,
                        'buttons': {OK: function(){}}
                    });
                }
            );
        }


        //------------------------------------------------------------------------
        //
        //   Init and open connector object
        //
        //------------------------------------------------------------------------
        function Connect(options) {
            this.$dialog = null;
            this.options = $.extend(true,{},defaultOptions,options || {});



            $(CONNECT_BTN_ID).on('click', {'caller': this}, function (event) {
                logger.debug("[connect_btn] Click.");
                var caller = event.data.caller;
                caller.open();
            });

        }

        //------------------------------------------------------------------------
        //
        //   Реагирует на нажатие кнпки.  В зависимости от текущего состояния
        //   запускает connect или disconnect
        //
        Connect.prototype.open = function () {

            var curState = $(CONNECT_BTN_ID).attr(CONNECTOR_STATE_BTN_ATTR);
            var siteId = $(CONNECT_BTN_ID).attr(CONNECTO_BTN_SITE_ID_ATTR);

            if (siteId) {

                switch (curState.toUpperCase()) {
                    case 'DISCONNECT' :
                        doConnect(siteId);
                        break;
                    case 'AUTH_WAIT':
                        doConnect(siteId);
                        break;
                    case 'CONNECT':
                        doDisconnect(siteId);
                        break;
                }
            }
        };

        //------------------------------------------------------------------------
        //
        //   Публичный метод для изменения состояния кнопки Connect
        //
        Connect.prototype.setState = function(state, siteId){
            changeState(state,siteId);
        };

        return Connect;
    }());


    return conectorObj;
});