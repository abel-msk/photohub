/**
 * Created by abel on 23.10.15.
 */
define(["jquery","api","modalDialog","logger"],function ($,Api,Dialog,logger) {
    "use strict";

    //$(document).on('notauthorized', function(){
    //
    //});

    var loginClass = (function () {
        var defaultOptions = {
            loginONFail: true
        };

        function html(options) {

            var stringHtml =
                '<div id="login-panel" class="modal fade col-md-offset-2 col-md-8" role="dialog" tabindex="-1" style="padding-top: 80px;">'+
                '    <div class="modal-dialog">'+
                '        <div class="modal-content">'+
                '            <div class="modal-header">'+
                '                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>'+
                '                <h4>Enter you user name and password</h4>'+
                '            </div>'+
                '            <div class="modal-body">'+
                //'                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>'+
                //'                <div style="padding: 40px 100px 40px 100px;">'+
                //'                <h4>Enter you user name and password</h4>'+
                '                    <div class="form-horizontal">'+
                '                        <div class="form-group ">'+
                '                            <label for="login" class="col-sm-3">Login</label>'+
                '                            <div class="col-sm-9">'+
                '                               <input id="username" type="text" class="form-control"  placeholder="Login">'+
                '                            </div>'+
                '                        </div>'+
                '                        <div class="form-group">'+
                '                            <label for="password" class="col-sm-3" >Password</label>'+
                '                            <div class="col-sm-9">'+
                '                               <input id="password" type="password" class="form-control" placeholder="Password">'+
                '                            </div>'+
                '                        </div>'+
                '                        <div  class="inline-error text-center" style="display: none;">'+
                '                           <div id="login_err_title" class="title"></div>'+
                '                           <div id="login_err_message" class="message"></div>'+
                '                        </div>'+
                '                    </div>'+
                //'                </div>'+
                '            </div>'+
                '            <div class="modal-footer">'+
                '                        <button id="doLogin" type="button" class="btn btn-primary">Login</button>'+
                '                        <button id="doCancel" type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>'+
                '            </div>'+
                '        </div>'+
                '    </div>'+
                '</div>';

            return stringHtml;
        }

        ////   Display connect/login error  inside login window
        //function onError(rc,msg) {
        //    logger.debug("[login] Error rc=" + rc + ", msg= " + msg);
        //
        //    if ( this.$el ) {
        //        //  Insert  response in login dialog
        //        this.$el.find('#error_msg').text(msg);
        //        this.$el.find('#error_msg').show();
        //    }
        //    else {
        //        if ( rc == 403 ) { return true; }  //  Forbiden - fire event for open login dialog
        //        //onError(response.rc,response.message);
        //        Dialog.open({
        //            'error': true,
        //            'title': "Connect",
        //            'text': msg,
        //            'buttons': {OK: function(){}}
        //        });
        //    }
        //    return false;
        //}

        function Login(options) {
            this.options = $.extend(true,{},defaultOptions,options || {});
            this.options = null;
            var caller = this;
            this.lastCallSettings = null;

            $(document).on('notauthorized',function(event,params) {
                //var caller = event.data.caller;
                if (params.ajaxObject) {
                    caller.lastCallSettings = params.ajaxObject;
                }
                if ( ! caller.$el) {
                    //caller.open.call(caller);
                    caller.open();
                }
            });
        }

        //---------------------------------------------------------------
        //
        //   Check is current auth token is valid
        //
        //---------------------------------------------------------------
        Login.prototype.checkLogin = function(openDialog,onSuccess) {
            var caller = this;
            Api.checkLogin(
                function(response)  {
                    if (typeof onSuccess == 'function') {
                        logger.debug("[Login] User is logged in." + response);
                        onSuccess(response);
                    }
                },
                function(rc,message) {  // onError
                    if ( openDialog ) {
                        logger.debug("[login.checkLogin] Error: rc="+rc+" msg="+message );
                        return caller.onError({'rc':rc,'message':message},onSuccess);
                    }
                    return false;
                }
            )
        };

        //---------------------------------------------------------------
        //
        //   Open login Dialog
        //
        //---------------------------------------------------------------
        Login.prototype.open = function(onLogin) {
            logger.debug("[login.open] Open login dialog");
            //this.$el.find('#login-panel .inline-error').hide(); //  Clear error zone

            //   Если логин диалог уже открыт то сначала закрываем его
            if ( this.$el) {
                this.$el.hide();
            }

            var dialogHtml = html(this.options);
            this.$el = $(dialogHtml).clone().appendTo('body');
            this.$el.find("#doLogin").on("click",{caller:this}, function(event){

                var caller = event.data.caller;

                var passwd = caller.$el.find("#password").val();
                var username = caller.$el.find("#username").val();
                var rememberMe = caller.$el.find("#rememberMe").val();

                caller.do(
                    {
                        'login': username,
                        'password': passwd,
                        'rememeberMe': rememberMe == null ? 'false' : rememberMe
                    },
                    onLogin);
            });

            this.$el.modal({'backdrop':true})
                .one('hidden.bs.modal',{'caller':this},
                function(event){
                    $(this).remove();
                    event.data.caller.$el = null;
                });
        };

        //---------------------------------------------------------------
        //
        //   Call Api for login to app
        //
        //---------------------------------------------------------------
        Login.prototype.do = function(userObject,onLogin) {

            var caller = this;

            Api.login(userObject.login,userObject.password,userObject.rememberMe,
                //  OnSuccess
                function(response) {
                    if (response.rc == 0) {
                        try {
                            logger.debug("[Login.do] Login success.");
                            $(caller.$el).modal('hide');

                            if ( typeof onLogin == "function") {
                                onLogin();
                            }
                            else if (caller.lastCallSettings) {
                                Api.recall(caller.lastCallSettings);
                            }
                        } catch (e) {
                            logger.trace("[login.do] Error", e.stack);
                        }
                    }
                },
                //  OnError
                function(response) {
                    return  caller.onError.call(caller,response);
                }
            );
        };

        //---------------------------------------------------------------
        //
        //   Display connect/login error  inside login window or as modalDialog
        //
        //---------------------------------------------------------------
        Login.prototype.onError = function(response,onLogin) {
            //logger.debug("[login] Error rc=" + response.rc + ", msg= " + response.message);

            //   Логин диалог уже открыт, выводим сообщение внутри логн формы
            if ( this.$el ) {
                //  Insert  response in login dialog
                this.$el.find('#login-panel .inline-error .message').text(response.message);
                this.$el.find('#login-panel .inline-error').show();
            }

            //   Логин Диалог не открыт - принимаем решение выводить сообщение об ошибке
            //   или открывть Логин диалог
            else {
                if ( response.rc == 600 )  {  response.message = "Сервер недоступен." ; }  // 600 сервер недоступен
                if ( response.rc == 403 )  { return true; }  // 403 пользователь не авторизован для запросов не checkLogin и не Login
                if ( response.rc == 52 )   { this.open(onLogin); return false;  }

                Dialog.open({
                    'error': true,
                    'title': "Server error",
                    'text': response.message,
                    'buttons': {OK: function(){
                    }}
                });
            }
            return false;
        };

        return Login;
    }());


    return new loginClass();
});
