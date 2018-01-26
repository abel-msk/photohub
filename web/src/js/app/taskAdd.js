//   Schedule object
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

define(["jquery","api","modalDialog","logger","schedule"],
    function($,Api,Dialog,logger,Schedule) {

        "use strict";


        // var TASK_DESCR_GRP_ID  = "tdescr-grp";
        // var TASK_SCHED_GRP_ID = "tsched-grp";


        var ADD_TASK_DIALOG_ID = "add_task_dialog";
        var TASK_SELECTOR_GRP_ID = "tsel-grp";
        var TASK_SCHED_FORM_ID = "tsched-form";
        var SAVE_BTN_ID = "save_new_task_btn";



        function AddTask() {
            // this.btnId = addTaskBtnId;
            // this.formId = addTaskFormId;
            this.taskList = {};
            this.scheduleObj = null;
            this.scheduleObj = {};
        }

        //------------------------------------------------------------------------
        //------------------------------------------------------------------------


        AddTask.prototype.open = function(siteId) {
            this.siteId = siteId;
            this.render(this.siteId);

            $('#'+TASK_SELECTOR_GRP_ID + " select")
                .show()
                .off('change')
                .on('change',{caller:this},function(event){
                    var target = event.target || event.srcElement;
                    var caller = event.data.caller;

                    logger.debug( "[AddTask.open] Selecctor chnged. New value is = "+$(target).val());
                    if ( caller.taskList && $(target).val() && caller.taskList[$(target).val()]) {
                        caller.renderSchedule(caller.tdescr2schedule(caller.taskList[$(target).val()]));
                    }
                });

            $('#'+SAVE_BTN_ID)
                .off('click')
                .on('click',{caller:this},function(event){
                    event.data.caller.save();
                });


            $("#"+ADD_TASK_DIALOG_ID).modal({'backdrop': true});
        };


        //------------------------------------------------------------------------
        // {
        //      "message" : "OK",
        //      "rc" : 0,
        //      "object" : [ {
        //          "displayName" : "Scan site",
        //          "name" : "TNAME_SCAN",
        //          "description" : "Scanning site for new objects and add to local db.",
        //          "params" : null,
        //          "visible" : true
        //      }, {
        //          "displayName" : "Dummy",
        //          "name" : "TNAME_EMPTY",
        //          "description" : "Empty Task",
        //          "params" : {
        //              "PARAM1" : "Test param for empty task"
        //          },
        //          "visible" : true
        //      } ]
        // }
        //------------------------------------------------------------------------

        AddTask.prototype.render = function(siteId) {
            var caller = this;
            caller.taskList = {};
            $('#'+TASK_SELECTOR_GRP_ID + " span").hide();
            $('#'+TASK_SELECTOR_GRP_ID + " select").show();

            //            listTaskDescr: function(siteId, callbackResult, callbackError) {
            Api.listTaskDescr(siteId,
                //  on Success
                function(response) {
                    try {
                        if (response.object) {
                            var defTaskDescr =  response.object[0];

                            //   Зполняем список задач с описанием.
                            var output = [];
                            $.each(response.object, function(key, obj)  {
                                output.push('<option '+(defTaskDescr.name === obj.name ? "selected":"")+' value="'+ obj.name +'">'+ obj.displayName +" | "+obj.description +'</option>');
                                caller.taskList[obj.name]=obj;
                            });
                            $('#'+TASK_SELECTOR_GRP_ID + " select").html(output.join(''));
                        }
                        caller.schedule = caller.renderSchedule(caller.tdescr2schedule(defTaskDescr));
                    }
                    catch (e) {
                        logger.debug("[AddTask.render] Error: ",e);
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
        //------------------------------------------------------------------------
        AddTask.prototype.tdescr2schedule = function(taskDescr) {

            var schedule = {
                'taskName':taskDescr.name,
                'displayName': taskDescr.displayName,
                'siteId':this.siteId,
                'params':[],
                'enable':false,
                "seconds":"*",
                "minute" : "*",
                "hour" : "*",
                "dayOfMonth" : "*",
                "month" : "*",
                "dayOfWeek" : "*"
            };

            $.each(taskDescr.params,function(key,obj) {
                    schedule.params.push({
                            'name': key,
                            'type': 'string',
                            'value': "",
                            'descr': obj
                        });
                }
            );

            this.schedule = schedule;
            return schedule;
        };

        //------------------------------------------------------------------------
        //
        //------------------------------------------------------------------------

        AddTask.prototype.renderSchedule = function(schedule) {

            $('#'+TASK_SCHED_FORM_ID).html('');
            this.scheduleObj = new Schedule({'el':TASK_SCHED_FORM_ID,'cronObj':schedule});


            $("#"+ADD_TASK_DIALOG_ID +" .task-param").remove();
            if (schedule.params) {
                var output = [];
                $.each(schedule.params, function(key, obj)  {
                        var paramBlk =
                            "<div  class='form-group task-param'>"+
                            "    <label for='"+obj.name+"' class='col-sm-2 control-label'>"+obj.name+"</label>"+
                            "    <div class='col-sm-10'>"+
                            "        <input id='"+obj.name+"' type='text' class='form-control' value='"+(obj.value?obj.value:"")+"' placeholder='"+(obj.descr?obj.descr:"")+"'>"+
                            //"        <div class='descr-xs'>"+obj+"</div>"+
                            "    </div>"+
                            "</div>";

                        output.push(paramBlk);
                });
                $('#'+ADD_TASK_DIALOG_ID + " form").append(output.join(''));
            }

            return schedule;
        };

        //------------------------------------------------------------------------
        //------------------------------------------------------------------------
        AddTask.prototype.getParams = function() {
            var ar = [];
            $.each($("#"+ADD_TASK_DIALOG_ID +" .task-param"), function(key,obj) {
                ar.push({
                    'name':$(obj).find("label").html(),
                    'value':$(obj).find("input").val(),
                    'type':"string"
                });
            });
            return ar;
        };


        //------------------------------------------------------------------------
        //
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
        //------------------------------------------------------------------------

        AddTask.prototype.save = function() {
            this.schedule.id = null;
            this.schedule = this.scheduleObj.getCron(this.schedule);
            var caller = this;

            this.schedule.params = this.getParams();

            //  Send Schedule object to backend
            Api.startTask(this.siteId,this.schedule,
                //  on Success
                function(response) {
                    try {
                        if ( response.object && (response.rc == 0)) {

                            logger.debug("[AddTask.save] Object Saved",response.object );

                            $('body').trigger('task:add', [response.object]);
                        }
                    }
                    catch (e) {
                        logger.debug("[AddTask.save] Error: ",e);
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
            //$("#"+ADD_TASK_DIALOG_ID).modal('hide');
            caller.close();

        };

        //------------------------------------------------------------------------
        //------------------------------------------------------------------------
        AddTask.prototype.close = function() {
            $("#"+ADD_TASK_DIALOG_ID).modal('hide');
            $('#'+TASK_SELECTOR_GRP_ID + " select").off('change');
            $('#'+SAVE_BTN_ID).off('click');
        };




        return AddTask;
    });