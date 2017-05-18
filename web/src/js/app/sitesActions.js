/**
 * Created by abel on 14.12.15.
 */

define(["jquery","logger","modalDialog","api"],function ($,logger,Dialog,Api) {
    "use strict";

    var actionClass = (function () {

        var defaultOptions = {
            'siteId': null,
            'name': "scan",
            'date': null,
            'status': "OK",
            'message': ""
        };

        var CALL_TIMEOUT_MSEC = 5000;
        var monitorTask = null;
        var currentSiteId = null;


        function changeState(data) {
            $("body").trigger("scanner.changeState",data);
        }

        //------------------------------------------------------------------------
        //
        //   Вызываем API для получения статуса задачи.
        //   Если задача в заботе то заряжаем повторную проверку через CALL_TIMEOUT_MSEC
        //   Генерируем событие с текущим статусом задачи для состояния.
        //   Перезаряжаем задучу только если сайт не сменился
        //
        //------------------------------------------------------------------------
        function getStatus(siteId) {

            if ( currentSiteId != siteId ) return;

            Api.scanSite(siteId,{'start':false},
                //  on Success
                function(response) {
                    if (response.object) {
                        logger.debug("[scanner.getStatus] SiteId=" + siteId + ", status=" + response.object.status);
                        changeState(response.object);
                        if ((response.object) && (response.object.status == "RUN")) {
                            monitorStatus(siteId);
                        }
                    }
                    // No Active scan tasks, retrieve history.
                    else {
                        getScanHistory(siteId,"scan");
                        logger.debug("[scanner.getStatus] response empty, Try get last task from history.");
                    }

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
            )
        }
        //------------------------------------------------------------------------
        //
        //   Вызываем API для получения списка полседних задач.
        //
        //------------------------------------------------------------------------
        function getScanHistory(siteId,taskName) {
            if ( currentSiteId != siteId ) return;

            Api.listTasks(siteId,
                //  on Success
                function(response) {
                    if (response.object) {
                        var taskRecord = response.object[0];
                        changeState(taskRecord);
                    }
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
            )
        }

        //------------------------------------------------------------------------
        //    Установка перезапуска проверки через CALL_TIMEOUT_MSEC
        //------------------------------------------------------------------------
        function monitorStatus (siteId){
            monitorTask = setTimeout(function (){
                try {
                    getStatus(siteId)
                } catch (e) {
                    logger.debug("[monitorStatus] on timeout execution error ", e.stack);
                }
            }, CALL_TIMEOUT_MSEC);
        }


        //------------------------------------------------------------------------
        //
        //    Инициализируем  класс для нового siteId
        //    запускаем проверку состояни задачи
        //
        //------------------------------------------------------------------------
        function action(options) {
            this.options = $.extend(true,{},defaultOptions,options || {});
            if (! this.options.siteId) {
                logger.debug("[action.init] siteId cannot be null");
                throw Error();
            }
            currentSiteId = this.options.siteId;
            getStatus(this.options.siteId);

        }

        //------------------------------------------------------------------------
        //
        //      Запуск сканирования сайта
        //
        //------------------------------------------------------------------------
        action.prototype.doScan = function() {

            if (! this.options.siteId) {
                logger.debug("[action.doScan] siteId cannot be null");
                throw Error();
            }
            var siteId = this.options.siteId;

            Api.scanSite(this.options.siteId,{'start':true},
                //  on Success
                function(response) {
                    if (response.object) {
                        logger.debug("[action.startScan] SiteId=" + siteId + ", status=" + response.object.status);
                        changeState(response.object);
                        if (response.object.status == "RUN") {
                            monitorStatus(siteId);
                        }
                    }
                    else {
                        logger.debug("[action.startScan] Error. Api success return w/o task object");
                    }
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
            )
        };

        //------------------------------------------------------------------------
        //
        //      Удаление всех фото объектов сайта из базы
        //
        //------------------------------------------------------------------------
        action.prototype.doClean = function() {

            if (! this.options.siteId) {
                logger.debug("[action.doScan] siteId cannot be null");
                throw Error();
            }

            Api.cleanSite(this.options.siteId,{'start':true,'deleteSite':false},
                //  on Success
                function(response) {
                        logger.debug("[action.doClean] rc=" + response.rc);

                    //TODO : OPEN cleaning monitor dialog
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
            )
        };

        //------------------------------------------------------------------------
        //
        //      Удаление сайта из базы
        //
        //------------------------------------------------------------------------
        action.prototype.deleteSite = function(model) {

            if (! model) {
                logger.debug("[action.deleteSite] model parameter cannot be null");
                throw Error();
            }
            //var siteId = this.options.siteId;
            var siteName = model.get("name");
            Dialog.open({
                'title': "Site remove",
                'text': "Are you sure want to delete site '" + siteName + "' ?",
                'buttons': {
                    OK: function () {
                        //var model = collection.get(siteId);
                        model.destroy({wait: true});
                    },
                    'Cancel': function () {
                    }
                }
            });
            // Get Site Id
        };

        return action;
    }());


    //------------------------------------------------------------------------
    //
    //   Статический метод - Добавление сайта
    //
    //------------------------------------------------------------------------
    actionClass.add = function(collection) {

        $('#add_site_dialog .inline-error').hide();
        $('#site_name_group').removeClass('has-error');

        Api.siteTypes(
            function (response) {
                $('#site_type').html("");
                _.each(response.object, function (Item) {
                    $('#site_type').append('<option>' + Item + '</option>');
                }, this);
                $('#site_type').val('Local');

                //------------------------------------------
                //   Dialog OK handler
                //
                $('#save_new_site_btn').one('click', function (event) {
                    var SiteName = $('#new_site_name').val();
                    var SiteType = $('#site_type').val();

                    //   Validate Site Name.  It cannot be empty.
                    if (SiteName.length === 0 || !SiteName.trim()) {
                        $('#site_name_group').addClass('has-error');
                        $('#add_site_error_msg').text("Site name and type are required.")
                        $('#add_site_dialog .inline-error').show();

                        $('#new_site_name').one('change', function () {
                            $('#site_name_group').removeClass('has-error');
                            $('#add_site_dialog .inline-error').hide();
                        });
                    }
                    else {
                        //------------------------------------------
                        //    Pass to create new site object
                        //
                        $("#add_site_dialog").modal('hide');
                        collection.create({'name': SiteName, 'connectorType': SiteType}, {'wait': true});
                    }
                });

                //------------------------------------------
                //  Open add dialog.
                //
                $("#add_site_dialog").modal({'backdrop': true});
            },
            function (response) {
                Dialog.open({
                    'error': true,
                    'title': "Server error",
                    'text': response.message,
                    'buttons': {
                        OK: function () {
                        }
                    }
                });
                return false;
            }
        );
    };







    return actionClass;
});