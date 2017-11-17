/**
 * Created by abel on 10.05.17.
 */


define(["jquery","api","modalDialog","utils","logger","moment","cronEdit","cron2Human"],
    function($,Api,Dialog,Utils,logger,moment,CronEdit,Cron2Human) {


        "use strict";

        var SHEDLIST_HOLDER_ID = "sched-list";
        var SCHED_TBLROW       = "-sched-row";
        var EDIT_FRAME_SUFFIX = '-sched-frame';
        var BTN_ID_SUFFIX = "-save-edit";
        var INSERT_FRAME_SUFFIX = "-cron-form";
        var c2h = new Cron2Human();

        //------------------------------------------------------------------------
        //
        //------------------------------------------------------------------------
        function schedRowHTML(siteId, schedule) {

            var taskName = schedule.taskName;

            var resHtml =  '<div id="'+schedule.id+SCHED_TBLROW+'" class="row panel-body-row">' +
                '   <span class="text-right row-label">'+taskName+'</span>'+
                '   <div class="row-data" style="max-width: 55%;">' +
                    //
                    //    Строка состояния и рассписания заполняется из кода. _displaySched
                    //
                //'      <div  class="status '+(schedule.enable?"status-succ":"status-norm")+'" style="text-transform: uppercase;">'+(schedule.enable?"Enabled":"Disabled")+'</div>' +
                //'   <div data-toggle="collapse" data-target="#' + schedule.id + EDIT_FRAME_SUFFIX +'" aria-expanded="false" aria-controls="subpanel">' +
                //'      <span  class="schedule ">'+schedules+'</span>' +
                //'      <span  class="next ">'+next+'</span>' +
                '   </div>'+
                '   <div class="row-cmd pull-right"> ' +
                '   <div class="pull-right">'+
                '       <a id="'+schedule.id+BTN_ID_SUFFIX+'"  data-siteId="'+siteId+'" data-schedId="'+schedule.id+'" data-taskName="'+taskName+'"  class="edit link text-right" >'+
                '           <span class="round"><span class="glyphicon glyphicon-menu-left transition-rotate rotate0"> </span></span>'+
                '       </a>' +
                '   </div>'+
                '   <div class="pull-right">'+
                '       <a data-siteId="'+siteId+'" data-schedId="'+schedule.id+'" class="enable link text-right" >'+(schedule.enable?"Deactivate":"Activate")+'</a>'+
                '   </div>'+
                '   </div>'+
                '   <div id="'+schedule.id+EDIT_FRAME_SUFFIX+'"  data-schedId="'+schedule.id+'"  class="cron-form collapse row" style="clear: both; padding-top: 5px">' +
                '       <div class="text-right row-label">&nbsp;</div>' +
                '       <div id="'+schedule.id+INSERT_FRAME_SUFFIX+'" data-schedId="'+schedule.id+'"  class="cron-form-ins">  </div>'+
                '       <div class="pull-right clearfix" style="width:11%">'+
                '         <a class="save link pull-right text-right clearfix" data-siteId="'+siteId+'" data-schedId="'+schedule.id+'" >Save</a>'+
                '         <a class="cancel link pull-right text-right clearfix" data-siteId="'+siteId+'" data-schedId="'+schedule.id+'">Cancel</a>'+
                '      </div>' +
                '   </div>'+
                '</div>';
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

            $('body').off('click',"#"+SHEDLIST_HOLDER_ID+' a.edit')
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

                .off('click',"#"+SHEDLIST_HOLDER_ID+' a.enable')
                .on('click',"#"+SHEDLIST_HOLDER_ID+' a.enable',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    event.data.caller._saveSched(
                        target.getAttribute('data-siteId'),
                        target.getAttribute('data-schedId')
                    );
                })

                // -----------------------------------------------------
                //
                //    Перехватывает события  раскрытия и закрытия  формы едактирования рассписанием
                //
                .off('show.bs.collapse')
                .on('show.bs.collapse',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    logger.debug("[SitesSched.show.bs.collapse] Cancel shedule ", target);
                    var schedId = target.getAttribute("data-schedId");

                    $('#'+schedId+BTN_ID_SUFFIX + ' span span').removeClass('rotate0').addClass('rotate-90');

                })
                .off('hide.bs.collapse')
                .on('hide.bs.collapse',{'caller':this},function(event) {
                    var target = event.target || event.srcElement;
                    logger.debug("[SitesSched.hide.bs.collapse] Cancel shedule ", target);
                    var schedId = target.getAttribute("data-schedId");
                    $('#'+schedId+BTN_ID_SUFFIX + ' span span').removeClass('rotate-90').addClass('rotate0');
                });

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
                            var schedRecordsList = response.object;
                            $("#"+SHEDLIST_HOLDER_ID).html("");

                            var listHtml = "";
                            for (var i = 0; i < schedRecordsList.length; i++) {
                                var scheduleObj = schedRecordsList[i];

                                if (! scheduleObj.id) {
                                    scheduleObj.id = Math.floor((Math.random() * 10000)) * (-1);
                                }
                                caller.schedIdMap[scheduleObj.id] = {
                                    'id': scheduleObj.id,
                                    'tName': scheduleObj.taskName,
                                    'obj': scheduleObj,
                                    'elId': scheduleObj.id+INSERT_FRAME_SUFFIX
                                };

                                $("#"+SHEDLIST_HOLDER_ID).append(schedRowHTML(siteId, scheduleObj));
                                caller._displaySched(caller.schedIdMap[scheduleObj.id].elId , scheduleObj);
                            }


                            //   Для всех срон записей создаем класс упраления ред.формой
                            //   и добвляем в масив.
                            //   В процессе создания класса будет генерироваться штмл форма внутри фрейма.
                            for ( var key in caller.schedIdMap) {
                                caller.schedIdMap[key].class = new CronEdit(caller.schedIdMap[key].elId,caller.schedIdMap[key].obj);

                                //   Добавляем кнопки Save и Cancel после добавления формы
                                //$('#'+caller.schedIdMap[key].elId).append(formsBtnHTML(siteId,caller.schedIdMap[key]));

                                caller.schedIdMap[key].class.setCron(caller.schedIdMap[key].obj);
                                caller.schedIdMap[key].class.setListeners(function(elId,cronObj){
                                    caller._displaySched(elId,cronObj);
                                });
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
        SitesSched.prototype._displaySched = function(formRowId,cronObj) {
            logger.debug("[SitesSched._displaySched] Display human readable schedules for id=" +formRowId+ ", cron=",cronObj);
            var isError = true;

            var message = c2h.validate(cronObj);
            if ( ! message ) {
                message = 'Scheduled: ' +c2h.toString(cronObj);
                isError = false;
            }
            // else {
            //     message = "Error: " + message;
            // }



            //
            //     Заполняем строку состояния  рассписания
            //
            var shedId = $('#'+formRowId).attr('data-schedId');
            var htmlStr = '<div  class="status '+(cronObj.enable?"status-succ":"status-norm")+'" style="text-transform: uppercase;">'+
                (cronObj.enable?"Enabled":"Disabled")+
                '</div>' +
                '<span  class="schedule '+(isError?"status-err":"")+'">'+message+'</span>' +
                '<span  class="next ">'+""+'</span>'
                ;
            $('#'+shedId + SCHED_TBLROW+ " .row-data").html(htmlStr);


            //
            //     Устанавливаем  название кнопки enable/disable
            //
            $('#'+shedId + SCHED_TBLROW+ " a.enable").text(cronObj.enable?"Deactivate":"Activate");

        };

        //------------------------------------------------------------------------
        //    Вызывается когда пользователь нажал на кнлпку  EDIT
        //------------------------------------------------------------------------
        SitesSched.prototype._editSched = function(siteId,schedId) {
            logger.debug("[SitesSched._editSched] Edit shedule edit for siteId="+siteId+", shedId="+schedId);

            //  Открыть выпадающий фрейм редактирования
            $('#'+SHEDLIST_HOLDER_ID+" "+' .collapse.in').collapse('hide');
            $('#'+schedId+EDIT_FRAME_SUFFIX).collapse('show')
        };

        //------------------------------------------------------------------------
        //    Вызывается когда пользователь нажал на кнлпку  CANCEL
        //------------------------------------------------------------------------
        SitesSched.prototype._cancelSched = function(siteId,schedId) {
            logger.debug("[SitesSched._cancelSched] Cancel shedule edit for siteId="+siteId+", shedId="+schedId);
            //  Открыть выпадающий фрейм редактирования
            $('#'+schedId+EDIT_FRAME_SUFFIX).collapse('hide');
        };


        //------------------------------------------------------------------------
        //    Вызывается когда пользователь нажал на кнлпку  SAVE
        //------------------------------------------------------------------------
        SitesSched.prototype._saveSched = function(siteId,schedId) {
            logger.debug("[SitesSched._saveSched] Save shedule edit for siteId="+siteId+", shedId="+schedId);

            var cronObj = this.schedIdMap[schedId].class.getCron();
            var caller = this;

            Api.setSched(siteId,cronObj,

                //  on Success
                function(response) {
                    try {
                        if (response.object) {
                            //   Отображаем текущее значение  в основной строке
                            caller._displaySched(caller.schedIdMap[schedId].elId,response.object);
                            //   Сохраняем крон объект
                            caller.schedIdMap[schedId].class.setCron(response.object);
                            caller.schedIdMap[schedId].obj = response.object;
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