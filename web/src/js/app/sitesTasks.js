/**
 * Created by abel on 29.04.17.
 */


define(["jquery","api","modalDialog","utils","logger","moment"],
    function($,Api,Dialog,Utils,logger,moment) {

        "use strict";


        var DEBUG = true;
        var TASKLIST_HOLDER_ID = "#task-list";
        var CALL_TIMEOUT_MSEC = 5000;


        var monitorTask = null;

        //------------------------------------------------------------------------
        //
        //    Создание HTML для  отображения строки из таблицы списка задач
        //
        //------------------------------------------------------------------------
        function taskRowHTML(siteId,TaskRecord) {

            logger.debug("[sitesTasks.taskRowHTML] Render TaskList ("+TaskRecord.name+"): ", TaskRecord);


            var statusHtml = "";
            var dateHTML   = "";
            var activeBtn  = true;

            if (TaskRecord.status) {
                switch (TaskRecord.status) {
                    case 'IDLE':
                    case 'RUN':
                        activeBtn = false;
                        dateHTML = moment?moment(TaskRecord.startTime).format("LLL"):Utils.toDateString(TaskRecord.startTime);
                        statusHtml = '<span class="status-succ">RUNNING. <i class="fa fa-spinner fa-pulse fa-fw"></i></span>';
                        break;
                    case 'ERR':
                        dateHTML = moment?moment(TaskRecord.stopTime).format("LLL"):Utils.toDateString(TaskRecord.stopTime);
                        statusHtml = '<span class="status-err">ERROR:</span> <span>&nbsp;' + TaskRecord.message + '</span>';
                        break;
                    default:  // FIN
                        dateHTML = moment?moment(TaskRecord.stopTime).format("LLL"):Utils.toDateString(TaskRecord.stopTime);
                        statusHtml = '<span class="status-norm">FINISHED</span>';
                        break;
                }
            }

            return '<div class="row panel-body-row">' +
                '   <span class="text-right row-label">'+TaskRecord.name+'</span>' +
                '   <span  class="row-data">'+statusHtml+'</span>' +
                '   <span  class="row-data">'+dateHTML+'</span>' +
                '   <span class="row-cmd pull-right">' +
                '      <a  data-siteId="'+siteId+'" data-taskName="'+TaskRecord.name+'" class="run link text-right '+(activeBtn?"":"disable")+'">Run task</a>' +
                '   </span>' +
                '   <span class="row-cmd pull-right">' +
                '      <a  data-siteId="'+siteId+'" data-taskName="'+TaskRecord.name+'" class="log link text-right">Show logs</a>' +
                '   </span>' +
                '</div>';
        }


        //------------------------------------------------------------------------
        //
        //    Установка перезапуска проверки через CALL_TIMEOUT_MSEC
        //
        //------------------------------------------------------------------------
        function monitorStatus (siteId) {
            monitorTask = setTimeout(function () {
                try {
                    renderTasksList(siteId)
                } catch (e) {
                    logger.debug("[monitorStatus] on timeout execution error ", e.stack);
                }
            }, CALL_TIMEOUT_MSEC);
        }


        //------------------------------------------------------------------------
        //
        //   Загрузка списка задач
        //
        //   Tasks list return:
        //    {
        //        "message": "string",
        //        "object": [
        //        {
        //            "id": "string",
        //            "message": "string",
        //            "name": "string",
        //            "startTime": "2017-05-02T15:39:40.475Z",
        //            "status": "string",
        //            "stopTime": "2017-05-02T15:39:40.475Z"
        //        }
        //    ],
        //        "rc": 0
        //    }
        //
        //------------------------------------------------------------------------
        function renderTasksList(siteId) {

            Api.listTasks(siteId,
                //  on Success
                function(response) {
                    try {
                        if (response.object) {
                            var hasRunningTask = false;
                            var taskRecordsList = response.object;
                            var listHtml = "";

                            for (var i = 0; i < taskRecordsList.length; i++) {
                                listHtml =  listHtml + taskRowHTML(siteId, taskRecordsList[i]);
                                if ( taskRecordsList[i].status == "RUN") {
                                    logger.debug("[sitesTasks.renderTasksList]  Found running task  "+taskRecordsList[i].name+". Start ststue monitoring.");
                                    hasRunningTask = true;
                                }
                            }
                            $(TASKLIST_HOLDER_ID).html("").append(listHtml);

                            if (hasRunningTask) {
                                monitorStatus(siteId);
                            }
                        }
                    }
                    catch (e) {
                        logger.debug("[SitesTask.renderTasksList] Error: ",e);
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




        /*------------------------------------------------------------------------
        *
        *
        *
        *    SITETASK OBJECT INIT
        *
        *
         ------------------------------------------------------------------------*/
        function SitesTask(model) {

            if ( ! model ) {
                throw Error("[SitesTask.init] Model parameter required.");
            }

            this.siteId = model.get("id");
            this.cronList = {};

            //-----------------------------------------------
            //   Activate btn listeners for task running
            //-----------------------------------------------
            $('body').off("click",'a.run')
                .on("click",'a.run',{'caller':this},function(event){
                    var caller = event.data.caller;
                    var target = event.target || event.srcElement;
                    if (!target) { return;  }
                    caller.runTask.call(caller,target.getAttribute("data-siteId"),target.getAttribute("data-taskName"));
                })
            .off("click",'a.log')
                .on("click",'a.log',{'caller':this},function(event){
                    var caller = event.data.caller;
                    var target = event.target || event.srcElement;
                    if (!target) { return;  }
                    caller.getLog.call(caller,target.getAttribute("data-siteId"),target.getAttribute("data-taskName"));
                })
                ;

            //  Get tasks
            renderTasksList(this.siteId);
        }

        //------------------------------------------------------------------------
        //
        //     Получение списка задач, обработка рендеринг и если есть запущенные задачии
        //     установка мониторинга завершения.
        //
        //------------------------------------------------------------------------
        SitesTask.prototype.runTask = function(siteId, taskName) {
            DEBUG && logger.debug("[SitesTask.runTask] Running task for site="+siteId+", taskname="+taskName);
            var caller = this;

            Api.startTask(siteId, taskName,
                function(response) {
                    try {
                        if (response.object) {
                            logger.debug("[SitesTask.runTask] SiteId=" + siteId + ", Task=" + taskName);
                            renderTasksList(siteId);
                        }
                        else {
                            logger.debug("[action.startScan] Error. Api success return w/o task object");
                        }
                    } catch (e) {
                        logger.debug("[SitesTask.runTask] Error: ", e);
                    }
                    monitorStatus(siteId);
                },
                function(response) {
                    Dialog.open({
                        'error': true,
                        'title': "Server error",
                        'text': response.message,
                        'buttons': {OK: function(){}}
                    });
                }
            );
        };

        //------------------------------------------------------------------------
        //   Выдает историю запуска данной задачи для данного сайта
        //------------------------------------------------------------------------
            SitesTask.prototype.getLog = function(siteId, taskName) {
            DEBUG && logger.debug("[SitesTask.getLog] Show log for site="+siteId+", taskname="+taskName);
            //monitorStatus(siteId);
        };






    return SitesTask;
});