/**
 * Created by abel on 10.05.17.
 */


define(["jquery","api","modalDialog","utils","logger","sitesShEdit"],
    function($,Api,Dialog,Utils,logger,ShEdit) {


        "use strict";

        var SHEDLIST_HOLDER_ID = "sched-list";
        var SCHED_TBLROW       = "-sched-row";
        var EDIT_FRAME_SUFFIX = '-sched-frame';
        var SHED_INFO_SUFFIX = "-open-btn";
        var BTN_ID_SUFFIX = "-save-edit";
        //var INSERT_FRAME_SUFFIX = "-cron-form";


        //------------------------------------------------------------------------
        //
        //------------------------------------------------------------------------
        function schedRowHTML(siteId, schedule) {

            var taskName = schedule.taskName;

            var mainPanelId = schedule.id+SCHED_TBLROW;
            var subpanelId = schedule.id+EDIT_FRAME_SUFFIX;
            var openPanelBtnId = schedule.id+BTN_ID_SUFFIX;

                var resHtml = "<div id='"+mainPanelId+"' class='row panel-body-row'>"+
                    "    <span class='text-right row-label'>"+taskName+"</span>"+
                    "    <div data-toggle='collapse' data-parent='#"+mainPanelId+"' data-target='#"+subpanelId+"' aria-expanded='false' aria-controls='subpanel' >"+
                    "        <span id='"+schedule.id+SHED_INFO_SUFFIX+"'  class='row-data'>&nbsp;</span>"+
                    "        <a id='"+openPanelBtnId+"' class='link row-cmd pull-right' style='margin-top: -12px;'>"+
                    "            <span><span class='glyphicon glyphicon-menu-down  fa-stack-1x transition-rotate' style='padding-top: 6px'></span></span>"+
                    "        </a>"+
                    "    </div>"+
                    "    <div id='"+subpanelId+"' class='collapse row' data-siteId='"+siteId+"' data-schedId='"+schedule.id+"' style='clear: both'>"+
                    "         <div class='pull-right clearfix' style='width:11%'>"+
                    "             <a class='save link pull-right text-right clearfix' data-siteId='"+siteId+"' data-schedId='"+schedule.id+"'>Save</a>"+
                    "             <a class='cancel link pull-right text-right clearfix' data-siteId='"+siteId+"' data-schedId='"+schedule.id+"'>Cancel</a>"+
                    "         </div>"+
                    "    </div>"+
                    "</div>";

            //"       <a data-siteId='"+siteId+"' data-schedId='"+schedule.id+"' class='enable link text-right pull-right' >"+(schedule.enable?"Deactivate":"Activate")+"</a>"+


            return resHtml;
        }

        /*------------------------------------------------------------------------
        *
        *
        *
        *
        *
        *
        *
         ------------------------------------------------------------------------*/

        function SitesSched(model) {
            this.schedIdMap = {};

            this._renderSchedList(model.get("id"));

            $('body')
/*
                .off('click',"#"+SHEDLIST_HOLDER_ID+' a.edit')
                .on('click',"#"+SHEDLIST_HOLDER_ID+' a.edit',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    logger.debug("[SitesSched.click] a.edit event fired target=", target);

                    while (target.tagName !== "A") {
                        target = target.parentElement
                    }

                    event.data.caller._editSched(
                        target.getAttribute('data-siteId'),
                        target.getAttribute('data-schedId')
                    );
                })
*/

                .off('click',"#"+SHEDLIST_HOLDER_ID+' a.cancel')
                .on('click',"#"+SHEDLIST_HOLDER_ID+' a.cancel',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;

                    event.data.caller._cancelSched(
                        target.getAttribute('data-siteId'),
                        target.getAttribute('data-schedId')
                    );
                })

                .off('click',"#"+SHEDLIST_HOLDER_ID+' a.save')
                .on('click',"#"+SHEDLIST_HOLDER_ID+' a.save',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    event.data.caller._saveSched(
                        target.getAttribute('data-siteId'),
                        target.getAttribute('data-schedId')
                    );
                })

                // .off('click',"#"+SHEDLIST_HOLDER_ID+' a.enable')
                // .on('click',"#"+SHEDLIST_HOLDER_ID+' a.enable',{'caller':this},function(event) {
                //     var target = event.target || event.srcElement;
                //     event.data.caller._saveSched(
                //         target.getAttribute('data-siteId'),
                //         target.getAttribute('data-schedId')
                //     );
                // })

                // -----------------------------------------------------
                //
                //    Перехватывает события  раскрытия и закрытия формы редактирования рассписания
                //

                .off('show.bs.collapse')
                .on('show.bs.collapse',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    //logger.debug("[SitesSched.show.bs.collapse] Cancel shedule ", target);
                    var schedId = target.getAttribute("data-schedId");

                    //   Возможно вставить загрузку текущего значения рассписания для этой задачи

                    //   поворачиваем указатель ракрытия фрейма
                    $('#'+schedId+BTN_ID_SUFFIX + ' span span').removeClass('fa-rotate-0').addClass('fa-rotate-90');
                })
                .off('hide.bs.collapse')
                .on('hide.bs.collapse',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    //logger.debug("[SitesSched.hide.bs.collapse] Cancel shedule ", target);
                    var schedId = target.getAttribute("data-schedId");
                    $('#'+schedId+BTN_ID_SUFFIX + ' span span').removeClass('fa-rotate-90').addClass('fa-rotate-0');
                })
            ;

        }

        //
        // .off("click",'a.edit')
        //     .on("click",'a.edit',{'caller':this},function(event) {
        //         logger.debug("[SitesTask] Click event edit schedule.");
        //     })


        //------------------------------------------------------------------------
        //
        //   Загрузка списка рассписаний
        //
        //     {
        //         "message": "OK",
        //         "rc": 0,
        //         "object": [
        //             {
        //                 "id": null,
        //                 ""
        //                 "taskName": "TNAME_EMPTY",
        //                 "seconds": "*",
        //                 "minute": "*",
        //                 "hour": "*",
        //                 "dayOfMonth": "*",
        //                 "month": "*",
        //                 "dayOfWeek": "*"
        //             },
        //             {
        //                 "id": null,
        //                 "taskName": "TNAME_SCAN",
        //                 "seconds": "*",
        //                 "minute": "*",
        //                 "hour": "*",
        //                 "dayOfMonth": "*",
        //                 "month": "*",
        //                 "dayOfWeek": "*"
        //             }
        //         ]
        //     }
        //
        //------------------------------------------------------------------------
        SitesSched.prototype._renderSchedList = function(siteId) {

            var caller = this;

            Api.listSched(siteId,
                //  on Success
                function(response) {
                    try {
                        if (response.object) {
                            var cronObjList = response.object;
                            $("#"+SHEDLIST_HOLDER_ID).html("");

                            var listHtml = "";
                            for (var i = 0; i < cronObjList.length; i++) {
                                var cronObj = cronObjList[i];

                                if (! cronObj.id) {
                                    cronObj.id = Math.floor((Math.random() * 10000));
                                }
                                caller.schedIdMap[cronObj.id] = {
                                    'id': cronObj.id,
                                    'tName': cronObj.taskName,
                                    'cronObj': cronObj,
                                    'elId': cronObj.id+EDIT_FRAME_SUFFIX   //  ID для HTML-ного  блока куда всавится форма редактирования рассписания
                                };

                                $("#"+SHEDLIST_HOLDER_ID).append(schedRowHTML(siteId, cronObj));
                            }

                            //   Для всех 'cron' записей создаем класс упраления ред.формой
                            //   и добвляем в масив.
                            //   В процессе создания класса будет генерироваться html форма внутри фрейма.
                            for ( var key in caller.schedIdMap) {
                                caller.schedIdMap[key].schedObj = new ShEdit(caller.schedIdMap[key].elId,caller.schedIdMap[key].cronObj);
                                caller._displaySched(key);
                            }
                        }
                    }
                    catch (e) {
                        logger.debug("[SitesTask.renderTasksList] Error: ",e);
                    }
                },

                //  on Error
                function(response) {
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
        //    Вызывается при изменении значения формы редактирования CRON
        //------------------------------------------------------------------------
        SitesSched.prototype._displaySched = function(cronId) {
            var message = this.schedIdMap[cronId].schedObj.getNextDate();
            var cronObj = this.schedIdMap[cronId].cronObj;
            if (message) message = "Next start at: " + message;

            logger.debug("[SitesSched._displaySched] Display human readable schedules with msg= "+message+", cron=",cronObj);

            //
            //     Заполняем строку состояния  рассписания
            //
            var htmlStr = '<span  class="status '+(cronObj.enable?"status-succ":"status-norm")+'" style="text-transform: uppercase;">'+
                    (cronObj.enable?"Enabled":"Disabled")+
                '</span>' +
                '<span  class="schedule" style="padding-left: 10px;">'+message+'</span>'
                ;
            $('#'+cronId+SHED_INFO_SUFFIX).html(htmlStr);
        };

/*
        //------------------------------------------------------------------------
        //    Вызывается когда пользователь нажал на кнлпку  EDIT
        //------------------------------------------------------------------------
        SitesSched.prototype._editSched = function(siteId,schedId) {
            logger.debug("[SitesSched._editSched] Edit shedule edit for siteId="+siteId+", shedId="+schedId);

            //  Открыть выпадающий фрейм редактирования
            $('#'+SHEDLIST_HOLDER_ID+" "+' .collapse.in').collapse('hide');
            $('#'+schedId+EDIT_FRAME_SUFFIX).collapse('show')
        };
*/

        //------------------------------------------------------------------------
        //    Вызывается когда пользователь нажал на кнлпку  CANCEL // ресет формы
        //------------------------------------------------------------------------
        SitesSched.prototype._cancelSched = function(siteId,schedId) {
            logger.debug("[SitesSched._cancelSched] Cancel shedule edit for siteId="+siteId+", shedId="+schedId);

            //  Возвращаем значения рассписание в последнее сохраненное состояние
            this.schedIdMap[schedId].schedObj.setCron(this.schedIdMap[schedId].cronObj);

            //$('#'+schedId+EDIT_FRAME_SUFFIX).collapse('hide');
        };


        //------------------------------------------------------------------------
        //    Вызывается когда пользователь нажал на кнлпку  SAVE
        //------------------------------------------------------------------------
        SitesSched.prototype._saveSched = function(siteId,schedId) {
            logger.debug("[SitesSched._saveSched] Save shedule edit for siteId="+siteId+", shedId="+schedId);

            var cronObj = this.schedIdMap[schedId].schedObj.getCron();
            cronObj.site = siteId;
            var caller = this;

            Api.setSched(siteId,cronObj,

                //  on Success
                function(response) {
                    try {
                        if (response.object) {
                            //   Сохраняем крон объект
                            caller.schedIdMap[schedId].schedObj.setCron(response.object);
                            caller.schedIdMap[schedId].cronObj = response.object;
                            //   Отображаем текущее значение  в основной строке
                            caller._displaySched(schedId);
                        }
                    }
                    catch (e) {
                        logger.debug("[SitesSched.saveSched] Error: ",e);
                    }
                },

                //  on Error
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


    return SitesSched;
});