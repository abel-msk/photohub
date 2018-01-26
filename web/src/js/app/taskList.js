/*

    Display task item


        {
            "schedule" : {
              "id" : "3",
              "taskName" : "TNAME_EMPTY",
              "params" : [ {
                "id" : "3",
                "name" : "PARAM1",
                "value" : "theVALUE",
                "type" : null
              } ],
              "seconds" : "10",
              "minute" : "*",
              "hour" : "*",
              "dayOfMonth" : "*",
              "month" : "*",
              "dayOfWeek" : "*",
              "enable" : true
            },
            "taskRecord" : {
              "id" : null,
              "name" : "TNAME_EMPTY",
              "status" : "FIN",
              "message" : "Task TNAME_EMPTY/Dummy(task Id=3|site=TEST1(2)), Schedule=TNAME_EMPTY(id=3|en=true|s=5|m=*|h=*|d=*|M=*|dW=*). Execution success.",
              "scheduleId" : "3",
              "startTime" : null,
              "stopTime" : 1515527825010
            },
            "displayName" : "Dummy",
            "description" : "Empty Task",
            "id" : "3",
            "siteId" : "2"
        }



 */




define(["jquery","api","modalDialog","utils","logger","taskEdit","schedule","form/veList","moment"],
    function($,Api,Dialog,Utils,logger,TaskEdit,Schedule,VEList,moment) {

        "use strict";

        var TASK_BLOCK = "taskblock_";
        var HIDE_PANEL = "hidepanel_";
        var HIDE_PANEL_BTN = "hidepanelbtn_";
        var LOG_PANEL="logpanel_";

        var CRON_PANEL = "cronpanel_";
        var PARAM_PANEL = "parampanel_";
        var NEXT_TXT = "tnext_";
        var LAST_PTR = "tlastptr_";
        var LAST_TXT = "tlasttime_";
        var SCHED_TXT = "tsched_";
        var NOW_TXT = "tnowstat_";
        var DESCR_TXT="inline-descr";
        var LOG_LINE = "tasklog_";

        var TASKLIST_HOLDER_ID = "#task-list";

        function taskRowHTML(taskItem) {

            var id = taskItem.id;
            var strHtml =
                "<div id='"+TASK_BLOCK+id+"' class='row panel-body-row task'>"+
                "    <span class='text-right row-label bold' style='padding-top: 4px;'>"+taskItem.displayName+"</span>"+
                "    <div data-toggle='collapse' data-parent='#"+TASK_BLOCK+id+"' data-target='#"+HIDE_PANEL+id+"' aria-expanded='false' aria-controls='subpanel' >"+
                "         <div class='row-data wide taskDescrBlk'>"+

                "            <div id='"+DESCR_TXT+id+"' class='inline-line inline-descr styled-header-4'></div>"+
                "               <div class='inline-line'>"+
                "                   <span  class='inline-cell' >" +
                "                       <span class='highlight'>" +
                "                           <i class='fa fa-calendar-check-o' aria-hidden='true'></i>" +
                "                       </span>" +
                "                       <span id='"+SCHED_TXT+id+"' class='schedule'></span>" +
                "                   </span>"+

                "                   <span  class='inline-cell' >" +
                "                       <span id='"+LAST_PTR+id+"' class=''>" +
                "                           <i class='fa fa-history' aria-hidden='true'></i>" +
                "                       </span>"+
                "                       <span id='"+LAST_TXT+id+"' class='last-run-status '></span>" +
                "                   </span>"+

                "                   <span  class='inline-cell' >" +
                "                       <span class='highlight'>" +
                "                           <i class='fa fa-clock-o' aria-hidden='true'></i>" +
                "                       </span>"+
                "                       <span id='"+NEXT_TXT+id+"' class='next-run'></span>" +
                "                   </span>"+
                "               </div>" +
                "           </div>"+

                "        <a id='"+HIDE_PANEL_BTN+id+"' class='link row-cmd pull-right' >"+
                "             <span><span class='glyphicon glyphicon-menu-down  fa-stack-1x transition-rotate fa-rotate-90' style='padding-top: 6px'></span></span>"+
                "        </a>"+
                "    </div>"+
                "    <div id='"+HIDE_PANEL+id+"' class='row collapse' style='clear: both'>"+

                "       <div class='text-right row-label'>Task params</div>"+
                "       <div id='"+PARAM_PANEL+id+"' class='params-block row-data wide'></div>"+
                "       <div class='text-right row-label'>Execution log</div>"+
                "       <div class='row-data wide'>"+
                "           <div class='code wide code-block'>"+
                "               <div id='"+LOG_LINE+id+"' class='nowrap code-block-body'></div>"+
                "               <div class='pull-left text-left'>"+
                "                   <a data-siteid='"+taskItem.siteId+"' data-taskid='"+id+"' class='morelog link code'>more...</a>"+
                "               </div>"+
                "           </div>"+
                "       </div>"+

                "        <div class='pull-right clearfix panel-body-row' >"+
                "            <a data-siteid='"+taskItem.siteId+"' data-taskid='"+id+"' class='remove link pull-right text-right ' >Remove</a>"+
                "            <a data-siteid='"+taskItem.siteId+"' data-taskid='"+id+"' class='edit link pull-right text-right ' >Edit</a>"+
                "            <a data-siteid='"+taskItem.siteId+"' data-taskid='"+id+"' class='run-now link pull-right text-right ' >Run Now</a>"+
                "        </div>"+
                "    </div>"+
                "</span>";
            return strHtml;
        }

        function paramHtml(param) {
            var htmlStr =
                "<div id='param_"+param.id+"' class='row subrow' style='clear:both'>"+
                "    <div class='text-left row-label' style='padding-left:0px'>"+
                "        <span>"+param.name+"</span>"+
                "    </div>"+
                ""+
                "    <div class='row-data wide' style='width: 78%; padding-right: 0;'>"+
                "                    <span class='save-edit'>"+param.value+"</span>"+
                "    </div>"+
                "</div>"
            ;
            return htmlStr;
        }

        /*------------------------------------------------------------------------
        *
        *
        *    SITES TASKS LIST OBJECT INIT
        *
        *
         ------------------------------------------------------------------------*/
        function SitesTask(model,listEl) {


            if ( ! model ) {
                throw Error("[SitesTask.init] Model parameter required.");
            }

            this.siteId = model.get("id");
            this.listEl = listEl;
            this.curentTasks = {};

            this.render(this.siteId,this.listEl);
            $('body')
                .off('click',"a.remove")
                .on('click',"a.remove",{'caller':this},function(event){
                    var target = event.target || event.srcElement;
                    var caller = event.data.caller;

                    //caller
                    caller.deleteTask(target.getAttribute("data-taskid"));
                })

                .off('click',"a.edit")
                .on('click',"a.edit",{'caller':this},function(event){
                    var target = event.target || event.srcElement;
                    var caller = event.data.caller;
                    caller.editTaskDialog(target.getAttribute("data-taskid"));
                })

                .off('click',"a.morelog")
                .on('click',"a.morelog",{'caller':this},function(event){
                    var target = event.target || event.srcElement;
                    var caller = event.data.caller;
                    caller.moreLog(target.getAttribute("data-taskid"));
                })

                // -----------------------------------------------------
                //
                //    Перехватывает события  раскрытия и закрытия формы редактирования рассписания
                //

                .off('show.bs.collapse')
                .on('show.bs.collapse',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    $(target).parent().find(".glyphicon.glyphicon-menu-down").removeClass('fa-rotate-90').addClass('fa-rotate-0');
                    $(".collapse.row.in").collapse('hide');

                })
                .off('hide.bs.collapse')
                .on('hide.bs.collapse',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    $(target).parent().find(".glyphicon.glyphicon-menu-down").removeClass('fa-rotate-0').addClass('fa-rotate-90');
                })
            ;
        }

        //------------------------------------------------------------------------
        //     Перечитывает и перерисовывает все задачи.
        //------------------------------------------------------------------------
        SitesTask.prototype.refresh = function() {
            this.render(this.siteId,this.listEl);
        };


        //------------------------------------------------------------------------
        //     Добавляет задачу к конец списка
        //------------------------------------------------------------------------
        SitesTask.prototype.addTask = function(taskObj) {
            logger.debug("[SitesTask.addTask] Delete task for task ", (taskObj?taskObj:"NULL"));
            if (taskObj ) {
                this.renderTask(this.listEl,taskObj);
            } else {
                this.refresh();
            }
        };

        //------------------------------------------------------------------------
        //     Удаление задачи.
        //     Выводит уточняющий диалог. После чего удаляет задачу.
        //------------------------------------------------------------------------
        SitesTask.prototype.deleteTask = function(taskId) {
            logger.debug("[SitesTask.deleteTask] Delete task for task ID="+taskId);
            var caller = this;

            Dialog.open({
                'error': false,
                'title': "Delete task",
                'text':  "Are you sure want to delete task "+caller.curentTasks[taskId].task.displayName ,
                'buttons': {
                    'OK': function(){
                        Api.deleteTask(caller.curentTasks[taskId].task.siteId,taskId,
                            //  on Success
                            function(response) {
                                try {
                                    if (response.object) {

                                        $("#"+TASK_BLOCK+taskId).remove();
                                        caller.curentTasks[taskId] = null;

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
                        );
                    },
                    'Cancel':function() {}
                } // buttons
            });
        };

        //------------------------------------------------------------------------
        //
        //    Открывает диалог редактирования задачи
        //
        //------------------------------------------------------------------------

        SitesTask.prototype.editTaskDialog = function(taskId) {
            var taskEdit = new TaskEdit();
            taskEdit.open(this.curentTasks[taskId].task);
        };


        //------------------------------------------------------------------------
        //
        //    Выводит лог для задачи
        //
        //------------------------------------------------------------------------

        SitesTask.prototype.moreLog = function(taskId) {
            logger.debug("[taskList.morelog] Click ");
        };


        //------------------------------------------------------------------------
        //
        //     Получает с сервера список задач и отрисовывает весь список на странице.
        //
        //
        //     [
        //         "schedule" : {
        //             "id" : "3",
        //             "taskName" : "TNAME_EMPTY",
        //             "params" : [ {
        //                 "id" : "3",
        //                 "name" : "PARAM1",
        //                 "value" : "theVALUE",
        //                 "type" : null
        //             } ],
        //             "seconds" : "*/5",
        //             "minute" : "*",
        //             "hour" : "*",
        //             "dayOfMonth" : "*",
        //             "month" : "*",
        //             "dayOfWeek" : "*",
        //             "enable" : true
        //             },
        //         "taskRecord" : {
        //             "id" : null,
        //                 "name" : "TNAME_EMPTY",
        //                 "status" : "FIN",
        //                 "message" : "Task TNAME_EMPTY/Dummy(task Id=3|site=TEST1(2)), Schedule=TNAME_EMPTY(id=3|en=true|s=*/5|m=*|h=*|d=*|M=*|dW=*). Execution success.",
        //                 "scheduleId" : "3",
        //                 "startTime" : null,
        //                 "stopTime" : 1515533760014
        //         },
        //         "displayName" : "Dummy",
        //         "description" : "Empty Task",
        //         "id" : "3",
        //         "siteId" : "2"
        //     ]
        //
        //
        //------------------------------------------------------------------------
        SitesTask.prototype.render = function(siteId,el) {
            var caller = this;

            Api.listTasks(siteId,
                //  on Success
                function(response) {
                    try {
                        if (response.object) {

                            //  Clear old task list
                            $(el).html("");
                            $.each(response.object,function(key,value){
                                caller.renderTask(el,value);
                            });
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
            );
        };

        //------------------------------------------------------------------------
        //
        //     Отрисовывает одну задачу.
        //
        //------------------------------------------------------------------------
        SitesTask.prototype.renderTask = function(el,task) {
            try {
                if (!el) {
                        $("#" + TASK_BLOCK + task.id).replaceWith(taskRowHTML(task));
                }
                else {
                    $(el).append(taskRowHTML(task));
                }

                this.curentTasks[task.id] = {
                    'task': task,
                    'schedObj': new Schedule(
                        {
                        'el': CRON_PANEL + task.id,  //element
                        'cronObj':task.schedule,   //object
                        'display':false
                        }
                    )
                };

                this.renderParams(task);
                this.renderStatus(task, this.curentTasks[task.id].schedObj);
            }
            catch (e) {
                logger.debug("[SitesTask.renderTask] Error: ",e);
            }
        };

        //------------------------------------------------------------------------
        //
        //   Добавлет отрисовку статусов в задачу
        //
        //------------------------------------------------------------------------
        SitesTask.prototype.renderStatus = function(task,scheduleObj) {


            var dateStr = moment(task.taskRecord.stopTime).format('DD/MM/YYYY HH:mm');
            var status = task.taskRecord.status;
            var statusColor = "status-err";

            if ((status === "IDLE") ||(status==="FIN")) {
                status = "WAIT";
                //statusColor = "status-succ";
                statusColor = "highlight";
                }

            //inline-descr
            $("#"+TASK_BLOCK+task.id+" #"+SCHED_TXT+task.id).text(scheduleObj.sched2text());
            $("#"+TASK_BLOCK+task.id+" #"+NEXT_TXT+task.id).text(scheduleObj.getNextDate());

            $("#"+TASK_BLOCK+task.id+" #"+DESCR_TXT+task.id).text(task.description);
            $("#"+TASK_BLOCK+task.id+" #"+LAST_PTR+task.id).addClass(statusColor);
            $("#"+TASK_BLOCK+task.id+" #"+LAST_TXT+task.id).text(dateStr);

            $("#"+TASK_BLOCK+task.id+" #"+LOG_LINE+task.id).text(task.taskRecord.message);

        };

        //------------------------------------------------------------------------
        //
        //    Добавляет отрисоку параметра к задаче
        //
        //------------------------------------------------------------------------
        SitesTask.prototype.renderParams = function(task) {
            var output = [];
            $.each(task.schedule.params, function(key,param) {
                if (param.value) {
                    output.push(paramHtml(param));
                }
                else {
                    output.push('&nbsp;');
                }
            });
            $("#"+PARAM_PANEL+task.id).html(output.join(" "));
        };

        return SitesTask;
    });